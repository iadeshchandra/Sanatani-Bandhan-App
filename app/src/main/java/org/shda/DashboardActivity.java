package org.shda;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    private String workspaceType = "Community"; 

    private Button btnUpgradeBadge;
    private TextView tvDashboardBranding;

    private Long chartStartTs = null;
    private Long chartEndTs = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getUserId() == null || session.getCommunityId() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnUpgradeBadge = findViewById(R.id.btnUpgradeBadge);
        tvDashboardBranding = findViewById(R.id.tvDashboardBranding);
        pieChart = findViewById(R.id.pieChart);

        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
        ((TextView) findViewById(R.id.tvTithiAlert)).setText("Today's Tithi: Loading...");
        ((TextView) findViewById(R.id.shlokaText)).setText("\"Karmanye vadhikaraste Ma Phaleshu Kadachana\"\n- Bhagavad Gita");

        syncWorkspacePlan();

        findViewById(R.id.btnPanjika).setOnClickListener(v -> startActivity(new Intent(this, PanjikaActivity.class)));
        findViewById(R.id.btnFilterChartDate).setOnClickListener(v -> showChartDateFilterDialog());

        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        
        // 🔒 THE GATEKEEPER: Mass Sandesh (1 Free Per Month)
        findViewById(R.id.cardComms).setOnClickListener(v -> 
            checkQuotaAndProceed("sandesh_sent", 1, () -> startActivity(new Intent(this, CommsActivity.class)))
        );

        // 🔒 THE GATEKEEPER: Master Reports (3 Free Per Month)
        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> 
            checkQuotaAndProceed("pdfs_generated", 3, this::showGlobalPdfGeneratorDialog)
        );

        if (!"ADMIN".equals(session.getRole())) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
        } else {
            // 🔒 THE GATEKEEPER: Security Audit (1 Free Per Month)
            findViewById(R.id.btnDownloadAudit).setOnClickListener(v -> 
                checkQuotaAndProceed("audits_downloaded", 1, () -> {
                    new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.btn_security_audit))
                        .setItems(new String[]{"Specific Date Range", "All Time"}, (dialog, which) -> {
                            if (which == 0) {
                                pickDateRange((startTs, endTs) -> generateAuditPdf(startTs, endTs));
                            } else {
                                generateAuditPdf(0, Long.MAX_VALUE);
                            }
                        }).show();
                })
            );
        }

        findViewById(R.id.btnMyProfile).setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));
        findViewById(R.id.btnChangeLanguage).setOnClickListener(v -> showLanguageDialog());
        findViewById(R.id.btnHelpSupport).setOnClickListener(v -> contactSupport());
        findViewById(R.id.btnWorkspaceSettings).setOnClickListener(v -> startActivity(new Intent(this, CommunityInfoActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                session.logout(); 
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Logout processed with minor errors", Toast.LENGTH_SHORT).show();
            }
        });

        loadWorkspaceType();
        loadFinancialData();
    }

    private void syncWorkspacePlan() {
        db.child("communities").child(session.getCommunityId()).child("info").child("plan")
          .addValueEventListener(new ValueEventListener() {
              @Override
              public void onDataChange(@NonNull DataSnapshot snapshot) {
                  String currentPlan = snapshot.getValue(String.class);
                  if (currentPlan != null) {
                      session.setPlan(currentPlan);
                  } else {
                      db.child("communities").child(session.getCommunityId()).child("info").child("plan").setValue("FREE");
                      session.setPlan("FREE");
                  }
                  updatePlanBadgeUI();
              }
              @Override
              public void onCancelled(@NonNull DatabaseError error) {}
          });
    }

    // ✨ THE NEW DYNAMIC BADGE ENGINE
    private void updatePlanBadgeUI() {
        String role = session.getRole();
        
        // Hide badge for regular members to keep their UI peaceful and simple
        if ("MEMBER".equalsIgnoreCase(role) || "DEVOTEE".equalsIgnoreCase(role)) {
            btnUpgradeBadge.setVisibility(View.GONE);
            tvDashboardBranding.setVisibility(View.VISIBLE);
            tvDashboardBranding.setText("Sanatani Bandhan Community");
        } else {
            // Admin and Manager View
            tvDashboardBranding.setVisibility(View.GONE);
            btnUpgradeBadge.setVisibility(View.VISIBLE);

            if ("PREMIUM".equalsIgnoreCase(session.getPlan())) {
                btnUpgradeBadge.setText("👑 SAMRAT PRO ACTIVE");
                btnUpgradeBadge.setBackgroundTintList(ColorStateList.valueOf(0xFF388E3C)); // Rich Green
                btnUpgradeBadge.setOnClickListener(v -> 
                    Toast.makeText(DashboardActivity.this, "Your Mandir is fully upgraded!", Toast.LENGTH_SHORT).show()
                );
            } else {
                btnUpgradeBadge.setText("⭐ SEVA FREE PLAN - TAP TO UPGRADE");
                btnUpgradeBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE65100)); // Alert Orange
                btnUpgradeBadge.setOnClickListener(v -> 
                    startActivity(new Intent(DashboardActivity.this, UpgradeActivity.class))
                );
            }
        }
    }

    // ✨ THE MONTHLY CHAKRA GATEKEEPER ENGINE ✨
    private void checkQuotaAndProceed(String feature, int freeLimit, Runnable action) {
        if ("PREMIUM".equalsIgnoreCase(session.getPlan())) {
            action.run(); 
            return;
        }

        String currentMonth = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
        DatabaseReference usageRef = db.child("communities").child(session.getCommunityId()).child("usage_tracking");

        usageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usage usage = snapshot.getValue(Usage.class);
                
                if (usage == null || !currentMonth.equals(usage.current_month)) {
                    usage = new Usage();
                    usage.current_month = currentMonth;
                }

                int currentUsage = 0;
                if (feature.equals("pdfs_generated")) currentUsage = usage.pdfs_generated;
                else if (feature.equals("sandesh_sent")) currentUsage = usage.sandesh_sent;
                else if (feature.equals("audits_downloaded")) currentUsage = usage.audits_downloaded;

                if (currentUsage < freeLimit) {
                    if (feature.equals("pdfs_generated")) usage.pdfs_generated++;
                    else if (feature.equals("sandesh_sent")) usage.sandesh_sent++;
                    else if (feature.equals("audits_downloaded")) usage.audits_downloaded++;

                    usageRef.setValue(usage);
                    
                    int remaining = freeLimit - (currentUsage + 1);
                    Toast.makeText(DashboardActivity.this, "Free Seva Limit: " + remaining + " remaining this month.", Toast.LENGTH_SHORT).show();
                    
                    action.run();
                } else {
                    startActivity(new Intent(DashboardActivity.this, UpgradeActivity.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChartDateFilterDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.txt_filter_dates))
            .setItems(new String[]{"Select Specific Date Range", "Clear Filter (All Time)"}, (dialog, which) -> {
                if (which == 0) {
                    pickDateRange((startTs, endTs) -> {
                        chartStartTs = startTs;
                        chartEndTs = endTs;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                        ((Button) findViewById(R.id.btnFilterChartDate)).setText("FILTER ACTIVE: " + sdf.format(new Date(startTs)) + " - " + sdf.format(new Date(endTs)));
                        loadFinancialData();
                    });
                } else {
                    chartStartTs = null;
                    chartEndTs = null;
                    ((Button) findViewById(R.id.btnFilterChartDate)).setText(getString(R.string.txt_filter_dates));
                    loadFinancialData();
                }
            }).show();
    }

    private void generateAuditPdf(long startTs, long endTs) {
        db.child("communities").child(session.getCommunityId()).child("audit_logs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AuditLog> logs = new ArrayList<>();
                for (DataSnapshot d : snapshot.getChildren()) {
                    AuditLog log = d.getValue(AuditLog.class);
                    if (log != null && log.timestamp >= startTs && log.timestamp <= endTs) {
                        logs.add(log);
                    }
                }
                if (logs.isEmpty()) {
                    Toast.makeText(DashboardActivity.this, "No audit logs found for this range.", Toast.LENGTH_SHORT).show();
                } else {
                    String title = startTs > 0 ? "Security Audit Report (Filtered)" : "Security Audit Report (All Time)";
                    PdfReportService.generateSecurityAudit(DashboardActivity.this, session.getCommunityName(), logs, title);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadWorkspaceType() {
        db.child("communities").child(session.getCommunityId()).child("info").child("type").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String type = snapshot.getValue(String.class);
                if (type != null && !type.isEmpty()) workspaceType = type;
                Button btnWorkspace = findViewById(R.id.btnWorkspaceSettings);
                if (btnWorkspace != null) {
                    btnWorkspace.setText("🏛️ " + workspaceType.toUpperCase() + " " + getString(R.string.txt_info_settings));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Bengali (বাংলা)", "Hindi (हिन्दी)"};
        String[] langCodes = {"en", "bn", "hi"}; 
        
        new AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages, (dialog, which) -> {
                LocaleHelper.setLocale(DashboardActivity.this, langCodes[which]);
                Toast.makeText(this, "Language updated to " + languages[which], Toast.LENGTH_SHORT).show();
                
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }).show();
    }

    private void contactSupport() {
        String finalMessage = "🙏 *Namaskar / Jay Sanatan Dharma* 🙏\n\n" +
                              "🛠️ *SYSTEM SUPPORT REQUEST*\n\n" +
                              "Workspace: *" + session.getCommunityName() + "* (" + session.getCommunityId() + ")\n" +
                              "User: *" + session.getUserName() + "* (" + session.getUserId() + ")\n\n" +
                              "Please describe your issue here:\n\n\n" +
                              "-----------------------------------\n" +
                              "Sent via *" + session.getCommunityName() + " Portal*\n" +
                              "Powered by Sanatani SaaS";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/8801608533529?text=" + Uri.encode(finalMessage)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGlobalPdfGeneratorDialog() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.btn_generate_pdfs))
            .setItems(new String[]{"Donation Ledger (Select Dates)", "Expenses Ledger (Select Dates)", "Income vs Expense Comparison (Select Dates)"}, (dialog, which) -> {
                pickDateRange((startTs, endTs) -> {
                    if (which == 0) generateGlobalChandaPdf(startTs, endTs);
                    else if (which == 1) generateGlobalExpensePdf(startTs, endTs);
                    else generateGlobalComparisonPdf(startTs, endTs); 
                });
            }).show();
    }

    private void pickDateRange(DateRangeCallback callback) {
        final Calendar startCal = Calendar.getInstance();
        new DatePickerDialog(this, (view1, y1, m1, d1) -> {
            startCal.set(y1, m1, d1, 0, 0, 0);
            final Calendar endCal = Calendar.getInstance();
            new DatePickerDialog(this, (view2, y2, m2, d2) -> {
                endCal.set(y2, m2, d2, 23, 59, 59);
                if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                else callback.onSelected(startCal.getTimeInMillis(), endCal.getTimeInMillis());
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            Toast.makeText(this, "Now select End Date", Toast.LENGTH_SHORT).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }
    private interface DateRangeCallback { void onSelected(long start, long end); }

    private void generateGlobalChandaPdf(long startTs, long endTs) {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Donation").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> dates = new ArrayList<>(); List<String> names = new ArrayList<>();
                List<Float> amounts = new ArrayList<>(); List<String> notes = new ArrayList<>();
                float totalExport = 0f;
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                for (DataSnapshot d : snapshot.getChildren()) {
                    TransactionActivity.SingleDonation sd = d.getValue(TransactionActivity.SingleDonation.class);
                    if (sd != null && sd.timestamp >= startTs && sd.timestamp <= endTs) {
                        dates.add(sdf.format(new Date(sd.timestamp)));
                        names.add(sd.name); amounts.add(sd.amount); notes.add(sd.note != null ? sd.note : "");
                        totalExport += sd.amount;
                    }
                }
                if (dates.isEmpty()) Toast.makeText(DashboardActivity.this, "No Donations found in this range.", Toast.LENGTH_SHORT).show();
                else PdfReportService.generateFinancialReport(DashboardActivity.this, session.getCommunityName(), dates, names, amounts, notes, totalExport, "Custom Date Donation Ledger");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void generateGlobalExpensePdf(long startTs, long endTs) {
        db.child("communities").child(session.getCommunityId()).child("logs").child("Expense").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ExpenseActivity.Expense> exps = new ArrayList<>();
                float totalExport = 0f;
                for (DataSnapshot d : snapshot.getChildren()) {
                    ExpenseActivity.Expense e = d.getValue(ExpenseActivity.Expense.class);
                    if (e != null && e.timestamp >= startTs && e.timestamp <= endTs) {
                        exps.add(e); totalExport += e.amount;
                    }
                }
                if (exps.isEmpty()) Toast.makeText(DashboardActivity.this, "No Expenses found in this range.", Toast.LENGTH_SHORT).show();
                else PdfReportService.generateExpenseReport(DashboardActivity.this, session.getCommunityName(), exps, totalExport, "Custom Date Expenses Ledger");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void generateGlobalComparisonPdf(long startTs, long endTs) {
        db.child("communities").child(session.getCommunityId()).child("logs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<TransactionActivity.SingleDonation> donations = new ArrayList<>();
                List<ExpenseActivity.Expense> expenses = new ArrayList<>();
                float totalInc = 0f; float totalExp = 0f;

                if (snapshot.hasChild("Donation")) {
                    for (DataSnapshot d : snapshot.child("Donation").getChildren()) {
                        TransactionActivity.SingleDonation sd = d.getValue(TransactionActivity.SingleDonation.class);
                        if (sd != null && sd.timestamp >= startTs && sd.timestamp <= endTs) {
                            donations.add(sd); totalInc += sd.amount;
                        }
                    }
                }
                if (snapshot.hasChild("Expense")) {
                    for (DataSnapshot d : snapshot.child("Expense").getChildren()) {
                        ExpenseActivity.Expense e = d.getValue(ExpenseActivity.Expense.class);
                        if (e != null && e.timestamp >= startTs && e.timestamp <= endTs) {
                            expenses.add(e); totalExp += e.amount;
                        }
                    }
                }

                if (donations.isEmpty() && expenses.isEmpty()) {
                    Toast.makeText(DashboardActivity.this, "No data found for this date range.", Toast.LENGTH_SHORT).show();
                } else {
                    PdfReportService.generateComparisonReport(DashboardActivity.this, session.getCommunityName(), donations, expenses, startTs, endTs, totalInc, totalExp);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFinancialData() {
        DatabaseReference logsRef = db.child("communities").child(session.getCommunityId()).child("logs");
        logsRef.keepSynced(true);
        logsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalIncome = 0f; 
                totalExpense = 0f;

                if (snapshot.hasChild("Donation")) { 
                    for (DataSnapshot d : snapshot.child("Donation").getChildren()) { 
                        Float amt = d.child("amount").getValue(Float.class); 
                        Long ts = d.child("timestamp").getValue(Long.class);

                        boolean inRange = true;
                        if (chartStartTs != null && chartEndTs != null && ts != null) {
                            inRange = (ts >= chartStartTs && ts <= chartEndTs);
                        }

                        if (amt != null && inRange) totalIncome += amt; 
                    } 
                }

                if (snapshot.hasChild("Expense")) { 
                    for (DataSnapshot d : snapshot.child("Expense").getChildren()) { 
                        Float amt = d.child("amount").getValue(Float.class); 
                        Long ts = d.child("timestamp").getValue(Long.class);

                        boolean inRange = true;
                        if (chartStartTs != null && chartEndTs != null && ts != null) {
                            inRange = (ts >= chartStartTs && ts <= chartEndTs);
                        }

                        if (amt != null && inRange) totalExpense += amt; 
                    } 
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePieChart() {
        if (pieChart == null) return;
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(totalIncome, "Income"));
        entries.add(new PieEntry(totalExpense, "Expense"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{0xFF2E7D32, 0xFFC62828}); 
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Total Analysis");
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    public static class AuditLog {
        public String managerName, actionType, description;
        public long timestamp;
        public AuditLog() {}
    }

    public static class Usage {
        public String current_month = "";
        public int pdfs_generated = 0;
        public int sandesh_sent = 0;
        public int audits_downloaded = 0;
        public Usage() {}
    }
}
