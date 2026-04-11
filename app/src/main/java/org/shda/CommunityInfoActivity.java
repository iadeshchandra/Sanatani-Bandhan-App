package org.shda;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CommunityInfoActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    
    private EditText inputId, inputEmail, inputName, inputPhone, inputAddress;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_info);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        inputId = findViewById(R.id.infoWorkspaceId);
        inputEmail = findViewById(R.id.infoEmail);
        inputName = findViewById(R.id.infoName);
        inputPhone = findViewById(R.id.infoPhone);
        inputAddress = findViewById(R.id.infoAddress);
        btnSave = findViewById(R.id.btnSaveInfo);

        if (!"ADMIN".equals(session.getRole())) {
            inputName.setEnabled(false);
            inputPhone.setEnabled(false);
            inputAddress.setEnabled(false);
            btnSave.setVisibility(View.GONE);
            Toast.makeText(this, "Read-Only Mode: Only Admins can modify Workspace Info.", Toast.LENGTH_LONG).show();
        }

        loadCommunityData();

        btnSave.setOnClickListener(v -> saveCommunityData());
    }

    private void loadCommunityData() {
        inputId.setText(session.getCommunityId());
        
        // ✨ SMART FALLBACK: If email is missing in Realtime DB (older accounts), fetch from Firebase Auth!
        String fallbackEmail = session.getWorkspaceEmail();
        if ((fallbackEmail == null || fallbackEmail.isEmpty()) && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            fallbackEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
        if (fallbackEmail != null && !fallbackEmail.isEmpty()) {
            inputEmail.setText(fallbackEmail);
        }

        db.child("communities").child(session.getCommunityId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("info/email").getValue(String.class);
                    String phone = snapshot.child("info/phone").getValue(String.class);
                    String address = snapshot.child("info/address").getValue(String.class);

                    if (name != null) inputName.setText(name);
                    
                    // Only overwrite with DB email if it actually exists
                    if (email != null && !email.isEmpty()) {
                        inputEmail.setText(email);
                    }
                    
                    if (phone != null) inputPhone.setText(phone);
                    if (address != null) inputAddress.setText(address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CommunityInfoActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCommunityData() {
        String newName = inputName.getText().toString().trim();
        String newPhone = inputPhone.getText().toString().trim();
        String newAddress = inputAddress.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setText("SAVING...");
        btnSave.setEnabled(false);

        db.child("communities").child(session.getCommunityId()).child("name").setValue(newName);
        db.child("communities").child(session.getCommunityId()).child("info").child("phone").setValue(newPhone);
        db.child("communities").child(session.getCommunityId()).child("info").child("address").setValue(newAddress)
            .addOnSuccessListener(aVoid -> {
                session.updateCommunityName(newName);
                Toast.makeText(CommunityInfoActivity.this, "Workspace Info Updated Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(CommunityInfoActivity.this, "Error saving updates.", Toast.LENGTH_SHORT).show();
                btnSave.setText("SAVE CHANGES");
                btnSave.setEnabled(true);
            });
    }
}
