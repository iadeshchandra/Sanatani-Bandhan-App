package org.shda;

import android.app.DatePickerDialog;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout expensesContainer;
    private TextView tvTotalExpense;
    private float totalExpenseValue = 0f;
    private List<Expense> expenseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        expensesContainer = findViewById(R.id.expensesContainer);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        
        Button btnAddExpense = findViewById(R.id.btnAddExpense);
        
        if (session.getCommunityId() == null) { finish(); return; }

        // Only Admins and Managers can add new expenses
        if ("MEMBER".equals(session.getRole())) {
            btnAddExpense.setVisibility(View.GONE);
        } else {
            btnAddExpense.setOnClickListener(v -> showAddExpenseDialog());
        }

        findViewById(R.id.btnExportExpense).setOnClickListener(v -> triggerExpenseReportPDF());

        loadExpenses();
    }

    private void loadExpenses() {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Expense")
          .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expensesContainer.removeAllViews();
                expenseList.clear();
                totalExpenseValue = 0f;
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

                for (DataSnapshot data : snapshot.getChildren()) {
                    Expense exp = data.getValue(Expense.class);
                    if (exp != null) {
                        expenseList.add(exp);
                        totalExpenseValue += exp.amount;
                        
                        View view = LayoutInflater.from(ExpenseActivity.this).inflate(R.layout.item_expense, expensesContainer, false);
                        ((TextView) view.findViewById(R.id.tvExpPurpose)).setText(exp.purpose);
                        ((TextView) view.findViewById(R.id.tvExpAmount)).setText("৳" + exp.amount);
                        ((TextView) view.findViewById(R.id.tvExpComment)).setText("Note: " + (exp.comment.isEmpty() ? "N/A" : exp.comment));
                        ((TextView) view.findViewById(R.id.tvExpDate)).setText(sdf.format(new Date(exp.timestamp)));
                        ((TextView) view.findViewById(R.id.tvExpUser)).setText("Logged by: " + exp.loggedBy);
                        
                        expensesContainer.addView(view, 0); // Add to top
                    }
                }
                tvTotalExpense.setText("Total Expenses: ৳" + totalExpenseValue);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log New Expense");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        final EditText inputPurpose = new EditText(this);
        inputPurpose.setHint("Purpose (e.g. Saraswati Puja)");
        layout.addView(inputPurpose);

        final EditText inputAmount = new EditText(this);
        inputAmount.setHint("Amount (৳)");
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputAmount);

        final EditText inputComment = new EditText(this);
        inputComment.setHint("Admin Comment / Details (Optional)");
        layout.addView(inputComment);

        builder.setView(layout);

        builder.setPositiveButton("SAVE EXPENSE", (dialog, which) -> {
            String purpose = inputPurpose.getText().toString().trim();
            String amountStr = inputAmount.getText().toString().trim();
            String comment = inputComment.getText().toString().trim();

            if (purpose.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Purpose and Amount are required", Toast.LENGTH_SHORT).show();
                return;
            }

            float amount = Float.parseFloat(amountStr);
            String expId = db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").push().getKey();
            
            Expense newExpense = new Expense(expId, amount, purpose, comment, System.currentTimeMillis(), session.getUserName());
            
            db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(expId).setValue(newExpense);
            
            // Update Master Finance Tracker
            db.child("communities").child(session.getCommunityId()).child("finances").child("Expense").get().addOnSuccessListener(snap -> {
                float currentTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
                db.child("communities").child(session.getCommunityId()).child("finances").child("Expense").setValue(currentTotal + amount);
            });

            AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "EXPENSE_ADDED", "Logged expense of ৳" + amount + " for " + purpose);
            Toast.makeText(this, "Expense Recorded", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void triggerExpenseReportPDF() {
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                
                long startTimestamp = startCal.getTimeInMillis();
                long endTimestamp = endCal.getTimeInMillis();
                
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String reportRange = "Period: " + displayFormat.format(startCal.getTime()) + " to " + displayFormat.format(endCal.getTime());

                List<Expense> filteredList = new ArrayList<>();
                float filteredTotal = 0f;

                for (Expense exp : expenseList) {
                    if (exp.timestamp >= startTimestamp && exp.timestamp <= endTimestamp) {
                        filteredList.add(exp);
                        filteredTotal += exp.amount;
                    }
                }
                
                if (filteredList.isEmpty()) {
                    Toast.makeText(this, "No expenses found in this date range", Toast.LENGTH_SHORT).show();
                } else {
                    PdfReportService.generateExpenseReport(this, session.getCommunityName(), filteredList, filteredTotal, reportRange);
                }
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
