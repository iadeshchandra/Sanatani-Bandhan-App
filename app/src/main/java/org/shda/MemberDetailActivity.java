package org.shda;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MemberDetailActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private String passedMemberId;
    private Member activeMember;
    private List<String> autocompleteManagers = new ArrayList<>();

    private TextView tvId, tvName, tvRole, tvPhone, tvGotra, tvBlood, tvJoined, tvVerified, tvDonated;
    private LinearLayout containerAdminControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        passedMemberId = getIntent().getStringExtra("MEMBER_ID");

        if (passedMemberId == null) { finish(); return; }

        tvId = findViewById(R.id.tvInsightId);
        tvName = findViewById(R.id.tvInsightName);
        tvRole = findViewById(R.id.tvInsightRole);
        tvPhone = findViewById(R.id.tvInsightPhone);
        tvGotra = findViewById(R.id.tvInsightGotra);
        tvBlood = findViewById(R.id.tvInsightBlood);
        tvJoined = findViewById(R.id.tvInsightJoined);
        tvVerified = findViewById(R.id.tvInsightVerified);
        tvDonated = findViewById(R.id.tvInsightDonated);
        containerAdminControls = findViewById(R.id.containerAdminControls);

        loadMemberDetails();
        loadManagersForAutocomplete();

        findViewById(R.id.btnInsightPdf).setOnClickListener(v -> {
            if (activeMember != null) PdfReportService.generateMemberProfile(this, session.getCommunityName(), activeMember);
        });

        Button btnAddDonation = findViewById(R.id.btnInsightAddDonation);
        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAddDonation.setOnClickListener(v -> showQuickDonationDialog());
            findViewById(R.id.tvInsightPhone).setOnLongClickListener(v -> { editMemberField("phone", "Phone Number"); return true; });
            findViewById(R.id.tvInsightGotra).setOnLongClickListener(v -> { editMemberField("gotra", "Gotra"); return true; });
            findViewById(R.id.tvInsightBlood).setOnLongClickListener(v -> { editMemberField("bloodGroup", "Blood Group"); return true; });
        } else {
            btnAddDonation.setVisibility(View.GONE);
        }

        if ("ADMIN".equals(session.getRole())) {
            containerAdminControls.setVisibility(View.VISIBLE);
            findViewById(R.id.btnPromote).setOnClickListener(v -> changeMemberRole("MANAGER"));
            findViewById(R.id.btnDemote).setOnClickListener(v -> changeMemberRole("MEMBER"));
            
            // ✨ FIX: "VIEW USER PIN" Button exclusively for Admin
            Button btnViewPin = new Button(this);
            btnViewPin.setText("👁️ VIEW DEVOTEE PIN");
            btnViewPin.setBackgroundColor(android.graphics.Color.parseColor("#1976D2")); // Blue
            btnViewPin.setTextColor(android.graphics.Color.WHITE);
            btnViewPin.setOnClickListener(v -> showUserPin());
            containerAdminControls.addView(btnViewPin);

            Button btnDelete = new Button(this);
            btnDelete.setText("🗑️ DELETE DEVOTEE RECORD");
            btnDelete.setBackgroundColor(android.graphics.Color.parseColor("#D32F2F")); // Red
            btnDelete.setTextColor(android.graphics.Color.WHITE);
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
            containerAdminControls.addView(btnDelete);
        }
    }

    private void loadManagersForAutocomplete() {
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                autocompleteManagers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null && ("MANAGER".equals(m.role) || "ADMIN".equals(m.role))) autocompleteManagers.add(m.name + " (" + m.id + ")");
                }
                if (!autocompleteManagers.contains(session.getUserName())) autocompleteManagers.add(session.getUserName() + " (" + session.getUserId() + ")");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMemberDetails() {
        DatabaseReference memberRef = db.child("communities").child(session.getCommunityId()).child("members").child(passedMemberId);
        memberRef.keepSynced(true);
        memberRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { finish(); return; } 
                activeMember = snapshot.getValue(Member.class);
                if (activeMember != null) {
                    tvId.setText(activeMember.id); tvName.setText(activeMember.name); tvRole.setText(activeMember.role);
                    tvPhone.setText("📞 " + (activeMember.phone != null ? activeMember.phone : "N/A"));
                    tvGotra.setText("🕉️ Gotra: " + (activeMember.gotra != null && !activeMember.gotra.isEmpty() ? activeMember.gotra : "N/A"));
                    tvBlood.setText("🩸 Blood Group: " + (activeMember.bloodGroup != null && !activeMember.bloodGroup.isEmpty() ? activeMember.bloodGroup : "N/A"));
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    tvJoined.setText("Joined: " + (activeMember.timestamp > 0 ? sdf.format(new Date(activeMember.timestamp)) : "Unknown"));
                    tvVerified.setText("✍️ Profile verified by: " + (activeMember.addedBySignature != null ? activeMember.addedBySignature : "System"));
                    tvDonated.setText("Total Donated: ৳" + activeMember.totalDonated);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showUserPin() {
        db.child("communities").child(session.getCommunityId()).child("logins").child(passedMemberId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String pin = snapshot.getValue(String.class);
                new AlertDialog.Builder(MemberDetailActivity.this).setTitle("Devotee PIN").setMessage("Current PIN for " + activeMember.name + " is:\n\n" + (pin != null ? pin : "Not Found")).setPositiveButton("CLOSE", null).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showQuickDonationDialog() {
        if (activeMember == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Chanda: " + activeMember.name);

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 20);
        
        final EditText inputAmt = new EditText(this); inputAmt.setHint("Amount in BDT (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText inputNote = new EditText(this); inputNote.setHint("Admin/Manager Note (Optional)");
        
        final AutoCompleteTextView inputCollector = new AutoCompleteTextView(this);
        inputCollector.setHint("Collected By (Name with ID)");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autocompleteManagers);
        inputCollector.setAdapter(adapter); inputCollector.setThreshold(1); inputCollector.setText(session.getUserName() + " (" + session.getUserId() + ")");

        layout.addView(inputAmt); layout.addView(inputNote); layout.addView(inputCollector);
        builder.setView(layout);

        builder.setPositiveButton("RECORD", (dialog, which) -> {
            String amtStr = inputAmt.getText().toString();
            String note = inputNote.getText().toString().trim();
            String collector = inputCollector.getText().toString().trim();
            
            if (amtStr.isEmpty() || collector.isEmpty()) { Toast.makeText(this, "Amount and Collector required", Toast.LENGTH_SHORT).show(); return; }
            
            float amt = Float.parseFloat(amtStr);
            long ts = System.currentTimeMillis();
            
            // 1. Update Member Total & PDF Trigger Timestamp
            db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("totalDonated").setValue(ServerValue.increment(amt));
            db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("lastDonationTimestamp").setValue(ts);
            
            // 2. Log to Transactions
            String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
            TransactionActivity.SingleDonation sd = new TransactionActivity.SingleDonation(transId, activeMember.name + " [Member]", amt, note, "", "", collector, ts, session.getRole());
            db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);
            
            Toast.makeText(this, "৳" + amt + " recorded! PDF ready for generation.", Toast.LENGTH_LONG).show();
            
            // ✨ FIX: Triggers PDF generation automatically upon Chanda completion
            try { PdfReportService.generateDonationReceipt(this, session.getCommunityName(), activeMember, sd); } catch (Exception e) {}
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }

    private void editMemberField(String dbKey, String displayName) {
        if (activeMember == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this); input.setHint("Enter new " + displayName);
        builder.setTitle("Update " + displayName).setView(input)
               .setPositiveButton("SAVE", (d, w) -> {
                   String newVal = input.getText().toString().trim();
                   db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child(dbKey).setValue(newVal);
                   logAudit("MEMBER_EDITED", "Updated " + displayName + " for " + activeMember.name);
                   Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show();
               }).setNegativeButton("CANCEL", null).show();
    }

    private void showDeleteConfirmation() {
        if (activeMember == null) return;
        new AlertDialog.Builder(this).setTitle("DANGER: Delete Member")
            .setMessage("This will permanently erase " + activeMember.name + " and revoke their login access. Are you sure?")
            .setPositiveButton("PERMANENTLY DELETE", (d, w) -> {
                db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).removeValue();
                db.child("communities").child(session.getCommunityId()).child("logins").child(activeMember.id).removeValue();
                logAudit("MEMBER_DELETED", "Erased member record for: " + activeMember.name + " (" + activeMember.id + ")");
                Toast.makeText(this, "Devotee Record Erased.", Toast.LENGTH_LONG).show();
                finish();
            }).setNegativeButton("CANCEL", null).show();
    }

    private void changeMemberRole(String newRole) {
        if (activeMember == null) return;
        db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("role").setValue(newRole);
        logAudit("ROLE_CHANGED", "Changed " + activeMember.name + "'s role to " + newRole);
        Toast.makeText(this, activeMember.name + " is now a " + newRole + "!", Toast.LENGTH_SHORT).show();
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }
}
