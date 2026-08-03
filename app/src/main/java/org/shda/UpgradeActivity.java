package org.shda;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Locale;

public class UpgradeActivity extends AppCompatActivity {

    private LinearLayout layoutPayment, layoutRestricted, layoutDynamicFeatures;
    private Button btnGoBack;

    private Button btnPayBD, btnPayIntl, btnCheckoutLink, btnVerifyPayment, btnCopyNumber, btnHowToPay;
    private MaterialCardView cardBanglaQR, cardIntlPayment;
    private TextView tvPriceBdt, tvPriceUsd;
    private SessionManager session;

    private final String INTL_PAYMENT_LINK = "https://wise.com/pay/me/adeshc"; 
    private final String MERCHANT_NUMBER = "01701987744"; 

    // Cached live limits for dynamic feature cards
    private long liveMemberLimit = 50;
    private long livePdfLimit = 3;
    private long liveSandeshLimit = 1;
    private long liveAuditLimit = 1;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        session = new SessionManager(this);

        layoutPayment = findViewById(R.id.layoutPayment);
        layoutRestricted = findViewById(R.id.layoutRestricted);
        btnGoBack = findViewById(R.id.btnGoBack);

        tvPriceBdt = findViewById(R.id.tvPriceBdt);
        tvPriceUsd = findViewById(R.id.tvPriceUsd);
        layoutDynamicFeatures = findViewById(R.id.layoutDynamicFeatures);

        btnPayBD = findViewById(R.id.btnPayBD);
        btnPayIntl = findViewById(R.id.btnPayIntl);
        cardBanglaQR = findViewById(R.id.cardBanglaQR);
        cardIntlPayment = findViewById(R.id.cardIntlPayment);
        btnCheckoutLink = findViewById(R.id.btnCheckoutLink);
        btnVerifyPayment = findViewById(R.id.btnVerifyPayment);
        btnCopyNumber = findViewById(R.id.btnCopyNumber);
        btnHowToPay = findViewById(R.id.btnHowToPay);

        String userRole = session.getRole();
        if ("MEMBER".equalsIgnoreCase(userRole) || "DEVOTEE".equalsIgnoreCase(userRole)) {
            layoutPayment.setVisibility(View.GONE);
            layoutRestricted.setVisibility(View.VISIBLE);
        } else {
            layoutPayment.setVisibility(View.VISIBLE);
            layoutRestricted.setVisibility(View.GONE);
            fetchLiveSaaSConfig();
        }

        btnGoBack.setOnClickListener(v -> finish());

        btnPayBD.setOnClickListener(v -> {
            cardBanglaQR.setVisibility(View.VISIBLE);
            cardIntlPayment.setVisibility(View.GONE);
            btnPayBD.setBackgroundColor(0xFF2E7D32); 
            btnPayBD.setTextColor(0xFFFFFFFF);
            btnPayIntl.setBackgroundColor(0xFFE0E0E0); 
            btnPayIntl.setTextColor(0xFF424242);
        });

        btnPayIntl.setOnClickListener(v -> {
            cardBanglaQR.setVisibility(View.GONE);
            cardIntlPayment.setVisibility(View.VISIBLE);
            btnPayIntl.setBackgroundColor(0xFF1976D2); 
            btnPayIntl.setTextColor(0xFFFFFFFF);
            btnPayBD.setBackgroundColor(0xFFE0E0E0); 
            btnPayBD.setTextColor(0xFF424242);
        });

