package org.shda;

import android.content.Intent;
import android.os.Bundle;
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
        
        // ✨ FIX: Properly checks your SessionManager to stop the White Screen Loop!
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
        findViewById(R.id.tvCreateWorkspace).setOnClickListener(v -> startActivity(new Intent(this, RegisterCommunityActivity.class)));
    }

    private void performLogin() {
        String workspace = inputWorkspace.getText().toString().trim();
        String userId = inputUserId.getText().toString().trim();
        String secret = inputPin.getText().toString().trim(); // This acts as Password OR Pin

        if (workspace.isEmpty() || secret.isEmpty()) {
            Toast.makeText(this, "Workspace/Email and Password/PIN are required", Toast.LENGTH_SHORT).show(); return;
        }

        btnLogin.setEnabled(false); btnLogin.setText("AUTHENTICATING...");

        // ✨ SMART ROUTING: If Devotee ID is empty, treat as Super Admin Login!
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
                                
                                // ✨ FIX: Uses your exact method to securely write to memory and stop crashes!
                                session.createLoginSession(commId, role, commName, name, "ADMIN-001", workspace);
                                
                                Toast.makeText(LoginActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                            } else { fail("Admin Profile missing."); }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { fail("Database Error"); }
                    });
                } else { fail("Invalid Admin Email or Password."); }
            });
            
        } else {
            // ✨ SMART ROUTING: If Devotee ID is typed, treat as Staff PIN Login!
            db.child("communities").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String targetCommId = null; String targetCommName = null;
                    
                    for (DataSnapshot comm : snapshot.getChildren()) {
                        String cId = comm.getKey();
                        String cEmail = comm.child("info").child("email").getValue(String.class);
                        if (workspace.equalsIgnoreCase(cId) || workspace.equalsIgnoreCase(cEmail)) {
                            targetCommId = cId; targetCommName = comm.child("name").getValue(String.class); break;
                        }
                    }
                    if (targetCommId == null) { fail("Workspace not found."); return; }

                    String dbPin = snapshot.child(targetCommId).child("logins").child(userId).getValue(String.class);
                    if (dbPin != null && dbPin.equals(secret)) {
                        Member m = snapshot.child(targetCommId).child("members").child(userId).getValue(Member.class);
                        if (m != null) {
                            
                            // ✨ FIX: Uses your exact method to securely write to memory!
                            session.createLoginSession(targetCommId, m.role, targetCommName, m.name, m.id, m.email != null ? m.email : "");
                            
                            Toast.makeText(LoginActivity.this, "Staff Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class)); finish();
                        } else { fail("Devotee Profile not found."); }
                    } else { fail("Invalid Devotee ID or PIN."); }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { fail("Database Error"); }
            });
        }
    }

    private void fail(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        btnLogin.setEnabled(true); btnLogin.setText("SECURE LOGIN");
    }
}
