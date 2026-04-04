package org.shda;

import android.os.Bundle;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout transactionsContainer;
    private TextView tvTotalDonations;
    private float totalDonationsValue = 0f;

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
        
        // 🔒 RBAC Security: Only Admins/Managers can add Chanda. Members can only view.
        if (btnAddTransaction != null) {
            if ("MEMBER".equals(session.getRole())) {
                btnAddTransaction.setVisibility(View.GONE);
            } else {
                btnAddTransaction.setVisibility(View.VISIBLE);
                btnAddTransaction.setOnClickListener(v -> showAddChandaDialog());
            }
        }

        loadTransactions();
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
                            
                            TextView tvName = view.findViewById(R.id.tvTransName);
                            TextView tvAmount = view.findViewById(R.id.tvTransAmount);
                            TextView tvDate = view.findViewById(R.id.tvTransDate);
                            TextView tvNote = view.findViewById(R.id.tvTransNote);

                            if(tvName != null) tvName.setText(name);
                            if(tvAmount != null) tvAmount.setText("৳" + amount);
                            if(tvDate != null && timestamp != null) tvDate.setText(sdf.format(new Date(timestamp)));
                            
                            // Combine the user note with the permanent Manager signature
                            String finalNote = (note != null ? note : "") + "\n✍️ Logged by: " + (loggedBy != null ? loggedBy : "Admin");
                            if(tvNote != null) tvNote.setText(finalNote);

                            transactionsContainer.addView(view, 0); 
                        }
                    } catch (Exception e) {
                        // Safely ignore corrupted data
                    }
                }
                if (tvTotalDonations != null) {
                    tvTotalDonations.setText("Total Chanda: ৳" + totalDonationsValue);
                }
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

        // Radio Group for Member vs Guest
        android.widget.RadioGroup rgType = new android.widget.RadioGroup(this);
        rgType.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.RadioButton rbMember = new android.widget.RadioButton(this); rbMember.setText("Member"); rbMember.setChecked(true);
        android.widget.RadioButton rbGuest = new android.widget.RadioButton(this); rbGuest.setText("Guest");
        rgType.addView(rbMember); rgType.addView(rbGuest);
        layout.addView(rgType);

        final EditText inputName = new EditText(this); inputName.setHint("Donor Name / SB-ID"); layout.addView(inputName);
        final EditText inputAmount = new EditText(this); inputAmount.setHint("Amount (৳)"); inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(inputAmount);
        final EditText inputNote = new EditText(this); inputNote.setHint("Purpose / Gotra / Note"); layout.addView(inputNote);

        builder.setView(layout);

        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String amountStr = inputAmount.getText().toString().trim();
            String note = inputNote.getText().toString().trim();
            String donorType = rbMember.isChecked() ? "[Member]" : "[Guest]";

            if (name.isEmpty() || amountStr.isEmpty()) { Toast.makeText(this, "Name and Amount required", Toast.LENGTH_SHORT).show(); return; }

            try {
                float amount = Float.parseFloat(amountStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                
                HashMap<String, Object> transMap = new HashMap<>();
                transMap.put("name", donorType + " " + name);
                transMap.put("amount", amount);
                transMap.put("note", note);
                transMap.put("timestamp", System.currentTimeMillis());
                transMap.put("loggedBy", session.getRole() + " - " + session.getUserName());

                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(transMap);
                Toast.makeText(this, "Chanda Recorded Securely", Toast.LENGTH_SHORT).show();
                
                // Automatically log to the Audit Trail
                AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "CHANDA_RECORDED", "Recorded ৳" + amount + " from " + name);
                
            } catch (NumberFormatException e) { Toast.makeText(this, "Invalid Amount", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
