package org.shda;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SamratProUpgradeActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvPlanTitle;
    private TextView tvPlanPrice;
    private TextView tvPlanFeatures;
    private Button btnUpgrade;
    private ProgressBar progressBar;

    // Firebase Backend Reference
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Link to your XML layout design
        setContentView(R.layout.activity_samrat_pro_upgrade);

        // Initialize UI components matching your XML IDs
        tvPlanTitle = findViewById(R.id.tvPlanTitle);
        tvPlanPrice = findViewById(R.id.tvPlanPrice);
        tvPlanFeatures = findViewById(R.id.tvPlanFeatures);
        btnUpgrade = findViewById(R.id.btnUpgrade);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Firestore database
        db = FirebaseFirestore.getInstance();

        // Setup initial UI state (hide text and disable button until data loads)
        btnUpgrade.setEnabled(false);
        tvPlanPrice.setVisibility(View.GONE);
        tvPlanFeatures.setVisibility(View.GONE);

        // Fetch dynamic pricing and features from the backend
        fetchPlanDetailsFromBackend();

        // Handle the upgrade button click
        btnUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiatePaymentProcess();
            }
        });
    }

    private void fetchPlanDetailsFromBackend() {
        // Show loading spinner while fetching data
        progressBar.setVisibility(View.VISIBLE);

        // Access the 'Subscriptions' collection and 'SamratPro' document in Firestore
        DocumentReference docRef = db.collection("Subscriptions").document("SamratPro");
        
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Hide loading spinner once the backend responds
                progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        
                        // Extract dynamically updated price and features from Firestore
                        String price = document.getString("price");
                        String features = document.getString("features");
                        
                        // Apply the backend data to our screen
                        if (price != null) {
                            tvPlanPrice.setText(price);
                        }
                        
                        if (features != null) {
                            // Properly format new lines sent from the backend
                            features = features.replace("\\n", "\n");
                            tvPlanFeatures.setText(features);
                        }

                        // Make the updated details visible to the user and enable the button
                        tvPlanFeatures.setVisibility(View.VISIBLE);
                        tvPlanPrice.setVisibility(View.VISIBLE);
                        btnUpgrade.setEnabled(true);
                        
                    } else {
                        Toast.makeText(SamratProUpgradeActivity.this, "Plan details not found in database.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SamratProUpgradeActivity.this, "Failed to load plan details. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initiatePaymentProcess() {
        // Future integration: Add your payment gateway logic here (e.g., SSLCommerz, Google Play Billing)
        Toast.makeText(this, "Redirecting to secure payment gateway...", Toast.LENGTH_LONG).show();
    }
}
