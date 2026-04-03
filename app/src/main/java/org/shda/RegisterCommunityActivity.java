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
    private FirebaseAuth auth;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_community);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        EditText inputOrgName = findViewById(R.id.inputCommunityName);
        EditText inputEmail = findViewById(R.id.inputAdminEmail);
        EditText inputPassword = findViewById(R.id.inputAdminPassword);
        Button btnRegister = findViewById(R.id.btnRegisterWorkspace);

        btnRegister.setOnClickListener(v -> {
            String orgName = inputOrgName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (orgName.isEmpty() || email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Please fill all fields (Password min 6 chars)", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Creating Workspace...", Toast.LENGTH_SHORT).show();

            // 1. Create the user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = auth.getCurrentUser().getUid();
                    
                    // 2. Generate a unique ID for this specific community
                    String communityId = db.child("communities").push().getKey();

                    // 3. Save Community Settings
                    HashMap<String, Object> orgData = new HashMap<>();
                    orgData.put("communityName", orgName);
                    orgData.put("createdAt", System.currentTimeMillis());
                    db.child("communities").child(communityId).child("settings").setValue(orgData);

                    // 4. Save User Profile with ADMIN role and link to communityId
                    HashMap<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("role", "ADMIN"); // Highest level access
                    userData.put("communityId", communityId);
                    db.child("users").child(uid).setValue(userData);

                    Toast.makeText(this, "Workspace Created! Please Login.", Toast.LENGTH_LONG).show();
                    finish(); // Return to Login Screen
                } else {
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
