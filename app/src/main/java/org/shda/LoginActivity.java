package org.shda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private SessionManager session;

    private MaterialCardView tabDevotee, tabAdmin;
    private LinearLayout layoutDevoteeForm, layoutAdminForm;
    private TextView tvDevoteeText, tvAdminText;

    private TextInputEditText inputWorkspaceId, inputUserId, inputDevoteePin;
    private TextInputEditText inputAdminEmail, inputAdminPassword;
    private Button btnLogin;

    private boolean isAdminMode = false;

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

        tabDevotee = findViewById(R.id.tabDevotee);
        tabAdmin = findViewById(R.id.tabAdmin);
        layoutDevoteeForm = findViewById(R.id.layoutDevoteeForm);
        layoutAdminForm = findViewById(R.id.layoutAdminForm);

        tvDevoteeText = (TextView) tabDevotee.getChildAt(0);
        tvAdminText = (TextView) tabAdmin.getChildAt(0);

        inputWorkspaceId = findViewById(R.id.inputWorkspaceId);
        inputUserId = findViewById(R.id.inputUserId);
        inputDevoteePin = findViewById(R.id.inputDevoteePin);

        inputAdminEmail = findViewById(R.id.inputAdminEmail);
        inputAdminPassword = findViewById(R.id.inputAdminPassword);

        btnLogin = findViewById(R.id.btnLogin);

        tabDevotee.setOnClickListener(v -> switchTab(false));
        tabAdmin.setOnClickListener(v -> switchTab(true));

        btnLogin.setOnClickListener(v -> performLogin());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.tvCreateWorkspace).setOnClickListener(v -> {
            try {
                startActivity(new Intent(LoginActivity.this, RegisterCommunityActivity.class));
            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, "CRASH PREVENTED: Please declare 'RegisterCommunityActivity' inside your AndroidManifest.xml file!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void switchTab(boolean switchToAdmin) {
        isAdminMode = switchToAdmin;

        if (switchToAdmin) {
            layoutDevoteeForm.setVisibility(View.GONE);
            layoutAdminForm.setVisibility(View.VISIBLE);

            tabAdmin.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            tabAdmin.setCardElevation(4f);
            tvAdminText.setTextColor(Color.parseColor("#E65100"));

            tabDevotee.setCardBackgroundColor(Color.TRANSPARENT);
            tabDevotee.setCardElevation(0f);
            tvDevoteeText.setTextColor(Color.parseColor("#757575"));
        } else {
            layoutAdminForm.setVisibility(View.GONE);
            layoutDevoteeForm.setVisibility(View.VISIBLE);

            tabDevotee.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            tabDevotee.setCardElevation(4f);
            tvDevoteeText.setTextColor(Color.parseColor("#E65100"));

            tabAdmin.setCardBackgroundColor(Color.TRANSPARENT);
            tabAdmin.setCardElevation(0f);
            tvAdminText.setTextColor(Color.parseColor("#757575"));
        }
    }

    private void showForgotPasswordDialog() {
        if (!isAdminMode) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("🙏 Recover Access");
            // ✨ UPGRADED MESSAGE
            builder.setMessage("For security reasons, Devotee Passwords cannot be reset via email. Please contact your Mandir's Chief Admin to issue you a new temporary password.");
            builder.setPositiveButton("UNDERSTOOD", null);
            builder.show();
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Master Password");
        builder.setMessage("Enter your Admin Email to receive a secure password reset link.");

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
        String workspace, userId, secret;

        if (isAdminMode) {
            workspace = inputAdminEmail.getText() != null ? inputAdminEmail.getText().toString().trim() : "";
            userId = "admin"; 
            secret = inputAdminPassword.getText() != null ? inputAdminPassword.getText().toString().trim() : "";
        } else {
            workspace = inputWorkspaceId.getText() != null ? inputWorkspaceId.getText().toString().trim() : "";
            userId = inputUserId.getText() != null ? inputUserId.getText().toString().trim() : "";
            secret = inputDevoteePin.getText() != null ? inputDevoteePin.getText().toString().trim() : "";
        }

        if (workspace.isEmpty() || secret.isEmpty() || (!isAdminMode && userId.isEmpty())) {
            Toast.makeText(this, "Please fill out all required fields.", Toast.LENGTH_SHORT).show(); 
            return;
        }

        btnLogin.setEnabled(false); 
        btnLogin.setText("AUTHENTICATING...");

        if (isAdminMode || userId.equalsIgnoreCase("admin")) {
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

                                db.child("communities").child(commId).child("info").child("status").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot statusSnap) {
                                        String status = statusSnap.getValue(String.class);

                                        if ("BANNED".equalsIgnoreCase(status)) {
                                            mAuth.signOut();
                                            fail("🚫 ACCESS DENIED: Your workspace is permanently banned for violating terms.");
                                        } else if ("SUSPENDED".equalsIgnoreCase(status)) {
                                            mAuth.signOut();
                                            fail("🟠 WORKSPACE SUSPENDED: Please contact Master Support to restore access.");
                                        } else {
                                            saveOfflineCredentials(workspace, userId, secret, commId, role, commName, name);
                                            session.createLoginSession(commId, role, commName, name, "ADMIN-001", workspace);
                                            Toast.makeText(LoginActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); 
                                            finish();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        attemptOfflineLogin(workspace, userId, secret, "Security Check Failed: " + error.getMessage());
                                    }
                                });

                            } else { attemptOfflineLogin(workspace, userId, secret, "Admin Profile missing."); }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { 
                            attemptOfflineLogin(workspace, userId, secret, "Server Rejected: " + error.getMessage()); 
                        }
                    });
                } else { attemptOfflineLogin(workspace, userId, secret, "Invalid Admin Email or Password."); }
            });

        } else {
            db.child("communities").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String targetCommId = null; String targetCommName = null;
                    DataSnapshot targetCommSnap = null;

                    for (DataSnapshot comm : snapshot.getChildren()) {
                        String cId = comm.getKey();
                        String cEmail = comm.child("info").child("email").getValue(String.class);
                        if (workspace.equalsIgnoreCase(cId) || (cEmail != null && workspace.equalsIgnoreCase(cEmail))) {
                            targetCommId = cId; 
                            targetCommSnap = comm;
                            targetCommName = comm.child("info").child("communityName").getValue(String.class); 
                            if (targetCommName == null) targetCommName = "Sanatani Community";
                            break;
                        }
                    }

                    if (targetCommId == null) { attemptOfflineLogin(workspace, userId, secret, "Workspace ID not found."); return; }

                    String status = targetCommSnap.child("info").child("status").getValue(String.class);
                    if ("BANNED".equalsIgnoreCase(status)) {
                        fail("🚫 ACCESS DENIED: This Mandir has been permanently removed from the network.");
                        return;
                    }
                    if ("SUSPENDED".equalsIgnoreCase(status)) {
                        fail("🟠 WORKSPACE SUSPENDED: Please ask your Mandir Admin to restore network access.");
                        return;
                    }

                    String dbPin = targetCommSnap.child("logins").child(userId).getValue(String.class);
                    if (dbPin != null && dbPin.equals(secret)) {
                        Member m = targetCommSnap.child("members").child(userId).getValue(Member.class);
                        if (m != null) {
                            saveOfflineCredentials(workspace, userId, secret, targetCommId, m.role, targetCommName, m.name);
                            session.createLoginSession(targetCommId, m.role, targetCommName, m.name, m.id, m.email != null ? m.email : "");
                            Toast.makeText(LoginActivity.this, "🙏 Welcome to the Mandir Portal!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                        } else { 
                            // ✨ UPGRADED MESSAGING
                            attemptOfflineLogin(workspace, userId, secret, "Phone number not registered in this Workspace."); 
                        }
                    } else { 
                        // ✨ UPGRADED MESSAGING
                        attemptOfflineLogin(workspace, userId, secret, "Invalid Phone Number or Password."); 
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { 
                    attemptOfflineLogin(workspace, userId, secret, "Server Rejected: " + error.getMessage()); 
                }
            });
        }
    }

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
            Toast.makeText(this, "🔐 Logged in securely via Offline Cache", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        } else {
            fail(defaultErrorMsg); 
        }
    }

    private void fail(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        btnLogin.setEnabled(true); 
        btnLogin.setText("ENTER MANDIR");
    }
}
