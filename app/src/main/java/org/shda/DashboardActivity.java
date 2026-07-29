package org.shda;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
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
    private Button btnGenerateReports; // Added for Feature Flag control

    // ✨ Dynamic UI Elements
    private ImageView bannerImageView;
    private View mainLayout;
    private DatabaseReference uiRef;

    // ✨ Notification Center Elements
    private FrameLayout btnNotificationCenter;
    private TextView tvNotificationBadge;
    private View badgeContainer;
    
    // ✨ System Control Elements
    private AlertDialog maintenanceDialog;

    private Long chartStartTs = null;
    private Long chartEndTs = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh unread count every time user returns to dashboard
        if (session != null && session.getCommunityId() != null) {
            calculateUnreadNotifications();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 🚀 SMART OFFLINE ENABLEMENT
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {}

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
        btnGenerateReports = findViewById(R.id.btnGenerateReports);

        // Initialize Dynamic Banners
        bannerImageView = findViewById(R.id.bannerImageView);
        mainLayout = findViewById(R.id.mainLayout);
        uiRef = FirebaseDatabase.getInstance().getReference("app_ui_settings/home_screen");

        // Initialize Notification Center Views
        btnNotificationCenter = findViewById(R.id.btnNotificationCenter);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        badgeContainer = findViewById(R.id.badgeContainer);

        btnNotificationCenter.setOnClickListener(v -> 
            startActivity(new Intent(DashboardActivity.this, NotificationCenterActivity.class))
        );

        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
        ((TextView) findViewById(R.id.tvTithiAlert)).setText("Today's Tithi: Loading...");
        ((TextView) findViewById(R.id.shlokaText)).setText("\"Karmanye vadhikaraste Ma Phaleshu Kadachana\"\n- Bhagavad Gita");

        // ✨ INITIALIZE ALL REMOTE DATA SYNC ✨
        syncWorkspacePlan();
        fetchDynamicUI();
        listenForGlobalBroadcasts();
        fetchGlobalConfig(); // Listen to the React Web Dashboard Kill Switches!

        findViewById(R.id.btnPanjika).setOnClickListener(v -> startActivity(new Intent(this, PanjikaActivity.class)));
        findViewById(R.id.btnFilterChartDate).setOnClickListener(v -> showChartDateFilterDialog());

        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));

        findViewById(R.id.cardComms).setOnClickListener(v -> 
            checkQuotaAndProceed("sandesh_sent", 1, () -> startActivity(new Intent(this, CommsActivity.class)))
        );

        btnGenerateReports.setOnClickListener(v -> 
            checkQuotaAndProceed("pdfs_generated", 3, this::showGlobalPdfGeneratorDialog)
        );

        if (!"ADMIN".equals(session.getRole())) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
        } else {
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

    // ✨ THE NEW SYSTEM CONTROL DESK LISTENER ✨
    private void fetchGlobalConfig() {
        DatabaseReference configRef = db.child("app_config").child("global_settings");
        configRef.keepSynced(true);
        configRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    
                    // 1. App Kill Switch (Maintenance)
                    Boolean isMaintenance = snapshot.child("maintenance_mode").getValue(Boolean.class);
                    if (isMaintenance != null && isMaintenance) showMaintenanceLock();
                    else hideMaintenanceLock();

                    // 2. Feature Toggle: PDF Engine
                    Boolean isPdfEnabled = snapshot.child("pdf_engine_enabled").getValue(Boolean.class);
                    if (btnGenerateReports != null) {
                        btnGenerateReports.setVisibility(isPdfEnabled != null && !isPdfEnabled ? View.GONE : View.VISIBLE);
                    }

                    // 3. Dynamic Festival Paywall Engine
                    Boolean isDiscountActive = snapshot.child("is_discount_active").getValue(Boolean.class);
                    String festivalName = snapshot.child("festival_name").getValue(String.class);
                    String localPrice = snapshot.child("local_price").getValue(String.class);
                    String intlPrice = snapshot.child("intl_price").getValue(String.class);

                    if (!"PREMIUM".equalsIgnoreCase(session.getPlan()) && btnUpgradeBadge != null && btnUpgradeBadge.getVisibility() == View.VISIBLE) {
                        if (isDiscountActive != null && isDiscountActive && festivalName != null) {
                            btnUpgradeBadge.setText("⭐ " + festivalName.toUpperCase() + " OFFER: " + localPrice + " ৳ / $" + intlPrice);
                            btnUpgradeBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFD32F2F)); // Festival Red Alert
                        } else {
                            btnUpgradeBadge.setText("⭐ SEVA FREE PLAN - TAP TO UPGRADE");
                            btnUpgradeBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE65100)); // Default Orange
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showMaintenanceLock() {
        if (maintenanceDialog != null && maintenanceDialog.isShowing()) return;
        maintenanceDialog = new AlertDialog.Builder(this)
            .setTitle("🙏 System Maintenance")
            .setMessage("The Sanatani Bandhan network is undergoing sacred structural upgrades. Please check back shortly.")
            .setCancelable(false)
            .create();
        maintenanceDialog.show();
    }

    private void hideMaintenanceLock() {
        if (maintenanceDialog != null && maintenanceDialog.isShowing()) {
            maintenanceDialog.dismiss();
        }
    }

    // ✨ THE SMART UNREAD NOTIFICATION COUNTER ✨
    private void calculateUnreadNotifications() {
        SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        long lastReadTs = prefs.getLong("last_read_ts", 0);

        DatabaseReference globalRef = db.child("global_notifications");
        DatabaseReference communityRef = db.child("communities").child(session.getCommunityId()).child("notifications");

        final int[] unreadCount = {0};

        globalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) {
                    Long ts = d.child("timestamp").getValue(Long.class);
                    if (ts != null && ts > lastReadTs) unreadCount[0]++;
                }
                updateBadgeUI(unreadCount[0]);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        communityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot d : snapshot.getChildren()) {
                    Long ts = d.child("timestamp").getValue(Long.class);
                    if (ts != null && ts > lastReadTs) unreadCount[0]++;
                }
                updateBadgeUI(unreadCount[0]);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateBadgeUI(int count) {
        if (count > 0) {
            badgeContainer.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            badgeContainer.setVisibility(View.GONE);
        }
    }

    private void listenForGlobalBroadcasts() {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("global_notifications");
        notificationsRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    long currentTime = System.currentTimeMillis();
                    if (timestamp != null && (currentTime - timestamp) < 60000) {
                        triggerAndroidSystemNotification(title, message);
                        calculateUnreadNotifications(); // Refresh badge instantly on new alert
                    }
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void triggerAndroidSystemNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "sanatani_global_alerts";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Mandir Global Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Important broadcasts from the Sanatani network.");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(this, NotificationCenterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title != null ? title : "New Mandir Broadcast")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void fetchDynamicUI() {
        uiRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isFestivalMode = snapshot.child("festival_mode").getValue(Boolean.class);
                    if (mainLayout != null) {
                        if (isFestivalMode != null && isFestivalMode) {
                            mainLayout.setBackgroundColor(Color.parseColor("#FFF3E0")); 
                            if (tvDashboardBranding != null) tvDashboardBranding.setTextColor(Color.parseColor("#E65100"));
                        } else {
                            mainLayout.setBackgroundColor(Color.parseColor("#F3F4F6")); 
                            if (tvDashboardBranding != null) tvDashboardBranding.setTextColor(Color.parseColor("#333333"));
                        }
                    }

                    String bannerUrl = snapshot.child("banner_url").getValue(String.class);
                    if (bannerImageView != null) {
                        if (bannerUrl != null && !bannerUrl.trim().isEmpty()) {
                            bannerImageView.setVisibility(View.VISIBLE);
                            Glide.with(DashboardActivity.this)
                                    .load(bannerUrl)
                                    .placeholder(android.R.color.darker_gray)
                                    .into(bannerImageView);
                        } else {
                            bannerImageView.setVisibility(View.GONE);
                        }
                    }

                    final String actionLink = snapshot.child("action_link").getValue(String.class);
                    if (bannerImageView != null) {
                        bannerImageView.setOnClickListener(v -> {
                            if (actionLink != null && !actionLink.isEmpty()) {
                                if (actionLink.equals("UPGRADE_SCREEN")) {
                                    startActivity(new Intent(DashboardActivity.this, UpgradeActivity.class));
                                } else if (actionLink.startsWith("http")) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(actionLink)));
                                }
                            }
                        });
                    }

                    String welcomeMessage = snapshot.child("welcome_message").getValue(String.class);
                    if (welcomeMessage != null && !welcomeMessage.isEmpty() && tvDashboardBranding != null) {
                        if (tvDashboardBranding.getVisibility() == View.VISIBLE) {
                            tvDashboardBranding.setText(welcomeMessage);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    private void updatePlanBadgeUI() {
        String role = session.getRole();
        if ("MEMBER".equalsIgnoreCase(role) || "DEVOTEE".equalsIgnoreCase(role)) {
            btnUpgradeBadge.setVisibility(View.GONE);
            tvDashboardBranding.setVisibility(View.VISIBLE);
        } else {
            tvDashboardBranding.setVisibility(View.GONE);
            btnUpgradeBadge.setVisibility(View.VISIBLE);
            if ("PREMIUM".equalsIgnoreCase(session.getPlan())) {
                btnUpgradeBadge.setText("👑 SAMRAT PRO ACTIVE");
                btnUpgradeBadge.setBackgroundTintList(ColorStateList.valueOf(0xFF388E3C)); 
                btnUpgradeBadge.setOnClickListener(v -> 
                    Toast.makeText(DashboardActivity.this, "Your Mandir is fully upgraded!", Toast.LENGTH_SHORT).show()
                );
            } else {
                // If a discount is NOT active, this runs as fallback.
                btnUpgradeBadge.setText("⭐ SEVA FREE PLAN - TAP TO UPGRADE");
                btnUpgradeBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE65100)); 
                btnUpgradeBadge.setOnClickListener(v -> 
                    startActivity(new Intent(DashboardActivity.this, UpgradeActivity.class))
                );
            }
        }
    }

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
