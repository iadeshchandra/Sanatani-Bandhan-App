package org.shda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    
    private LinearLayout layoutAdminLogin, layoutStaffLogin;
    private EditText inputAdminEmail, inputAdminPassword;
    private EditText inputWorkspace, inputUserId, inputPin;
    private Button btnAdminLogin, btnStaffLogin;
    private TextView tvToggleLoginMode;
    
    private boolean isAdminMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("SanataniPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("IS_LOGGED_IN", false)) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish(); return;
        }

        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        layoutAdminLogin = findViewById(R.id.layoutAdminLogin);
        layoutStaffLogin = findViewById(R.id.layoutStaffLogin);
        inputAdminEmail = findViewById(R.id.inputAdminEmail);
        inputAdminPassword = findViewById(R.id.inputAdminPassword);
        
        inputWorkspace = findViewById(R.id.inputWorkspace);
        inputUserId = findViewById(R.id.inputUserId);
        inputPin = findViewById(R.id.inputPin);
        
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        btnStaffLogin = findViewById(R.id.btnStaffLogin);
        tvToggleLoginMode = findViewById(R.id.tvToggleLoginMode);

        btnAdminLogin.setOnClickListener(v -> performAdminLogin());
        btnStaffLogin.setOnClickListener(v -> performStaffLogin());
        
        tvToggleLoginMode.setOnClickListener(v -> toggleLoginMode());
        findViewById(R.id.tvCreateWorkspace).setOnClickListener(v -> startActivity(new Intent(this, RegisterCommunityActivity.class)));
    }

    private void toggleLoginMode() {
        isAdminMode = !isAdminMode;
        if (isAdminMode) {
            layoutAdminLogin.setVisibility(View.VISIBLE);
            layoutStaffLogin.setVisibility(View.GONE);
            tvToggleLoginMode.setText("Switch to Staff / Member Login");
        } else {
            layoutAdminLogin.setVisibility(View.GONE);
            layoutStaffLogin.setVisibility(View.VISIBLE);
            tvToggleLoginMode.setText("Switch to Super Admin Login");
        }
    }

    // ✨ ADMINS: Email + Password Only
    private void performAdminLogin() {
        String email = inputAdminEmail.getText().toString().trim();
        String password = inputAdminPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter Email and Password", Toast.LENGTH_SHORT).show(); return;
        }

        btnAdminLogin.setEnabled(false); btnAdminLogin.setText("AUTHENTICATING...");

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                db.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            SharedPreferences.Editor editor = getSharedPreferences("SanataniPrefs", MODE_PRIVATE).edit();
                            editor.putBoolean("IS_LOGGED_IN", true);
                            editor.putString("USER_ID", "ADMIN-001");
                            editor.putString("USER_NAME", snapshot.child("name").getValue(String.class));
                            editor.putString("ROLE", snapshot.child("role").getValue(String.class));
                            editor.putString("COMMUNITY_ID", snapshot.child("communityId").getValue(String.class));
                            editor.putString("COMMUNITY_NAME", snapshot.child("communityName").getValue(String.class));
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Admin Profile not found.", Toast.LENGTH_SHORT).show();
                            btnAdminLogin.setEnabled(true); btnAdminLogin.setText("ADMIN LOGIN");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        btnAdminLogin.setEnabled(true); btnAdminLogin.setText("ADMIN LOGIN");
                    }
                });
            } else {
                Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                btnAdminLogin.setEnabled(true); btnAdminLogin.setText("ADMIN LOGIN");
            }
        });
    }

    // ✨ STAFF: Workspace ID/Email + User ID + User PIN
    private void performStaffLogin() {
        String workspaceInput = inputWorkspace.getText().toString().trim();
        String userId = inputUserId.getText().toString().trim();
        String pin = inputPin.getText().toString().trim();

        if (workspaceInput.isEmpty() || userId.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); return;
        }

        btnStaffLogin.setEnabled(false); btnStaffLogin.setText("AUTHENTICATING...");

        db.child("communities").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String targetCommunityId = null; String targetCommunityName = null;
                for (DataSnapshot comm : snapshot.getChildren()) {
                    String commId = comm.getKey();
                    String commEmail = comm.child("info").child("email").getValue(String.class);
                    if (workspaceInput.equalsIgnoreCase(commId) || workspaceInput.equalsIgnoreCase(commEmail)) {
                        targetCommunityId = commId;
                        targetCommunityName = comm.child("name").getValue(String.class); break;
                    }
                }

                if (targetCommunityId == null) {
                    Toast.makeText(LoginActivity.this, "Workspace ID or Email not found.", Toast.LENGTH_LONG).show();
                    btnStaffLogin.setEnabled(true); btnStaffLogin.setText("STAFF / MEMBER LOGIN"); return;
                }

                String dbPin = snapshot.child(targetCommunityId).child("logins").child(userId).getValue(String.class);
                if (dbPin != null && dbPin.equals(pin)) {
                    Member m = snapshot.child(targetCommunityId).child("members").child(userId).getValue(Member.class);
                    if (m != null) {
                        SharedPreferences.Editor editor = getSharedPreferences("SanataniPrefs", MODE_PRIVATE).edit();
                        editor.putBoolean("IS_LOGGED_IN", true);
                        editor.putString("USER_ID", m.id);
                        editor.putString("USER_NAME", m.name);
                        editor.putString("ROLE", m.role);
                        editor.putString("COMMUNITY_ID", targetCommunityId);
                        editor.putString("COMMUNITY_NAME", targetCommunityName);
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid User ID or User PIN.", Toast.LENGTH_SHORT).show();
                    btnStaffLogin.setEnabled(true); btnStaffLogin.setText("STAFF / MEMBER LOGIN");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { btnStaffLogin.setEnabled(true); btnStaffLogin.setText("STAFF / MEMBER LOGIN"); }
        });
    }
}
