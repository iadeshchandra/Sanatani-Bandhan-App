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

public class ExpenseActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout expensesContainer;
    private TextView tvTotalExpense;
    private EditText inputSearch;
    private Spinner spinnerSort;

    private float totalExpenseValue = 0f;
    private boolean isAdminOrManager;

    // Auto-complete lists
    private List<String> memberSearchList = new ArrayList<>();
    private List<String> eventSearchList = new ArrayList<>();

    // CRM Grouping Data Models
    private HashMap<String, GroupedExpense> groupedMap = new HashMap<>();
    private List<GroupedExpense> displayList = new ArrayList<>();
    private String currentSort = "Recent First";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        expensesContainer = findViewById(R.id.expensesContainer);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerSort = findViewById(R.id.spinnerSort);

        if (session.getCommunityId() == null) { finish(); return; }

        isAdminOrManager = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        setupRBAC();
        setupSearchAndSort();
        fetchAutoCompleteData();
        loadExpenses();
    }

    private void setupRBAC() {
        View btnAddExpense = findViewById(R.id.btnAddExpense);
        if (btnAddExpense != null) {
            if (!isAdminOrManager) {
                btnAddExpense.setVisibility(View.GONE);
            } else {
                btnAddExpense.setOnClickListener(v -> showExpenseDialog(null));
            }
        }
        
        findViewById(R.id.btnGenerateReport).setOnClickListener(v -> triggerMasterReportGeneration());
    }

    private void setupSearchAndSort() {
        String[] sortOptions = {"Recent First", "Oldest First", "Highest Cost", "Name (A-Z)"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortOptions);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position]; applyFilterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilterAndSort(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchAutoCompleteData() {
        // Fetch Members for "Person Involved"
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberSearchList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) memberSearchList.add(m.name + " (" + m.id + ")");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fetch past Events to suggest Puja Names
        db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventSearchList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String evtName = data.child("eventName").getValue(String.class);
                    if (evtName != null && !eventSearchList.contains(evtName)) eventSearchList.add(evtName);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadExpenses() {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Expense")
          .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupedMap.clear(); totalExpenseValue = 0f;

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Expense exp = data.getValue(Expense.class);
                        if (exp != null && exp.eventName != null) {
                            totalExpenseValue += exp.amount;
                            
                            String groupKey = exp.eventName.toLowerCase().trim();

                            if (!groupedMap.containsKey(groupKey)) {
                                GroupedExpense ge = new GroupedExpense();
                                ge.eventDisplayName = exp.eventName;
                                groupedMap.put(groupKey, ge);
                            }

                            GroupedExpense ge = groupedMap.get(groupKey);
                            ge.totalSpent += exp.amount;
                            if (exp.timestamp > ge.lastUpdated) ge.lastUpdated = exp.timestamp;
                            ge.history.add(exp);
                        }
                    } catch (Exception e) {}
                }
                tvTotalExpense.setText("Total Mandir Expenses: ৳" + totalExpenseValue);
                applyFilterAndSort();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilterAndSort() {
        displayList.clear();
        String query = inputSearch.getText().toString().toLowerCase().trim();

        for (GroupedExpense ge : groupedMap.values()) {
            if (ge.eventDisplayName.toLowerCase().contains(query)) { displayList.add(ge); }
        }

        Collections.sort(displayList, (a, b) -> {
            if (currentSort.equals("Recent First")) return Long.compare(b.lastUpdated, a.lastUpdated);
            if (currentSort.equals("Oldest First")) return Long.compare(a.lastUpdated, b.lastUpdated);
            if (currentSort.equals("Highest Cost")) return Float.compare(b.totalSpent, a.totalSpent);
            return a.eventDisplayName.compareToIgnoreCase(b.eventDisplayName);
        });

        renderCards();
    }

    private void renderCards() {
        transactionsContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (GroupedExpense ge : displayList) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_transaction, transactionsContainer, false);
            
            ((TextView) view.findViewById(R.id.tvTransName)).setText("🪔 " + ge.eventDisplayName);
            ((TextView) view.findViewById(R.id.tvTransName)).setTextColor(android.graphics.Color.parseColor("#E65100"));
            
            ((TextView) view.findViewById(R.id.tvTransAmount)).setText("Total Spent: ৳" + ge.totalSpent);
            ((TextView) view.findViewById(R.id.tvTransAmount)).setTextColor(android.graphics.Color.parseColor("#D32F2F"));
            
            ((TextView) view.findViewById(R.id.tvTransDate)).setText("Last Update: " + sdf.format(new Date(ge.lastUpdated)));
            ((TextView) view.findViewById(R.id.tvTransNote)).setText("Total Items/Sevas: " + ge.history.size() + "\n👉 Tap to download Full Utsav Ledger");

            // 📄 DRILL-DOWN PDF FOR THIS SPECIFIC UTSAV
            view.setOnClickListener(v -> {
                PdfReportService.generateUtsavStatement(this, session.getCommunityName(), ge);
                Toast.makeText(this, "Generating Utsav Ledger...", Toast.LENGTH_SHORT).show();
            });

            transactionsContainer.addView(view);
        }
    }

    private void showExpenseDialog(Expense existingExp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingExp == null ? "Record Utsav Expense" : "Edit Expense");

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final AutoCompleteTextView inputEvent = new AutoCompleteTextView(this); inputEvent.setHint("Utsav/Puja Name (e.g. Durga Puja)"); 
        inputEvent.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, eventSearchList)); inputEvent.setThreshold(1);
        
        final EditText inputItem = new EditText(this); inputItem.setHint("Item Details (e.g. Flowers, Murti)");
        
        final EditText inputAmount = new EditText(this); inputAmount.setHint("Amount (৳)"); inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        final AutoCompleteTextView inputPerson = new AutoCompleteTextView(this); inputPerson.setHint("Handled By (Member Name)"); 
        inputPerson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, memberSearchList)); inputPerson.setThreshold(1);

        if (existingExp != null) {
            inputEvent.setText(existingExp.eventName); inputItem.setText(existingExp.itemName); 
            inputAmount.setText(String.valueOf(existingExp.amount)); inputPerson.setText(existingExp.involvedPerson);
        }

        layout.addView(inputEvent); layout.addView(inputItem); layout.addView(inputAmount); layout.addView(inputPerson);
        builder.setView(layout);

        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String event = inputEvent.getText().toString().trim();
            String item = inputItem.getText().toString().trim();
            String amtStr = inputAmount.getText().toString().trim();
            String person = inputPerson.getText().toString().trim();

            if (event.isEmpty() || item.isEmpty() || amtStr.isEmpty()) { Toast.makeText(this, "Event, Item, and Amount required", Toast.LENGTH_SHORT).show(); return; }

            try {
                float amt = Float.parseFloat(amtStr);
                String expId = existingExp == null ? db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").push().getKey() : existingExp.id;
                
                String strictSignature = "ADMIN".equals(session.getRole()) ? "Super Admin - " + session.getUserName() : "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";

                Expense newExp = new Expense(expId, event, item, amt, person.isEmpty() ? session.getUserName() : person, 
                                             existingExp == null ? System.currentTimeMillis() : existingExp.timestamp, 
                                             strictSignature);

                db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(expId).setValue(newExp);
                Toast.makeText(this, "Expense Logged", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {}
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void triggerMasterReportGeneration() {
        Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0); 
            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                long startTs = startCal.getTimeInMillis(); long endTs = endCal.getTimeInMillis();
                String reportRange = "Period: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(startCal.getTime()) + " to " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(endCal.getTime());

                List<Expense> filteredLogs = new ArrayList<>();
                float total = 0f;
                for(GroupedExpense ge : groupedMap.values()) {
                    for(Expense e : ge.history) {
                        if(e.timestamp >= startTs && e.timestamp <= endTs) { filteredLogs.add(e); total += e.amount; }
                    }
                }
                PdfReportService.generateExpenseReport(this, session.getCommunityName(), filteredLogs, total, reportRange);

            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // INTERNAL DATA MODEL FOR GROUPING
    public static class GroupedExpense {
        public String eventDisplayName;
        public float totalSpent = 0f;
        public long lastUpdated = 0L;
        public List<Expense> history = new ArrayList<>();
    }
}
