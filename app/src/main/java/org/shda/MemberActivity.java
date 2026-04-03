package org.shda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class MemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private LinearLayout membersContainer;
    private EditText inputName, inputPhone;
    private long currentIdCounter = 1000; // Starting ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member);

        db = FirebaseDatabase.getInstance().getReference();
        membersContainer = findViewById(R.id.membersContainer);
        inputName = findViewById(R.id.inputMemberName);
        inputPhone = findViewById(R.id.inputMemberPhone);
        Button btnAdd = findViewById(R.id.btnAddMember);

        loadMembers();

        btnAdd.setOnClickListener(v -> addNewMember());
    }

    private void addNewMember() {
        String name = inputName.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic to fetch the highest ID and generate the next one
        db.child("metadata").child("lastMemberId").get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                currentIdCounter = snap.getValue(Long.class);
            }
            
            currentIdCounter++;
            String newMemberId = "SB-" + currentIdCounter; // Generates SB-1001, SB-1002...

            Member newMember = new Member(newMemberId, name, phone, System.currentTimeMillis());
            
            // Save to database
            db.child("members").child(newMemberId).setValue(newMember);
            db.child("metadata").child("lastMemberId").setValue(currentIdCounter);

            // Clear inputs
            inputName.setText("");
            inputPhone.setText("");
            Toast.makeText(this, "Success! Member ID: " + newMemberId, Toast.LENGTH_LONG).show();
        });
    }

    private void loadMembers() {
        db.child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersContainer.removeAllViews(); // Clear the list
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member member = data.getValue(Member.class);
                    if (member != null) {
                        addMemberView(member);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addMemberView(Member member) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_member, membersContainer, false);
        TextView tvName = view.findViewById(R.id.tvMemberName);
        TextView tvId = view.findViewById(R.id.tvMemberId);
        TextView tvTotal = view.findViewById(R.id.tvTotalDonated);

        tvName.setText(member.name);
        tvId.setText(member.id + " | " + member.phone);
        tvTotal.setText("Donated: ৳" + member.totalDonated);

        membersContainer.addView(view);
    }
}
