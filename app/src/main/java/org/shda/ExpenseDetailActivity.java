package org.shda;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseDetailActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseReference db;
    private String eventName;
    
    private TextView tvUtsavTitle, tvUtsavTotal;
    private Button btnDownloadPdf;
    private LinearLayout detailItemsContainer;
    
    private List<ExpenseActivity.Expense> expenseList = new ArrayList<>();
    private float totalSpent = 0f;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_detail);

        session = new SessionManager(this);
        db = FirebaseDatabase.getInstance().getReference();
        
        eventName = getIntent().getStringExtra("EVENT_NAME");
        if (eventName == null || eventName.isEmpty()) { finish(); return; }

        tvUtsavTitle = findViewById(R.id.tvUtsavTitle);
        tvUtsavTotal = findViewById(R.id.tvUtsavTotal);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        detailItemsContainer = findViewById(R.id.detailItemsContainer);

        tvUtsavTitle.setText("🪔 " + eventName);

        btnDownloadPdf.setOnClickListener(v -> generateStatement());

        loadUtsavDetails();
    }

    private void loadUtsavDetails() {
        // Query Firebase strictly for this specific Utsav
        DatabaseReference expRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Expense");
        expRef.orderByChild("eventName").equalTo(eventName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expenseList.clear();
                totalSpent = 0f;
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    ExpenseActivity.Expense e = data.getValue(ExpenseActivity.Expense.class);
                    if (e != null) {
                        expenseList.add(e);
                        totalSpent += e.amount;
                    }
                }
                
                tvUtsavTotal.setText("Total Spent: ৳" + totalSpent);
                renderItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExpenseDetailActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderItems() {
        detailItemsContainer.removeAllViews();
        // Sort by newest first
        Collections.sort(expenseList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        boolean canEdit = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        for (ExpenseActivity.Expense e : expenseList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(30, 30, 30, 30);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            card.setLayoutParams(params);
            card.setElevation(2f);

            TextView tvItem = new TextView(this);
            tvItem.setText(e.itemName);
            tvItem.setTextSize(18f);
            tvItem.setTypeface(null, android.graphics.Typeface.BOLD);
            tvItem.setTextColor(Color.parseColor("#212121"));

            TextView tvAmount = new TextView(this);
            tvAmount.setText("Cost: ৳" + e.amount);
            tvAmount.setTextColor(Color.parseColor("#D32F2F"));
            tvAmount.setTextSize(16f);
            tvAmount.setPadding(0, 5, 0, 5);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Date: " + sdf.format(new Date(e.timestamp)) + "\nHandled By: " + e.involvedPerson);
            tvMeta.setTextColor(Color.parseColor("#757575"));
            tvMeta.setTextSize(14f);

            card.addView(tvItem);
            card.addView(tvAmount);
            card.addView(tvMeta);

            // Render Edit History if it exists
            if (e.editHistory != null && !e.editHistory.isEmpty()) {
                TextView tvHistoryTitle = new TextView(this);
                tvHistoryTitle.setText("\n📋 Edit History:");
                tvHistoryTitle.setTextSize(12f);
                tvHistoryTitle.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
                card.addView(tvHistoryTitle);

                for (String historyMsg : e.editHistory.values()) {
                    TextView tvMsg = new TextView(this);
                    tvMsg.setText("• " + historyMsg);
                    tvMsg.setTextSize(11f);
                    tvMsg.setTextColor(Color.parseColor("#9E9E9E"));
                    card.addView(tvMsg);
                }
            }

            // Edit Controls
            if (canEdit) {
                Button btnEdit = new Button(this);
                btnEdit.setText("EDIT RECORD");
                btnEdit.setBackgroundColor(Color.TRANSPARENT);
                btnEdit.setTextColor(Color.parseColor("#1976D2"));
                btnEdit.setOnClickListener(v -> showEditDialog(e));
                card.addView(btnEdit);
            }

            detailItemsContainer.addView(card);
        }
    }

    private void showEditDialog(ExpenseActivity.Expense e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Expense Record");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        final EditText inputItem = new EditText(this);
        inputItem.setHint("Item Name");
        inputItem.setText(e.itemName);

        final EditText inputAmt = new EditText(this);
        inputAmt.setHint("Cost (৳)");
        inputAmt.setText(String.valueOf(e.amount));
        inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        layout.addView(inputItem);
        layout.addView(inputAmt);
        builder.setView(layout);

        builder.setPositiveButton("UPDATE", (dialog, which) -> {
            String newItem = inputItem.getText().toString().trim();
            String amtStr = inputAmt.getText().toString().trim();

            if (newItem.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            float newAmt = Float.parseFloat(amtStr);
            
            // Check if anything actually changed
            if (newItem.equals(e.itemName) && newAmt == e.amount) {
                Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
                return;
            }

            // Build the Audit Trail Message
            String historyMsg = sdf.format(new Date()) + " - Edited by " + session.getUserName() + ": ";
            if (!newItem.equals(e.itemName)) historyMsg += "Item changed from '" + e.itemName + "' to '" + newItem + "'. ";
            if (newAmt != e.amount) historyMsg += "Amount changed from ৳" + e.amount + " to ৳" + newAmt + ".";

            String pushKey = db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(e.id).child("editHistory").push().getKey();

            // Firebase Offline-Safe Update
            Map<String, Object> updates = new HashMap<>();
            updates.put("itemName", newItem);
            updates.put("amount", newAmt);
            updates.put("editHistory/" + pushKey, historyMsg);

            db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").child(e.id).updateChildren(updates);
            
            // Global Audit Log
            logAudit("EXPENSE_EDITED", "Edited record in Utsav: " + e.eventName);

            Toast.makeText(this, "Record Updated & History Logged", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }
    
    private void generateStatement() {
        if (expenseList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // We dynamically build the GroupedExpense object to feed it perfectly to the PdfReportService
        ExpenseActivity.GroupedExpense ge = new ExpenseActivity.GroupedExpense(eventName);
        for (ExpenseActivity.Expense e : expenseList) {
            ge.addExpense(e);
        }
        
        try { 
            PdfReportService.generateUtsavStatement(this, session.getCommunityName(), ge); 
        } catch (Exception ex) { 
            Toast.makeText(this, "Error generating PDF.", Toast.LENGTH_SHORT).show(); 
        }
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }
}
