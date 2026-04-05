package org.shda;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class MemberDetailActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private String memberId;
    private Member currentMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        memberId = getIntent().getStringExtra("MEMBER_ID");

        if (memberId == null || session.getCommunityId() == null) { finish(); return; }

        LinearLayout layoutAdminControls = findViewById(R.id.layoutAdminControls);
        if ("MEMBER".equals(session.getRole()) && layoutAdminControls != null) {
            layoutAdminControls.setVisibility(View.GONE);
        }

        // QUICK CHANDA BUTTON LOGIC
        Button btnQuickDonation = findViewById(R.id.btnQuickDonation);
        if (btnQuickDonation != null && ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole()))) {
            btnQuickDonation.setVisibility(View.VISIBLE);
            btnQuickDonation.setOnClickListener(v -> showQuickDonationDialog());
        }

        loadMemberData();

        Button btnExport = findViewById(R.id.btnExportProfilePdf);
        if(btnExport != null) {
            btnExport.setOnClickListener(v -> {
                if (currentMember != null) {
                    PdfReportService.generateMemberProfile(this, session.getCommunityName(), currentMember);
                }
            });
        }

        Button btnPromote = findViewById(R.id.btnPromote);
        if(btnPromote != null) btnPromote.setOnClickListener(v -> changeRole("MANAGER"));
        
        Button btnDemote = findViewById(R.id.btnDemote);
        if(btnDemote != null) btnDemote.setOnClickListener(v -> changeRole("MEMBER"));
    }

    private void loadMemberData() {
        db.child("communities").child(session.getCommunityId()).child("members").child(memberId)
          .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentMember = snapshot.getValue(Member.class);
                if (currentMember != null) { populateUI(); }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateUI() {
        TextView tvId = findViewById(R.id.tvProfileId); if(tvId != null) tvId.setText(currentMember.id);
        TextView tvName = findViewById(R.id.tvProfileName); if(tvName != null) tvName.setText(currentMember.name);
        TextView tvRole = findViewById(R.id.tvProfileRole); if(tvRole != null) tvRole.setText(currentMember.role);
        TextView tvPhone = findViewById(R.id.tvProfilePhone); if(tvPhone != null) tvPhone.setText("📞 " + currentMember.phone);
        TextView tvGotra = findViewById(R.id.tvProfileGotra); if(tvGotra != null) tvGotra.setText("🕉 Gotra: " + currentMember.gotra);
        TextView tvBlood = findViewById(R.id.tvProfileBlood); if(tvBlood != null) tvBlood.setText("🩸 Blood Group: " + currentMember.bloodGroup);
        TextView tvDonated = findViewById(R.id.tvProfileDonated); if(tvDonated != null) tvDonated.setText("Total Donated: ৳" + currentMember.totalDonated);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        TextView tvJoined = findViewById(R.id.tvProfileJoined); 
        if(tvJoined != null) tvJoined.setText("Joined: " + sdf.format(new Date(currentMember.timestamp)));

        TextView tvFather = findViewById(R.id.tvProfileFather);
        if (tvFather != null && currentMember.fatherName != null && !currentMember.fatherName.isEmpty()) { tvFather.setVisibility(View.VISIBLE); tvFather.setText("Father: " + currentMember.fatherName); }
        
        TextView tvMother = findViewById(R.id.tvProfileMother);
        if (tvMother != null && currentMember.motherName != null && !currentMember.motherName.isEmpty()) { tvMother.setVisibility(View.VISIBLE); tvMother.setText("Mother: " + currentMember.motherName); }
        
        TextView tvNid = findViewById(R.id.tvProfileNid);
        if (tvNid != null && currentMember.nid != null && !currentMember.nid.isEmpty()) { tvNid.setVisibility(View.VISIBLE); tvNid.setText("NID: " + currentMember.nid); }
        
        TextView tvAddress = findViewById(R.id.tvProfileAddress);
        if (tvAddress != null && currentMember.address != null && !currentMember.address.isEmpty()) { tvAddress.setVisibility(View.VISIBLE); tvAddress.setText("Address: " + currentMember.address); }

        TextView tvSignature = findViewById(R.id.tvProfileSignature);
        if (tvSignature != null && currentMember.addedBySignature != null && !currentMember.addedBySignature.isEmpty()) { tvSignature.setVisibility(View.VISIBLE); tvSignature.setText("✍️ Profile verified by: " + currentMember.addedBySignature); }
    }

    private void showQuickDonationDialog() {
        if (currentMember == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Chanda for " + currentMember.name);

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);
        final android.widget.EditText inputAmount = new android.widget.EditText(this); inputAmount.setHint("Amount (৳)"); inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(inputAmount);
        final android.widget.EditText inputNote = new android.widget.EditText(this); inputNote.setHint("Purpose / Note"); layout.addView(inputNote);

        builder.setView(layout);
        builder.setPositiveButton("SAVE & RECEIPT", (dialog, which) -> {
            String amtStr = inputAmount.getText().toString().trim();
            String note = inputNote.getText().toString().trim();
            if (amtStr.isEmpty()) { Toast.makeText(this, "Amount required", Toast.LENGTH_SHORT).show(); return; }

            try {
                float amount = Float.parseFloat(amtStr);
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                
                String strictSignature = "ADMIN".equals(session.getRole()) ? "Super Admin - " + session.getUserName() : "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";
                String finalName = "[Member] " + currentMember.name + " (" + currentMember.id + ")";
                
                HashMap<String, Object> transMap = new HashMap<>();
                transMap.put("name", finalName); 
                transMap.put("amount", amount); 
                transMap.put("note", note);
                transMap.put("timestamp", System.currentTimeMillis()); 
                transMap.put("loggedBy", strictSignature);

                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(transMap);
                db.child("communities").child(session.getCommunityId()).child("members").child(memberId).child("totalDonated").setValue(currentMember.totalDonated + amount);
                
                AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "CHANDA_RECORDED", "Recorded ৳" + amount + " from profile: " + currentMember.name);
                Toast.makeText(this, "Chanda Added Successfully!", Toast.LENGTH_SHORT).show();

                String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                String finalNote = note + "\n✍️ Collected By: " + strictSignature;
                PdfReportService.generateDonorReceipt(this, session.getCommunityName(), finalName, amount, finalNote, dateStr);

            } catch (Exception e) {}
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }

    private void changeRole(String newRole) {
        if ("ADMIN".equals(currentMember.role)) { Toast.makeText(this, "Cannot change Super Admin role", Toast.LENGTH_SHORT).show(); return; }
        db.child("communities").child(session.getCommunityId()).child("members").child(memberId).child("role").setValue(newRole);
        String encodedEmail = session.getWorkspaceEmail().replace(".", ",");
        db.child("login_vault").child(encodedEmail).child(memberId).child("role").setValue(newRole);
        Toast.makeText(this, "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
        AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "ROLE_CHANGED", "Changed role of " + currentMember.name + " to " + newRole);
    }
}
