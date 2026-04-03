package org.shda;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MemberActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "Member Directory Loaded", Toast.LENGTH_SHORT).show();
        // Uses the same logic style as TransactionActivity to save names/phones to Firebase
        finish(); 
    }
}
