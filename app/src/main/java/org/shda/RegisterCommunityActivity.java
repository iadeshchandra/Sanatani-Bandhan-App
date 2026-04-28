package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class RegisterCommunityActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private SessionManager session;
    private EditText inputCommName, inputAdminName, inputEmail, inputAdminPhone, inputPassword;
    private RadioGroup rgWorkspaceType;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_community); 

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this); 

        inputCommName = findViewById(R.id.inputCommName);
        inputAdminName = findViewById(R.id.inputAdminName);
        inputEmail = findViewById(R.id.inputEmail);
        inputAdminPhone = findViewById(R.id.inputAdminPhone);
        inputPassword = findViewById(R.id.inputPassword);
        rgWorkspaceType = findViewById(R.id.rgWorkspaceType);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerWorkspace());
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> { 
            startActivity(new Intent(this, LoginActivity.class)); 
            finish(); 
        });
    }

    private void registerWorkspace() {
        String commName = inputCommName.getText().toString().trim();
        String adminName = inputAdminName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = inputAdminPhone.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (commName.isEmpty() || adminName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show(); 
            return;
        }

        btnRegister.setEnabled(false); 
        btnRegister.setText("CREATING WORKSPACE...");

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                String uid = mAuth.getCurrentUser().getUid();

                // 1. Generate Workspace ID based on Mandir/Community
                String type = rgWorkspaceType.getCheckedRadioButtonId() == R.id.rbMandir ? "Mandir" : "Community";
                String prefix = type.equals("Mandir") ? "MND-" : "ORG-";
                String commId = prefix + (1000 + new java.util.Random().nextInt(9000));

                // 2. Initialize Community Info
                db.child("communities").child(commId).child("name").setValue(commName);
                HashMap<String, Object> infoMap = new HashMap<>();
                infoMap.put("type", type); 
                infoMap.put("email", email); 
                infoMap.put("phone", phone);
                infoMap.put("communityName", commName); // FIX: Ensures LoginActivity can grab this easily

                // ✨ FREEMIUM INJECTION: Setup default plan and counters
                infoMap.put("plan", "FREE"); 
                infoMap.put("devoteeCount", 1); // Starts at 1 because we are adding the Admin
                infoMap.put("pdfsGeneratedThisMonth", 0);
                infoMap.put("broadcastsSentThisMonth", 0);
                
                db.child("communities").child(commId).child("info").setValue(infoMap);

                // 3. Create Global Admin Record for Firebase Auth Login
                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("communityId", commId);
                userMap.put("communityName", commName);
                userMap.put("role", "ADMIN");
                userMap.put("name", adminName);
                userMap.put("email", email);
                db.child("users").child(uid).setValue(userMap);

                // 4. Create Admin Profile in Directory
                HashMap<String, Object> adminMap = new HashMap<>();
                adminMap.put("id", "ADMIN-001");
                adminMap.put("name", adminName);
                adminMap.put("email", email);
                adminMap.put("phone", phone);
                adminMap.put("role", "ADMIN");
                adminMap.put("timestamp", System.currentTimeMillis());
                db.child("communities").child(commId).child("members").child("ADMIN-001").setValue(adminMap);

                // 5. Save local session securely 
                session.createLoginSession(commId, "ADMIN", commName, adminName, "ADMIN-001", email);
                session.setPlan("FREE"); // ✨ FREEMIUM: Save the plan to offline cache!

                Toast.makeText(this, "Registration Complete!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, DashboardActivity.class)); 
                finish();
            } else {
                Toast.makeText(this, "Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true); 
                btnRegister.setText("CREATE SECURE WORKSPACE");
            }
        });
    }
}
