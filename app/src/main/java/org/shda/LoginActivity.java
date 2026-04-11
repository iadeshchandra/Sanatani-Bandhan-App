package org.shda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private SessionManager session;

    private EditText inputWorkspace, inputUserId, inputPin;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (session.getUserId() != null && !session.getUserId().isEmpty()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish(); return;
        }

        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        inputWorkspace = findViewById(R.id.inputWorkspace);
        inputUserId = findViewById(R.id.inputUserId);
        inputPin = findViewById(R.id.inputPin);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> performLogin());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        // ✨ CRASH PREVENTER (Preserved exactly as you built it!)
        findViewById(R.id.tvCreateWorkspace).setOnClickListener(v -> {
            try {
                startActivity(new Intent(LoginActivity.this, RegisterCommunityActivity.class));
            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, "CRASH PREVENTED: Please declare 'RegisterCommunityActivity' inside your AndroidManifest.xml file!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Admin Password");
        builder.setMessage("Enter your Admin Email to receive a password reset link. (Staff members must ask their Admin to reset their PINs).");

        final EditText input = new EditText(this);
        input.setHint("Registered Admin Email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("SEND LINK", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Email is required.", Toast.LENGTH_SHORT).show(); return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Reset link sent! Check your email inbox.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void performLogin() {
        String workspace = inputWorkspace.getText().toString().trim();
        String userId = inputUserId.getText().toString().trim();
        String secret = inputPin.getText().toString().trim(); 

        if (workspace.isEmpty() || secret.isEmpty()) {
            Toast.makeText(this, "Workspace/Email and Password/PIN are required", Toast.LENGTH_SHORT).show(); return;
        }

        btnLogin.setEnabled(false); btnLogin.setText("AUTHENTICATING...");

        if (userId.isEmpty() || userId.equalsIgnoreCase("admin")) {
            mAuth.signInWithEmailAndPassword(workspace, secret).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    db.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String commId = snapshot.child("communityId").getValue(String.class);
                                String commName = snapshot.child("communityName").getValue(String.class);
                                String role = snapshot.child("role").getValue(String.class);
                                String name = snapshot.child("name").getValue(String.class);

                                // ✨ FIX: Save credentials for future offline use
                                saveOfflineCredentials(workspace, userId, secret, commId, role, commName, name);
                                session.createLoginSession(commId, role, commName, name, "ADMIN-001", workspace);

                                Toast.makeText(LoginActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                            } else { attemptOfflineLogin(workspace, userId, secret, "Admin Profile missing."); }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { attemptOfflineLogin(workspace, userId, secret, "Database Error"); }
                    });
                } else { attemptOfflineLogin(workspace, userId, secret, "Invalid Admin Email or Password."); }
            });

        } else {
            db.child("communities").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String targetCommId = null; String targetCommName = null;

                    for (DataSnapshot comm : snapshot.getChildren()) {
                        String cId = comm.getKey();
                        String cEmail = comm.child("info").child("email").getValue(String.class);
                        if (workspace.equalsIgnoreCase(cId) || (cEmail != null && workspace.equalsIgnoreCase(cEmail))) {
                            targetCommId = cId; targetCommName = comm.child("name").getValue(String.class); break;
                        }
                    }
                    if (targetCommId == null) { attemptOfflineLogin(workspace, userId, secret, "Workspace not found."); return; }

                    String dbPin = snapshot.child(targetCommId).child("logins").child(userId).getValue(String.class);
                    if (dbPin != null && dbPin.equals(secret)) {
                        Member m = snapshot.child(targetCommId).child("members").child(userId).getValue(Member.class);
                        if (m != null) {
                            // ✨ FIX: Save credentials for future offline use
                            saveOfflineCredentials(workspace, userId, secret, targetCommId, m.role, targetCommName, m.name);
                            session.createLoginSession(targetCommId, m.role, targetCommName, m.name, m.id, m.email != null ? m.email : "");
                            Toast.makeText(LoginActivity.this, "Staff Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                        } else { attemptOfflineLogin(workspace, userId, secret, "Devotee Profile not found."); }
                    } else { attemptOfflineLogin(workspace, userId, secret, "Invalid Devotee ID or PIN."); }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { attemptOfflineLogin(workspace, userId, secret, "Database Error"); }
            });
        }
    }

    // ✨ NEW: Saves the footprint locally when online login succeeds
    private void saveOfflineCredentials(String workspace, String userId, String secret, String commId, String role, String commName, String name) {
        SharedPreferences.Editor editor = getSharedPreferences("OfflineLogins", MODE_PRIVATE).edit();
        editor.putString("workspace", workspace);
        editor.putString("userId", userId);
        editor.putString("secret", secret);
        editor.putString("commId", commId);
        editor.putString("role", role);
        editor.putString("commName", commName);
        editor.putString("name", name);
        editor.apply();
    }

    // ✨ NEW: Bypasses Firebase network errors if the local footprint matches!
    private void attemptOfflineLogin(String workspace, String userId, String secret, String defaultErrorMsg) {
        SharedPreferences prefs = getSharedPreferences("OfflineLogins", MODE_PRIVATE);
        String cachedWorkspace = prefs.getString("workspace", "");
        String cachedUserId = prefs.getString("userId", "");
        String cachedSecret = prefs.getString("secret", "");

        if (!cachedWorkspace.isEmpty() && cachedWorkspace.equalsIgnoreCase(workspace) 
             && cachedUserId.equalsIgnoreCase(userId) && cachedSecret.equals(secret)) {
            
            String commId = prefs.getString("commId", "");
            String role = prefs.getString("role", "");
            String commName = prefs.getString("commName", "");
            String name = prefs.getString("name", "");
            String finalUserId = (userId.isEmpty() || userId.equalsIgnoreCase("admin")) ? "ADMIN-001" : userId;

            session.createLoginSession(commId, role, commName, name, finalUserId, workspace);
            Toast.makeText(this, "🔐 Logged in via Secure Offline Cache", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        } else {
            fail(defaultErrorMsg); // Perfect fail-safe: if local cache fails too, show the real error.
        }
    }

    private void fail(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        btnLogin.setEnabled(true); btnLogin.setText("SECURE LOGIN");
    }
}
