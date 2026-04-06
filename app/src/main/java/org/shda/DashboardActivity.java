package org.shda;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

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
    private Button btnFilterChartDate;
    
    private float totalIncome = 0f;
    private float totalExpense = 0f;
    private long filterStartTs = 0L;
    private long filterEndTs = Long.MAX_VALUE;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private Handler shlokaHandler = new Handler(Looper.getMainLooper());
    private Runnable shlokaRunnable = new Runnable() {
        @Override public void run() {
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

        if (session.getCommunityId() == null) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }

        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        pieChart = findViewById(R.id.pieChart);
        btnFilterChartDate = findViewById(R.id.btnFilterChartDate);
        
        setupDates();
        applyPermissions(session.getRole());
        setupNavigation();
        setupVisualAnalytics(); 
        
        // ✨ START THE NOTIFICATION ENGINE
        listenForSmartNotifications();

        btnFilterChartDate.setOnClickListener(v -> showChartDateFilterDialog());

        findViewById(R.id.tvDashboardBranding).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://linktr.ee/Adesh_Chandra"));
            startActivity(browserIntent);
        });

        findViewById(R.id.btnEditCommunity).setOnClickListener(v -> showEditCommunityDialog());
        findViewById(R.id.btnChangeLanguage).setOnClickListener(v -> showLanguageDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); session.logout();
            startActivity(new Intent(this, LoginActivity.class)); finish();
        });
    }

    // 🔔 THE ZERO-COST PUSH NOTIFICATION ENGINE
    private void listenForSmartNotifications() {
        // Assume session.getUserId() holds the member's ID (e.g., SB-1001)
        // If they don't have an ID, we skip
        if (session.getUserId() == null) return;

        DatabaseReference notifRef = db.child("communities").child(session.getCommunityId()).child("notifications").child(session.getUserId());
        
        notifRef.addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                AppNotification notif = snapshot.getValue(AppNotification.class);
                if (notif != null && !notif.isRead) {
                    
                    // 1. Trigger Android Push Notification
                    triggerLocalPushNotification(notif.title, notif.message);
                    
                    // 2. Mark as read in the database so it doesn't trigger again next time
                    snapshot.getRef().child("isRead").setValue(true);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void triggerLocalPushNotification(String title, String message) {
        String channelId = "Sanatani_Alerts";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Required for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Mandir Updates", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // When they click the notification, open the Transaction Activity so they can download their PDF!
        Intent intent = new Intent(this, TransactionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override protected void onResume() { super.onResume(); shlokaHandler.post(shlokaRunnable); }
    @Override protected void onPause() { super.onPause(); shlokaHandler.removeCallbacks(shlokaRunnable); }

    // ------------- ALL OTHER EXISTING METHODS BELOW -------------
    // setupDates, applyPermissions, setupVisualAnalytics, showLanguageDialog, etc. remain unchanged.
    
    private void setupDates() {
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
    }

    private void applyPermissions(String role) {
        if (role.equals("MEMBER")) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
            findViewById(R.id.btnGenerateReports).setVisibility(View.GONE);
        } else if (role.equals("MANAGER")) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
            findViewById(R.id.btnGenerateReports).setVisibility(View.VISIBLE);
        } else if (role.equals("ADMIN")) {
            findViewById(R.id.btnEditCommunity).setVisibility(View.VISIBLE);
            findViewById(R.id.btnGenerateReports).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDownloadAudit).setVisibility(View.VISIBLE);
        }
    }

    private void setupVisualAnalytics() {
        pieChart.getDescription().setEnabled(false); pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(50f); pieChart.setCenterTextSize(14f); pieChart.setCenterTextColor(Color.parseColor("#E65100"));
        String logsPath = "communities/" + session.getCommunityId() + "/logs";

        db.child(logsPath).child("Donation").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalIncome = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Long ts = data.child("timestamp").getValue(Long.class); Float amt = data.child("amount").getValue(Float.class);
                    if (amt != null && ts != null && ts >= filterStartTs && ts <= filterEndTs) totalIncome += amt;
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        db.child(logsPath).child("Expense").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalExpense = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Long ts = data.child("timestamp").getValue(Long.class); Float amt = data.child("amount").getValue(Float.class);
                    if (amt != null && ts != null && ts >= filterStartTs && ts <= filterEndTs) totalExpense += amt;
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        if (totalIncome > 0) entries.add(new PieEntry(totalIncome, ""));
        if (totalExpense > 0) entries.add(new PieEntry(totalExpense, ""));

        if (entries.isEmpty()) { entries.add(new PieEntry(1f, "")); pieChart.setCenterText("No Data"); } 
        else { pieChart.setCenterText("Net Balance\n৳" + (totalIncome - totalExpense)); }

        PieDataSet dataSet = new PieDataSet(entries, "");
        List<Integer> colors = new ArrayList<>(); colors.add(Color.parseColor("#388E3C")); 
        if (totalExpense > 0) colors.add(Color.parseColor("#D32F2F")); else if (totalIncome == 0) colors.add(Color.LTGRAY);
        dataSet.setColors(colors); dataSet.setValueTextColor(Color.WHITE); dataSet.setValueTextSize(14f);
        
        pieChart.setData(new PieData(dataSet)); pieChart.animateY(800); pieChart.invalidate(); 
    }

    private void showChartDateFilterDialog() {
        Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0); 
            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                filterStartTs = startCal.getTimeInMillis(); filterEndTs = endCal.getTimeInMillis();
                btnFilterChartDate.setText("🗓️ " + new SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(startCal.getTime()) + " to " + new SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(endCal.getTime()));
                setupVisualAnalytics();
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "বাংলা (Bengali)", "हिन्दी (Hindi)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language / ভাষা নির্বাচন করুন");
        builder.setItems(languages, (dialog, which) -> {
            if (which == 0) LocaleHelper.setLocale(DashboardActivity.this, "en");
            else if (which == 1) LocaleHelper.setLocale(DashboardActivity.this, "bn");
            else if (which == 2) LocaleHelper.setLocale(DashboardActivity.this, "hi");
            Intent intent = new Intent(DashboardActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent); finish();
        });
        builder.show();
    }

    private void setupNavigation() {
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        findViewById(R.id.cardComms).setOnClickListener(v -> startActivity(new Intent(this, CommsActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));

        Button btnGen = findViewById(R.id.btnGenerateReports);
        if (btnGen != null) btnGen.setOnClickListener(v -> Toast.makeText(this, "Master Reports Generating...", Toast.LENGTH_SHORT).show());
        Button btnAud = findViewById(R.id.btnDownloadAudit);
        if (btnAud != null) btnAud.setOnClickListener(v -> Toast.makeText(this, "Audit Downloaded", Toast.LENGTH_SHORT).show());
    }

    private void showEditCommunityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Community Name");
        final android.widget.EditText input = new android.widget.EditText(this); input.setText(session.getCommunityName());
        android.widget.LinearLayout container = new android.widget.LinearLayout(this); container.setPadding(50, 20, 50, 0); container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && session.getUserId() != null) {
                db.child("users").child(session.getUserId()).child("communityName").setValue(newName)
                    .addOnSuccessListener(aVoid -> {
                        session.updateCommunityName(newName);
                        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(newName);
                    });
            }
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }
}
