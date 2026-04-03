package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CommsActivity extends AppCompatActivity {
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        session = new SessionManager(this);
        if (session.getCommunityId() == null) {
            finish();
            return;
        }

        RadioGroup radioGroup = findViewById(R.id.radioGroupTemplate);
        EditText inputMessage = findViewById(R.id.inputCustomMessage);
        Button btnSend = findViewById(R.id.btnSendWhatsapp);

        btnSend.setOnClickListener(v -> {
            String customText = inputMessage.getText().toString().trim();
            
            if (customText.isEmpty()) {
                Toast.makeText(this, "Please type a message first", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = radioGroup.getCheckedRadioButtonId();
            String finalMessage = buildFormattedMessage(selectedId, customText);

            shareToWhatsApp(finalMessage);
        });
    }

    private String buildFormattedMessage(int selectedId, String customText) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("🙏 *Namaskar / Jay Sanatan Dharma* 🙏\n\n");

        if (selectedId == R.id.radioMeeting) {
            sb.append("📋 *COMMUNITY MEETING NOTICE*\n\n");
        } else if (selectedId == R.id.radioUtsav) {
            sb.append("🪔 *UTSAV GREETINGS & SEVA*\n\n");
        } else {
            sb.append("📢 *IMPORTANT ANNOUNCEMENT*\n\n");
        }

        sb.append(customText).append("\n\n");

        sb.append("----------------------------\n");
        // Dynamically inserts the specific Mandir's name!
        sb.append("Sent via *").append(session.getCommunityName()).append(" Portal*\n");
        sb.append("Powered by Sanatani SaaS");

        return sb.toString();
    }

    private void shareToWhatsApp(String message) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        
        sendIntent.setPackage("com.whatsapp");

        try {
            startActivity(sendIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "WhatsApp not installed. Opening default share...", Toast.LENGTH_SHORT).show();
            Intent fallbackIntent = new Intent(Intent.ACTION_SEND);
            fallbackIntent.setType("text/plain");
            fallbackIntent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(fallbackIntent, "Share Sandesh via..."));
        }
    }
}
