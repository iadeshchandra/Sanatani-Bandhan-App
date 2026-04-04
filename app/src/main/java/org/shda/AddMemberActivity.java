package org.shda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Random;

public class AddMemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private long currentIdCounter = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null || "MEMBER".equals(session.getRole())) { 
            Toast.makeText(this, "Access Denied. Admins and Managers only.", Toast.LENGTH_LONG).show();
            finish(); return; 
        }

        EditText inputName = findViewById(R.id.inputName);
        EditText inputPhone = findViewById(R.id.inputPhone);
        EditText inputGotra = findViewById(R.id.inputGotra);
        EditText inputBloodGroup = findViewById(R.id.inputBloodGroup);
        
        // New Optional Fields
        EditText inputFather = findViewById(R.id.inputFather);
        EditText inputMother = findViewById(R.id.inputMother);
        EditText inputNid = findViewById(R.id.inputNid);
        EditText inputAddress = findViewById(R.id.inputAddress);
        
        Button btnSave = findViewById(R.id.btnSaveMember);

        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();
            String gotra = inputGotra.getText().toString().trim();
            String bloodGroup = inputBloodGroup.getText().toString().trim();

            String father = inputFather.getText().toString().trim();
            String mother = inputMother.getText().toString().trim();
            String nid = inputNid.getText().toString().trim();
            String address = inputAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || gotra.isEmpty() || bloodGroup.isEmpty()) { 
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show(); 
                return; 
            }

            String autoPassword = String.format("%06d", new Random().nextInt(999999));
            String role = "MEMBER"; 
            String commId = session.getCommunityId();
            String signature = session.getRole() + " - " + session.getUserName();
            
            db.child("communities").child(commId).child("metadata").child("lastMemberId").get().addOnSuccessListener(snap -> {
                if (snap.exists()) { currentIdCounter = snap.getValue(Long.class); }
                currentIdCounter++;
                String newMemberId = "SB-" + currentIdCounter; 

                // Inject the new expanded model
                Member newMember = new Member(newMemberId, name, phone, gotra, bloodGroup, System.currentTimeMillis(), role, autoPassword, father, mother, nid, address, signature);
                
                db.child("communities").child(commId).child("members").child(newMemberId).setValue(newMember);
                db.child("communities").child(commId).child("metadata").child("lastMemberId").setValue(currentIdCounter);

                String encodedWorkspaceEmail = session.getWorkspaceEmail().replace(".", ",");
                HashMap<String, Object> loginData = new HashMap<>();
                loginData.put("communityId", commId);
                loginData.put("communityName", session.getCommunityName());
                loginData.put("password", autoPassword);
                loginData.put("role", role);
                loginData.put("name", name);
                
                db.child("login_vault").child(encodedWorkspaceEmail).child(newMemberId).setValue(loginData);

                AuditLogger.logAction(commId, session.getUserName(), "MEMBER_ADDED", "Added " + role + ": " + name + " (" + newMemberId + ")");
                
                showCredentialsDialog(newMemberId, autoPassword, name, role);
            });
        });
    }

    private void showCredentialsDialog(String id, String password, String name, String role) {
        new AlertDialog.Builder(this)
            .setTitle("User Created Successfully")
            .setMessage("Login ID: " + id + "\nPassword: " + password)
            .setPositiveButton("Done", (dialog, which) -> finish())
            .setNeutralButton("Share as PDF", (dialog, which) -> {
                PdfReportService.generateLoginCredentialsPdf(this, session.getCommunityName(), name, id, password, role);
                finish();
            })
            .setCancelable(false).show();
    }
}
