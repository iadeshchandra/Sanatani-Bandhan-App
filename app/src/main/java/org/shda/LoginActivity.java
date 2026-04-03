package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fallback layout creation for brevity. In a real app, use activity_login.xml
        setContentView(R.layout.activity_transactions); 
        
        EditText user = findViewById(R.id.inputTitle); user.setHint("Username (admin)");
        EditText pass = findViewById(R.id.inputAmount); pass.setHint("Password (admin123)");
        Button btn = findViewById(R.id.btnSave); btn.setText("Login");

        btn.setOnClickListener(v -> {
            if (user.getText().toString().equals("admin") && pass.getText().toString().equals("admin123")) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
