package org.shda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class UserProfileActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private Member currentMember;

    private TextView tvNameId, tvRole;
    private EditText editPhone, editGotra, editBlood, editAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile); // You will need to create this XML

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        tvNameId = findViewById(R.id.tvProfileNameId);
        tvRole = findViewById(R.id.tvProfileRole);
        editPhone = findViewById(R.id.editProfilePhone);
        editGotra = findViewById(R.id.editProfileGotra);
        editBlood = findViewById(R.id.editProfileBlood);
        editAddress = findViewById(R.id.editProfileAddress);

        loadUserData();

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileChanges());
        findViewById(R.id.btnDownloadMyPdf).setOnClickListener(v -> {
            if (currentMember != null) {
                PdfReportService.generateElegantProfilePdf(this, session.getCommunityName(), currentMember, session.getUserName());
            }
        });
    }

    private void loadUserData() {
        // Fetch the user's specific node from the database
        db.child("communities").child(session.getCommunityId()).child("members").child(session.getUserId())
          .addListenerForSingleValueEvent(new ValueEventListener() {
              @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                  currentMember = snapshot.getValue(Member.class);
                  if (currentMember != null) {
                      tvNameId.setText(currentMember.name + " (" + currentMember.id + ")");
                      tvRole.setText("Assigned Role: " + currentMember.role);
                      
                      editPhone.setText(currentMember.phone);
                      editGotra.setText(currentMember.gotra);
                      editBlood.setText(currentMember.bloodGroup);
                      editAddress.setText(currentMember.address);
                  }
              }
              @Override public void onCancelled(@NonNull DatabaseError error) {}
          });
    }

    private void saveProfileChanges() {
        if (currentMember == null) return;
        
        String newPhone = editPhone.getText().toString();
        String newGotra = editGotra.getText().toString();
        String newBlood = editBlood.getText().toString();
        String newAddress = editAddress.getText().toString();

        DatabaseReference userRef = db.child("communities").child(session.getCommunityId()).child("members").child(session.getUserId());
        
        // Log history of changes
        String changes = "Profile updated: Phone[" + newPhone + "], Gotra[" + newGotra + "]";
        String historyId = userRef.child("editHistory").push().getKey();
        userRef.child("editHistory").child(historyId).setValue(changes + " - Logged on " + System.currentTimeMillis());

        // Update active profile
        userRef.child("phone").setValue(newPhone);
        userRef.child("gotra").setValue(newGotra);
        userRef.child("bloodGroup").setValue(newBlood);
        userRef.child("address").setValue(newAddress);

        Toast.makeText(this, "Profile Updated & History Logged!", Toast.LENGTH_SHORT).show();
    }
}
