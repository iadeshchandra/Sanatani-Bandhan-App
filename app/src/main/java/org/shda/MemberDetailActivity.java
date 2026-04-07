package org.shda;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MemberDetailActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private String passedMemberId;
    private Member activeMember;

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

        findViewById(R.id.btnInsightPdf).setOnClickListener(v -> {
            if (activeMember != null) PdfReportService.generateMemberProfile(this, session.getCommunityName(), activeMember);
        });

        Button btnAddDonation = findViewById(R.id.btnInsightAddDonation);
        
        // ✨ WHAT ADDED & WHY: Permission check. Only Admins and Managers can Edit data or add Chanda. Members only view it.
        if ("ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole())) {
            btnAddDonation.setOnClickListener(v -> showQuickDonationDialog());
            
            // ✨ WHAT ADDED & WHY: Long-click listeners on the text fields allow Managers to fix typos in the KYC data securely.
            findViewById(R.id.tvInsightPhone).setOnLongClickListener(v -> { editMemberField("phone", "Phone Number"); return true; });
            findViewById(R.id.tvInsightGotra).setOnLongClickListener(v -> { editMemberField("gotra", "Gotra"); return true; });
            findViewById(R.id.tvInsightBlood).setOnLongClickListener(v -> { editMemberField("bloodGroup", "Blood Group"); return true; });
        } else {
            btnAddDonation.setVisibility(View.GONE);
        }

        // ✨ WHAT ADDED & WHY: Super Admin strict controls. Only they can change user roles or permanently wipe a record.
        if ("ADMIN".equals(session.getRole())) {
            containerAdminControls.setVisibility(View.VISIBLE);
            findViewById(R.id.btnPromote).setOnClickListener(v -> changeMemberRole("MANAGER"));
            findViewById(R.id.btnDemote).setOnClickListener(v -> changeMemberRole("MEMBER"));
            
            Button btnDelete = new Button(this);
            btnDelete.setText("DELETE DEVOTEE RECORD");
            btnDelete.setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"));
            btnDelete.setTextColor(android.graphics.Color.WHITE);
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
            containerAdminControls.addView(btnDelete);
        }
    }

    private void loadMemberDetails() {
        DatabaseReference memberRef = db.child("communities").child(session.getCommunityId()).child("members").child(passedMemberId);
        memberRef.keepSynced(true);
        memberRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { finish(); return; } // ✨ WHY: If an admin deletes the user while this page is open, it auto-closes to prevent a crash.
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

    // ✨ WHAT ADDED & WHY: The CRUD Edit function. Prompts for the new value, writes it, and drops a log in the Security Audit.
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

    // ✨ WHAT ADDED & WHY: The CRUD Delete function. Erases the member, revokes their login PIN, and writes to the Security Audit.
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

    // ✨ WHAT ADDED & WHY: Allows admins to instantly add Chanda while viewing a profile, saving them from having to switch screens.
    private void showQuickDonationDialog() {
        if (activeMember == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Donation for " + activeMember.name);

        final EditText inputAmt = new EditText(this); inputAmt.setHint("Amount in BDT (৳)"); inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout layout = new LinearLayout(this); layout.setPadding(50, 20, 50, 20); layout.addView(inputAmt);
        builder.setView(layout);

        builder.setPositiveButton("RECORD", (dialog, which) -> {
            String amtStr = inputAmt.getText().toString();
            if (!amtStr.isEmpty()) {
                float amt = Float.parseFloat(amtStr);
                db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("totalDonated").setValue(ServerValue.increment(amt));
                
                String transId = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").push().getKey();
                TransactionActivity.SingleDonation sd = new TransactionActivity.SingleDonation(transId, activeMember.name + " [Member]", amt, "Added via Profile Insights", "", "", session.getUserName(), System.currentTimeMillis(), session.getRole());
                db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").child(transId).setValue(sd);
                Toast.makeText(this, "৳" + amt + " donation recorded!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }

    private void changeMemberRole(String newRole) {
        if (activeMember == null) return;
        db.child("communities").child(session.getCommunityId()).child("members").child(activeMember.id).child("role").setValue(newRole);
        logAudit("ROLE_CHANGED", "Changed " + activeMember.name + "'s role to " + newRole);
        Toast.makeText(this, activeMember.name + " is now a " + newRole + "!", Toast.LENGTH_SHORT).show();
    }

    // ✨ WHAT ADDED & WHY: The core Audit Engine hook. Creates an un-editable log in the backend every time an Admin takes action.
    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }
}
