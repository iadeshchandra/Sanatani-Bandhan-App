package org.shda;

import android.content.Intent;
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
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvDashboardTitle);
        tvTitle.setText(session.getCommunityName());
        setupDynamicShloka();

        String userRole = session.getRole();
        applyPermissions(userRole);
        setupNavigation();

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void applyPermissions(String role) {
        if (role.equals("MEMBER")) {
            // Regular members only see Utsavs & their own profiles
            findViewById(R.id.cardMembers).setVisibility(View.GONE);
            findViewById(R.id.cardDonations).setVisibility(View.GONE);
            findViewById(R.id.cardComms).setVisibility(View.GONE);
            findViewById(R.id.btnGenerateReports).setVisibility(View.GONE);
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE); // Hide Audit
        } else if (role.equals("MANAGER")) {
            // Managers can manage CRM but cannot pull full financial or security audits
            findViewById(R.id.btnGenerateReports).setVisibility(View.GONE);
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE); // Hide Audit
        }
        // Admin sees everything
    }

    private void setupNavigation() {
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        findViewById(R.id.cardComms).setOnClickListener(v -> startActivity(new Intent(this, CommsActivity.class)));

        // --- 1. FINANCIAL REPORTS (With Date Picker) ---
        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> {
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();

            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCal.set(year, month, dayOfMonth, 0, 0, 0); 
                new android.app.DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                    endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 

                    long startTimestamp = startCal.getTimeInMillis();
                    long endTimestamp = endCal.getTimeInMillis();
                    
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String reportRange = "Period: " + displayFormat.format(startCal.getTime()) + " to " + displayFormat.format(endCal.getTime());

                    Toast.makeText(this, "Auditing Financials...", Toast.LENGTH_SHORT).show();

                    db.child("communities").child(session.getCommunityId()).child("logs").child("Donation")
                      .orderByChild("timestamp").startAt(startTimestamp).endAt(endTimestamp)
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<String> names = new ArrayList<>();
                            List<Float> amounts = new ArrayList<>();
                            List<String> notes = new ArrayList<>();
                            List<String> dates = new ArrayList<>();
                            float total = 0f;
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

                            for (DataSnapshot data : snapshot.getChildren()) {
                                String name = data.child("name").getValue(String.class);
                                Float amt = data.child("amount").getValue(Float.class);
                                String note = data.child("note").getValue(String.class);
                                Long ts = data.child("timestamp").getValue(Long.class);
                                
                                if(name != null && amt != null) {
                                    names.add(name);
                                    amounts.add(amt);
                                    notes.add(note != null ? note : "No note");
                                    dates.add(ts != null ? sdf.format(new Date(ts)) : "Unknown Date");
                                    total += amt;
                                }
                            }
                            PdfReportService.generateFinancialReport(DashboardActivity.this, session.getCommunityName(), dates, names, amounts, notes, total, reportRange);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // --- 2. SECURITY AUDIT REPORT (Admin Only, With Date Picker) ---
        findViewById(R.id.btnDownloadAudit).setOnClickListener(v -> {
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();

            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCal.set(year, month, dayOfMonth, 0, 0, 0); 
                new android.app.DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                    endCal.set(year2, month2, dayOfMonth2, 23, 59, 59); 

                    long startTimestamp = startCal.getTimeInMillis();
                    long endTimestamp = endCal.getTimeInMillis();
                    
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String reportRange = "Audit Period: " + displayFormat.format(startCal.getTime()) + " to " + displayFormat.format(endCal.getTime());

                    Toast.makeText(this, "Compiling Audit Logs...", Toast.LENGTH_SHORT).show();

                    db.child("communities").child(session.getCommunityId()).child("audit_logs")
                      .orderByChild("timestamp").startAt(startTimestamp).endAt(endTimestamp)
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
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

    private void setupDynamicShloka() {
        TextView tv = findViewById(R.id.shlokaText);
        String[] shlokas = {
            "“You have the right to work, but for the work's sake only...”\n\n- Bhagavad Gita 2.47",
            "“Whenever dharma declines and the purpose of life is forgotten...”\n\n- Bhagavad Gita 4.7",
            "“Truth is one, paths are many.”\n\n- Rig Veda",
            "“Arise, awake, and stop not till the goal is reached.”\n\n- Katha Upanishad"
        };
        Calendar calendar = Calendar.getInstance();
        tv.setText(shlokas[calendar.get(Calendar.DAY_OF_YEAR) % shlokas.length]);
    }
}
