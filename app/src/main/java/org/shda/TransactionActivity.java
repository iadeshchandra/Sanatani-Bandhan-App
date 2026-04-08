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
    private LinearLayout transactionsContainer;
    private EditText inputSearch;
    private TextView tvTotalDonations;
    
    private List<SingleDonation> fullDonationList = new ArrayList<>();
    private List<SingleDonation> currentlyDisplayedList = new ArrayList<>();
    private List<Member> autocompleteMembers = new ArrayList<>();
    
    // ✨ NEW: Date Filter Variables
    private Long filterStartTs = null;
    private Long filterEndTs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        transactionsContainer = findViewById(R.id.transactionsContainer);
        inputSearch = findViewById(R.id.inputSearch);
        tvTotalDonations = findViewById(R.id.tvTotalDonations);

        View btnAdd = findViewById(R.id.btnAddTransaction);
        View btnExportMaster = findViewById(R.id.btnExportMaster);
        View btnFilterDates = findViewById(R.id.btnFilterDates); // ✨ NEW

        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAdd.setOnClickListener(v -> showAddDonationDialog());
        } else {
            btnAdd.setVisibility(View.GONE);
        }

        if (btnExportMaster != null) {
            btnExportMaster.setOnClickListener(v -> {
                if (currentlyDisplayedList.isEmpty()) { Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show(); return; }
                String title = filterStartTs != null ? "Filtered Chanda Ledger" : "All Time Chanda Ledger";
                
                List<String> dates = new ArrayList<>(); List<String> names = new ArrayList<>();
                List<Float> amounts = new ArrayList<>(); List<String> notes = new ArrayList<>();
                float totalExport = 0f;
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                for (SingleDonation sd : currentlyDisplayedList) {
                    dates.add(sdf.format(new Date(sd.timestamp)));
                    names.add(sd.name); amounts.add(sd.amount); notes.add(sd.note != null ? sd.note : "");
                    totalExport += sd.amount;
                }
                try { PdfReportService.generateFinancialReport(this, session.getCommunityName(), dates, names, amounts, notes, totalExport, title); } catch(Exception e){}
            });
        }

        if (btnFilterDates != null) btnFilterDates.setOnClickListener(v -> showGlobalDateFilterDialog());

        loadMembersForAutocomplete();
        loadDonations();
        setupSearch();
    }

    private void loadMembersForAutocomplete() {
        db.child("communities").child(session.getCommunityId()).child("members").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteMembers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) autocompleteMembers.add(m);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDonations() {
        DatabaseReference donationsRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation");
        donationsRef.keepSynced(true);
        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullDonationList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    SingleDonation sd = data.getValue(SingleDonation.class);
                    if (sd != null) fullDonationList.add(sd);
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ✨ NEW: Master Filter Engine
    private void applyFilters() {
        currentlyDisplayedList.clear();
        String query = inputSearch != null ? inputSearch.getText().toString().toLowerCase().trim() : "";
        float totalCollected = 0f;

        for (SingleDonation sd : fullDonationList) {
            boolean matchesSearch = query.isEmpty() || (sd.name != null && sd.name.toLowerCase().contains(query)) || (sd.note != null && sd.note.toLowerCase().contains(query));
            boolean matchesDate = true;
            
            if (filterStartTs != null && filterEndTs != null) {
                matchesDate = (sd.timestamp >= filterStartTs && sd.timestamp <= filterEndTs);
            }
            
            if (matchesSearch && matchesDate) {
                currentlyDisplayedList.add(sd);
                totalCollected += sd.amount;
            }
        }
        
        tvTotalDonations.setText("Total Filtered: ৳" + totalCollected);
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

    // ✨ NEW: Date Selection Dialog for Screen
    private void showGlobalDateFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Ledger by Date");
        builder.setItems(new String[]{"Select Specific Date Range", "Clear Filters (Show All Time)"}, (dialog, which) -> {
            if (which == 0) {
                pickDateRange((startTs, endTs) -> {
                    filterStartTs = startTs; filterEndTs = endTs; applyFilters();
                    Toast.makeText(this, "Dates Filtered Successfully", Toast.LENGTH_SHORT).show();
                });
            } else {
                filterStartTs = null; filterEndTs = null; applyFilters();
            }
        });
        builder.show();
    }

    // Standard Android double-date picker utility
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
        HashMap<String, GroupedDonation> groupedMap = new HashMap<>();
        for (SingleDonation sd : rawList) {
            String key = sd.name.trim().toLowerCase();
            if (!groupedMap.containsKey(key)) groupedMap.put(key, new GroupedDonation(sd.name));
            groupedMap.get(key).addDonation(sd);
        }
        
        List<GroupedDonation> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (a, b) -> Float.compare(b.totalDonated, a.totalDonated));
        
        transactionsContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault());

        for (GroupedDonation gd : groupedList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_transaction, transactionsContainer, false);
                ((TextView) view.findViewById(R.id.tvTransName)).setText(gd.displayName);
                ((TextView) view.findViewById(R.id.tvTransAmount)).setText("Total: ৳" + gd.totalDonated);
                ((TextView) view.findViewById(R.id.tvTransNote)).setText(gd.history.size() + " Contributions in this range");
                
                // ✨ FIX: Showing Last Donation Date/Time on Card!
                TextView tvLast = view.findViewById(R.id.tvTransLastDonation);
                if (gd.lastUpdated > 0) tvLast.setText("Last Donation: " + sdf.format(new Date(gd.lastUpdated)));
                else tvLast.setVisibility(View.GONE);

                // ✨ FIX: On Card Click -> Ask if they want All Time PDF or Specific Dates PDF!
                view.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                        .setTitle("Generate Donor Statement")
                        .setItems(new String[]{"Statement of Currently Filtered Dates", "Statement for Specific Dates", "All Time Statement"}, (d, w) -> {
                            try {
                                if (w == 0) {
                                    PdfReportService.generateDonorStatement(this, session.getCommunityName(), gd);
                                } else if (w == 1) {
                                    pickDateRange((startTs, endTs) -> {
                                        GroupedDonation customGd = new GroupedDonation(gd.displayName);
                                        for (SingleDonation sd : gd.history) { if (sd.timestamp >= startTs && sd.timestamp <= endTs) customGd.addDonation(sd); }
                                        if (customGd.history.isEmpty()) Toast.makeText(this, "No donations in this range", Toast.LENGTH_SHORT).show();
                                        else PdfReportService.generateDonorStatement(this, session.getCommunityName(), customGd);
                                    });
                                } else {
                                    // Pull global un-filtered history just for this user
                                    GroupedDonation globalGd = new GroupedDonation(gd.displayName);
                                    for(SingleDonation sd : fullDonationList) { if(sd.name.equalsIgnoreCase(gd.displayName)) globalGd.addDonation(sd); }
                                    PdfReportService.generateDonorStatement(this, session.getCommunityName(), globalGd);
                                }
                            } catch(Exception e) {}
                        }).show();
                });
                
                if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
                    view.setOnLongClickListener(v -> { showTransactionManagerDialog(gd); return true; });
                }
                transactionsContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showTransactionManagerDialog(GroupedDonation gd) {
        if (gd.history.isEmpty()) return;
        SingleDonation latestTrans = gd.history.get(gd.history.size() - 1);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Transaction").setMessage("Donor: " + latestTrans.name + "\nAmount: ৳" + latestTrans.amount + "\nNote: " + latestTrans.note);

        builder.setPositiveButton("EDIT NOTE", (dialog, which) -> {
            AlertDialog.Builder editBuilder = new AlertDialog.Builder(this);
            final EditText inputNote = new EditText(this); inputNote.setText(latestTrans.note);
            editBuilder.setView(inputNote).setPositiveButton("SAVE", (d, w) -> {
                String newNote = inputNote.getText().toString();
                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(latestTrans.id).child("note").setValue(newNote);
                logAudit("DONATION_EDITED", "Updated note for " + latestTrans.name + " to: " + newNote);
                Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
            }).show();
        });

        builder.setNegativeButton("DELETE & ROLLBACK", (dialog, which) -> {
            new AlertDialog.Builder(this).setTitle("Confirm Rollback").setMessage("Delete ৳" + latestTrans.amount + " from total?")
                .setPositiveButton("YES, DELETE", (d, w) -> {
                    db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(latestTrans.id).removeValue();
                    if (latestTrans.name.contains("[Member]")) {
                        String cleanName = latestTrans.name.replace("[Member]", "").trim();
                        for (Member m : autocompleteMembers) {
                            if (m.name.equalsIgnoreCase(cleanName) || m.name.equalsIgnoreCase(cleanName + " ("+m.id+")")) {
                                db.child("communities").child(session.getCommunityId()).child("members").child(m.id).child("totalDonated").setValue(ServerValue.increment(-latestTrans.amount));
                                break;
                            }
                        }
                    }
                    logAudit("DONATION_DELETED", "Rolled back ৳" + latestTrans.amount + " from " + latestTrans.name);
                    Toast.makeText(this, "Transaction Deleted & Totals Recalculated", Toast.LENGTH_LONG).show();
                }).setNegativeButton("CANCEL", null).show();
        });
        builder.show();
    }

    private void showAddDonationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record New Chanda");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final AutoCompleteTextView inputName = new AutoCompleteTextView(this);
        inputName.setHint("Donor Name / Member ID");
        List<String> suggestions = new ArrayList<>();
        for (Member m : autocompleteMembers) { suggestions.add(m.name + " ("+m.id+")"); }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
        inputName.setAdapter(adapter); inputName.setThreshold(1);

        final EditText inputAmt = new EditText(this); inputAmt.setHint("Amount (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText inputNote = new EditText(this); inputNote.setHint("Purpose / Gotra / Note");
        final EditText inputCollector = new EditText(this); inputCollector.setHint("Collected By (Your Name)"); inputCollector.setText(session.getUserName() + " (" + session.getUserId() + ")");

        layout.addView(inputName); layout.addView(inputAmt); layout.addView(inputNote); layout.addView(inputCollector);
        builder.setView(layout);
        builder.setPositiveButton("RECORD", null); builder.setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create(); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nameRaw = inputName.getText().toString().trim();
            String amtStr = inputAmt.getText().toString().trim();
            String note = inputNote.getText().toString().trim();
            String collector = inputCollector.getText().toString().trim();

            if (nameRaw.isEmpty() || amtStr.isEmpty() || collector.isEmpty()) { Toast.makeText(this, "Name, Amount, and Collector required", Toast.LENGTH_SHORT).show(); return; }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Saving...");
            
            float amt = Float.parseFloat(amtStr);
            String finalName = nameRaw + " [Guest]";
            String matchedId = null;

            for (Member m : autocompleteMembers) {
                if (nameRaw.contains(m.name) || nameRaw.contains(m.id)) {
                    finalName = m.name + " [Member]"; matchedId = m.id; break;
                }
            }

            long ts = System.currentTimeMillis();
            String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
            
            HashMap<String, Object> chandaMap = new HashMap<>();
            chandaMap.put("id", transId); chandaMap.put("name", finalName); chandaMap.put("amount", amt); chandaMap.put("note", note); chandaMap.put("collectedBy", collector); chandaMap.put("timestamp", ts); chandaMap.put("role", session.getRole());
            
            db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(chandaMap);

            if (matchedId != null) {
                db.child("communities").child(session.getCommunityId()).child("members").child(matchedId).child("totalDonated").setValue(ServerValue.increment(amt));
                db.child("communities").child(session.getCommunityId()).child("members").child(matchedId).child("lastDonationTimestamp").setValue(ts);
            }

            Toast.makeText(this, "Chanda Recorded Locally!", Toast.LENGTH_SHORT).show(); dialog.dismiss();
        });
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }

    public static class SingleDonation {
        public String id, name, note, method, phone, collectedBy, role; public float amount; public long timestamp;
        public SingleDonation() {}
        public SingleDonation(String id, String name, float amt, String note, String method, String phone, String collectedBy, long ts, String role) {
            this.id = id; this.name = name; this.amount = amt; this.note = note; this.method = method; this.phone = phone; this.collectedBy = collectedBy; this.timestamp = ts; this.role = role;
        }
    }

    public static class GroupedDonation {
        public String displayName; public float totalDonated = 0f; public long lastUpdated = 0L; public List<SingleDonation> history = new ArrayList<>();
        public GroupedDonation(String name) { this.displayName = name; }
        public void addDonation(SingleDonation d) { 
            this.history.add(d); this.totalDonated += d.amount; 
            if(d.timestamp > this.lastUpdated) this.lastUpdated = d.timestamp;
        }
    }
}
