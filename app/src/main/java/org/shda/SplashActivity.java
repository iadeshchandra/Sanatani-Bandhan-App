package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide the top action bar for a full-screen immersive feel
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Wait for 2.5 seconds (2500 milliseconds), then go to Login
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish(); // Close the splash screen so the user can't hit the "back" button to return to it
        }, 2500);
    }
}
