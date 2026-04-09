package org.shda;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout donationsContainer;
    private TextView tvTotalDonations;
    private EditText inputSearch;
    
    private List<SingleDonation> fullDonationList = new ArrayList<>();
    private List<SingleDonation> currentlyDisplayedList = new ArrayList<>();
    
    private List<String> autocompleteMembers = new ArrayList<>();
    private List<String> autocompleteManagers = new ArrayList<>();
    
    // ✨ FIX: Background map to match Phone Numbers to Donors instantly!
    private HashMap<String, String> phoneMap = new HashMap<>();
    
    private float totalDonated = 0f;
    private Long filterStartTs = null;
    private Long filterEndTs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        donationsContainer = findViewById(R.id.donationsContainer);
        tvTotalDonations = findViewById(R.id.tvTotalDonations);
        inputSearch = findViewById(R.id.inputSearch);

        View btnAdd = findViewById(R.id.btnAddDonation);
        View btnExportMaster = findViewById(R.id.btnExportMaster);
        View btnFilterDates = findViewById(R.id.btnFilterDates);

        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAdd.setOnClickListener(v -> showAddDonationDialog());
        } else { btnAdd.setVisibility(View.GONE); }

        if (btnFilterDates != null) { btnFilterDates.setOnClickListener(v -> showGlobalDateFilterDialog()); }

        if (btnExportMaster != null) {
            btnExportMaster.setOnClickListener(v -> {
                if (currentlyDisplayedList.isEmpty()) { Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show(); return; }
                String title = filterStartTs != null ? "Filtered Chanda Ledger" : "All Time Chanda Ledger";
                
                List<String> dates = new ArrayList<>(); List<String> names = new ArrayList<>();
                List<Float> amounts = new ArrayList<>(); List<String> notes = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                for(SingleDonation sd : currentlyDisplayedList) {
                    dates.add(sdf.format(new Date(sd.timestamp))); names.add(sd.name); amounts.add(sd.amount); notes.add(sd.note!=null?sd.note:"");
                }
                
                try { PdfReportService.generateFinancialReport(this, session.getCommunityName(), dates, names, amounts, notes, totalDonated, title);
                } catch (Exception e) { Toast.makeText(this, "Error generating Master PDF", Toast.LENGTH_SHORT).show(); }
            });
        }

        setupSearch();
        loadMembersAndManagers();
        loadDonations();
    }

    private void loadMembersAndManagers() {
        db.child("communities").child(session.getCommunityId()).child("members").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteMembers.clear(); autocompleteManagers.clear(); phoneMap.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) {
                        autocompleteMembers.add(m.name + " (" + m.id + ")");
                        if (m.phone != null) phoneMap.put(m.id, m.phone);
                        if ("MANAGER".equals(m.role) || "ADMIN".equals(m.role)) autocompleteManagers.add(m.name + " (" + m.id + ")");
                    }
                }
                if (!autocompleteManagers.contains(session.getUserName())) autocompleteManagers.add(session.getUserName() + " (" + session.getUserId() + ")");
                applyFilters(); // Re-render once phones load
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDonations() {
        DatabaseReference donRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation");
        donRef.keepSynced(true);
        donRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullDonationList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    SingleDonation d = data.getValue(SingleDonation.class);
                    if (d != null) fullDonationList.add(d);
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        currentlyDisplayedList.clear(); totalDonated = 0f;
        String query = inputSearch != null ? inputSearch.getText().toString().toLowerCase().trim() : "";

        for (SingleDonation d : fullDonationList) {
            boolean matchesSearch = query.isEmpty() || (d.name != null && d.name.toLowerCase().contains(query));
            boolean matchesDate = true;
            if (filterStartTs != null && filterEndTs != null) { matchesDate = (d.timestamp >= filterStartTs && d.timestamp <= filterEndTs); }
            
            if (matchesSearch && matchesDate) { currentlyDisplayedList.add(d); totalDonated += d.amount; }
        }
        
        tvTotalDonations.setText("Total Filtered: ৳" + totalDonated);
        processAndRenderList(currentlyDisplayedList);
    }

    private void setupSearch() {
        if (inputSearch == null) return;
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showGlobalDateFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Donations by Date");
        builder.setItems(new String[]{"Select Specific Date Range", "Clear Filters (Show All Time)"}, (dialog, which) -> {
            if (which == 0) {
                pickDateRange((startTs, endTs) -> {
                    filterStartTs = startTs; filterEndTs = endTs; applyFilters();
                    Toast.makeText(this, "Dates Filtered Successfully", Toast.LENGTH_SHORT).show();
                });
            } else { filterStartTs = null; filterEndTs = null; applyFilters(); }
        });
        builder.show();
    }

    private void pickDateRange(DateRangeCallback callback) {
        final Calendar startCal = Calendar.getInstance();
        new DatePickerDialog(this, (view1, y1, m1, d1) -> {
            startCal.set(y1, m1, d1, 0, 0, 0);
            final Calendar endCal = Calendar.getInstance();
            new DatePickerDialog(this, (view2, y2, m2, d2) -> {
                endCal.set(y2, m2, d2, 23, 59, 59);
                if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                else callback.onSelected(startCal.getTimeInMillis(), endCal.getTimeInMillis());
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            Toast.makeText(this, "Now select End Date", Toast.LENGTH_SHORT).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }
    private interface DateRangeCallback { void onSelected(long start, long end); }

    // ✨ FIX: Brilliantly groups donations, maps phone numbers dynamically, and finds the exact latest contribution!
    private void processAndRenderList(List<SingleDonation> rawList) {
        HashMap<String, GroupedDonation> groupedMap = new HashMap<>();
        for (SingleDonation d : rawList) {
            String key = d.name != null ? d.name.trim().toLowerCase() : "unknown";
            if (!groupedMap.containsKey(key)) {
                GroupedDonation gd = new GroupedDonation();
                gd.displayName = d.name != null ? d.name : "Unknown Donor";
                groupedMap.put(key, gd);
            }
            groupedMap.get(key).history.add(d);
            groupedMap.get(key).totalDonated += d.amount;
        }
        
        List<GroupedDonation> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (a, b) -> Float.compare(b.totalDonated, a.totalDonated));
        
        donationsContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (GroupedDonation gd : groupedList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_donation, donationsContainer, false);
                ((TextView) view.findViewById(R.id.tvDonorName)).setText(gd.displayName);
                ((TextView) view.findViewById(R.id.tvDonorTotal)).setText("Total: ৳" + gd.totalDonated);
                ((TextView) view.findViewById(R.id.tvDonorContributions)).setText(gd.history.size() + " Contributions in this range");
                
                // Fetch Phone via ID parsing
                String phoneStr = "Phone: N/A";
                if (gd.displayName.contains("(") && gd.displayName.contains(")")) {
                    String possibleId = gd.displayName.substring(gd.displayName.lastIndexOf("(") + 1, gd.displayName.lastIndexOf(")"));
                    if (phoneMap.containsKey(possibleId)) { phoneStr = "📞 " + phoneMap.get(possibleId); }
                }
                ((TextView) view.findViewById(R.id.tvDonorPhone)).setText(phoneStr);

                // Fetch absolute latest donation from history
                Collections.sort(gd.history, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                SingleDonation latest = gd.history.get(0);
                ((TextView) view.findViewById(R.id.tvDonorLastDonation)).setText("Last Donation: ৳" + latest.amount + " on " + sdf.format(new Date(latest.timestamp)));

                view.setOnClickListener(v -> {
                    try { PdfReportService.generateDonorStatement(this, session.getCommunityName(), gd); } catch (Exception ex) { Toast.makeText(this, "Error generating statement.", Toast.LENGTH_SHORT).show(); }
                });
                donationsContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showAddDonationDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Log New Chanda");
            LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

            final AutoCompleteTextView inputDonor = new AutoCompleteTextView(this);
            inputDonor.setHint("Select Devotee / Donor Name");
            ArrayAdapter<String> donorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteMembers);
            inputDonor.setAdapter(donorAdapter); inputDonor.setThreshold(1);

            final EditText inputAmt = new EditText(this); inputAmt.setHint("Amount (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            final EditText inputNote = new EditText(this); inputNote.setHint("Note / Purpose (Optional)");
            
            final AutoCompleteTextView inputHandler = new AutoCompleteTextView(this);
            inputHandler.setHint("Collected By");
            ArrayAdapter<String> handlerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteManagers);
            inputHandler.setAdapter(handlerAdapter); inputHandler.setThreshold(1); inputHandler.setText(session.getUserName() + " (" + session.getUserId() + ")");

            layout.addView(inputDonor); layout.addView(inputAmt); layout.addView(inputNote); layout.addView(inputHandler);
            builder.setView(layout);
            builder.setPositiveButton("RECORD", null); builder.setNegativeButton("CANCEL", null);

            AlertDialog dialog = builder.create(); dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String donor = inputDonor.getText().toString().trim();
                String amtStr = inputAmt.getText().toString().trim();
                String handler = inputHandler.getText().toString().trim();

                if (donor.isEmpty() || amtStr.isEmpty() || handler.isEmpty()) { Toast.makeText(this, "Fields missing", Toast.LENGTH_SHORT).show(); return; }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Saving...");
                
                float amt = Float.parseFloat(amtStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                
                SingleDonation sd = new SingleDonation();
                sd.id = transId; sd.name = donor; sd.amount = amt; sd.note = inputNote.getText().toString().trim();
                sd.collectedBy = handler; sd.timestamp = System.currentTimeMillis();
                
                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);
                Toast.makeText(this, "Chanda Logged!", Toast.LENGTH_SHORT).show(); dialog.dismiss();
            });
        } catch (Exception e) { Toast.makeText(this, "UI Error", Toast.LENGTH_SHORT).show(); }
    }

    public static class SingleDonation {
        public String id, name, note, collectedBy;
        public float amount; public long timestamp;
        public SingleDonation() {}
    }

    public static class GroupedDonation {
        public String displayName; public float totalDonated = 0f;
        public List<SingleDonation> history = new ArrayList<>();
        public GroupedDonation() {}
    }
}
