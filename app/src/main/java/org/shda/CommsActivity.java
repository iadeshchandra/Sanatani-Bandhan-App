package org.shda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class CommsActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private List<String> phoneNumbers = new ArrayList<>();
    private TextView tvMemberCount;
    private EditText inputMessage;
    private RadioGroup radioGroupType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        tvMemberCount = findViewById(R.id.tvMemberCount);
        inputMessage = findViewById(R.id.inputMessage);
        radioGroupType = findViewById(R.id.radioGroupType);

        if (!"ADMIN".equals(session.getRole()) && !"MANAGER".equals(session.getRole())) {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show(); finish(); return;
        }

        loadPhoneNumbers();

        findViewById(R.id.btnWhatsApp).setOnClickListener(v -> sendViaWhatsApp());
        findViewById(R.id.btnSms).setOnClickListener(v -> sendViaSms());
    }

    private void loadPhoneNumbers() {
        DatabaseReference membersRef = db.child("communities").child(session.getCommunityId()).child("members");
        membersRef.keepSynced(true);
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                phoneNumbers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String phone = data.child("phone").getValue(String.class);
                    if (phone != null && phone.length() >= 10) {
                        phoneNumbers.add(phone.trim());
                    }
                }
                tvMemberCount.setText("Found " + phoneNumbers.size() + " Members with valid phone numbers ready for broadcast.");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String buildFinalMessage() {
        String baseMsg = inputMessage.getText().toString().trim();
        if (baseMsg.isEmpty()) return "";

        String header = "📣 GENERAL ANNOUNCEMENT";
        int selectedId = radioGroupType.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMeeting) header = "🏛️ MANDIR / COMMITTEE MEETING";
        else if (selectedId == R.id.radioUtsav) header = "🪔 UTSAV GREETINGS & SEVA";

        return "🙏 Namaskar / Jay Sanatan Dharma 🙏\n\n"
             + header + "\n\n"
             + baseMsg + "\n\n"
             + "------------------------------\n"
             + "Sent via " + session.getCommunityName() + " Portal\n"
             + "Powered by Sanatani Bandhan SaaS";
    }

    private void sendViaWhatsApp() {
        String finalMsg = buildFinalMessage();
        if (finalMsg.isEmpty() || phoneNumbers.isEmpty()) { Toast.makeText(this, "Message or contacts empty", Toast.LENGTH_SHORT).show(); return; }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String allPhones = String.join(",", phoneNumbers);
            // Using standard WhatsApp intent (WhatsApp usually requires clicking send for mass lists without API)
            intent.setData(Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(finalMsg)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed properly", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendViaSms() {
        String finalMsg = buildFinalMessage();
        if (finalMsg.isEmpty() || phoneNumbers.isEmpty()) { Toast.makeText(this, "Message or contacts empty", Toast.LENGTH_SHORT).show(); return; }

        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + String.join(";", phoneNumbers)));
            intent.putExtra("sms_body", finalMsg);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "SMS App failed to open", Toast.LENGTH_SHORT).show();
        }
    }
}
