package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo = findViewById(R.id.imgLogo);
        TextView tvBrandName = findViewById(R.id.tvBrandName);

        // Fade-in Animation (1.5 seconds)
        imgLogo.animate().alpha(1f).setDuration(1500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        tvBrandName.animate().alpha(1f).setDuration(1500).setInterpolator(new AccelerateDecelerateInterpolator()).start();

        // Wait 2.5 seconds, then check login status
        new Handler().postDelayed(() -> {
            SessionManager session = new SessionManager(SplashActivity.this);
            
            // If communityId exists, they are already logged in -> Go to Dashboard
            if (session.getCommunityId() != null && !session.getCommunityId().isEmpty()) {
                startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
            } else {
                // Otherwise -> Go to Login
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish(); // Destroy Splash so user can't press "Back" to return to it
        }, 2500);
    }
}
