package org.shda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout membersContainer;
    private AutoCompleteTextView inputSearch;
    private TextView tvPlanStatus;
    private Button btnAddNew;
    private Button btnProactiveUpgrade;

    private List<Member> fullMemberList = new ArrayList<>();
    private HashMap<String, String> lastDonationTracker = new HashMap<>();

    private final int FREE_PLAN_LIMIT = 50;
    private int currentMemberCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member); 

        // 🚀 SMART OFFLINE ENABLEMENT
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {}

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        membersContainer = findViewById(R.id.membersContainer);
        inputSearch = findViewById(R.id.inputSearch);
        tvPlanStatus = findViewById(R.id.tvPlanStatus);
        btnAddNew = findViewById(R.id.btnAddNew);
        btnProactiveUpgrade = findViewById(R.id.btnProactiveUpgrade);

        // ✨ RBAC TRANSPARENCY ENGINE ✨
        View adminControlsLayout = findViewById(R.id.adminControlsLayout);
        String role = session.getRole();
        
        if ("MEMBER".equalsIgnoreCase(role) || "DEVOTEE".equalsIgnoreCase(role)) {
            // Read-only Transparency: Hide controls for standard users
            if (adminControlsLayout != null) adminControlsLayout.setVisibility(View.GONE);
            if (btnProactiveUpgrade != null) btnProactiveUpgrade.setVisibility(View.GONE);
        } else {
            // Admin/Manager Authority
            if (adminControlsLayout != null) adminControlsLayout.setVisibility(View.VISIBLE);
            
            if (btnProactiveUpgrade != null) {
                btnProactiveUpgrade.setOnClickListener(v -> {
                    startActivity(new Intent(MemberActivity.this, UpgradeActivity.class));
                });
            }
        }

        if (btnAddNew != null) {
            btnAddNew.setOnClickListener(v -> {
                if ("FREE".equalsIgnoreCase(session.getPlan()) && currentMemberCount >= FREE_PLAN_LIMIT) {
                    // ✨ UPGRADE: Material 3 Dialog
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("Community Limit Reached!")
                        .setMessage("Your Sanatani community is growing beautifully! You have reached the " + FREE_PLAN_LIMIT + " devotee limit on the Seva Plan.\n\nOffer Dakshina for SAMRAT PRO to welcome unlimited devotees.")
                        .setPositiveButton("UPGRADE NOW", (dialog, which) -> {
                            startActivity(new Intent(MemberActivity.this, UpgradeActivity.class));
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
                } else {
                    startActivity(new Intent(this, AddMemberActivity.class));
                }
            });
        }

        View btnSharePdf = findViewById(R.id.btnSharePdf);
        if (btnSharePdf != null) {
            btnSharePdf.setOnClickListener(v -> {
                if (!fullMemberList.isEmpty()) PdfReportService.generateMemberDirectory(this, session.getCommunityName(), fullMemberList);
            });
        }

        setupOfflineEngineAndLoadData();
        setupSearch();
    }

    private void setupOfflineEngineAndLoadData() {
        DatabaseReference membersRef = db.child("communities").child(session.getCommunityId()).child("members");
        membersRef.keepSynced(true);
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullMemberList.clear();
                List<String> suggestions = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) {
                        fullMemberList.add(m);
                        suggestions.add(m.name + " (" + m.id + ")");
                    }
                }

                currentMemberCount = fullMemberList.size();

                // ✨ UI LOGIC: Hide button if they are already Premium
                if ("PREMIUM".equalsIgnoreCase(session.getPlan())) {
                    tvPlanStatus.setText("Total Devotees: " + currentMemberCount + " (Samrat Pro: Unlimited)");
                    tvPlanStatus.setTextColor(0xFF2E7D32); // Deep Green for Premium Success
                    if (btnProactiveUpgrade != null) btnProactiveUpgrade.setVisibility(View.GONE);
                } else {
                    tvPlanStatus.setText("Total Devotees: " + currentMemberCount + " / " + FREE_PLAN_LIMIT + " (Seva Free Plan)");
                    // Show upgrade button only if Admin/Manager
                    if (!"MEMBER".equalsIgnoreCase(session.getRole()) && !"DEVOTEE".equalsIgnoreCase(session.getRole())) {
                        if (btnProactiveUpgrade != null) btnProactiveUpgrade.setVisibility(View.VISIBLE);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(MemberActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions);
                inputSearch.setAdapter(adapter);
                renderList(fullMemberList);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        DatabaseReference donationsRef = db.child("communities").child(session.getCommunityId()).child("logs").child("Donation");
        donationsRef.keepSynced(true);
        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastDonationTracker.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                for (DataSnapshot data : snapshot.getChildren()) {
                    String name = data.child("name").getValue(String.class);
                    Long ts = data.child("timestamp").getValue(Long.class);
                    Float amt = data.child("amount").getValue(Float.class);
                    if (name != null && ts != null && amt != null) {
                        lastDonationTracker.put(name, "Last: ৳" + new java.text.DecimalFormat("#,##0.00").format(amt) + " on " + sdf.format(new Date(ts)));
                    }
                }
                renderList(fullMemberList); 
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        inputSearch.setOnItemClickListener((parent, view, position, id) -> filterList(inputSearch.getText().toString()));
    }

    private void filterList(String query) {
        query = query.toLowerCase().trim();
        List<Member> filteredList = new ArrayList<>();
        for (Member m : fullMemberList) {
            if ((m.name != null && m.name.toLowerCase().contains(query)) || 
                (m.id != null && m.id.toLowerCase().contains(query)) ||
                (m.phone != null && m.phone.contains(query))) {
                filteredList.add(m);
            }
        }
        renderList(filteredList);
    }

    private void renderList(List<Member> listToRender) {
        if (membersContainer == null) return;
        membersContainer.removeAllViews();

        SimpleDateFormat sdfJoined = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (Member member : listToRender) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_member, membersContainer, false);

            ((TextView) view.findViewById(R.id.tvMemberName)).setText(member.name);
            ((TextView) view.findViewById(R.id.tvMemberDonation)).setText("Lifetime: ৳" + new java.text.DecimalFormat("#,##0.00").format(member.totalDonated));
            ((TextView) view.findViewById(R.id.tvMemberId)).setText(member.id);
            ((TextView) view.findViewById(R.id.tvMemberPhone)).setText("📞 " + (member.phone != null && !member.phone.isEmpty() ? member.phone : "N/A"));
            ((TextView) view.findViewById(R.id.tvMemberGotra)).setText("Gotra: " + (member.gotra != null && !member.gotra.isEmpty() ? member.gotra : "Not specified"));
            ((TextView) view.findViewById(R.id.tvMemberJoined)).setText("Joined: " + (member.timestamp > 0 ? sdfJoined.format(new Date(member.timestamp)) : "N/A"));

            TextView tvLast = view.findViewById(R.id.tvLastDonation);
            if (lastDonationTracker.containsKey(member.name + " [Member]")) {
                tvLast.setText(lastDonationTracker.get(member.name + " [Member]"));
            } else {
                tvLast.setText("No donations recorded yet.");
            }

            ImageButton btnWhatsApp = view.findViewById(R.id.btnMemberWhatsApp);
            btnWhatsApp.setOnClickListener(v -> {
                try {
                    String phoneStr = member.phone;
                    if (phoneStr != null && !phoneStr.isEmpty()) {
                        if (!phoneStr.startsWith("+88")) phoneStr = "+88" + phoneStr;
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://wa.me/" + phoneStr));
                        startActivity(intent);
                    } else {
                        Toast.makeText(MemberActivity.this, "No phone number available", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MemberActivity.this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                }
            });

            ImageButton btnDelete = view.findViewById(R.id.btnMemberDelete);
            if ("ADMIN".equals(session.getRole())) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    // ✨ UPGRADE: Material 3 Dialog
                    new MaterialAlertDialogBuilder(MemberActivity.this)
                        .setTitle("Remove Devotee")
                        .setMessage("Are you sure you want to remove " + member.name + "?")
                        .setPositiveButton("REMOVE", (dialog, which) -> {
                            db.child("communities").child(session.getCommunityId())
                              .child("members").child(member.id).removeValue();
                            Toast.makeText(MemberActivity.this, "Devotee removed", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            view.setOnClickListener(v -> {
                Intent intent = new Intent(MemberActivity.this, MemberDetailActivity.class);
                intent.putExtra("MEMBER_ID", member.id);
                startActivity(intent);
            });
            membersContainer.addView(view);
        }
    }
}
