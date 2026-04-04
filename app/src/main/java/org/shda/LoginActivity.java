package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

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
                inputSbId.setVisibility(View.GONE);
                inputEmail.setHint("Admin Email");
            } else {
                inputSbId.setVisibility(View.VISIBLE);
                inputEmail.setHint("Workspace Admin Email");
            }
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> processLogin());
        findViewById(R.id.btnGoToRegister).setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterCommunityActivity.class)));

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            if (email.isEmpty()) Toast.makeText(this, "Enter Admin Email first", Toast.LENGTH_SHORT).show();
            else mAuth.sendPasswordResetEmail(email).addOnSuccessListener(aVoid -> Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show());
        });
    }

    private void processLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String sbId = inputSbId.getText().toString().trim().toUpperCase();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (radioAdmin.isChecked()) {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    db.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String commId = snapshot.child("communityId").getValue(String.class);
                            String commName = snapshot.child("communityName").getValue(String.class);
                            session.createLoginSession(commId, "ADMIN", commName, "Super Admin", "ADMIN-001");
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else Toast.makeText(this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show();
            });
        } else {
            if (sbId.isEmpty()) { Toast.makeText(this, "Please enter your SB-ID", Toast.LENGTH_SHORT).show(); return; }

            db.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String commId = null, commName = null;
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            commId = userSnap.child("communityId").getValue(String.class);
                            commName = userSnap.child("communityName").getValue(String.class);
                            break;
                        }
                        
                        if (commId != null) {
                            final String finalCommName = commName;
                            final String finalCommId = commId;
                            db.child("communities").child(finalCommId).child("members").child(sbId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot memSnap) {
                                    if (memSnap.exists()) {
                                        String dbPassword = memSnap.child("password").getValue(String.class);
                                        String role = memSnap.child("role").getValue(String.class);
                                        String name = memSnap.child("name").getValue(String.class);

                                        if (dbPassword != null && dbPassword.equals(password)) {
                                            session.createLoginSession(finalCommId, role != null ? role : "MEMBER", finalCommName, name, sbId);
                                            AuditLogger.logAction(finalCommId, name, "LOGIN", "Logged into portal");
                                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                            finish();
                                        } else Toast.makeText(LoginActivity.this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                                    } else Toast.makeText(LoginActivity.this, "SB-ID not found in this Workspace", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        }
                    } else Toast.makeText(LoginActivity.this, "Workspace not found", Toast.LENGTH_SHORT).show();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}
