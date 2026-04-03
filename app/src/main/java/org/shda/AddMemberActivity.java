package org.shda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        if (session.getCommunityId() == null) {
            finish();
            return;
        }

        EditText inputName = findViewById(R.id.inputName);
        EditText inputPhone = findViewById(R.id.inputPhone);
        EditText inputGotra = findViewById(R.id.inputGotra);
        EditText inputBloodGroup = findViewById(R.id.inputBloodGroup);
        Button btnSave = findViewById(R.id.btnSaveMember);

        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();
            String gotra = inputGotra.getText().toString().trim();
            String bloodGroup = inputBloodGroup.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
                return;
            }

            String commId = session.getCommunityId();
            
            db.child("communities").child(commId).child("metadata").child("lastMemberId").get().addOnSuccessListener(snap -> {
                if (snap.exists()) {
                    currentIdCounter = snap.getValue(Long.class);
                }
                
                currentIdCounter++;
                String newMemberId = "SB-" + currentIdCounter; 

                Member newMember = new Member(newMemberId, name, phone, gotra, bloodGroup, System.currentTimeMillis());
                
                db.child("communities").child(commId).child("members").child(newMemberId).setValue(newMember);
                db.child("communities").child(commId).child("metadata").child("lastMemberId").setValue(currentIdCounter);

                Toast.makeText(this, "Success! Generated: " + newMemberId, Toast.LENGTH_LONG).show();
                finish(); 
            });
        });
    }
}
