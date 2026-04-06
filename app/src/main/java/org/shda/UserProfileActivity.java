package org.shda;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.HashMap;

public class UserProfileActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private Member currentMember;

    private TextView tvNameId, tvRole;
    private EditText editPhone, editGotra, editBlood, editAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile); 

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        tvNameId = findViewById(R.id.tvProfileNameId);
        tvRole = findViewById(R.id.tvProfileRole);
        editPhone = findViewById(R.id.editProfilePhone);
        editGotra = findViewById(R.id.editProfileGotra);
        editBlood = findViewById(R.id.editProfileBlood);
        editAddress = findViewById(R.id.editProfileAddress);

        loadUserData();

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileChanges());
        findViewById(R.id.btnDownloadMyPdf).setOnClickListener(v -> {
            if (currentMember != null) {
                PdfReportService.generateMemberProfile(this, session.getCommunityName(), currentMember);
            } else {
                Toast.makeText(this, "Profile data loading...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData() {
        if (session.getUserId() == null) return;
        DatabaseReference userRef = db.child("communities").child(session.getCommunityId()).child("members").child(session.getUserId());
        userRef.keepSynced(true); 
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
              @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                  currentMember = snapshot.getValue(Member.class);
                  if (currentMember != null) {
                      tvNameId.setText(currentMember.name + " (" + currentMember.id + ")");
                      tvRole.setText("Assigned Role: " + currentMember.role);
                      
                      editPhone.setText(currentMember.phone != null ? currentMember.phone : "");
                      editGotra.setText(currentMember.gotra != null ? currentMember.gotra : "");
                      editBlood.setText(currentMember.bloodGroup != null ? currentMember.bloodGroup : "");
                      editAddress.setText(currentMember.address != null ? currentMember.address : "");
                  }
              }
              @Override public void onCancelled(@NonNull DatabaseError error) {}
          });
    }

    private void saveProfileChanges() {
        if (currentMember == null) return;
        
        String newPhone = editPhone.getText().toString().trim();
        String newGotra = editGotra.getText().toString().trim();
        String newBlood = editBlood.getText().toString().trim();
        String newAddress = editAddress.getText().toString().trim();

        DatabaseReference userRef = db.child("communities").child(session.getCommunityId()).child("members").child(session.getUserId());
        
        // ✨ FIXED AUDIT LOG PUSH
        String changes = "Profile self-updated. New Data -> Phone: " + newPhone + ", Gotra: " + newGotra + ", Blood: " + newBlood;
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName());
        auditMap.put("actionType", "SELF_UPDATE");
        auditMap.put("description", changes);
        auditMap.put("timestamp", System.currentTimeMillis());
        
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);

        // Update active profile silently
        userRef.child("phone").setValue(newPhone);
        userRef.child("gotra").setValue(newGotra);
        userRef.child("bloodGroup").setValue(newBlood);
        userRef.child("address").setValue(newAddress);

        Toast.makeText(this, "Profile Saved & Synced Successfully!", Toast.LENGTH_SHORT).show();
    }
}
