package org.shda;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseReference db;
    private AutoCompleteTextView autoCompleteMember;
    private EditText inputGuestName, inputAmount, inputNote;
    private RadioButton radioMember;
    private ArrayList<String> memberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        db = FirebaseDatabase.getInstance().getReference();

        RadioGroup radioGroup = findViewById(R.id.radioGroupType);
        radioMember = findViewById(R.id.radioMember);
        autoCompleteMember = findViewById(R.id.autoCompleteMember);
        inputGuestName = findViewById(R.id.inputGuestName);
        inputAmount = findViewById(R.id.inputAmount);
        inputNote = findViewById(R.id.inputNote);
        Button btnSave = findViewById(R.id.btnSaveTransaction);

        // Toggle UI dynamically based on selection
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioMember) {
                autoCompleteMember.setVisibility(View.VISIBLE);
                inputGuestName.setVisibility(View.GONE);
            } else {
                autoCompleteMember.setVisibility(View.GONE);
                inputGuestName.setVisibility(View.VISIBLE);
            }
        });

        loadMembersForSearch();

        btnSave.setOnClickListener(v -> processTransaction());
    }

    private void loadMembersForSearch() {
        db.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member member = data.getValue(Member.class);
                    if (member != null) {
                        // Creates the format: "Adesh Chandra (SB-1001)"
                        memberList.add(member.name + " (" + member.id + ")");
                    }
                }
                // Attaches the data to the AutoComplete bar
                ArrayAdapter<String> adapter = new ArrayAdapter<>(TransactionActivity.this, android.R.layout.simple_dropdown_item_1line, memberList);
                autoCompleteMember.setAdapter(adapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void processTransaction() {
        String amountStr = inputAmount.getText().toString().trim();
        String note = inputNote.getText().toString().trim();
        boolean isMember = radioMember.isChecked();
        
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        float amount = Float.parseFloat(amountStr);
        String nameToSave = "";
        String memberIdToUpdate = null;

        if (isMember) {
            String selection = autoCompleteMember.getText().toString().trim();
            // Validate that an actual member from the dropdown was selected
            if (selection.isEmpty() || !selection.contains("(") || !selection.contains(")")) {
                Toast.makeText(this, "Please select a valid member from the list", Toast.LENGTH_SHORT).show();
                return;
            }
            nameToSave = selection;
            // Magically extract the SB-100X ID out of the string "Adesh Chandra (SB-1001)"
            memberIdToUpdate = selection.substring(selection.indexOf("(") + 1, selection.indexOf(")"));
        } else {
            nameToSave = inputGuestName.getText().toString().trim() + " (Guest)";
            if (nameToSave.equals(" (Guest)")) {
                Toast.makeText(this, "Please enter Guest Name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Action 1: Save to Global Transaction Logs
        HashMap<String, Object> logData = new HashMap<>();
        logData.put("name", nameToSave);
        logData.put("amount", amount);
        logData.put("note", note);
        logData.put("timestamp", System.currentTimeMillis());
        db.child("logs").child("Donation").push().setValue(logData);

        // Action 2: Update Dashboard Graph
        db.child("finances").child("Donation").get().addOnSuccessListener(snap -> {
            float currentTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
            db.child("finances").child("Donation").setValue(currentTotal + amount);
        });

        // Action 3: Update Member's Personal Lifetime Total
        if (isMember && memberIdToUpdate != null) {
            db.child("members").child(memberIdToUpdate).child("totalDonated").get().addOnSuccessListener(snap -> {
                float currentMemberTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
                db.child("members").child(memberIdToUpdate).child("totalDonated").setValue(currentMemberTotal + amount);
            });
        }

        Toast.makeText(this, "Chanda Recorded Successfully!", Toast.LENGTH_LONG).show();
        finish(); // Go back to Dashboard
    }
}
