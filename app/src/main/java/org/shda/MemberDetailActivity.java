package org.shda;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemberDetailActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private Member currentMember;
    private String memberId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        memberId = getIntent().getStringExtra("MEMBER_ID");
        if (memberId == null) { finish(); return; }

        // Role-Based UI: Only Super Admins see the Role Management buttons
        LinearLayout adminRoleLayout = findViewById(R.id.adminRoleLayout);
        if ("ADMIN".equals(session.getRole())) {
            adminRoleLayout.setVisibility(View.VISIBLE);
        }

        loadMemberData(memberId);

        findViewById(R.id.btnDownloadProfile).setOnClickListener(v -> {
            if (currentMember != null) {
                PdfReportService.generateMemberProfile(this, session.getCommunityName(), currentMember);
            }
        });

        // Admin Actions
        findViewById(R.id.btnMakeManager).setOnClickListener(v -> updateMemberRole("MANAGER"));
        findViewById(R.id.btnMakeMember).setOnClickListener(v -> updateMemberRole("MEMBER"));
    }

    private void loadMemberData(String mId) {
        db.child("communities").child(session.getCommunityId()).child("members").child(mId)
          .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentMember = snapshot.getValue(Member.class);
                if (currentMember != null) {
                    ((TextView) findViewById(R.id.tvDetailId)).setText(currentMember.id);
                    ((TextView) findViewById(R.id.tvDetailName)).setText(currentMember.name);
                    ((TextView) findViewById(R.id.tvDetailPhone)).setText("📞 " + currentMember.phone);
                    ((TextView) findViewById(R.id.tvDetailGotra)).setText("🕉 Gotra: " + currentMember.gotra);
                    ((TextView) findViewById(R.id.tvDetailBlood)).setText("🩸 Blood Group: " + currentMember.bloodGroup);
                    
                    // Display Current Role
                    String role = currentMember.role != null ? currentMember.role : "MEMBER";
                    ((TextView) findViewById(R.id.tvDetailRole)).setText(role);
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    ((TextView) findViewById(R.id.tvDetailJoinDate)).setText("Joined: " + sdf.format(new Date(currentMember.timestamp)));
                    ((TextView) findViewById(R.id.tvDetailTotal)).setText("Total Donated: ৳" + currentMember.totalDonated);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateMemberRole(String newRole) {
        if (currentMember != null) {
            String commId = session.getCommunityId();
            
            db.child("communities").child(commId).child("members").child(memberId).child("role").setValue(newRole)
              .addOnSuccessListener(aVoid -> {
                  Toast.makeText(this, currentMember.name + " is now a " + newRole, Toast.LENGTH_SHORT).show();
                  
                  // Log the admin's action in the security audit
                  AuditLogger.logAction(commId, session.getUserName(), "ROLE_CHANGED", "Changed " + currentMember.name + "'s role to " + newRole);
                  
                  // Reload the data to reflect changes on screen
                  loadMemberData(memberId);
              });
        }
    }
}
