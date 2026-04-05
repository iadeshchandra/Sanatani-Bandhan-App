package org.shda;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
    private LinearLayout transactionsContainer;
    private TextView tvTotalDonations;
    private EditText inputSearch;
    private Spinner spinnerSort;

    private float totalDonationsValue = 0f;
    private List<String> memberSearchList = new ArrayList<>(); 
    private List<String> guestSearchList = new ArrayList<>();

    private HashMap<String, GroupedDonation> groupedMap = new HashMap<>();
    private List<GroupedDonation> displayList = new ArrayList<>();
    private String currentSort = "Recent First";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        transactionsContainer = findViewById(R.id.transactionsContainer);
        tvTotalDonations = findViewById(R.id.tvTotalDonations);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerSort = findViewById(R.id.spinnerSort);

        if (session.getCommunityId() == null) { finish(); return; }

        setupRBAC();
        setupSearchAndSort();
        loadTransactions();
        fetchMembersForAutoComplete();
    }

    private void setupRBAC() {
        View btnAddTransaction = findViewById(R.id.btnAddTransaction); 
        if (btnAddTransaction != null) {
            if ("MEMBER".equals(session.getRole())) {
                btnAddTransaction.setVisibility(View.GONE);
            } else {
                btnAddTransaction.setVisibility(View.VISIBLE);
                btnAddTransaction.setOnClickListener(v -> showDynamicChandaDialog());
            }
        }
        findViewById(R.id.btnGenerateReport).setOnClickListener(v -> triggerMasterReportGeneration());
    }

    private void setupSearchAndSort() {
        String[] sortOptions = {"Recent First", "Oldest First", "Name (A-Z)"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortOptions);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position];
                applyFilterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilterAndSort(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchMembersForAutoComplete() {
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberSearchList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) memberSearchList.add(m.name + " (" + m.id + ")");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String extractIdFromName(String rawName) {
        if (rawName != null && rawName.contains("(") && rawName.contains(")")) {
            return rawName.substring(rawName.lastIndexOf("(") + 1, rawName.lastIndexOf(")")).trim();
        }
        return null;
    }

    private void loadTransactions() {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
          .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupedMap.clear(); guestSearchList.clear(); totalDonationsValue = 0f;

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        String name = data.child("name").getValue(String.class);
                        Float amount = data.child("amount").getValue(Float.class);
                        String note = data.child("note").getValue(String.class);
                        Long timestamp = data.child("timestamp").getValue(Long.class);
                        String loggedBy = data.child("loggedBy").getValue(String.class);

                        if (name != null && amount != null && timestamp != null) {
                            totalDonationsValue += amount;
                            
                            // 🧠 ROBUST GROUPING LOGIC: Groups by SB-ID instead of string name
                            String groupKey = extractIdFromName(name);
                            if (groupKey == null) groupKey = name.toLowerCase().trim(); // Fallback for guests

                            if (!groupedMap.containsKey(groupKey)) {
                                GroupedDonation gd = new GroupedDonation();
                                gd.displayName = name; 
                                groupedMap.put(groupKey, gd);
                                if (name.contains("[Guest]")) guestSearchList.add(name.replace("[Guest]", "").trim());
                            }

                            GroupedDonation gd = groupedMap.get(groupKey);
                            gd.totalDonated += amount;
                            if (timestamp > gd.lastDonationTime) { gd.lastDonationTime = timestamp; }
                            
                            SingleDonation sd = new SingleDonation();
                            sd.amount = amount; sd.timestamp = timestamp; sd.note = note; sd.loggedBy = loggedBy;
                            gd.history.add(sd);
                        }
                    } catch (Exception e) {}
                }
                tvTotalDonations.setText("Total Collected: ৳" + totalDonationsValue);
                applyFilterAndSort();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilterAndSort() {
        displayList.clear();
        String query = inputSearch.getText().toString().toLowerCase().trim();

        for (GroupedDonation gd : groupedMap.values()) {
            if (gd.displayName.toLowerCase().contains(query)) { displayList.add(gd); }
        }

        Collections.sort(displayList, (a, b) -> {
            if (currentSort.equals("Recent First")) return Long.compare(b.lastDonationTime, a.lastDonationTime);
            if (currentSort.equals("Oldest First")) return Long.compare(a.lastDonationTime, b.lastDonationTime);
            return a.displayName.compareToIgnoreCase(b.displayName); // A-Z
        });

        renderCards();
    }

    private void renderCards() {
        transactionsContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (GroupedDonation gd : displayList) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_transaction, transactionsContainer, false);
            
            ((TextView) view.findViewById(R.id.tvTransName)).setText(gd.displayName);
            ((TextView) view.findViewById(R.id.tvTransAmount)).setText("Total: ৳" + gd.totalDonated);
            ((TextView) view.findViewById(R.id.tvTransDate)).setText("Last Chanda: " + sdf.format(new Date(gd.lastDonationTime)));
            ((TextView) view.findViewById(R.id.tvTransNote)).setText("Total Entries: " + gd.history.size() + "\n👉 Tap card to download Ledger PDF");

            view.setOnClickListener(v -> {
                PdfReportService.generateDonorStatement(this, session.getCommunityName(), gd);
                Toast.makeText(this, "Generating Statement PDF...", Toast.LENGTH_SHORT).show();
            });

            transactionsContainer.addView(view);
        }
    }

    private void showDynamicChandaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Smart Chanda");

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        RadioGroup rgType = new RadioGroup(this); rgType.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.RadioButton rbMember = new android.widget.RadioButton(this); rbMember.setId(View.generateViewId()); rbMember.setText("Member");
        android.widget.RadioButton rbGuest = new android.widget.RadioButton(this); rbGuest.setId(View.generateViewId()); rbGuest.setText("Guest");
        rgType.addView(rbMember); rgType.addView(rbGuest); rbMember.setChecked(true);
        layout.addView(rgType);

        final AutoCompleteTextView inputName = new AutoCompleteTextView(this); inputName.setHint("Donor Name / SB-ID"); 
        inputName.setThreshold(1); layout.addView(inputName);
        
        final EditText inputAmount = new EditText(this); inputAmount.setHint("Amount (৳) *"); inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(inputAmount);
        final EditText inputNote = new EditText(this); inputNote.setHint("Purpose / Gotra / Note *"); layout.addView(inputNote);

        inputName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, memberSearchList));
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbMember.getId()) {
                inputName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, memberSearchList));
            } else {
                inputName.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, guestSearchList));
            }
        });

        builder.setView(layout);
        builder.setPositiveButton("SAVE & RECEIPT", (dialog, which) -> {
            String rawName = inputName.getText().toString().trim();
            String amountStr = inputAmount.getText().toString().trim();
            String note = inputNote.getText().toString().trim();
            boolean isMember = rbMember.isChecked();

            if (rawName.isEmpty() || amountStr.isEmpty() || note.isEmpty()) { Toast.makeText(this, "Name, Amount, and Note required", Toast.LENGTH_SHORT).show(); return; }

            try {
                float amount = Float.parseFloat(amountStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                String finalNameToSave = (isMember ? "[Member] " : "[Guest] ") + rawName;
                String strictSignature = "ADMIN".equals(session.getRole()) ? "Super Admin - " + session.getUserName() : "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";

                HashMap<String, Object> transMap = new HashMap<>();
                transMap.put("name", finalNameToSave);
                transMap.put("amount", amount);
                transMap.put("note", note);
                transMap.put("timestamp", System.currentTimeMillis());
                transMap.put("loggedBy", strictSignature);

                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(transMap);

                AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "CHANDA_RECORDED", "Recorded ৳" + amount + " from " + rawName);
                Toast.makeText(this, "Chanda Recorded. Generating PDF...", Toast.LENGTH_SHORT).show();
                
                String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                PdfReportService.generateDonorReceipt(this, session.getCommunityName(), finalNameToSave, amount, note + "\n✍️ Logged by: " + strictSignature, dateStr);
                
            } catch (Exception e) {}
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }

    private void triggerMasterReportGeneration() {
        Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0); 
            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                long startTimestamp = startCal.getTimeInMillis(); long endTimestamp = endCal.getTimeInMillis();
                String reportRange = "Period: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(startCal.getTime()) + " to " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(endCal.getTime());

                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
                  .orderByChild("timestamp").startAt(startTimestamp).endAt(endTimestamp)
                  .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> names = new ArrayList<>(); List<Float> amounts = new ArrayList<>();
                        List<String> notes = new ArrayList<>(); List<String> dates = new ArrayList<>();
                        float total = 0f; SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String name = data.child("name").getValue(String.class); Float amt = data.child("amount").getValue(Float.class);
                            String note = data.child("note").getValue(String.class); Long ts = data.child("timestamp").getValue(Long.class);
                            if(name != null && amt != null) {
                                names.add(name); amounts.add(amt); notes.add(note != null ? note : "No note");
                                dates.add(ts != null ? sdf.format(new Date(ts)) : "Unknown Date"); total += amt;
                            }
                        }
                        PdfReportService.generateFinancialReport(TransactionActivity.this, session.getCommunityName(), dates, names, amounts, notes, total, reportRange);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    public static class GroupedDonation {
        public String displayName;
        public float totalDonated = 0f;
        public long lastDonationTime = 0L;
        public List<SingleDonation> history = new ArrayList<>();
    }
    public static class SingleDonation {
        public float amount;
        public long timestamp;
        public String note, loggedBy;
    }
}
