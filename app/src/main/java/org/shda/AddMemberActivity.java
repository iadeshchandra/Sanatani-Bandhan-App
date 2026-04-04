package org.shda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Random;

public class AddMemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private long currentIdCounter = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        EditText inputName = findViewById(R.id.inputName);
        EditText inputPhone = findViewById(R.id.inputPhone);
        EditText inputGotra = findViewById(R.id.inputGotra);
        EditText inputBloodGroup = findViewById(R.id.inputBloodGroup);
        Button btnSave = findViewById(R.id.btnSaveMember);

        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();
            String gotra = inputGotra.getText().toString().trim();
            String bloodGroup = inputBloodGroup.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) { Toast.makeText(this, "Name and Phone required", Toast.LENGTH_SHORT).show(); return; }

            String autoPassword = String.format("%06d", new Random().nextInt(999999));
            String role = "MEMBER"; 
            String commId = session.getCommunityId();
            
            db.child("communities").child(commId).child("metadata").child("lastMemberId").get().addOnSuccessListener(snap -> {
                if (snap.exists()) { currentIdCounter = snap.getValue(Long.class); }
                currentIdCounter++;
                String newMemberId = "SB-" + currentIdCounter; 

                // 1. Save standard profile in the secure community folder
                Member newMember = new Member(newMemberId, name, phone, gotra, bloodGroup, System.currentTimeMillis(), role, autoPassword);
                db.child("communities").child(commId).child("members").child(newMemberId).setValue(newMember);
                db.child("communities").child(commId).child("metadata").child("lastMemberId").setValue(currentIdCounter);

                // 2. SECURITY UPGRADE: Save credentials to the Login Vault
                String encodedWorkspaceEmail = session.getWorkspaceEmail().replace(".", ",");
                HashMap<String, Object> loginData = new HashMap<>();
                loginData.put("communityId", commId);
                loginData.put("communityName", session.getCommunityName());
                loginData.put("password", autoPassword);
                loginData.put("role", role);
                loginData.put("name", name);
                
                db.child("login_vault").child(encodedWorkspaceEmail).child(newMemberId).setValue(loginData);

                // Log the action
                AuditLogger.logAction(commId, session.getUserName(), "MEMBER_ADDED", "Added " + role + ": " + name + " (" + newMemberId + ")");
                showCredentialsDialog(newMemberId, autoPassword, name);
            });
        });
    }

    private void showCredentialsDialog(String id, String password, String name) {
        new AlertDialog.Builder(this)
            .setTitle("User Created Successfully")
            .setMessage("Share these login details with " + name + ":\n\nLogin ID: " + id + "\nPassword: " + password)
            .setPositiveButton("Done", (dialog, which) -> finish())
            .setCancelable(false).show();
    }
}
