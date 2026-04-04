package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private SessionManager session;
    private EditText inputEmail, inputSbId, inputPassword;
    private RadioButton radioAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        inputEmail = findViewById(R.id.inputEmail);
        inputSbId = findViewById(R.id.inputSbId);
        inputPassword = findViewById(R.id.inputPassword);
        radioAdmin = findViewById(R.id.radioAdmin);
        RadioGroup radioGroupLoginType = findViewById(R.id.radioGroupLoginType);

        radioGroupLoginType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAdmin) {
                inputSbId.setVisibility(View.GONE); inputEmail.setHint("Admin Email");
            } else {
                inputSbId.setVisibility(View.VISIBLE); inputEmail.setHint("Workspace Admin Email");
            }
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> processLogin());
        findViewById(R.id.btnGoToRegister).setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterCommunityActivity.class)));
    }

    private void processLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String sbId = inputSbId.getText().toString().trim().toUpperCase();

        if (email.isEmpty() || password.isEmpty()) { Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show(); return; }

        if (radioAdmin.isChecked()) {
            // ADMIN LOGIN
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    db.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String commId = snapshot.child("communityId").getValue(String.class);
                            String commName = snapshot.child("communityName").getValue(String.class);
                            session.createLoginSession(commId, "ADMIN", commName, "Super Admin", "ADMIN-001", email);
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else Toast.makeText(this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show();
            });
        } else {
            // STAFF/MEMBER SECURE LOGIN
            if (sbId.isEmpty()) { Toast.makeText(this, "Enter SB-ID", Toast.LENGTH_SHORT).show(); return; }
            String encodedEmail = email.replace(".", ","); // Firebase keys cannot contain dots

            mAuth.signInAnonymously().addOnCompleteListener(anonTask -> {
                if (anonTask.isSuccessful()) {
                    String tempUid = mAuth.getCurrentUser().getUid();
                    
                    // Look inside the isolated Login Vault
                    db.child("login_vault").child(encodedEmail).child(sbId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap) {
                            if (snap.exists()) {
                                String dbPass = snap.child("password").getValue(String.class);
                                if (dbPass != null && dbPass.equals(password)) {
                                    String commId = snap.child("communityId").getValue(String.class);
                                    String role = snap.child("role").getValue(String.class);
                                    String name = snap.child("name").getValue(String.class);
                                    String commName = snap.child("communityName").getValue(String.class);

                                    // VITAL SECURITY STEP: Link this temporary UID to the Community ID
                                    HashMap<String, Object> userMap = new HashMap<>();
                                    userMap.put("communityId", commId);
                                    userMap.put("role", role);
                                    
                                    db.child("users").child(tempUid).setValue(userMap).addOnSuccessListener(aVoid -> {
                                        // Once mapped, the Firebase Rules will now allow access to the Community folder!
                                        session.createLoginSession(commId, role, commName, name, sbId, email);
                                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                        finish();
                                    });
                                } else Toast.makeText(LoginActivity.this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                            } else Toast.makeText(LoginActivity.this, "SB-ID not found in this Workspace", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
