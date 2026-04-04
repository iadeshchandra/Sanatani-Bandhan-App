package org.shda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.Date;
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

        // THE FIX: RBAC Security for Adding Chanda
        // Find the button that opens the "Record Smart Chanda" dialog
        View btnAddTransaction = findViewById(R.id.btnAddTransaction); 
        
        if (btnAddTransaction != null) {
            if ("MEMBER".equals(session.getRole())) {
                // Hide the add button completely from normal members
                btnAddTransaction.setVisibility(View.GONE);
            } else {
                // Admins and Managers get the click listener to open your Chanda dialog
                // (Ensure your existing showAddChandaDialog method is called here)
                // btnAddTransaction.setOnClickListener(v -> showAddChandaDialog());
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

                        if (name != null && amount != null) {
                            totalDonationsValue += amount;
                            
                            // Re-inflate your standard transaction item view
                            View view = LayoutInflater.from(TransactionActivity.this).inflate(R.layout.item_transaction, transactionsContainer, false);
                            
                            TextView tvName = view.findViewById(R.id.tvTransName);
                            TextView tvAmount = view.findViewById(R.id.tvTransAmount);
                            TextView tvDate = view.findViewById(R.id.tvTransDate);
                            TextView tvNote = view.findViewById(R.id.tvTransNote);

                            if(tvName != null) tvName.setText(name);
                            if(tvAmount != null) tvAmount.setText("৳" + amount);
                            if(tvDate != null && timestamp != null) tvDate.setText(sdf.format(new Date(timestamp)));
                            if(tvNote != null) tvNote.setText(note != null ? note : "");

                            transactionsContainer.addView(view, 0); // Add newest to the top
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

    // Include your existing showAddChandaDialog() logic here from your previous file!
}
