package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {
    DatabaseReference db;
    BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseDatabase.getInstance().getReference();
        barChart = findViewById(R.id.financeChart);

        setupShloka();
        loadGraph();

        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardTransactions).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.btnPDFManual).setOnClickListener(v -> {
            PdfReportService.generateReport(this);
            Toast.makeText(this, "Generating Professional Report...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupShloka() {
        TextView tv = findViewById(R.id.shlokaText);
        tv.setText("“Karmanye vadhikaraste ma phaleshu kadachana.” - Bhagavad Gita 2.47");
    }

    private void loadGraph() {
        db.child("finances").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                float don = snap.child("Donation").exists() ? snap.child("Donation").getValue(Float.class) : 0f;
                float exp = snap.child("Social Expense").exists() ? snap.child("Social Expense").getValue(Float.class) : 0f;
                exp += snap.child("Festival Expense").exists() ? snap.child("Festival Expense").getValue(Float.class) : 0f;

                ArrayList<BarEntry> entries = new ArrayList<>();
                entries.add(new BarEntry(0, don));
                entries.add(new BarEntry(1, exp));

                BarDataSet ds = new BarDataSet(entries, "Donations vs Expenses");
                ds.setColors(new int[]{0xFF388E3C, 0xFFD32F2F}); 
                barChart.setData(new BarData(ds));
                barChart.getDescription().setEnabled(false);
                barChart.animateY(1000);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}