        btnCopyNumber.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("TaliPay Number", MERCHANT_NUMBER);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(UpgradeActivity.this, "Number Copied! Open your payment App.", Toast.LENGTH_LONG).show();
            }
        });

        // ✨ Pop up the EXACT Instruction Manual Image 
        btnHowToPay.setOnClickListener(v -> {
            ImageView instructionImage = new ImageView(this);
            instructionImage.setImageResource(R.drawable.talipay_instructions); 
            instructionImage.setAdjustViewBounds(true);
            instructionImage.setPadding(20, 20, 20, 20);

            new AlertDialog.Builder(this)
                .setTitle("Supported Apps & Instructions")
                .setView(instructionImage)
                .setPositiveButton("CLOSE", null)
                .show();
        });

        btnCheckoutLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(INTL_PAYMENT_LINK));
            startActivity(browserIntent);
        });

        btnVerifyPayment.setOnClickListener(v -> showVerificationDialog());

        autoSelectPaymentTab();
    }

    private void fetchLiveSaaSConfig() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        db.getReference("app_config/global_settings").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("free_member_limit").getValue(Long.class) != null) 
                        liveMemberLimit = snapshot.child("free_member_limit").getValue(Long.class);
                    if (snapshot.child("free_pdf_limit").getValue(Long.class) != null) 
                        livePdfLimit = snapshot.child("free_pdf_limit").getValue(Long.class);
                    if (snapshot.child("free_sandesh_limit").getValue(Long.class) != null) 
                        liveSandeshLimit = snapshot.child("free_sandesh_limit").getValue(Long.class);
                    if (snapshot.child("free_audit_limit").getValue(Long.class) != null) 
                        liveAuditLimit = snapshot.child("free_audit_limit").getValue(Long.class);
                }
                fetchLivePricingAndFeatures(db);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                fetchLivePricingAndFeatures(db);
            }
        });
    }

    private void fetchLivePricingAndFeatures(FirebaseDatabase db) {
        db.getReference("platform_settings/samrat_pro").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long bdtPrice = snapshot.child("price_bdt").getValue(Long.class);
                    Long usdPrice = snapshot.child("price_usd").getValue(Long.class);

                    if (tvPriceBdt != null && bdtPrice != null) {
                        tvPriceBdt.setText("৳" + bdtPrice + " / Year");
                    }
                    if (tvPriceUsd != null && usdPrice != null) {
                        tvPriceUsd.setText("$" + usdPrice + " / Year");
                    }

                    if (layoutDynamicFeatures != null) {
                        layoutDynamicFeatures.removeAllViews();

                        addFeatureRow("✅ Unlimited Devotees (Free: Max " + liveMemberLimit + ")");
                        addFeatureRow("✅ Unlimited Master PDFs (Free: " + livePdfLimit + "/mo)");
                        addFeatureRow("✅ Unlimited Mass Sandesh (Free: " + liveSandeshLimit + "/mo)");
                        addFeatureRow("✅ Security Audit Logs (Free: " + liveAuditLimit + "/mo)");
                        addFeatureRow("✅ Priority Developer Support");

                        if (snapshot.hasChild("features")) {
                            for (DataSnapshot featureSnap : snapshot.child("features").getChildren()) {
                                String text = featureSnap.getValue(String.class);
                                if (text != null && !text.isEmpty() && !text.toLowerCase().contains("devotee")) {
                                    addFeatureRow("✅ " + text);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addFeatureRow(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#212121"));
        tv.setTextSize(13f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 6);
        tv.setLayoutParams(params);
        layoutDynamicFeatures.addView(tv);
    }

    private void autoSelectPaymentTab() {
        String countryCode = "";
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                countryCode = tm.getNetworkCountryIso();
                if (countryCode == null || countryCode.isEmpty()) {
                    countryCode = tm.getSimCountryIso();
                }
            }
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = Locale.getDefault().getCountry();
            }
        } catch (Exception e) {
            countryCode = Locale.getDefault().getCountry();
        }

        if (countryCode != null && countryCode.equalsIgnoreCase("bd")) {
            btnPayBD.performClick();
        } else {
            btnPayIntl.performClick();
        }
    }

    private void showVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Verify Dakshina");
        tvTitle.setTextSize(20f);
        tvTitle.setTextColor(Color.parseColor("#E65100"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 20);
        layout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Please enter your payment details below. Our backend team will verify your transaction and activate SAMRAT PRO.");
        tvDesc.setTextColor(Color.parseColor("#424242"));
        tvDesc.setPadding(0, 0, 0, 30);
        layout.addView(tvDesc);

        final EditText inputContact = new EditText(this);
        inputContact.setHint("Your Phone Number or Email");
        inputContact.setBackgroundResource(android.R.drawable.edit_text);
        inputContact.setPadding(30, 30, 30, 30);
        layout.addView(inputContact);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        layout.addView(spacer);

        final EditText inputTrxId = new EditText(this);
        inputTrxId.setHint("Transaction ID (bKash / Nagad / TaliPay)");
        inputTrxId.setBackgroundResource(android.R.drawable.edit_text);
        inputTrxId.setPadding(30, 30, 30, 30);
        layout.addView(inputTrxId);

        builder.setView(layout);
        builder.setPositiveButton("SUBMIT", null); 
        builder.setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String contact = inputContact.getText().toString().trim();
            String trxId = inputTrxId.getText().toString().trim();

            if (contact.isEmpty() || trxId.isEmpty()) {
                Toast.makeText(UpgradeActivity.this, "Both fields are required for verification.", Toast.LENGTH_SHORT).show();
                return;
            }

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Verifying...");

            String pushKey = FirebaseDatabase.getInstance().getReference().child("upgrade_requests").push().getKey();

            HashMap<String, Object> requestData = new HashMap<>();
            requestData.put("requestId", pushKey);
            requestData.put("communityId", session.getCommunityId());
            requestData.put("communityName", session.getCommunityName());
            requestData.put("adminName", session.getUserName());
            requestData.put("contactInfo", contact);
            requestData.put("transactionId", trxId);
            requestData.put("timestamp", ServerValue.TIMESTAMP);
            requestData.put("status", "PENDING"); 

            if (pushKey != null) {
                FirebaseDatabase.getInstance().getReference()
                    .child("upgrade_requests")
                    .child(pushKey)
                    .setValue(requestData)
                    .addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        new AlertDialog.Builder(UpgradeActivity.this)
                            .setTitle("✅ Request Submitted")
                            .setMessage("Thank you! Your Dakshina details have been securely sent to our backend.\n\nYour workspace will be upgraded to SAMRAT PRO upon verification.")
                            .setPositiveButton("OK", (d, w) -> finish())
                            .show();
                    })
                    .addOnFailureListener(e -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("SUBMIT");
                        Toast.makeText(UpgradeActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    });
            }
        });
    }
}
