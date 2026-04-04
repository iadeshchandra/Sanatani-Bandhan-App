// Keep your existing imports...
package org.shda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    // ... (Keep your existing variable declarations and onCreate method exactly as they were) ...
    private DatabaseReference db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); return;
        }

        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        setupDynamicShloka();
        setupDates();
        applyPermissions(session.getRole());
        setupNavigation();

        findViewById(R.id.tvDashboardBranding).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://linktr.ee/Adesh_Chandra"));
            startActivity(browserIntent);
        });

        findViewById(R.id.btnEditCommunity).setOnClickListener(v -> showEditCommunityDialog());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    // ... (Keep setupDates and setupDynamicShloka exactly as they were) ...
    private void setupDates() {
        TextView tvDateEnglish = findViewById(R.id.tvDateEnglish);
        TextView tvDateBengali = findViewById(R.id.tvDateBengali);
        Date today = new Date();
        tvDateEnglish.setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(today));
        tvDateBengali.setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(today));
    }
    
    private void setupDynamicShloka() {
        TextView tv = findViewById(R.id.shlokaText);
        String[] shlokas = {
            "“You have the right to work, but for the work's sake only...”\n\n- Bhagavad Gita 2.47",
            "“Whenever dharma declines and the purpose of life is forgotten...”\n\n- Bhagavad Gita 4.7",
            "“Truth is one, paths are many.”\n\n- Rig Veda"
        };
        tv.setText(shlokas[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % shlokas.length]);
    }

    private void applyPermissions(String role) {
        // 🌟 THE FIX: Members can see ALL cards and reports! We only hide Admin tools.
        if (role.equals("MEMBER")) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
        } else if (role.equals("MANAGER")) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
        } else if (role.equals("ADMIN")) {
            findViewById(R.id.btnEditCommunity).setVisibility(View.VISIBLE);
        }
    }

    // ... (Keep setupNavigation, showEditCommunityDialog, updateCommunityNameInDatabase exactly as they were) ...
    // Note: To save text space, leave the rest of your DashboardActivity file untouched! Just make sure applyPermissions() looks exactly like the block above.
