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
    private EditText inputGreeting, inputMessage;
    private RadioGroup radioGroupCategory;

    // ✨ NEW: We now hold TWO separate lists of phone numbers!
    private ArrayList<String> memberPhoneNumbers = new ArrayList<>();
    private ArrayList<String> guestPhoneNumbers = new ArrayList<>();

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

        inputGreeting = findViewById(R.id.inputGreeting);
        inputMessage = findViewById(R.id.inputMessage);
        radioGroupCategory = findViewById(R.id.radioGroupCategory);
        btnSendWhatsApp = findViewById(R.id.btnSendWhatsApp);
        btnSendSms = findViewById(R.id.btnSendSms);

        loadPhoneNumbers();

        btnSendWhatsApp.setOnClickListener(v -> prepareAndSendBroadcast(true));
        btnSendSms.setOnClickListener(v -> prepareAndSendBroadcast(false));
    }

    private void loadPhoneNumbers() {
        // 1. Load official Member numbers
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

        // 2. Load Guest numbers silently in the background
        DatabaseReference guestsRef = db.child("communities").child(session.getCommunityId()).child("guests");
        guestsRef.keepSynced(true);
        guestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                guestPhoneNumbers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String phone = data.child("phone").getValue(String.class);
                    if (phone != null && phone.length() >= 10) {
                        // Ensure no duplicates just in case
                        if(!guestPhoneNumbers.contains(phone.trim())) {
                            guestPhoneNumbers.add(phone.trim());
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void prepareAndSendBroadcast(boolean isWhatsApp) {
        String customGreeting = inputGreeting.getText().toString().trim();
        String baseMessage = inputMessage.getText().toString().trim();

        if (customGreeting.isEmpty()) customGreeting = "🙏 Namaskar";
        if (baseMessage.isEmpty()) {
            Toast.makeText(this, "Main message cannot be empty", Toast.LENGTH_SHORT).show(); return;
        }

        // ✨ SMART ROUTING: Which list do we use based on the radio button?
        ArrayList<String> targetList = memberPhoneNumbers; // Default to members
        String category = "UPDATE";
        String emoji = "📢";
        int checkedId = radioGroupCategory.getCheckedRadioButtonId();

        if (checkedId == R.id.rbMeeting) { category = "COMMUNITY MEETING NOTICE"; emoji = "📋"; }
        else if (checkedId == R.id.rbUtsav) { category = "UTSAV GREETING"; emoji = "🪔"; }
        else if (checkedId == R.id.rbGeneral) { category = "GENERAL ANNOUNCEMENT"; emoji = "📢"; }
        else if (checkedId == R.id.rbGuest) { 
            category = "SPECIAL INVITATION"; 
            emoji = "💌"; 
            targetList = guestPhoneNumbers; // Switch routing to Guests!
        }

        // ✨ THE FIX: We ONLY block if they are trying to send Standard SMS with an empty list.
        // WhatsApp doesn't need numbers to open, so we let it pass perfectly!
        if (targetList.isEmpty() && !isWhatsApp) {
            String groupName = (checkedId == R.id.rbGuest) ? "Guests" : "Members";
            Toast.makeText(this, "No valid phone numbers found for " + groupName + " to send SMS.", Toast.LENGTH_SHORT).show(); 
            return;
        }

        btnSendWhatsApp.setEnabled(false); btnSendSms.setEnabled(false);
        btnSendWhatsApp.setText("ROUTING..."); btnSendSms.setText("ROUTING...");

        // BRANDING FIX: Exact requested formatting
        String finalMessage = customGreeting + "\n\n" +
                              emoji + " *" + category + "*\n\n" +
                              baseMessage + "\n\n" +
                              "-----------------------------------\n" +
                              "Sent via *" + session.getCommunityName() + " Portal*\n" +
                              "Powered by Sanatani Bandhan CRM";

        String joinedNumbers = android.text.TextUtils.join(",", targetList);
        String targetAudience = (checkedId == R.id.rbGuest) ? "Guests" : "Members";

        logAudit("MASS_SANDESH", "Sent " + category + " broadcast to " + targetList.size() + " " + targetAudience + " via " + (isWhatsApp ? "WhatsApp" : "SMS"));

        try {
            if (isWhatsApp) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                // Opens WhatsApp with the text ready to manually select contacts/groups
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
