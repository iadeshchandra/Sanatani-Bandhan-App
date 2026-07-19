package org.shda;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class UpgradeActivity extends AppCompatActivity {

    private Button btnPayBD, btnPayIntl, btnCheckoutLink, btnVerifyPayment, btnCopyNumber;
    private MaterialCardView cardBanglaQR, cardIntlPayment;
    private SessionManager session;

    // Paste your Wise or Payoneer Request Link here
    private final String INTL_PAYMENT_LINK = "https://wise.com/pay/me/adeshc"; 
    
    // Your exact TaliPay Number
    private final String MERCHANT_NUMBER = "01701987744"; 

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        session = new SessionManager(this);

        btnPayBD = findViewById(R.id.btnPayBD);
        btnPayIntl = findViewById(R.id.btnPayIntl);
        cardBanglaQR = findViewById(R.id.cardBanglaQR);
        cardIntlPayment = findViewById(R.id.cardIntlPayment);
        btnCheckoutLink = findViewById(R.id.btnCheckoutLink);
        btnVerifyPayment = findViewById(R.id.btnVerifyPayment);
        btnCopyNumber = findViewById(R.id.btnCopyNumber);

        // Toggle to Bangladesh QR View
        btnPayBD.setOnClickListener(v -> {
            cardBanglaQR.setVisibility(View.VISIBLE);
            cardIntlPayment.setVisibility(View.GONE);
            btnPayBD.setBackgroundColor(0xFF2E7D32); // Green
            btnPayBD.setTextColor(0xFFFFFFFF);
            btnPayIntl.setBackgroundColor(0xFFE0E0E0); // Gray
            btnPayIntl.setTextColor(0xFF424242);
        });

        // Toggle to International Link View
        btnPayIntl.setOnClickListener(v -> {
            cardBanglaQR.setVisibility(View.GONE);
            cardIntlPayment.setVisibility(View.VISIBLE);
            btnPayIntl.setBackgroundColor(0xFF1976D2); // Blue
            btnPayIntl.setTextColor(0xFFFFFFFF);
            btnPayBD.setBackgroundColor(0xFFE0E0E0); // Gray
            btnPayBD.setTextColor(0xFF424242);
        });
        
        // Clipboard Logic for TaliPay Number
        btnCopyNumber.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("TaliPay Number", MERCHANT_NUMBER);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(UpgradeActivity.this, "Number Copied! Paste into your Payment App", Toast.LENGTH_LONG).show();
            }
        });

        // Open Wise/Payoneer Link
        btnCheckoutLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(INTL_PAYMENT_LINK));
            startActivity(browserIntent);
        });

        // Verify Payment via WhatsApp
        btnVerifyPayment.setOnClickListener(v -> {
            String verifyMsg = "🙏 *Namaskar! I want to upgrade to SAMRAT PRO* 🙏\n\n" +
                               "Workspace Name: *" + session.getCommunityName() + "*\n" +
                               "Workspace ID: *" + session.getCommunityId() + "*\n" +
                               "Admin Name: *" + session.getUserName() + "*\n\n" +
                               "I have completed my payment. Please verify and unlock my features!";
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/8801608533529?text=" + Uri.encode(verifyMsg)));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
