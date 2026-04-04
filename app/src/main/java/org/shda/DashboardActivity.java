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
            finish(); return;
        }

        ((TextView) findViewById(R.id.tvDashboardTitle)).setText(session.getCommunityName());
        setupDynamicShloka();
        applyPermissions(session.getRole());
        setupNavigation();

        findViewById(R.id.btnEditCommunity).setOnClickListener(v -> showEditCommunityDialog());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void applyPermissions(String role) {
        if (role.equals("MEMBER")) {
            findViewById(R.id.cardMembers).setVisibility(View.GONE);
            findViewById(R.id.cardDonations).setVisibility(View.GONE);
            // Note: cardExpenses is visible so members can download reports
            findViewById(R.id.btnGenerateReports).setVisibility(View.GONE);
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
        } else if (role.equals("MANAGER")) {
            findViewById(R.id.btnGenerateReports).setVisibility(View.GONE);
            findViewById(R.id.btnDownloadAudit).setVisibility(View.GONE);
            findViewById(R.id.btnEditCommunity).setVisibility(View.GONE);
        } else if (role.equals("ADMIN")) {
            findViewById(R.id.btnEditCommunity).setVisibility(View.VISIBLE);
        }
    }

    private void setupNavigation() {
        findViewById(R.id.cardMembers).setOnClickListener(v -> startActivity(new Intent(this, MemberActivity.class)));
        findViewById(R.id.cardDonations).setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.cardExpenses).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class))); // NEW BINDING
        findViewById(R.id.cardEvents).setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));

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
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setPadding(50, 20, 50, 0);
        container.addView(input);
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
                    Toast.makeText(this, "Community Name Updated Successfully", Toast.LENGTH_SHORT).show();
                    AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "SETTINGS_CHANGED", "Updated community name to: " + newName);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Authentication error. Only Admins can do this.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDynamicShloka() {
        TextView tv = findViewById(R.id.shlokaText);
        String[] shlokas = {
            "“You have the right to work, but for the work's sake only...”\n\n- Bhagavad Gita 2.47",
            "“Whenever dharma declines and the purpose of life is forgotten...”\n\n- Bhagavad Gita 4.7",
            "“Truth is one, paths are many.”\n\n- Rig Veda",
            "“Arise, awake, and stop not till the goal is reached.”\n\n- Katha Upanishad"
        };
        tv.setText(shlokas[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % shlokas.length]);
    }
}
