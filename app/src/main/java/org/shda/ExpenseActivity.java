package org.shda;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout expensesContainer;
    private TextView tvTotalExpense;
    private float totalExpenseValue = 0f;
    private List<Expense> allExpenses = new ArrayList<>();
    private boolean isAdminOrManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        expensesContainer = findViewById(R.id.expensesContainer);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);

        if (session.getCommunityId() == null) { finish(); return; }

        isAdminOrManager = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        View btnAddExpense = findViewById(R.id.btnAddExpense);
        if (btnAddExpense != null) {
            if (!isAdminOrManager) {
                btnAddExpense.setVisibility(View.GONE);
            } else {
                btnAddExpense.setOnClickListener(v -> showExpenseDialog(null));
            }
        }
        
        View btnGenerateReport = findViewById(R.id.btnGenerateReport);
        if (btnGenerateReport != null) {
            btnGenerateReport.setOnClickListener(v -> {
                String range = "All Time Utsav Expenses";
                PdfReportService.generateExpenseReport(this, session.getCommunityName(), allExpenses, totalExpenseValue, range);
            });
        }

        loadExpenses();
    }

    private void loadExpenses() {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Expense")
          .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expensesContainer.removeAllViews();
                allExpenses.clear();
                totalExpenseValue = 0f;
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Expense exp = data.getValue(Expense.class);
                        if (exp != null) {
                            allExpenses.add(exp);
                            totalExpenseValue += exp.amount;

                            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(ExpenseActivity.this);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 0, 24); card.setLayoutParams(params);
                            card.setCardElevation(4f); card.setRadius(16f); card.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"));

                            LinearLayout layout = new LinearLayout(ExpenseActivity.this);
                            layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 40, 40, 40);

                            TextView tvEvent = new TextView(ExpenseActivity.this); tvEvent.setText("🪔 Utsav: " + exp.eventName); tvEvent.setTextSize(18f); tvEvent.setTypeface(null, android.graphics.Typeface.BOLD); tvEvent.setTextColor(android.graphics.Color.parseColor("#E65100"));
                            TextView tvItem = new TextView(ExpenseActivity.this); tvItem.setText("Seva/Item: " + exp.itemName); tvItem.setTextSize(16f); tvItem.setTextColor(android.graphics.Color.parseColor("#424242"));
                            TextView tvAmt = new TextView(ExpenseActivity.this); tvAmt.setText("Amount: ৳" + exp.amount); tvAmt.setTextSize(16f); tvAmt.setTypeface(null, android.graphics.Typeface.BOLD); tvAmt.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                            TextView tvPerson = new TextView(ExpenseActivity.this); tvPerson.setText("Assigned to: " + exp.involvedPerson); tvPerson.setTextSize(14f);
                            TextView tvLog = new TextView(ExpenseActivity.this); tvLog.setText("Logged by: " + exp.loggedBy + " on " + sdf.format(new Date(exp.timestamp))); tvLog.setTextSize(12f); tvLog.setTextColor(android.graphics.Color.GRAY);

                            layout.addView(tvEvent); layout.addView(tvItem); layout.addView(tvAmt); layout.addView(tvPerson); layout.addView(tvLog);
                            card.addView(layout);

                            if (isAdminOrManager) {
                                card.setOnLongClickListener(v -> {
                                    showEditDeleteDialog(exp);
                                    return true;
                                });
                            }
                            expensesContainer.addView(card, 0); 
                        }
                    } catch (Exception e) {}
                }
                if (tvTotalExpense != null) tvTotalExpense.setText("Total Utsav Expenses: ৳" + totalExpenseValue);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showExpenseDialog(Expense existingExp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingExp == null ? "Record Utsav Expense" : "Edit Expense");

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final EditText inputEvent = new EditText(this); inputEvent.setHint("Utsav/Puja Name (e.g. Durga Puja 2026)"); 
        final EditText inputItem = new EditText(this); inputItem.setHint("Item Details (e.g. Flowers, Murti)");
        final EditText inputAmount = new EditText(this); inputAmount.setHint("Amount (৳)"); inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText inputPerson = new EditText(this); inputPerson.setHint("Person Involved (Who bought it?)");

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
                
                // 🛡️ DYNAMIC STRICT SIGNATURE
                String strictSignature;
                if ("ADMIN".equals(session.getRole())) {
                    strictSignature = "Super Admin - " + session.getUserName();
                } else {
                    strictSignature = "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";
                }

                Expense newExp = new Expense(expId, event, item, amt, person.isEmpty() ? "Self" : person, 
                                             existingExp == null ? System.currentTimeMillis() : existingExp.timestamp, 
                                             strictSignature);

                db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(expId).setValue(newExp);
                Toast.makeText(this, "Expense Logged", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {}
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditDeleteDialog(Expense exp) {
        new AlertDialog.Builder(this)
            .setTitle("Manage Expense")
            .setMessage("Do you want to Edit or Delete this entry for " + exp.itemName + "?")
            .setPositiveButton("EDIT", (dialog, which) -> showExpenseDialog(exp))
            .setNegativeButton("DELETE", (dialog, which) -> {
                db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(exp.id).removeValue();
                Toast.makeText(this, "Expense Deleted", Toast.LENGTH_SHORT).show();
                AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "EXPENSE_DELETED", "Deleted: " + exp.itemName + " (৳" + exp.amount + ")");
            })
            .setNeutralButton("CANCEL", null)
            .show();
    }
}
