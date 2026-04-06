package org.shda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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
    private TextView tvMemberCount;
    private EditText inputBroadcastMessage;
    private List<String> phoneNumbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        
        tvMemberCount = findViewById(R.id.tvMemberCount);
        inputBroadcastMessage = findViewById(R.id.inputBroadcastMessage);

        if (session.getCommunityId() == null || (!session.getRole().equals("ADMIN") && !session.getRole().equals("MANAGER"))) {
            Toast.makeText(this, "Unauthorized Access", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        loadMemberContacts();

        findViewById(R.id.btnSendWhatsApp).setOnClickListener(v -> sendWhatsAppBroadcast());
        findViewById(R.id.btnSendSMS).setOnClickListener(v -> sendSmsBroadcast());
    }

    private void loadMemberContacts() {
        db.child("communities").child(session.getCommunityId()).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                phoneNumbers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String phone = data.child("phone").getValue(String.class);
                    if (phone != null && phone.length() >= 10) {
                        // Clean the phone number
                        phoneNumbers.add(phone.replaceAll("[^0-9+]", ""));
                    }
                }
                tvMemberCount.setText("Found " + phoneNumbers.size() + " Members with valid phone numbers ready for broadcast.");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendWhatsAppBroadcast() {
        String message = inputBroadcastMessage.getText().toString().trim();
        if (message.isEmpty() || phoneNumbers.isEmpty()) {
            Toast.makeText(this, "Message is empty or no contacts found.", Toast.LENGTH_SHORT).show(); return;
        }
        
        // WhatsApp API natively supports opening with text. 
        // Note: Bulk blasting requires the user to forward the message, or use a WhatsApp Broadcast list.
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message)));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSmsBroadcast() {
        String message = inputBroadcastMessage.getText().toString().trim();
        if (message.isEmpty() || phoneNumbers.isEmpty()) {
            Toast.makeText(this, "Message is empty or no contacts found.", Toast.LENGTH_SHORT).show(); return;
        }

        String allNumbers = String.join(";", phoneNumbers);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + allNumbers));
        intent.putExtra("sms_body", message);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "SMS App not found.", Toast.LENGTH_SHORT).show();
        }
    }
}
