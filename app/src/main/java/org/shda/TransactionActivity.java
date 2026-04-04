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
    private SessionManager session;
    private AutoCompleteTextView autoCompleteMember;
    private EditText inputGuestName, inputAmount, inputNote;
    private RadioButton radioMember;
    private ArrayList<String> memberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) {
            finish();
            return;
        }

        RadioGroup radioGroup = findViewById(R.id.radioGroupType);
        radioMember = findViewById(R.id.radioMember);
        autoCompleteMember = findViewById(R.id.autoCompleteMember);
        inputGuestName = findViewById(R.id.inputGuestName);
        inputAmount = findViewById(R.id.inputAmount);
        inputNote = findViewById(R.id.inputNote);
        Button btnSave = findViewById(R.id.btnSaveTransaction);

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
        db.child("communities").child(session.getCommunityId()).child("members")
          .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member member = data.getValue(Member.class);
                    if (member != null) {
                        memberList.add(member.name + " (" + member.id + ")");
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(TransactionActivity.this, 
                        android.R.layout.simple_dropdown_item_1line, memberList);
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
        String tempMemberId = null; 
        String commId = session.getCommunityId();

        if (isMember) {
            String selection = autoCompleteMember.getText().toString().trim();
            if (selection.isEmpty() || !selection.contains("(") || !selection.contains(")")) {
                Toast.makeText(this, "Select a valid member", Toast.LENGTH_SHORT).show();
                return;
            }
            nameToSave = selection;
            tempMemberId = selection.substring(selection.indexOf("(") + 1, selection.indexOf(")"));
        } else {
            nameToSave = inputGuestName.getText().toString().trim() + " (Guest)";
            if (nameToSave.equals(" (Guest)")) {
                Toast.makeText(this, "Please enter Guest Name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Creating final variables for Lambda expressions
        final String finalMemberId = tempMemberId;
        final String finalCommId = commId;
        final float finalAmount = amount;
        final String finalNameToSave = nameToSave;
        final String finalNote = note;

        HashMap<String, Object> logData = new HashMap<>();
        logData.put("name", finalNameToSave);
        logData.put("amount", finalAmount);
        logData.put("note", finalNote);
        logData.put("timestamp", System.currentTimeMillis());
        
        // 1. Save to community financial logs
        db.child("communities").child(finalCommId).child("logs").child("Donation").push().setValue(logData);

        // --- 2. NEW: DIGITAL SIGNATURE FOR THE AUDIT TRAIL ---
        AuditLogger.logAction(finalCommId, session.getUserName(), "CHANDA_COLLECTED", "Collected ৳" + finalAmount + " from " + finalNameToSave);

        // 3. Update community total finances
        db.child("communities").child(finalCommId).child("finances").child("Donation").get().addOnSuccessListener(snap -> {
            float currentTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
            db.child("communities").child(finalCommId).child("finances").child("Donation").setValue(currentTotal + finalAmount);
        });

        // 4. Update specific member's total if applicable
        if (isMember && finalMemberId != null) {
            db.child("communities").child(finalCommId).child("members").child(finalMemberId).child("totalDonated").get()
              .addOnSuccessListener(snap -> {
                float currentMemberTotal = snap.exists() ? snap.getValue(Float.class) : 0f;
                db.child("communities").child(finalCommId).child("members").child(finalMemberId).child("totalDonated").setValue(currentMemberTotal + finalAmount);
            });
        }

        Toast.makeText(this, "Chanda Recorded!", Toast.LENGTH_SHORT).show();
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
        String dateStr = sdf.format(new java.util.Date());
        
        // Branded Receipt Generation
        PdfReportService.generateDonorReceipt(this, session.getCommunityName(), finalNameToSave, finalAmount, finalNote, dateStr);
    }
}
