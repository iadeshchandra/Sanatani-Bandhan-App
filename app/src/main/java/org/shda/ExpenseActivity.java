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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExpenseActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout expensesContainer;
    private TextView tvTotalExpenses;
    private EditText inputSearch;
    
    private List<Expense> fullExpenseList = new ArrayList<>();
    private List<Expense> currentlyDisplayedList = new ArrayList<>();
    private List<String> autocompleteManagers = new ArrayList<>();
    
    // ✨ FIX: Event Autocomplete List!
    private List<String> autocompleteEvents = new ArrayList<>();
    private float totalSpent = 0f;

    private Long filterStartTs = null;
    private Long filterEndTs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        expensesContainer = findViewById(R.id.expensesContainer);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        inputSearch = findViewById(R.id.inputSearch);

        View btnAdd = findViewById(R.id.btnAddExpense);
        View btnExportMaster = findViewById(R.id.btnExportMaster);
        View btnFilterDates = findViewById(R.id.btnFilterDates);

        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAdd.setOnClickListener(v -> showAddExpenseDialog());
        } else {
            btnAdd.setVisibility(View.GONE);
        }

        if (btnFilterDates != null) {
            btnFilterDates.setOnClickListener(v -> showGlobalDateFilterDialog());
        }

        if (btnExportMaster != null) {
            btnExportMaster.setOnClickListener(v -> {
                if (currentlyDisplayedList.isEmpty()) { Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show(); return; }
                String title = filterStartTs != null ? "Filtered Expenses Ledger" : "All Time Expenses Ledger";
                try {
                    PdfReportService.generateExpenseReport(this, session.getCommunityName(), currentlyDisplayedList, totalSpent, title);
                } catch (Exception e) {
                    Toast.makeText(this, "Error generating Master PDF", Toast.LENGTH_SHORT).show();
                }
            });
        }

        setupSearch();
        loadManagersForAutocomplete();
        loadEventsForAutocomplete();
        loadExpenses();
    }

    private void loadManagersForAutocomplete() {
        db.child("communities").child(session.getCommunityId()).child("members").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteManagers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null && ("MANAGER".equals(m.role) || "ADMIN".equals(m.role))) autocompleteManagers.add(m.name + " (" + m.id + ")");
                }
                if (!autocompleteManagers.contains(session.getUserName())) autocompleteManagers.add(session.getUserName() + " (" + session.getUserId() + ")");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ✨ FIX: Silently fetches all Mandir Events in the background for autocomplete!
    private void loadEventsForAutocomplete() {
        db.child("communities").child(session.getCommunityId()).child("events").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("events").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteEvents.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String title = data.child("title").getValue(String.class);
                    if (title != null) autocompleteEvents.add(title);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadExpenses() {
        DatabaseReference expRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Expense");
        expRef.keepSynced(true);
        expRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullExpenseList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Expense e = data.getValue(Expense.class);
                    if (e != null) fullExpenseList.add(e);
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        currentlyDisplayedList.clear();
        totalSpent = 0f;
        String query = inputSearch != null ? inputSearch.getText().toString().toLowerCase().trim() : "";

        for (Expense e : fullExpenseList) {
            boolean matchesSearch = query.isEmpty() || (e.eventName != null && e.eventName.toLowerCase().contains(query)) || (e.itemName != null && e.itemName.toLowerCase().contains(query));
            boolean matchesDate = true;
            
            if (filterStartTs != null && filterEndTs != null) {
                matchesDate = (e.timestamp >= filterStartTs && e.timestamp <= filterEndTs);
            }
            
            if (matchesSearch && matchesDate) {
                currentlyDisplayedList.add(e);
                totalSpent += e.amount;
            }
        }
        
        tvTotalExpenses.setText("Total Filtered Spent: ৳" + totalSpent);
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
        builder.setTitle("Filter Expenses by Date");
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

    private void processAndRenderList(List<Expense> rawList) {
        HashMap<String, GroupedExpense> groupedMap = new HashMap<>();
        for (Expense e : rawList) {
            String key = e.eventName != null ? e.eventName.trim().toLowerCase() : "unknown";
            if (!groupedMap.containsKey(key)) groupedMap.put(key, new GroupedExpense(e.eventName != null ? e.eventName : "Unknown Event"));
            groupedMap.get(key).addExpense(e);
        }
        
        List<GroupedExpense> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (a, b) -> Float.compare(b.totalSpent, a.totalSpent));
        
        expensesContainer.removeAllViews();
        for (GroupedExpense ge : groupedList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_expense, expensesContainer, false);
                ((TextView) view.findViewById(R.id.tvExpenseEvent)).setText(ge.eventDisplayName);
                ((TextView) view.findViewById(R.id.tvExpenseAmount)).setText("Total: ৳" + ge.totalSpent);
                ((TextView) view.findViewById(R.id.tvExpenseDetails)).setText(ge.history.size() + " items logged in this range");
                
                view.setOnClickListener(v -> {
                    try { PdfReportService.generateUtsavStatement(this, session.getCommunityName(), ge); } catch (Exception ex) { Toast.makeText(this, "Error viewing details.", Toast.LENGTH_SHORT).show(); }
                });
                
                if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
                    view.setOnLongClickListener(v -> { 
                        try { showExpenseManagerDialog(ge); } catch (Exception ex) {}
                        return true; 
                    });
                }
                expensesContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showAddExpenseDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Log Community Expense");
            LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

            // ✨ FIX: AutoComplete wired directly to your Events List!
            final AutoCompleteTextView inputEvent = new AutoCompleteTextView(this);
            inputEvent.setHint("Type Event/Utsav Name...");
            ArrayAdapter<String> eventAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteEvents);
            inputEvent.setAdapter(eventAdapter); inputEvent.setThreshold(1);

            final EditText inputItem = new EditText(this); inputItem.setHint("Item Purchased / Service");
            final EditText inputAmt = new EditText(this); inputAmt.setHint("Cost (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            
            final AutoCompleteTextView inputHandler = new AutoCompleteTextView(this);
            inputHandler.setHint("Handled By");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteManagers);
            inputHandler.setAdapter(adapter); inputHandler.setThreshold(1); inputHandler.setText(session.getUserName() + " (" + session.getUserId() + ")");

            layout.addView(inputEvent); layout.addView(inputItem); layout.addView(inputAmt); layout.addView(inputHandler);
            builder.setView(layout);
            builder.setPositiveButton("RECORD", null); builder.setNegativeButton("CANCEL", null);

            AlertDialog dialog = builder.create(); dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String event = inputEvent.getText().toString().trim();
                String item = inputItem.getText().toString().trim();
                String amtStr = inputAmt.getText().toString().trim();
                String handler = inputHandler.getText().toString().trim();

                if (event.isEmpty() || item.isEmpty() || amtStr.isEmpty() || handler.isEmpty()) { Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show(); return; }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Saving...");
                
                float amt = Float.parseFloat(amtStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").push().getKey();
                
                HashMap<String, Object> expMap = new HashMap<>();
                expMap.put("id", transId); expMap.put("eventName", event); expMap.put("itemName", item); expMap.put("amount", amt);
                expMap.put("involvedPerson", handler); expMap.put("timestamp", System.currentTimeMillis()); expMap.put("loggedBy", session.getUserName());
                
                db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(transId).setValue(expMap);
                Toast.makeText(this, "Expense Logged!", Toast.LENGTH_SHORT).show(); dialog.dismiss();
            });
        } catch (Exception e) {
            Toast.makeText(this, "UI Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExpenseManagerDialog(GroupedExpense ge) {
        if (ge.history.isEmpty()) return;
        Expense latest = ge.history.get(ge.history.size() - 1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Expense").setMessage("Event: " + latest.eventName + "\nItem: " + latest.itemName + "\nCost: ৳" + latest.amount);

        builder.setPositiveButton("EDIT ITEM", (d, w) -> {
            AlertDialog.Builder editBuilder = new AlertDialog.Builder(this);
            final EditText inputItem = new EditText(this); inputItem.setText(latest.itemName);
            editBuilder.setView(inputItem).setPositiveButton("SAVE", (d2, w2) -> {
                String newItem = inputItem.getText().toString();
                db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(latest.id).child("itemName").setValue(newItem);
                logAudit("EXPENSE_EDITED", "Updated item for " + latest.eventName + " to: " + newItem);
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            }).show();
        });

        builder.setNegativeButton("DELETE", (d, w) -> {
            new AlertDialog.Builder(this).setTitle("Confirm Deletion").setMessage("Delete ৳" + latest.amount + " expense?")
                .setPositiveButton("YES", (d2, w2) -> {
                    db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(latest.id).removeValue();
                    logAudit("EXPENSE_DELETED", "Deleted ৳" + latest.amount + " expense for " + latest.eventName);
                    Toast.makeText(this, "Deleted & Totals Recalculated", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("CANCEL", null).show();
        });
        builder.show();
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }

    public static class Expense {
        public String id, eventName, itemName, involvedPerson, loggedBy;
        public float amount; public long timestamp;
        public Expense() {}
        public Expense(String id, String ev, String it, float a, String inv, long ts, String logBy) {
            this.id=id; this.eventName=ev; this.itemName=it; this.amount=a; this.involvedPerson=inv; this.timestamp=ts; this.loggedBy=logBy;
        }
    }

    public static class GroupedExpense {
        public String eventDisplayName; public float totalSpent = 0f; public List<Expense> history = new ArrayList<>();
        public GroupedExpense(String name) { this.eventDisplayName = name; }
        public void addExpense(Expense e) { this.history.add(e); this.totalSpent += e.amount; }
    }
}
