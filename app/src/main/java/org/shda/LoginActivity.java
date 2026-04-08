package org.shda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private DatabaseReference db;
    private EditText inputWorkspace, inputUserId, inputPin;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auto-login check
        SharedPreferences prefs = getSharedPreferences("SanataniPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("IS_LOGGED_IN", false)) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish(); return;
        }

        setContentView(R.layout.activity_login);
        db = FirebaseDatabase.getInstance().getReference();

        inputWorkspace = findViewById(R.id.inputWorkspace);
        inputUserId = findViewById(R.id.inputUserId);
        inputPin = findViewById(R.id.inputPin);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> performLogin());
        
        // ✨ FIX: Routing updated to your correct RegisterCommunityActivity file!
        findViewById(R.id.tvCreateWorkspace).setOnClickListener(v -> startActivity(new Intent(this, RegisterCommunityActivity.class)));
    }

    private void performLogin() {
        String workspaceInput = inputWorkspace.getText().toString().trim();
        String userId = inputUserId.getText().toString().trim();
        String pin = inputPin.getText().toString().trim();

        if (workspaceInput.isEmpty() || userId.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show(); return;
        }

        btnLogin.setEnabled(false); btnLogin.setText("AUTHENTICATING...");

        db.child("communities").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String targetCommunityId = null;
                String targetCommunityName = null;

                // 1. Scan for matching Workspace ID or Admin Email
                for (DataSnapshot comm : snapshot.getChildren()) {
                    String commId = comm.getKey();
                    String commEmail = comm.child("info").child("email").getValue(String.class);
                    if (workspaceInput.equalsIgnoreCase(commId) || workspaceInput.equalsIgnoreCase(commEmail)) {
                        targetCommunityId = commId;
                        targetCommunityName = comm.child("name").getValue(String.class);
                        break;
                    }
                }

                if (targetCommunityId == null) {
                    Toast.makeText(LoginActivity.this, "Workspace not found.", Toast.LENGTH_LONG).show();
                    resetButton(); return;
                }

                // 2. Validate PIN
                String dbPin = snapshot.child(targetCommunityId).child("logins").child(userId).getValue(String.class);
                if (dbPin != null && dbPin.equals(pin)) {
                    
                    // 3. Fetch Member Details
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
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Devotee Profile not found.", Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid ID or PIN.", Toast.LENGTH_SHORT).show();
                    resetButton();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { resetButton(); }
        });
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("SECURE LOGIN");
    }
}
