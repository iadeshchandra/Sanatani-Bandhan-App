package org.shda;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExpenseActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout expensesContainer;
    private TextView tvTotalExpenses;
    
    private List<Expense> fullExpenseList = new ArrayList<>();
    private List<String> autocompleteManagers = new ArrayList<>();
    private float totalSpent = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        expensesContainer = findViewById(R.id.expensesContainer);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);

        View btnAdd = findViewById(R.id.btnAddExpense);
        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAdd.setVisibility(View.VISIBLE);
            btnAdd.setOnClickListener(v -> showAddExpenseDialog());
        } else {
            btnAdd.setVisibility(View.GONE);
        }

        loadManagersForAutocomplete();
        loadExpenses();
    }

    private void loadManagersForAutocomplete() {
        db.child("communities").child(session.getCommunityId()).child("members").keepSynced(true);
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteManagers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null && ("MANAGER".equals(m.role) || "ADMIN".equals(m.role))) autocompleteManagers.add(m.name);
                }
                if (!autocompleteManagers.contains(session.getUserName())) autocompleteManagers.add(session.getUserName());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadExpenses() {
        DatabaseReference expRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Expense");
        expRef.keepSynced(true);
        expRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullExpenseList.clear(); totalSpent = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Expense e = data.getValue(Expense.class);
                    if (e != null) { fullExpenseList.add(e); totalSpent += e.amount; }
                }
                tvTotalExpenses.setText("Total Utsav Expenses: ৳" + totalSpent);
                processAndRenderList(fullExpenseList);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void processAndRenderList(List<Expense> rawList) {
        HashMap<String, GroupedExpense> groupedMap = new HashMap<>();
        for (Expense e : rawList) {
            String key = e.eventName.trim().toLowerCase();
            if (!groupedMap.containsKey(key)) groupedMap.put(key, new GroupedExpense(e.eventName));
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
                ((TextView) view.findViewById(R.id.tvExpenseDetails)).setText(ge.history.size() + " items logged");
                
                view.setOnClickListener(v -> PdfReportService.generateUtsavStatement(this, session.getCommunityName(), ge));
                
                // ✨ CRUD: Edit or Delete Expense
                if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
                    view.setOnLongClickListener(v -> { showExpenseManagerDialog(ge); return true; });
                }
                expensesContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Utsav Expense");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final EditText inputEvent = new EditText(this); inputEvent.setHint("Event/Puja Name (e.g. Rash Purnima)");
        final EditText inputItem = new EditText(this); inputItem.setHint("Item Purchased / Seva");
        final EditText inputAmt = new EditText(this); inputAmt.setHint("Cost (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        final AutoCompleteTextView inputHandler = new AutoCompleteTextView(this);
        inputHandler.setHint("Handled By");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteManagers);
        inputHandler.setAdapter(adapter); inputHandler.setThreshold(1); inputHandler.setText(session.getUserName());

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
            Expense e = new Expense(transId, event, item, amt, handler, System.currentTimeMillis(), session.getUserName());
            
            db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(transId).setValue(e);
            Toast.makeText(this, "Expense Logged!", Toast.LENGTH_SHORT).show(); dialog.dismiss();
        });
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
}
