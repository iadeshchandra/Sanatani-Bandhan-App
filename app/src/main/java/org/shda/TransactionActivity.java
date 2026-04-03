package org.shda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class TransactionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        EditText inputTitle = findViewById(R.id.inputTitle);
        EditText inputAmount = findViewById(R.id.inputAmount);
        Button btnSave = findViewById(R.id.btnSave);

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        btnSave.setOnClickListener(v -> {
            String type = typeSpinner.getSelectedItem().toString(); // Donation, Social Expense, or Festival Expense
            String title = inputTitle.getText().toString();
            String amountStr = inputAmount.getText().toString();

            if (!title.isEmpty() && !amountStr.isEmpty()) {
                HashMap<String, Object> data = new HashMap<>();
                data.put("title", title);
                data.put("amount", Float.parseFloat(amountStr));
                data.put("timestamp", System.currentTimeMillis());

                // Save to logs
                db.child("logs").child(type).push().setValue(data);
                
                // Update running totals for the graph
                db.child("finances").child(type).get().addOnSuccessListener(snap -> {
                    float current = snap.exists() ? snap.getValue(Float.class) : 0f;
                    db.child("finances").child(type).setValue(current + Float.parseFloat(amountStr));
                });

                Toast.makeText(this, type + " Saved!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
