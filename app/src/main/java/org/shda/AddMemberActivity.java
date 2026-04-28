package org.shda;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Random;

public class AddMemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private EditText inputName, inputPhone, inputEmail, inputGotra, inputBloodGroup, inputFather, inputMother, inputNid, inputAddress;
    private Spinner spinnerRole;
    private Button btnSaveMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        inputName = findViewById(R.id.inputName);
        inputPhone = findViewById(R.id.inputPhone);
        inputEmail = findViewById(R.id.inputEmail);
        inputGotra = findViewById(R.id.inputGotra);
        inputBloodGroup = findViewById(R.id.inputBloodGroup);
        inputFather = findViewById(R.id.inputFather);
        inputMother = findViewById(R.id.inputMother);
        inputNid = findViewById(R.id.inputNid);
        inputAddress = findViewById(R.id.inputAddress);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSaveMember = findViewById(R.id.btnSaveMember);

        String[] roles = {"MEMBER", "MANAGER", "ADMIN"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRole.setAdapter(adapter);

        if (!"ADMIN".equals(session.getRole())) spinnerRole.setEnabled(false);

        btnSaveMember.setOnClickListener(v -> checkLimitsAndSave());
    }

    // ✨ FREEMIUM ENFORCER: Checks limits before proceeding
    private void checkLimitsAndSave() {
        String name = inputName.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) { 
            Toast.makeText(this, "Name and Phone are required!", Toast.LENGTH_SHORT).show(); 
            return; 
        }

        btnSaveMember.setEnabled(false); 
        btnSaveMember.setText("VERIFYING WORKSPACE...");

        // Fast Offline-First Read
        db.child("communities").child(session.getCommunityId()).child("info")
          .addListenerForSingleValueEvent(new ValueEventListener() {
              @Override
              public void onDataChange(@NonNull DataSnapshot snapshot) {
                  String plan = snapshot.child("plan").getValue(String.class);
                  Integer count = snapshot.child("devoteeCount").getValue(Integer.class);
                  
                  if (plan == null) plan = "FREE";
                  if (count == null) count = 0;

                  if (plan.equals("FREE") && count >= 50) {
                      showUpgradeDialog();
                  } else {
                      executeSaveStrictly(name, phone);
                  }
              }

              @Override
              public void onCancelled(@NonNull DatabaseError error) {
                  // Fallback: If network fails, check local cache to be safe
                  if ("FREE".equals(session.getPlan())) {
                      Toast.makeText(AddMemberActivity.this, "Cannot verify Devotee limit while offline.", Toast.LENGTH_SHORT).show();
                      btnSaveMember.setEnabled(true);
                      btnSaveMember.setText("SAVE DEVOTEE");
                  } else {
                      executeSaveStrictly(name, phone);
                  }
              }
          });
    }

    private void showUpgradeDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🌟 Upgrade to Premium")
            .setMessage("Your community is growing! The Free Plan allows up to 50 Devotees.\n\nUpgrade to Sanatani Premium to unlock Unlimited Devotees, Master PDFs, and Mass WhatsApp Broadcasts.")
            .setPositiveButton("UPGRADE NOW", (d, w) -> {
                Toast.makeText(this, "Payment Gateway opening soon (Phase 3)!", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("CANCEL", null)
            .show();
            
        btnSaveMember.setEnabled(true);
        btnSaveMember.setText("SAVE DEVOTEE");
    }

    private void executeSaveStrictly(String name, String phone) {
        btnSaveMember.setText("QUEUED FOR SYNC...");

        String memberId = "SB-" + (1000 + new Random().nextInt(9000)); 
        String pinPassword = String.format("%04d", new Random().nextInt(10000));

        HashMap<String, Object> memberMap = new HashMap<>();
        memberMap.put("id", memberId);
        memberMap.put("name", name);
        memberMap.put("phone", phone);
        memberMap.put("email", inputEmail.getText().toString().trim());
        memberMap.put("gotra", inputGotra.getText().toString().trim());
        memberMap.put("bloodGroup", inputBloodGroup.getText().toString().trim());
        memberMap.put("role", spinnerRole.getSelectedItem().toString());
        memberMap.put("address", inputAddress.getText().toString().trim());
        memberMap.put("addedBySignature", session.getRole() + " - " + session.getUserName());
        memberMap.put("totalDonated", 0f);
        memberMap.put("timestamp", System.currentTimeMillis());

        // 1. Push new member
        db.child("communities").child(session.getCommunityId()).child("members").child(memberId).setValue(memberMap);
        db.child("communities").child(session.getCommunityId()).child("logins").child(memberId).setValue(pinPassword);
        
        // 2. ✨ INCREMENT THE COUNTER INSTANTLY
        db.child("communities").child(session.getCommunityId()).child("info").child("devoteeCount").setValue(ServerValue.increment(1));

        try { 
            PdfReportService.generateLoginCredentialsPdf(this, session.getCommunityName(), name, memberId, pinPassword, session.getRole()); 
            Toast.makeText(this, "Devotee Saved. PDF Credentials Generated!", Toast.LENGTH_LONG).show(); 
        } catch (Exception e) {
            Toast.makeText(this, "Devotee Saved, but error making PDF.", Toast.LENGTH_LONG).show(); 
        }

        finish(); 
    }
}
