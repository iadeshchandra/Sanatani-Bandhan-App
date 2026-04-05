package org.shda;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private PieChart pieChart;
    
    private float totalIncome = 0f;
    private float totalExpense = 0f;

    // ✨ THE 30-MINUTE SHLOKA ROTATION ENGINE (JSON Powered)
    private Handler shlokaHandler = new Handler(Looper.getMainLooper());
    private Runnable shlokaRunnable = new Runnable() {
        @Override
        public void run() {
            TextView tv = findViewById(R.id.shlokaText);
            if (tv != null) {
                tv.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                    tv.setText(ShlokaEngine.getRandomShloka(DashboardActivity.this));
                    tv.animate().alpha(1f).setDuration(500);
                });
            }
            shlokaHandler.postDelayed(this, 30 * 60 * 1000);
        }
    };

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
        pieChart = findViewById(R.id.pieChart);
        
        setupDates();
        applyPermissions(session.getRole());
        setupNavigation();
        setupVisualAnalytics(); // Load the Chart!

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

    @Override
    protected void onResume() {
        super.onResume();
        shlokaHandler.post(shlokaRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        shlokaHandler.removeCallbacks(shlokaRunnable);
    }

    private void setupDates() {
        TextView tvDateEnglish = findViewById(R.id.tvDateEnglish);
        TextView tvDateBengali = findViewById(R.id.tvDateBengali);
        
        Date today = new Date();
        SimpleDateFormat engFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH);
        SimpleDateFormat benFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD"));
        
        tvDateEnglish.setText("🕉 " + engFormat.format(today));
        tvDateBengali.setText("শুভ দিন: " + benFormat.format(today));
    }

    private void applyPermissions(String role) {
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

    // 📊 LIVE VISUAL ANALYTICS LOGIC
    private void setupVisualAnalytics() {
        // Setup Chart Appearance
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setCenterText("Net Balance\nCalculating...");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#E65100"));
        pieChart.getLegend().setTextSize(12f);

        String logsPath = "communities/" + session.getCommunityId() + "/logs";

        // Listen for Chanda (Income)
        db.child(logsPath).child("Donation").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalIncome = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Float amt = data.child("amount").getValue(Float.class);
                    if (amt != null) totalIncome += amt;
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listen for Utsav Costs (Expenses)
        db.child(logsPath).child("Expense").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalExpense = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Float amt = data.child("amount").getValue(Float.class);
                    if (amt != null) totalExpense += amt;
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        
        // Add Data
        if (totalIncome > 0) entries.add(new PieEntry(totalIncome, "Total Chanda"));
        if (totalExpense > 0) entries.add(new PieEntry(totalExpense, "Utsav Expenses"));

        // If completely empty, show placeholder
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, "No Data Yet"));
            pieChart.setCenterText("Welcome!");
        } else {
            float netBalance = totalIncome - totalExpense;
            pieChart.setCenterText("Net Balance\n৳" + netBalance);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        
        // Colors: Green for Income, Red for Expense
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#388E3C")); // Green
        if (totalExpense > 0) colors.add(Color.parseColor("#D32F2F")); // Red
        else if (entries.size() == 1 && totalIncome == 0) colors.add(Color.LTGRAY); // Placeholder
        dataSet.setColors(colors);

        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        
        // Animate the chart drawing beautifully
        pieChart.animateY(1000);
        pieChart.invalidate(); 
    }

    private void setupNavigation() {
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        findViewById(R.id.cardComms).setOnClickListener(v -> startActivity(new Intent(this, CommsActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));

        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> {
            Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCal.set(year, month, dayOfMonth, 0, 0, 0); 
                new android.app.DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                    endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                    long startTimestamp = startCal.getTimeInMillis(); long endTimestamp = endCal.getTimeInMillis();
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String reportRange = "Period: " + displayFormat.format(startCal.getTime()) + " to " + displayFormat.format(endCal.getTime());

                    Toast.makeText(this, "Auditing Financials...", Toast.LENGTH_SHORT).show();
                    db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
                      .orderByChild("timestamp").startAt(startTimestamp).endAt(endTimestamp)
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<String> names = new ArrayList<>(); List<Float> amounts = new ArrayList<>();
                            List<String> notes = new ArrayList<>(); List<String> dates = new ArrayList<>();
                            float total = 0f; SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                            for (DataSnapshot data : snapshot.getChildren()) {
                                String name = data.child("name").getValue(String.class); Float amt = data.child("amount").getValue(Float.class);
                                String note = data.child("note").getValue(String.class); Long ts = data.child("timestamp").getValue(Long.class);
                                if(name != null && amt != null) {
                                    names.add(name); amounts.add(amt); notes.add(note != null ? note : "No note");
                                    dates.add(ts != null ? sdf.format(new Date(ts)) : "Unknown Date"); total += amt;
                                }
                            }
                            PdfReportService.generateFinancialReport(DashboardActivity.this, session.getCommunityName(), dates, names, amounts, notes, total, reportRange);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        findViewById(R.id.btnDownloadAudit).setOnClickListener(v -> {
            Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCal.set(year, month, dayOfMonth, 0, 0, 0); 
                new android.app.DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                    endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                    long startTimestamp = startCal.getTimeInMillis(); long endTimestamp = endCal.getTimeInMillis();
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String reportRange = "Audit Period: " + displayFormat.format(startCal.getTime()) + " to " + displayFormat.format(endCal.getTime());

                    Toast.makeText(this, "Compiling Audit Logs...", Toast.LENGTH_SHORT).show();
                    db.child("communities").child(session.getCommunityId()).child("audit_logs")
                      .orderByChild("timestamp").startAt(startTimestamp).endAt(endTimestamp)
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<String> auditEntries = new ArrayList<>();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                            for (DataSnapshot data : snapshot.getChildren()) {
                                String manager = data.child("managerName").getValue(String.class);
                                String action = data.child("actionType").getValue(String.class);
                                String desc = data.child("description").getValue(String.class);
                                Long ts = data.child("timestamp").getValue(Long.class);
                                if (manager != null) {
                                    String dateStr = ts != null ? sdf.format(new Date(ts)) : "Unknown Date";
                                    auditEntries.add(dateStr + "\nUser: " + manager + " (" + action + ")\nDetails: " + desc);
                                }
                            }
                            PdfReportService.generateAuditReport(DashboardActivity.this, session.getCommunityName(), auditEntries, reportRange);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void showEditCommunityDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Edit Community Name");
        builder.setMessage("This name will appear on all new PDFs and reports.");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(session.getCommunityName());
        
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setPadding(50, 20, 50, 0); container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) { updateCommunityNameInDatabase(newName); } 
            else { Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateCommunityNameInDatabase(String newName) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            db.child("users").child(uid).child("communityName").setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    session.updateCommunityName(newName);
                    ((TextView) findViewById(R.id.tvDashboardTitle)).setText(newName);
                    Toast.makeText(this, "Community Name Updated", Toast.LENGTH_SHORT).show();
                    AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "SETTINGS_CHANGED", "Updated community name to: " + newName);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show());
        } else { Toast.makeText(this, "Authentication error. Only Admins can do this.", Toast.LENGTH_SHORT).show(); }
    }
}
