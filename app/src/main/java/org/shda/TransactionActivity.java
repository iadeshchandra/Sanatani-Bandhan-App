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
import android.widget.ScrollView;
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
import java.util.Random;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout donationsContainer;
    private TextView tvTotalDonations;
    private EditText inputSearch;

    private List<SingleDonation> fullDonationList = new ArrayList<>();
    private List<SingleDonation> currentlyDisplayedList = new ArrayList<>();

    // ✨ NEW: Segregated lists for clean Auto-Suggestions
    private List<String> autocompleteOnlyMembers = new ArrayList<>();
    private List<String> autocompleteOnlyGuests = new ArrayList<>();
    private List<String> autocompleteManagers = new ArrayList<>();
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
            if (btnAdd != null) {
                btnAdd.setVisibility(View.VISIBLE);
                btnAdd.setOnClickListener(v -> showAddDonationDialog());
            }
        } else { if (btnAdd != null) btnAdd.setVisibility(View.GONE); }

        if (btnFilterDates != null) { btnFilterDates.setOnClickListener(v -> showGlobalDateFilterDialog()); }

        if (btnExportMaster != null) {
            btnExportMaster.setOnClickListener(v -> {
                if (fullDonationList.isEmpty()) { Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show(); return; }
                
                new AlertDialog.Builder(this)
                    .setTitle("Generate Master Report")
                    .setItems(new String[]{"Specific Date Range", "All Time"}, (dialog, which) -> {
                        if (which == 0) {
                            pickDateRange((startTs, endTs) -> exportMasterPdf(startTs, endTs));
                        } else {
                            exportMasterPdf(0, Long.MAX_VALUE);
                        }
                    }).show();
            });
        }

        setupSearch();
        loadMembersAndManagers();
        loadDonations();
    }

    private void exportMasterPdf(long startTs, long endTs) {
        List<String> dates = new ArrayList<>(); List<String> names = new ArrayList<>();
        List<Float> amounts = new ArrayList<>(); List<String> notes = new ArrayList<>();
        float exportTotal = 0f;
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        
        for(SingleDonation sd : fullDonationList) {
            if (sd.timestamp >= startTs && sd.timestamp <= endTs) {
                dates.add(sdf.format(new Date(sd.timestamp))); 
                names.add(sd.name); 
                amounts.add(sd.amount); 
                notes.add(sd.note != null ? sd.note : "");
                exportTotal += sd.amount;
            }
        }
        
        if (dates.isEmpty()) {
            Toast.makeText(this, "No donations found in this range.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = (startTs > 0) ? "Filtered Donation Ledger" : "All Time Donation Ledger";
        try { 
            PdfReportService.generateFinancialReport(this, session.getCommunityName(), dates, names, amounts, notes, exportTotal, title);
            Toast.makeText(this, "Master PDF Generated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { 
            Toast.makeText(this, "Error generating Master PDF", Toast.LENGTH_SHORT).show(); 
        }
    }

    private void loadMembersAndManagers() {
        autocompleteOnlyMembers.clear(); autocompleteManagers.clear(); phoneMap.clear();
        
        db.child("communities").child(session.getCommunityId()).child("members").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) {
                        autocompleteOnlyMembers.add(m.name + " (" + m.id + ")");
                        if (m.phone != null && !m.phone.isEmpty()) phoneMap.put(m.id, m.phone);
                        if ("MANAGER".equals(m.role) || "ADMIN".equals(m.role)) autocompleteManagers.add(m.name + " (" + m.id + ")");
                    }
                }
                if (!autocompleteManagers.contains(session.getUserName())) autocompleteManagers.add(session.getUserName() + " (" + session.getUserId() + ")");
                loadGuestDonors(); 
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadGuestDonors() {
        autocompleteOnlyGuests.clear();
        db.child("communities").child(session.getCommunityId()).child("guests").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("guests").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Guest g = data.getValue(Guest.class);
                    if (g != null) {
                        autocompleteOnlyGuests.add(g.name + " (" + g.id + ")");
                        if (g.phone != null && !g.phone.isEmpty()) phoneMap.put(g.id, g.phone);
                    }
                }
                applyFilters(); 
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

        if (tvTotalDonations != null) tvTotalDonations.setText("Total Filtered: ৳" + totalDonated);
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

    private void processAndRenderList(List<SingleDonation> rawList) {
        if (donationsContainer == null) return;
        HashMap<String, GroupedDonation> groupedMap = new HashMap<>();
        for (SingleDonation d : rawList) {
            String key = d.name != null ? d.name.trim().toLowerCase() : "unknown";
            if (!groupedMap.containsKey(key)) {
                groupedMap.put(key, new GroupedDonation(d.name != null ? d.name : "Unknown Donor"));
            }
            groupedMap.get(key).addDonation(d);
        }

        List<GroupedDonation> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (a, b) -> Float.compare(b.totalDonated, a.totalDonated));

        donationsContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (GroupedDonation gd : groupedList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_transaction, donationsContainer, false);
                ((TextView) view.findViewById(R.id.tvDonorName)).setText(gd.displayName);
                ((TextView) view.findViewById(R.id.tvDonorTotal)).setText("Total: ৳" + gd.totalDonated);
                ((TextView) view.findViewById(R.id.tvDonorContributions)).setText(gd.history.size() + " Contributions in this range");

                String phoneStr = "Phone: N/A";
                if (gd.displayName.contains("(") && gd.displayName.contains(")")) {
                    String possibleId = gd.displayName.substring(gd.displayName.lastIndexOf("(") + 1, gd.displayName.lastIndexOf(")"));
                    if (phoneMap.containsKey(possibleId)) { phoneStr = "📞 " + phoneMap.get(possibleId); }
                }
                ((TextView) view.findViewById(R.id.tvDonorPhone)).setText(phoneStr);

                Collections.sort(gd.history, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                SingleDonation latest = gd.history.get(0);
                ((TextView) view.findViewById(R.id.tvDonorLastDonation)).setText("Last Donation: ৳" + latest.amount + " on " + sdf.format(new Date(latest.timestamp)));

                view.setOnClickListener(v -> {
                    try { PdfReportService.generateDonorStatement(this, session.getCommunityName(), gd); } catch (Exception ex) { Toast.makeText(this, "Error generating statement.", Toast.LENGTH_SHORT).show(); }
                });
                donationsContainer.addView(view);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ✨ NEW: Completely Rewritten Dual-Tab Dialog!
    private void showAddDonationDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Log New Donation");

            LinearLayout mainLayout = new LinearLayout(this);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(40, 20, 40, 0);

            // Toggle Tab Buttons
            LinearLayout toggleLayout = new LinearLayout(this);
            toggleLayout.setOrientation(LinearLayout.HORIZONTAL);
            toggleLayout.setWeightSum(2);
            toggleLayout.setPadding(0, 0, 0, 20);
            
            Button btnTabMember = new Button(this);
            btnTabMember.setText("MEMBER");
            btnTabMember.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            Button btnTabGuest = new Button(this);
            btnTabGuest.setText("GUEST / NEW");
            btnTabGuest.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            toggleLayout.addView(btnTabMember);
            toggleLayout.addView(btnTabGuest);
            mainLayout.addView(toggleLayout);

            // ================== MEMBER VIEW ==================
            LinearLayout memberLayout = new LinearLayout(this);
            memberLayout.setOrientation(LinearLayout.VERTICAL);
            
            final AutoCompleteTextView inputMemberName = new AutoCompleteTextView(this);
            inputMemberName.setHint("Select Official Member");
            inputMemberName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteOnlyMembers));
            inputMemberName.setThreshold(1);
            
            final EditText inputMemberAmt = new EditText(this); 
            inputMemberAmt.setHint("Amount (৳)"); 
            inputMemberAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            
            final EditText inputMemberNote = new EditText(this); 
            inputMemberNote.setHint("Note / Purpose (Optional)");
            
            final AutoCompleteTextView inputMemberHandler = new AutoCompleteTextView(this);
            inputMemberHandler.setHint("Collected By");
            inputMemberHandler.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteManagers));
            inputMemberHandler.setThreshold(1); 
            inputMemberHandler.setText(session.getUserName() + " (" + session.getUserId() + ")");

            memberLayout.addView(inputMemberName); 
            memberLayout.addView(inputMemberAmt); 
            memberLayout.addView(inputMemberNote); 
            memberLayout.addView(inputMemberHandler);

            // ================== GUEST VIEW (ScrollView) ==================
            ScrollView guestScroll = new ScrollView(this);
            LinearLayout guestLayout = new LinearLayout(this);
            guestLayout.setOrientation(LinearLayout.VERTICAL);
            guestScroll.addView(guestLayout);

            final AutoCompleteTextView inputGuestName = new AutoCompleteTextView(this);
            inputGuestName.setHint("Select or Type Guest Name");
            inputGuestName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteOnlyGuests));
            inputGuestName.setThreshold(1);
            
            final EditText inputGuestPhone = new EditText(this); inputGuestPhone.setHint("Phone Number"); inputGuestPhone.setInputType(InputType.TYPE_CLASS_PHONE);
            final EditText inputGuestAddress = new EditText(this); inputGuestAddress.setHint("Address");
            final EditText inputGuestEmail = new EditText(this); inputGuestEmail.setHint("Email Address"); inputGuestEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            final EditText inputGuestBloodGroup = new EditText(this); inputGuestBloodGroup.setHint("Blood Group (Optional)");
            final EditText inputGuestFather = new EditText(this); inputGuestFather.setHint("Father's Name (Optional)");
            final EditText inputGuestMother = new EditText(this); inputGuestMother.setHint("Mother's Name (Optional)");
            
            final EditText inputGuestAmt = new EditText(this); inputGuestAmt.setHint("Amount (৳)"); inputGuestAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            final EditText inputGuestNote = new EditText(this); inputGuestNote.setHint("Note / Purpose");
            final EditText inputGuestComment = new EditText(this); inputGuestComment.setHint("Admin Comment (Optional)");
            
            final AutoCompleteTextView inputGuestHandler = new AutoCompleteTextView(this);
            inputGuestHandler.setHint("Collected By");
            inputGuestHandler.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteManagers));
            inputGuestHandler.setText(session.getUserName() + " (" + session.getUserId() + ")");

            guestLayout.addView(inputGuestName); guestLayout.addView(inputGuestPhone); guestLayout.addView(inputGuestAddress);
            guestLayout.addView(inputGuestEmail); guestLayout.addView(inputGuestBloodGroup); guestLayout.addView(inputGuestFather);
            guestLayout.addView(inputGuestMother); guestLayout.addView(inputGuestAmt); guestLayout.addView(inputGuestNote);
            guestLayout.addView(inputGuestComment); guestLayout.addView(inputGuestHandler);

            mainLayout.addView(memberLayout);
            mainLayout.addView(guestScroll);

            // Logic to switch tabs dynamically
            final boolean[] isGuestMode = {false};
            Runnable updateUI = () -> {
                if (isGuestMode[0]) {
                    btnTabGuest.setBackgroundColor(0xFFE65100); btnTabGuest.setTextColor(0xFFFFFFFF);
                    btnTabMember.setBackgroundColor(0xFFE0E0E0); btnTabMember.setTextColor(0xFF000000);
                    memberLayout.setVisibility(View.GONE); guestScroll.setVisibility(View.VISIBLE);
                } else {
                    btnTabMember.setBackgroundColor(0xFF1976D2); btnTabMember.setTextColor(0xFFFFFFFF);
                    btnTabGuest.setBackgroundColor(0xFFE0E0E0); btnTabGuest.setTextColor(0xFF000000);
                    memberLayout.setVisibility(View.VISIBLE); guestScroll.setVisibility(View.GONE);
                }
            };
            btnTabMember.setOnClickListener(v -> { isGuestMode[0] = false; updateUI.run(); });
            btnTabGuest.setOnClickListener(v -> { isGuestMode[0] = true; updateUI.run(); });
            updateUI.run(); // Init

            builder.setView(mainLayout);
            builder.setPositiveButton("RECORD", null); 
            builder.setNegativeButton("CANCEL", null);

            AlertDialog dialog = builder.create(); 
            dialog.show();

            // Override click to prevent dialog closing if validation fails
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (isGuestMode[0]) {
                    String name = inputGuestName.getText().toString().trim();
                    String amtStr = inputGuestAmt.getText().toString().trim();
                    String handler = inputGuestHandler.getText().toString().trim();

                    if (name.isEmpty() || amtStr.isEmpty() || handler.isEmpty()) { 
                        Toast.makeText(this, "Guest Name, Amount, and Handler are required", Toast.LENGTH_SHORT).show(); return; 
                    }

                    float amt = Float.parseFloat(amtStr);
                    String note = inputGuestNote.getText().toString().trim();

                    if (name.contains("(") && name.contains(")")) {
                        // Existing guest from suggestion dropdown
                        logDonationToDatabase(name + " [Guest]", amt, note, handler);
                    } else {
                        // Brand New Guest! Register them fully.
                        String guestId = "GST-" + (1000 + new Random().nextInt(9000));
                        Guest newGuest = new Guest();
                        newGuest.id = guestId;
                        newGuest.name = name;
                        newGuest.phone = inputGuestPhone.getText().toString().trim();
                        newGuest.address = inputGuestAddress.getText().toString().trim();
                        newGuest.email = inputGuestEmail.getText().toString().trim();
                        newGuest.bloodGroup = inputGuestBloodGroup.getText().toString().trim();
                        newGuest.fatherName = inputGuestFather.getText().toString().trim();
                        newGuest.motherName = inputGuestMother.getText().toString().trim();
                        newGuest.adminComment = inputGuestComment.getText().toString().trim();
                        newGuest.timestamp = System.currentTimeMillis();

                        db.child("communities").child(session.getCommunityId()).child("guests").child(guestId).setValue(newGuest);
                        logDonationToDatabase(name + " (" + guestId + ") [Guest]", amt, note, handler);
                    }
                    dialog.dismiss();
                } else {
                    String name = inputMemberName.getText().toString().trim();
                    String amtStr = inputMemberAmt.getText().toString().trim();
                    String handler = inputMemberHandler.getText().toString().trim();

                    if (name.isEmpty() || amtStr.isEmpty() || handler.isEmpty()) { 
                        Toast.makeText(this, "Member Name, Amount, and Handler are required", Toast.LENGTH_SHORT).show(); return; 
                    }

                    float amt = Float.parseFloat(amtStr);
                    String note = inputMemberNote.getText().toString().trim();

                    if (!name.contains("[")) name = name + " [Member]";
                    logDonationToDatabase(name, amt, note, handler);
                    dialog.dismiss();
                }
            });
        } catch (Exception e) { Toast.makeText(this, "UI Error", Toast.LENGTH_SHORT).show(); }
    }

    private void logDonationToDatabase(String formattedDonorName, float amt, String note, String handler) {
        String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
        SingleDonation sd = new SingleDonation(transId, formattedDonorName, amt, note, "", "", handler, System.currentTimeMillis(), session.getRole());
        db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);
        Toast.makeText(this, "Donation Logged!", Toast.LENGTH_SHORT).show(); 
    }

    // ✨ EXPANDED: Now holds all the detailed Guest info!
    public static class Guest {
        public String id, name, phone, email, address, bloodGroup, fatherName, motherName, adminComment;
        public long timestamp;
        public Guest() {}
    }

    public static class SingleDonation {
        public String id, name, note, collectedBy, mndId, orgId, role;
        public float amount; 
        public long timestamp;
        
        public SingleDonation() {} 

        public SingleDonation(String id, String name, float amount, String note, String mndId, String orgId, String collectedBy, long timestamp, String role) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.note = note;
            this.mndId = mndId;
            this.orgId = orgId;
            this.collectedBy = collectedBy;
            this.timestamp = timestamp;
            this.role = role;
        }
    }

    public static class GroupedDonation {
        public String displayName; 
        public float totalDonated = 0f;
        public long lastUpdated;
        public List<SingleDonation> history = new ArrayList<>();
        
        public GroupedDonation() {} 

        public GroupedDonation(String displayName) {
            this.displayName = displayName;
        }

        public void addDonation(SingleDonation sd) {
            this.history.add(sd);
            this.totalDonated += sd.amount;
            if (sd.timestamp > this.lastUpdated) {
                this.lastUpdated = sd.timestamp;
            }
        }
    }
}
