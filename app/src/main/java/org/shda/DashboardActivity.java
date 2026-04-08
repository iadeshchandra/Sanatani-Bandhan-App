package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private PieChart pieChart;
    private float totalIncome = 0f;
    private float totalExpense = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        pieChart = findViewById(R.id.pieChart);
        
        // Header Setup
        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        
        // Date & Tithi Setup
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
        ((TextView) findViewById(R.id.tvTithiAlert)).setText("Today's Tithi: Loading...");
        
        // Shloka Setup
        ((TextView) findViewById(R.id.shlokaText)).setText("\"Karmanye vadhikaraste Ma Phaleshu Kadachana\"\n- Bhagavad Gita");

        // Action Buttons
        findViewById(R.id.btnEditCommunity).setOnClickListener(v -> showEditCommunityDialog());
        findViewById(R.id.btnPanjika).setOnClickListener(v -> startActivity(new Intent(this, PanjikaActivity.class)));
        findViewById(R.id.btnFilterChartDate).setOnClickListener(v -> Toast.makeText(this, "Date filter coming in Phase 2!", Toast.LENGTH_SHORT).show());
        
        // Grid Buttons
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        findViewById(R.id.cardComms).setOnClickListener(v -> startActivity(new Intent(this, CommsActivity.class)));

        // Utility Buttons
        findViewById(R.id.btnMyProfile).setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));
        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> Toast.makeText(this, "Use specific modules to generate PDFs.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnChangeLanguage).setOnClickListener(v -> showLanguageDialog());
        findViewById(R.id.btnHelpSupport).setOnClickListener(v -> contactSupport());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            session.logoutUser();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // ✨ FIX: Workspace Settings Wired Properly
        Button btnWorkspace = findViewById(R.id.btnWorkspaceSettings);
        if (btnWorkspace != null) btnWorkspace.setOnClickListener(v -> showEditCommunityDialog());

        // Role based restrictions
        if (!"ADMIN".equals(session.getRole())) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
            if (btnWorkspace != null) btnWorkspace.setVisibility(View.GONE);
        } else {
            findViewById(R.id.btnDownloadAudit).setOnClickListener(v -> PdfReportService.generateSecurityAudit(this, session.getCommunityId()));
        }

        loadFinancialData();
    }

    private void loadFinancialData() {
        DatabaseReference logsRef = db.child("communities").child(session.getCommunityId()).child("logs");
        logsRef.keepSynced(true);
        logsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalIncome = 0f;
                totalExpense = 0f;
                
                if (snapshot.hasChild("Donation")) {
                    for (DataSnapshot d : snapshot.child("Donation").getChildren()) {
                        Float amt = d.child("amount").getValue(Float.class);
                        if (amt != null) totalIncome += amt;
                    }
                }
                
                if (snapshot.hasChild("Expense")) {
                    for (DataSnapshot d : snapshot.child("Expense").getChildren()) {
                        Float amt = d.child("amount").getValue(Float.class);
                        if (amt != null) totalExpense += amt;
                    }
                }
                setupPieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupPieChart() {
        if (pieChart == null) return;
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (totalIncome == 0 && totalExpense == 0) {
            entries.add(new PieEntry(1f, "No Data"));
        } else {
            entries.add(new PieEntry(totalIncome, "Income"));
            entries.add(new PieEntry(totalExpense, "Expense"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(android.graphics.Color.parseColor("#2E7D32"), android.graphics.Color.parseColor("#D32F2F"), android.graphics.Color.GRAY);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Net Balance\n৳" + (totalIncome - totalExpense));
        pieChart.setCenterTextSize(16f);
        pieChart.setDrawEntryLabels(false);
        pieChart.invalidate();
    }

    private void showEditCommunityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Workspace Settings");
        final EditText input = new EditText(this);
        input.setText(session.getCommunityName());
        builder.setView(input);
        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                db.child("communities").child(session.getCommunityId()).child("name").setValue(newName);
                session.setCommunityName(newName);
                ((TextView) findViewById(R.id.tvDashboardTitle)).setText(newName);
                Toast.makeText(this, "Workspace Updated", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "বাংলা (Bengali)", "हिन्दी (Hindi)"};
        new AlertDialog.Builder(this).setTitle("Select Language")
            .setItems(languages, (dialog, which) -> Toast.makeText(this, "Language switched locally.", Toast.LENGTH_SHORT).show())
            .show();
    }

    private void contactSupport() {
        Toast.makeText(this, "Redirecting to Sanatani Bandhan Support...", Toast.LENGTH_SHORT).show();
    }
}
