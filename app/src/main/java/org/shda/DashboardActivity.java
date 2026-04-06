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
        listenForSmartNotifications();

        btnFilterChartDate.setOnClickListener(v -> showChartDateFilterDialog());

        findViewById(R.id.tvDashboardBranding).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://linktr.ee/Adesh_Chandra"));
            startActivity(browserIntent);
        });

        findViewById(R.id.btnEditCommunity).setOnClickListener(v -> showEditCommunityDialog());
        findViewById(R.id.btnChangeLanguage).setOnClickListener(v -> showLanguageDialog());
        findViewById(R.id.btnHelpSupport).setOnClickListener(v -> contactSupport());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); session.logout();
            startActivity(new Intent(this, LoginActivity.class)); finish();
        });
    }

    private void contactSupport() {
        try {
            String supportNumber = "+8801700000000"; 
            String message = "Namaskar Adesh, I need some technical support with the Sanatani Bandhan app.";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + supportNumber + "&text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForSmartNotifications() {
        if (session.getUserId() == null) return;
        DatabaseReference notifRef = db.child("communities").child(session.getCommunityId()).child("notifications").child(session.getUserId());
        notifRef.addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                AppNotification notif = snapshot.getValue(AppNotification.class);
                if (notif != null && !notif.isRead) {
                    triggerLocalPushNotification(notif.title, notif.message);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Mandir Updates", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, TransactionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override protected void onResume() { super.onResume(); shlokaHandler.post(shlokaRunnable); }
    @Override protected void onPause() { super.onPause(); shlokaHandler.removeCallbacks(shlokaRunnable); }

    private void setupDates() {
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
        TextView tvTithi = findViewById(R.id.tvTithiAlert);
        if (tvTithi != null) tvTithi.setText(PanchangEngine.getTodayTithiAlert());
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
        if (btnGen != null) {
            btnGen.setOnClickListener(v -> {
                Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
                new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    startCal.set(year, month, dayOfMonth, 0, 0, 0); 
                    new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                        endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                        long startTs = startCal.getTimeInMillis(); long endTs = endCal.getTimeInMillis();
                        String reportRange = "Period: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(startCal.getTime()) + " to " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(endCal.getTime());

                        db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
                          .orderByChild("timestamp").startAt(startTs).endAt(endTs)
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
        }

        Button btnAud = findViewById(R.id.btnDownloadAudit);
        if (btnAud != null) {
            btnAud.setOnClickListener(v -> {
                Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
                new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    startCal.set(year, month, dayOfMonth, 0, 0, 0); 
                    new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                        endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 
                        long startTs = startCal.getTimeInMillis(); long endTs = endCal.getTimeInMillis();
                        String reportRange = "Audit Period: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(startCal.getTime()) + " to " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(endCal.getTime());

                        db.child("communities").child(session.getCommunityId()).child("audit_logs")
                          .orderByChild("timestamp").startAt(startTs).endAt(endTs)
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
    }

    // ✨ ENTERPRISE OFFLINE FIX: Removed blocking success listeners
    private void showEditCommunityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Community Name");
        final android.widget.EditText input = new android.widget.EditText(this); input.setText(session.getCommunityName());
        android.widget.LinearLayout container = new android.widget.LinearLayout(this); container.setPadding(50, 20, 50, 0); container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("SAVE", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && session.getUserId() != null) {
                // Save locally to queue instantly
                db.child("users").child(session.getUserId()).child("communityName").setValue(newName);
                
                // Update UI instantly
                session.updateCommunityName(newName);
                ((TextView) findViewById(R.id.tvDashboardTitle)).setText(newName);
                Toast.makeText(this, "Updated Locally! Will sync when online.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }
}
