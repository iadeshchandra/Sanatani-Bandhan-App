package org.shda;

import android.os.Bundle;
import android.widget.Button;
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
    
    private TextView tvMyName, tvMyId, tvMyDonated;
    private EditText inputMyPhone, inputMyGotra, inputMyBlood, inputMyAddress;
    private Button btnUpdateProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getUserId() == null) { finish(); return; }

        tvMyName = findViewById(R.id.tvMyName);
        tvMyId = findViewById(R.id.tvMyId);
        tvMyDonated = findViewById(R.id.tvMyDonated);
        
        inputMyPhone = findViewById(R.id.inputMyPhone);
        inputMyGotra = findViewById(R.id.inputMyGotra);
        inputMyBlood = findViewById(R.id.inputMyBlood);
        inputMyAddress = findViewById(R.id.inputMyAddress);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);

        loadMyData();

        btnUpdateProfile.setOnClickListener(v -> updateMyData());
    }

    private void loadMyData() {
        DatabaseReference myRef = db.child("communities").child(session.getCommunityId()).child("members").child(session.getUserId());
        myRef.keepSynced(true);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Member m = snapshot.getValue(Member.class);
                if (m != null) {
                    tvMyName.setText(m.name);
                    tvMyId.setText("ID: " + m.id + " | Role: " + m.role);
                    tvMyDonated.setText("Lifetime Donated: ৳" + m.totalDonated);
                    
                    if (m.phone != null) inputMyPhone.setText(m.phone);
                    if (m.gotra != null) inputMyGotra.setText(m.gotra);
                    if (m.bloodGroup != null) inputMyBlood.setText(m.bloodGroup);
                    if (m.address != null) inputMyAddress.setText(m.address);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMyData() {
        btnUpdateProfile.setEnabled(false);
        btnUpdateProfile.setText("Saving...");

        DatabaseReference myRef = db.child("communities").child(session.getCommunityId()).child("members").child(session.getUserId());
        
        myRef.child("phone").setValue(inputMyPhone.getText().toString().trim());
        myRef.child("gotra").setValue(inputMyGotra.getText().toString().trim());
        myRef.child("bloodGroup").setValue(inputMyBlood.getText().toString().trim());
        myRef.child("address").setValue(inputMyAddress.getText().toString().trim());

        logAudit("SELF_UPDATE", session.getUserName() + " updated their own Devotee Profile.");

        Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
        
        // Re-enable button
        new android.os.Handler().postDelayed(() -> {
            btnUpdateProfile.setEnabled(true);
            btnUpdateProfile.setText("💾 UPDATE MY PROFILE");
        }, 2000);
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName() + " (Self)");
        auditMap.put("actionType", actionType);
        auditMap.put("description", description);
        auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }
}
