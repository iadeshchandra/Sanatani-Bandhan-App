package org.shda;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        pieChart = findViewById(R.id.pieChart);
        
        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        ((TextView) findViewById(R.id.tvDateEnglish)).setText("🕉 " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));
        ((TextView) findViewById(R.id.tvDateBengali)).setText("শুভ দিন: " + new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("bn", "BD")).format(new Date()));
        ((TextView) findViewById(R.id.tvTithiAlert)).setText("Today's Tithi: Loading...");
        ((TextView) findViewById(R.id.shlokaText)).setText("\"Karmanye vadhikaraste Ma Phaleshu Kadachana\"\n- Bhagavad Gita");

        findViewById(R.id.btnPanjika).setOnClickListener(v -> startActivity(new Intent(this, PanjikaActivity.class)));
        findViewById(R.id.btnFilterChartDate).setOnClickListener(v -> Toast.makeText(this, "Master Dashboard Date Filter active!", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardPolls).setOnClickListener(v -> startActivity(new Intent(this, PollActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        findViewById(R.id.cardComms).setOnClickListener(v -> startActivity(new Intent(this, CommsActivity.class)));

        findViewById(R.id.btnMyProfile).setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));
        findViewById(R.id.btnChangeLanguage).setOnClickListener(v -> showLanguageDialog());
        findViewById(R.id.btnHelpSupport).setOnClickListener(v -> contactSupport());
        
        // ✨ FIX: Powerful Hard-Wipe Logout Engine linked directly to SessionManager!
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                session.logout(); // Successfully calls editor.clear() on "SanataniSession"
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Logout processed with minor errors", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnWorkspaceSettings).setOnClickListener(v -> showMandirInfoDialog());
        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> showGlobalPdfGeneratorDialog());

        if (!"ADMIN".equals(session.getRole())) {
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
        } else {
            findViewById(R.id.btnDownloadAudit).setOnClickListener(v -> PdfReportService.generateSecurityAudit(this, session.getCommunityId()));
        }

        loadWorkspaceType();
        loadFinancialData();
    }

    private void loadWorkspaceType() {
        db.child("communities").child(session.getCommunityId()).child("info").child("type").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String type = snapshot.getValue(String.class);
                if (type != null && !type.isEmpty()) workspaceType = type;
                Button btnWorkspace = findViewById(R.id.btnWorkspaceSettings);
                if (btnWorkspace != null) btnWorkspace.setText("🏛️ " + workspaceType.toUpperCase() + " INFO & SETTINGS");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Bengali (বাংলা)", "Hindi (हिन्दी)"};
        new AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages, (dialog, which) -> {
                Toast.makeText(this, "Language updated to " + languages[which], Toast.LENGTH_SHORT).show();
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
        new AlertDialog.Builder(this).setTitle("Generate Master Report")
            .setItems(new String[]{"Donations Ledger (Select Dates)", "Expenses Ledger (Select Dates)", "Income vs Expense Comparison (Select Dates)"}, (dialog, which) -> {
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

    private void showMandirInfoDialog() {
        db.child("communities").child(session.getCommunityId()).child("info").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String phone = snapshot.child("phone").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String address = snapshot.child("address").getValue(String.class);

                AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
                builder.setTitle("🏛️ " + workspaceType + " Information");
                
                LinearLayout layout = new LinearLayout(DashboardActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 20, 50, 0);

                if ("ADMIN".equals(session.getRole())) {
                    final EditText inputName = new EditText(DashboardActivity.this); inputName.setHint(workspaceType + " Name"); inputName.setText(session.getCommunityName());
                    final EditText inputPhone = new EditText(DashboardActivity.this); inputPhone.setHint("Official Phone"); inputPhone.setText(phone);
                    final EditText inputEmail = new EditText(DashboardActivity.this); inputEmail.setHint("Official Email"); inputEmail.setText(email);
                    final EditText inputAddress = new EditText(DashboardActivity.this); inputAddress.setHint("Official Address"); inputAddress.setText(address);
                    layout.addView(inputName); layout.addView(inputPhone); layout.addView(inputEmail); layout.addView(inputAddress);
                    builder.setView(layout);
                    builder.setPositiveButton("SAVE", (d, w) -> {
                        String newName = inputName.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            db.child("communities").child(session.getCommunityId()).child("name").setValue(newName);
                            // ✨ FIX: Properly calling the SessionManager to save the new name
                            session.updateCommunityName(newName);
                            ((TextView) findViewById(R.id.tvDashboardTitle)).setText(newName);
                        }
                        db.child("communities").child(session.getCommunityId()).child("info").child("phone").setValue(inputPhone.getText().toString());
                        db.child("communities").child(session.getCommunityId()).child("info").child("email").setValue(inputEmail.getText().toString());
                        db.child("communities").child(session.getCommunityId()).child("info").child("address").setValue(inputAddress.getText().toString());
                        Toast.makeText(DashboardActivity.this, workspaceType + " Info Updated!", Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("CANCEL", null);
                } else {
                    TextView tvName = new TextView(DashboardActivity.this); tvName.setText("Name: " + session.getCommunityName()); tvName.setTextSize(16f); tvName.setPadding(0,0,0,10);
                    TextView tvPhone = new TextView(DashboardActivity.this); tvPhone.setText("Phone: " + (phone!=null?phone:"N/A")); tvPhone.setTextSize(16f); tvPhone.setPadding(0,0,0,10);
                    TextView tvEmail = new TextView(DashboardActivity.this); tvEmail.setText("Email: " + (email!=null?email:"N/A")); tvEmail.setTextSize(16f); tvEmail.setPadding(0,0,0,10);
                    TextView tvAddr = new TextView(DashboardActivity.this); tvAddr.setText("Address: " + (address!=null?address:"N/A")); tvAddr.setTextSize(16f);
                    layout.addView(tvName); layout.addView(tvPhone); layout.addView(tvEmail); layout.addView(tvAddr);
                    builder.setView(layout);
                    builder.setPositiveButton("CLOSE", null);
                }
                builder.show();
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
                        if (amt != null) totalIncome += amt; 
                    } 
                }
                
                if (snapshot.hasChild("Expense")) { 
                    for (DataSnapshot d : snapshot.child("Expense").getChildren()) { 
                        Float amt = d.child("amount").getValue(Float.class); 
                        if (amt != null) totalExpense += amt; 
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
        dataSet.setColors(new int[]{0xFF2E7D32, 0xFFC62828}); // Green for Income, Red for Expense
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Total Analysis");
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}
