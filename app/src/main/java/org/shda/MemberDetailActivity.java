package org.shda;

import android.os.Bundle;
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

public class MemberDetailActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private String memberId;
    private Member currentMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MATCHING YOUR EXACT XML FILE NAME HERE:
        setContentView(R.layout.activity_member_detail);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        memberId = getIntent().getStringExtra("MEMBER_ID");

        if (memberId == null || session.getCommunityId() == null) { finish(); return; }

        // 🔒 RBAC: Hide Super Admin Controls if the logged-in user is just a MEMBER
        LinearLayout layoutAdminControls = findViewById(R.id.layoutAdminControls);
        if ("MEMBER".equals(session.getRole()) && layoutAdminControls != null) {
            layoutAdminControls.setVisibility(View.GONE);
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

        // ✨ Reveal Optional KYC Fields if they exist
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

    private void changeRole(String newRole) {
        if ("ADMIN".equals(currentMember.role)) { Toast.makeText(this, "Cannot change Super Admin role", Toast.LENGTH_SHORT).show(); return; }
        
        db.child("communities").child(session.getCommunityId()).child("members").child(memberId).child("role").setValue(newRole);
        
        String encodedEmail = session.getWorkspaceEmail().replace(".", ",");
        db.child("login_vault").child(encodedEmail).child(memberId).child("role").setValue(newRole);
        
        Toast.makeText(this, "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
        AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "ROLE_CHANGED", "Changed role of " + currentMember.name + " to " + newRole);
    }
}
