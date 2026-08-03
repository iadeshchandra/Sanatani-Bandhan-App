package org.shda;

import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.util.HashMap;

public class UserProfileActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    
    private TextView tvMyName, tvMyId, tvMyDonated;
    private EditText inputMyPhone, inputMyGotra, inputMyBlood, inputMyAddress;
    private Button btnUpdateProfile;
    
    // ✨ NEW BUTTON FOR CHANGING PASSWORD
    private Button btnChangePassword; 

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
        
        // ✨ Look for your new button in the XML
        btnChangePassword = findViewById(R.id.btnChangePassword); 
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

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
        
        new android.os.Handler().postDelayed(() -> {
            btnUpdateProfile.setEnabled(true);
            btnUpdateProfile.setText("💾 UPDATE MY PROFILE");
        }, 2000);
    }

    // ✨ THE NEW CHANGE PASSWORD ENGINE
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Secure Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        final EditText inputNew = new EditText(this);
        inputNew.setHint("New Password");
        inputNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNew);

        final EditText inputConfirm = new EditText(this);
        inputConfirm.setHint("Confirm Password");
        inputConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 30, 0, 0);
        inputConfirm.setLayoutParams(params);
        layout.addView(inputConfirm);

        builder.setView(layout);

        builder.setPositiveButton("UPDATE", (dialog, which) -> {
            String p1 = inputNew.getText().toString().trim();
            String p2 = inputConfirm.getText().toString().trim();

            if (p1.isEmpty() || p1.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p1.equals(p2)) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Target the Devotee's secure /logins/ node directly
            db.child("communities").child(session.getCommunityId()).child("logins").child(session.getUserId()).setValue(p1)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Password Updated Successfully!", Toast.LENGTH_SHORT).show();
                    logAudit("PASSWORD_CHANGE", session.getUserName() + " updated their secure password.");
                    
                    // Update the offline cache so they aren't locked out later
                    SharedPreferences.Editor editor = getSharedPreferences("OfflineLogins", MODE_PRIVATE).edit();
                    editor.putString("secret", p1);
                    editor.apply();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
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
