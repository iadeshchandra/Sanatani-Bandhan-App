package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CommsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

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
        
        // Add traditional greeting
        sb.append("🙏 *Namaskar / Jay Sanatan Dharma* 🙏\n\n");

        if (selectedId == R.id.radioMeeting) {
            sb.append("📋 *COMMUNITY MEETING NOTICE*\n\n");
        } else if (selectedId == R.id.radioUtsav) {
            sb.append("🪔 *UTSAV GREETINGS & SEVA*\n\n");
        } else {
            sb.append("📢 *IMPORTANT ANNOUNCEMENT*\n\n");
        }

        // Add the custom text typed by the Admin
        sb.append(customText).append("\n\n");

        // Add standard sign-off
        sb.append("----------------------------\n");
        sb.append("Sent via *Sanatani Bandhan App*\n");
        sb.append("Your Community Management Portal");

        return sb.toString();
    }

    private void shareToWhatsApp(String message) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        
        // Optional: specifically target WhatsApp if installed
        sendIntent.setPackage("com.whatsapp");

        try {
            startActivity(sendIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            // Fallback if WhatsApp isn't installed
            Toast.makeText(this, "WhatsApp not installed. Opening default share...", Toast.LENGTH_SHORT).show();
            Intent fallbackIntent = new Intent(Intent.ACTION_SEND);
            fallbackIntent.setType("text/plain");
            fallbackIntent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(fallbackIntent, "Share Sandesh via..."));
        }
    }
}
