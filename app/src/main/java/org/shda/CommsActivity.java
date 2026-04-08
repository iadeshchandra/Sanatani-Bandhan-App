package org.shda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CommsActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private EditText inputMessage;
    private RadioGroup radioGroupCategory;
    private ArrayList<String> memberPhoneNumbers = new ArrayList<>();
    private Button btnSendWhatsApp, btnSendSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null || "MEMBER".equals(session.getRole())) {
            Toast.makeText(this, "Access Denied: Managers/Admins Only", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        inputMessage = findViewById(R.id.inputMessage);
        radioGroupCategory = findViewById(R.id.radioGroupCategory);
        btnSendWhatsApp = findViewById(R.id.btnSendWhatsApp);
        btnSendSms = findViewById(R.id.btnSendSms);

        loadPhoneNumbers();

        btnSendWhatsApp.setOnClickListener(v -> prepareAndSendBroadcast(true));
        btnSendSms.setOnClickListener(v -> prepareAndSendBroadcast(false));
    }

    private void loadPhoneNumbers() {
        DatabaseReference membersRef = db.child("communities").child(session.getCommunityId()).child("members");
        membersRef.keepSynced(true);
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberPhoneNumbers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String phone = data.child("phone").getValue(String.class);
                    if (phone != null && phone.length() >= 10) {
                        memberPhoneNumbers.add(phone.trim());
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void prepareAndSendBroadcast(boolean isWhatsApp) {
        String baseMessage = inputMessage.getText().toString().trim();
        if (baseMessage.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show(); return;
        }
        if (memberPhoneNumbers.isEmpty()) {
            Toast.makeText(this, "No valid phone numbers found in directory.", Toast.LENGTH_SHORT).show(); return;
        }

        btnSendWhatsApp.setEnabled(false); btnSendSms.setEnabled(false);
        btnSendWhatsApp.setText("ROUTING..."); btnSendSms.setText("ROUTING...");

        String category = "UPDATE";
        String emoji = "📢";
        int checkedId = radioGroupCategory.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMeeting) { category = "COMMUNITY MEETING NOTICE"; emoji = "📋"; }
        else if (checkedId == R.id.rbUtsav) { category = "UTSAV GREETING"; emoji = "🪔"; }
        else if (checkedId == R.id.rbGeneral) { category = "GENERAL ANNOUNCEMENT"; emoji = "📢"; }

        // ✨ FIX: Exact formatting from your screenshot!
        String finalMessage = "🙏 *Namaskar / Jay Sanatan Dharma* 🙏\n\n" +
                              emoji + " *" + category + "*\n\n" +
                              baseMessage + "\n\n" +
                              "-----------------------------------\n" +
                              "Sent via *" + session.getCommunityName() + " Portal*\n" +
                              "Powered by Sanatani SaaS";

        String joinedNumbers = android.text.TextUtils.join(",", memberPhoneNumbers);

        logAudit("MASS_SANDESH", "Sent " + category + " broadcast to " + memberPhoneNumbers.size() + " members via " + (isWhatsApp ? "WhatsApp" : "SMS"));

        try {
            if (isWhatsApp) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/?text=" + Uri.encode(finalMessage)));
                startActivity(intent);
            } else {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:" + joinedNumbers));
                intent.putExtra("sms_body", finalMessage);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error launching app. Check if installed.", Toast.LENGTH_LONG).show();
        }

        new android.os.Handler().postDelayed(() -> {
            btnSendWhatsApp.setEnabled(true); btnSendSms.setEnabled(true);
            btnSendWhatsApp.setText("🚀 BROADCAST VIA WHATSAPP"); btnSendSms.setText("💬 BROADCAST VIA STANDARD SMS");
        }, 3000);
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }
}
