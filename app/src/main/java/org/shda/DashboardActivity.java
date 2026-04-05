package org.shda;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
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
    private Button btnFilterChartDate;
    
    private float totalIncome = 0f;
    private float totalExpense = 0f;
    
    // ✨ Chart Filter Limits
    private long filterStartTs = 0L;
    private long filterEndTs = Long.MAX_VALUE;

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
        setupNavigation();
        setupVisualAnalytics(); 

        btnFilterChartDate.setOnClickListener(v -> showChartDateFilterDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); session.logout();
            startActivity(new Intent(this, LoginActivity.class)); finish();
        });
    }

    @Override protected void onResume() { super.onResume(); shlokaHandler.post(shlokaRunnable); }
    @Override protected void onPause() { super.onPause(); shlokaHandler.removeCallbacks(shlokaRunnable); }

    private void setupDates() {
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
    }

    // 📊 CHART LOGIC (NOW RESPONDS TO DATES)
    private void setupVisualAnalytics() {
        pieChart.getDescription().setEnabled(false); pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(50f); pieChart.setCenterTextSize(14f); pieChart.setCenterTextColor(Color.parseColor("#E65100"));

        String logsPath = "communities/" + session.getCommunityId() + "/logs";

        db.child(logsPath).child("Donation").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalIncome = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Long ts = data.child("timestamp").getValue(Long.class);
                    Float amt = data.child("amount").getValue(Float.class);
                    if (amt != null && ts != null && ts >= filterStartTs && ts <= filterEndTs) { totalIncome += amt; }
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        db.child(logsPath).child("Expense").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalExpense = 0f;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Long ts = data.child("timestamp").getValue(Long.class);
                    Float amt = data.child("amount").getValue(Float.class);
                    if (amt != null && ts != null && ts >= filterStartTs && ts <= filterEndTs) { totalExpense += amt; }
                }
                updatePieChart();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        if (totalIncome > 0) entries.add(new PieEntry(totalIncome, "Total Chanda"));
        if (totalExpense > 0) entries.add(new PieEntry(totalExpense, "Utsav Expenses"));

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, "No Data for Period")); pieChart.setCenterText("No Data");
        } else {
            pieChart.setCenterText("Net Balance\n৳" + (totalIncome - totalExpense));
        }

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
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy", Locale.getDefault());
                btnFilterChartDate.setText("🗓️ " + sdf.format(startCal.getTime()) + " to " + sdf.format(endCal.getTime()));
                
                // Re-trigger calculation!
                setupVisualAnalytics();
                
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupNavigation() {
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        findViewById(R.id.cardComms).setOnClickListener(v -> startActivity(new Intent(this, CommsActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));
    }
}
