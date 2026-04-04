package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        // Ensure these IDs match your XML layout
        EditText inputCommName = findViewById(R.id.inputCommName);
        EditText inputAdminName = findViewById(R.id.inputAdminName);
        EditText inputEmail = findViewById(R.id.inputEmail);
        EditText inputPassword = findViewById(R.id.inputPassword);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String commName = inputCommName.getText().toString().trim();
            String adminName = inputAdminName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (commName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Creating Secure Workspace...", Toast.LENGTH_LONG).show();

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    
                    // 1. GENERATE UNIQUE COMMUNITY ID
                    String commId = db.child("communities").push().getKey(); 

                    // 2. Initialize Community Settings
                    HashMap<String, Object> commMap = new HashMap<>();
                    commMap.put("id", commId);
                    commMap.put("name", commName);
                    commMap.put("createdAt", System.currentTimeMillis());
                    db.child("communities").child(commId).child("metadata").setValue(commMap);
                    
                    // Set member ID starting point
                    db.child("communities").child(commId).child("metadata").child("lastMemberId").setValue(1000);

                    // 3. Create Super Admin Profile
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("communityId", commId);
                    userMap.put("communityName", commName);
                    userMap.put("role", "ADMIN");
                    userMap.put("name", adminName);
                    userMap.put("email", email);

                    db.child("users").child(uid).setValue(userMap).addOnSuccessListener(aVoid -> {
                        // 4. THE FIX: Instantly inject the new ID and Name into memory
                        session.createLoginSession(commId, "ADMIN", commName, adminName, "ADMIN-001", email);
                        
                        Toast.makeText(this, "Registration Complete!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterCommunityActivity.this, DashboardActivity.class));
                        finish();
                    });

                } else {
                    Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
