package org.shda;

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
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemberDetailActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private String passedMemberId;
    private Member activeMember;

    private TextView tvId, tvName, tvRole, tvPhone, tvGotra, tvBlood, tvJoined, tvVerified, tvDonated;
    private LinearLayout containerAdminControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        passedMemberId = getIntent().getStringExtra("MEMBER_ID");

        if (passedMemberId == null) { finish(); return; }

        tvId = findViewById(R.id.tvInsightId);
        tvName = findViewById(R.id.tvInsightName);
        tvRole = findViewById(R.id.tvInsightRole);
        tvPhone = findViewById(R.id.tvInsightPhone);
        tvGotra = findViewById(R.id.tvInsightGotra);
        tvBlood = findViewById(R.id.tvInsightBlood);
        tvJoined = findViewById(R.id.tvInsightJoined);
        tvVerified = findViewById(R.id.tvInsightVerified);
        tvDonated = findViewById(R.id.tvInsightDonated);
        containerAdminControls = findViewById(R.id.containerAdminControls);

        loadMemberDetails();

        // Standard Button Actions
        findViewById(R.id.btnInsightPdf).setOnClickListener(v -> {
            if (activeMember != null) PdfReportService.generateMemberProfile(this, session.getCommunityName(), activeMember);
        });

        Button btnAddDonation = findViewById(R.id.btnInsightAddDonation);
        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAddDonation.setOnClickListener(v -> showQuickDonationDialog());
        } else {
            btnAddDonation.setVisibility(View.GONE);
        }

        // Admin Only Buttons
        if ("ADMIN".equals(session.getRole())) {
            containerAdminControls.setVisibility(View.VISIBLE);
            findViewById(R.id.btnPromote).setOnClickListener(v -> changeMemberRole("MANAGER"));
            findViewById(R.id.btnDemote).setOnClickListener(v -> changeMemberRole("MEMBER"));
        }
    }

    private void loadMemberDetails() {
        DatabaseReference memberRef = db.child("communities").child(session.getCommunityId()).child("members").child(passedMemberId);
        memberRef.keepSynced(true);
        memberRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeMember = snapshot.getValue(Member.class);
                if (activeMember != null) {
                    tvId.setText(activeMember.id);
                    tvName.setText(activeMember.name);
                    tvRole.setText(activeMember.role);
                    
                    tvPhone.setText("📞 " + (activeMember.phone != null ? activeMember.phone : "N/A"));
                    tvGotra.setText("🕉️ Gotra: " + (activeMember.gotra != null && !activeMember.gotra.isEmpty() ? activeMember.gotra : "N/A"));
                    tvBlood.setText("🩸 Blood Group: " + (activeMember.bloodGroup != null && !activeMember.bloodGroup.isEmpty() ? activeMember.bloodGroup : "N/A"));
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String joinDate = activeMember.timestamp > 0 ? sdf.format(new Date(activeMember.timestamp)) : "Unknown";
                    tvJoined.setText("Joined: " + joinDate);
                    
                    tvVerified.setText("✍️ Profile verified by: " + (activeMember.addedBySignature != null ? activeMember.addedBySignature : "System"));
                    tvDonated.setText("Total Donated: ৳" + activeMember.totalDonated);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showQuickDonationDialog() {
        if (activeMember == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Donation for " + activeMember.name);

        final EditText inputAmt = new EditText(this);
        inputAmt.setHint("Amount in BDT (৳)");
        inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(inputAmt);
        builder.setView(layout);

        builder.setPositiveButton("RECORD", (dialog, which) -> {
            String amtStr = inputAmt.getText().toString();
            if (!amtStr.isEmpty()) {
                float amt = Float.parseFloat(amtStr);
                db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("totalDonated").setValue(ServerValue.increment(amt));
                
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                TransactionActivity.SingleDonation sd = new TransactionActivity.SingleDonation(transId, activeMember.name, amt, "Added via Profile Insights", "", "", session.getUserName(), System.currentTimeMillis(), session.getRole());
                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);
                
                Toast.makeText(this, "৳" + amt + " donation recorded!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void changeMemberRole(String newRole) {
        if (activeMember == null) return;
        db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("role").setValue(newRole);
        Toast.makeText(this, activeMember.name + " is now a " + newRole + "!", Toast.LENGTH_SHORT).show();
    }
}
