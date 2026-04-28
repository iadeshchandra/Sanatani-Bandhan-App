package org.shda;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class UpgradeActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseReference db;

    private RadioGroup rgPaymentMethod;
    private RadioButton rbBangladesh, rbInternational;
    private TextView tvPaymentInstructions, tvManualNumber;
    private ImageView imgQrCode;
    private EditText inputTrxId;
    private Button btnSubmitUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        session = new SessionManager(this);
        db = FirebaseDatabase.getInstance().getReference();

        // If they are already premium, don't let them stay on this screen!
        if ("PREMIUM".equals(session.getPlan())) {
            Toast.makeText(this, "Workspace is already Premium!", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbBangladesh = findViewById(R.id.rbBangladesh);
        rbInternational = findViewById(R.id.rbInternational);
        tvPaymentInstructions = findViewById(R.id.tvPaymentInstructions);
        tvManualNumber = findViewById(R.id.tvManualNumber);
        imgQrCode = findViewById(R.id.imgQrCode);
        inputTrxId = findViewById(R.id.inputTrxId);
        btnSubmitUpgrade = findViewById(R.id.btnSubmitUpgrade);

        // Smart UI Switching based on Currency
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBangladesh) {
                setupTallyPayUI();
            } else if (checkedId == R.id.rbInternational) {
                setupPayoneerUI();
            }
        });

        btnSubmitUpgrade.setOnClickListener(v -> submitUpgradeRequest());
    }

    private void setupTallyPayUI() {
        tvPaymentInstructions.setText("Scan with bKash, Nagad, or Rocket to Pay via TallyPay (৳500).");
        imgQrCode.setVisibility(View.VISIBLE); // Shows the TallyPay QR
        tvManualNumber.setText("Or Send Money to: 01701 987 744");
    }

    private void setupPayoneerUI() {
        tvPaymentInstructions.setText("Please transfer $5 USD to our official Payoneer account.");
        imgQrCode.setVisibility(View.GONE); // Hide QR for international
        tvManualNumber.setText("Payoneer Email: iadeshchandra@gmail.com");
    }

    private void submitUpgradeRequest() {
        String trxId = inputTrxId.getText().toString().trim();

        if (trxId.isEmpty()) {
            Toast.makeText(this, "Please enter your Transaction ID", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitUpgrade.setEnabled(false);
        btnSubmitUpgrade.setText("SUBMITTING REQUEST...");

        String method = rbBangladesh.isChecked() ? "TALLYPAY/BKASH" : "PAYONEER";
        String requestId = db.child("admin_requests").child("upgrades").push().getKey();

        HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("workspaceId", session.getCommunityId());
        requestMap.put("communityName", session.getCommunityName());
        requestMap.put("adminName", session.getUserName());
        requestMap.put("adminEmail", session.getWorkspaceEmail());
        requestMap.put("paymentMethod", method);
        requestMap.put("trxId", trxId);
        requestMap.put("status", "PENDING_VERIFICATION");
        requestMap.put("timestamp", System.currentTimeMillis());

        // We push this to a secure top-level node for YOU to verify.
        db.child("admin_requests").child("upgrades").child(requestId).setValue(requestMap)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Request Received! Your Premium plan will be activated within 12 hours.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    btnSubmitUpgrade.setEnabled(true);
                    btnSubmitUpgrade.setText("SUBMIT PAYMENT FOR VERIFICATION");
                }
            });
    }
}
