package org.shda;

import android.app.DatePickerDialog;
import android.os.Bundle;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout transactionsContainer;
    private TextView tvTotalDonations;
    private float totalDonationsValue = 0f;
    private List<String> memberSearchList = new ArrayList<>(); 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        transactionsContainer = findViewById(R.id.transactionsContainer);
        tvTotalDonations = findViewById(R.id.tvTotalDonations);

        if (session.getCommunityId() == null) { finish(); return; }

        View btnAddTransaction = findViewById(R.id.btnAddTransaction); 
        if (btnAddTransaction != null) {
            if ("MEMBER".equals(session.getRole())) {
                btnAddTransaction.setVisibility(View.GONE); // strict RBAC
            } else {
                btnAddTransaction.setVisibility(View.VISIBLE);
                btnAddTransaction.setOnClickListener(v -> showAddChandaDialog());
            }
        }

        View btnGenerateReport = findViewById(R.id.btnGenerateReport);
        if (btnGenerateReport != null) {
            btnGenerateReport.setOnClickListener(v -> triggerReportGeneration());
        }

        loadTransactions();
        fetchMembersForAutoComplete();
    }

    private void fetchMembersForAutoComplete() {
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberSearchList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) {
                        memberSearchList.add(m.name + " (" + m.id + ")");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTransactions() {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
          .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionsContainer.removeAllViews();
                totalDonationsValue = 0f;
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        String name = data.child("name").getValue(String.class);
                        Float amount = data.child("amount").getValue(Float.class);
                        String note = data.child("note").getValue(String.class);
                        Long timestamp = data.child("timestamp").getValue(Long.class);
                        String loggedBy = data.child("loggedBy").getValue(String.class);

                        if (name != null && amount != null) {
                            totalDonationsValue += amount;
                            
                            View view = LayoutInflater.from(TransactionActivity.this).inflate(R.layout.item_transaction, transactionsContainer, false);
                            
                            ((TextView) view.findViewById(R.id.tvTransName)).setText(name);
                            ((TextView) view.findViewById(R.id.tvTransAmount)).setText("৳" + amount);
                            
                            String formattedDate = timestamp != null ? sdf.format(new Date(timestamp)) : "Unknown Date";
                            ((TextView) view.findViewById(R.id.tvTransDate)).setText(formattedDate);
                            
                            String finalNote = (note != null ? note : "") + "\n✍️ Collected By: " + (loggedBy != null ? loggedBy : "System");
                            ((TextView) view.findViewById(R.id.tvTransNote)).setText(finalNote);

                            // ✨ NEW: Click any card to instantly generate a Single Receipt PDF!
                            view.setOnClickListener(v -> {
                                new AlertDialog.Builder(TransactionActivity.this)
                                    .setTitle("Generate Receipt")
                                    .setMessage("Download official receipt for " + name + "?")
                                    .setPositiveButton("DOWNLOAD PDF", (dialog, which) -> {
                                        PdfReportService.generateDonorReceipt(TransactionActivity.this, session.getCommunityName(), name, amount, finalNote, formattedDate);
                                    }).setNegativeButton("CANCEL", null).show();
                            });

                            transactionsContainer.addView(view, 0); 
                        }
                    } catch (Exception e) {}
                }
                if (tvTotalDonations != null) tvTotalDonations.setText("Total Chanda: ৳" + totalDonationsValue);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddChandaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Smart Chanda");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        android.widget.RadioGroup rgType = new android.widget.RadioGroup(this);
        rgType.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.RadioButton rbMember = new android.widget.RadioButton(this); rbMember.setText("Member"); rbMember.setChecked(true);
        android.widget.RadioButton rbGuest = new android.widget.RadioButton(this); rbGuest.setText("Guest");
        rgType.addView(rbMember); rgType.addView(rbGuest);
        layout.addView(rgType);

        final AutoCompleteTextView inputName = new AutoCompleteTextView(this); 
        inputName.setHint("Donor Name / SB-ID"); 
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, memberSearchList);
        inputName.setAdapter(adapter);
        inputName.setThreshold(1); 
        layout.addView(inputName);

        final EditText inputAmount = new EditText(this); inputAmount.setHint("Amount (৳)"); inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(inputAmount);
        final EditText inputNote = new EditText(this); inputNote.setHint("Purpose / Gotra / Note"); layout.addView(inputNote);

        builder.setView(layout);

        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String rawName = inputName.getText().toString().trim();
            String amountStr = inputAmount.getText().toString().trim();
            String note = inputNote.getText().toString().trim();
            String donorType = rbMember.isChecked() ? "[Member]" : "[Guest]";

            if (rawName.isEmpty() || amountStr.isEmpty()) { Toast.makeText(this, "Name and Amount required", Toast.LENGTH_SHORT).show(); return; }

            try {
                float amount = Float.parseFloat(amountStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                
                String finalNameToSave = donorType + " " + rawName;

                HashMap<String, Object> transMap = new HashMap<>();
                transMap.put("name", finalNameToSave);
                transMap.put("amount", amount);
                transMap.put("note", note);
                transMap.put("timestamp", System.currentTimeMillis());
                
                String strictSignature;
                if ("ADMIN".equals(session.getRole())) {
                    strictSignature = "Super Admin - " + session.getUserName(); 
                } else {
                    strictSignature = "Manager - " + session.getUserName() + " (" + session.getUserId() + ")"; 
                }
                transMap.put("loggedBy", strictSignature);

                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(transMap);
                
                // ✨ NEW: AUTOMATICALLY UPDATE MEMBER'S TOTAL BALANCE
                String memberId = null;
                if (rawName.contains("(") && rawName.contains(")")) {
                    memberId = rawName.substring(rawName.lastIndexOf("(") + 1, rawName.lastIndexOf(")"));
                }
                if (memberId != null && memberId.startsWith("SB-")) {
                    DatabaseReference memRef = db.child("communities").child(session.getCommunityId()).child("members").child(memberId).child("totalDonated");
                    memRef.get().addOnSuccessListener(snap -> {
                        float currentTotal = 0f;
                        if (snap.exists() && snap.getValue() != null) { currentTotal = snap.getValue(Float.class); }
                        memRef.setValue(currentTotal + amount);
                    });
                }

                Toast.makeText(this, "Chanda Recorded Securely", Toast.LENGTH_SHORT).show();
                AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "CHANDA_RECORDED", "Recorded ৳" + amount + " from " + rawName);
                
            } catch (NumberFormatException e) { Toast.makeText(this, "Invalid Amount", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void triggerReportGeneration() {
        Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0); 
            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                long startTimestamp = startCal.getTimeInMillis(); long endTimestamp = endCal.getTimeInMillis();
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String reportRange = "Period: " + displayFormat.format(startCal.getTime()) + " to " + displayFormat.format(endCal.getTime());

                Toast.makeText(this, "Auditing Financials...", Toast.LENGTH_SHORT).show();
                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
                  .orderByChild("timestamp").startAt(startTimestamp).endAt(endTimestamp)
                  .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> names = new ArrayList<>(); List<Float> amounts = new ArrayList<>();
                        List<String> notes = new ArrayList<>(); List<String> dates = new ArrayList<>();
                        float total = 0f; SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                        
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String name = data.child("name").getValue(String.class); Float amt = data.child("amount").getValue(Float.class);
                            String note = data.child("note").getValue(String.class); Long ts = data.child("timestamp").getValue(Long.class);
                            String loggedBy = data.child("loggedBy").getValue(String.class);
                            
                            if(name != null && amt != null) {
                                names.add(name); amounts.add(amt); 
                                String finalNote = (note != null ? note : "") + "\n(Collected by: " + (loggedBy != null ? loggedBy : "System") + ")";
                                notes.add(finalNote);
                                dates.add(ts != null ? sdf.format(new Date(ts)) : "Unknown Date"); 
                                total += amt;
                            }
                        }
                        PdfReportService.generateFinancialReport(TransactionActivity.this, session.getCommunityName(), dates, names, amounts, notes, total, reportRange);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
