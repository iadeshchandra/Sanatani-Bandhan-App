package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class RegisterCommunityActivity extends AppCompatActivity {

    private DatabaseReference db;
    private EditText inputOrgName, inputAdminName, inputAdminEmail, inputAdminPhone, inputAdminPin;
    private RadioGroup rgWorkspaceType;
    private Button btnCreateWorkspace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // We use Realtime Database directly for the Custom PIN Auth!
        db = FirebaseDatabase.getInstance().getReference();

        inputOrgName = findViewById(R.id.inputOrgName);
        inputAdminName = findViewById(R.id.inputAdminName);
        inputAdminEmail = findViewById(R.id.inputAdminEmail);
        inputAdminPhone = findViewById(R.id.inputAdminPhone);
        inputAdminPin = findViewById(R.id.inputAdminPin);
        rgWorkspaceType = findViewById(R.id.rgWorkspaceType);
        btnCreateWorkspace = findViewById(R.id.btnCreateWorkspace);

        btnCreateWorkspace.setOnClickListener(v -> registerWorkspace());
        
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> { 
            startActivity(new Intent(this, LoginActivity.class)); 
            finish(); 
        });
    }

    private void registerWorkspace() {
        String orgName = inputOrgName.getText().toString().trim();
        String adminName = inputAdminName.getText().toString().trim();
        String email = inputAdminEmail.getText().toString().trim();
        String phone = inputAdminPhone.getText().toString().trim();
        String pin = inputAdminPin.getText().toString().trim();

        if (orgName.isEmpty() || adminName.isEmpty() || email.isEmpty() || pin.length() < 4) {
            Toast.makeText(this, "Please fill all required fields and set a 4-digit PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateWorkspace.setEnabled(false);
        btnCreateWorkspace.setText("GENERATING WORKSPACE...");

        // Auto-Generate Unique Workspace ID based on selection
        String type = rgWorkspaceType.getCheckedRadioButtonId() == R.id.rbMandir ? "Mandir" : "Community";
        String prefix = type.equals("Mandir") ? "MND-" : "ORG-";
        String workspaceId = prefix + (1000 + new java.util.Random().nextInt(9000));
        
        // Auto-Generate Super Admin ID
        String adminId = "ADMIN-" + (100 + new java.util.Random().nextInt(900));

        // 1. Create Organization Info
        db.child("communities").child(workspaceId).child("name").setValue(orgName);
        HashMap<String, Object> infoMap = new HashMap<>();
        infoMap.put("type", type); 
        infoMap.put("email", email); 
        infoMap.put("phone", phone);
        db.child("communities").child(workspaceId).child("info").setValue(infoMap);

        // 2. Create Super Admin Profile
        HashMap<String, Object> adminMap = new HashMap<>();
        adminMap.put("id", adminId); 
        adminMap.put("name", adminName); 
        adminMap.put("email", email);
        adminMap.put("phone", phone); 
        adminMap.put("role", "ADMIN"); 
        adminMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(workspaceId).child("members").child(adminId).setValue(adminMap);

        // 3. Save Secure PIN logic
        db.child("communities").child(workspaceId).child("logins").child(adminId).setValue(pin);

        Toast.makeText(this, "Workspace " + workspaceId + " created successfully!", Toast.LENGTH_LONG).show();

        // Redirect back to Login Page to test the new credentials
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
