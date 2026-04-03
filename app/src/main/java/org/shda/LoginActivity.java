package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        // Auto-login if already logged in securely
        if (auth.getCurrentUser() != null && session.getCommunityId() != null) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        EditText inputEmail = findViewById(R.id.inputEmail); // Ensure your XML IDs match
        EditText inputPassword = findViewById(R.id.inputPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);
        TextView tvForgotPass = findViewById(R.id.tvForgotPassword);

        // FORGOT PASSWORD LOGIC
        tvForgotPass.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
            } else {
                auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) Toast.makeText(this, "Reset link sent to email!", Toast.LENGTH_LONG).show();
                });
            }
        });

        // GO TO REGISTER
        btnGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterCommunityActivity.class)));

        // LOGIN LOGIC
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) return;

            Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show();

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = auth.getCurrentUser().getUid();
                    
                    // Fetch the user's SaaS Routing Data
                    db.child("users").child(uid).get().addOnSuccessListener(snap -> {
                        if (snap.exists()) {
                            String role = snap.child("role").getValue(String.class);
                            String communityId = snap.child("communityId").getValue(String.class);
                            
                            // Fetch the Community Name for the PDFs
                            db.child("communities").child(communityId).child("settings").child("communityName").get().addOnSuccessListener(nameSnap -> {
                                String communityName = nameSnap.exists() ? nameSnap.getValue(String.class) : "Community Portal";
                                
                                // Start the Session
                                session.createSession(uid, communityId, role, communityName);
                                
                                Toast.makeText(this, "Welcome to " + communityName, Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            });
                        }
                    });
                } else {
                    Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
