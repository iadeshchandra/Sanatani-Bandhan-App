package org.shda;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Random;

public class AddMemberActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;

    private EditText inputName, inputPhone, inputGotra, inputBloodGroup, inputFather, inputMother, inputNid, inputAddress;
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

        if (!"ADMIN".equals(session.getRole())) {
            spinnerRole.setEnabled(false);
        }

        btnSaveMember.setOnClickListener(v -> saveMemberOfflineFriendly());
    }

    private void saveMemberOfflineFriendly() {
        String name = inputName.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();
        String gotra = inputGotra.getText().toString().trim();
        String blood = inputBloodGroup.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✨ 1. INSTANTLY DISABLE BUTTON TO PREVENT DOUBLE CLICKS
        btnSaveMember.setEnabled(false);
        btnSaveMember.setText("Saving...");

        String memberId = "SB-" + (1000 + new Random().nextInt(9000)); 
        String pinPassword = String.format("%04d", new Random().nextInt(10000));
        String strictSignature = session.getRole() + " - " + session.getUserName();

        Member newMember = new Member(
                memberId, name, phone, gotra, blood, role,
                inputFather.getText().toString().trim(),
                inputMother.getText().toString().trim(),
                inputNid.getText().toString().trim(),
                inputAddress.getText().toString().trim(),
                strictSignature, 0f, System.currentTimeMillis()
        );

        // ✨ 2. SAVE LOCALLY (Instant execution)
        db.child("communities").child(session.getCommunityId()).child("members").child(memberId).setValue(newMember);
        db.child("communities").child(session.getCommunityId()).child("logins").child(memberId).setValue(pinPassword);

        try {
            PdfReportService.generateLoginCredentialsPdf(this, session.getCommunityName(), name, memberId, pinPassword, role);
        } catch (Exception e) {}
        
        Toast.makeText(this, "Member saved! Synced to background.", Toast.LENGTH_SHORT).show();
        
        // ✨ 3. INSTANTLY CLOSE SCREEN
        finish(); 
    }
}
