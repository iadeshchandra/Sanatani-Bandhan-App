package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout membersContainer;
    private EditText inputSearch;
    private List<Member> fullMemberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member); 

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        try {
            membersContainer = findViewById(R.id.membersContainer);
            inputSearch = findViewById(R.id.inputSearch);
            
            View btnAddNew = findViewById(R.id.btnAddNew);
            if (btnAddNew != null) {
                btnAddNew.setOnClickListener(v -> startActivity(new Intent(MemberActivity.this, AddMemberActivity.class)));
            }

            View btnSharePdf = findViewById(R.id.btnSharePdf);
            if (btnSharePdf != null) {
                btnSharePdf.setOnClickListener(v -> {
                    if (!fullMemberList.isEmpty()) {
                        PdfReportService.generateMemberDirectory(this, session.getCommunityName(), fullMemberList);
                    } else {
                        Toast.makeText(this, "No members to export", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        setupOfflineEngineAndLoadData();
        setupSearch();
    }

    private void setupOfflineEngineAndLoadData() {
        DatabaseReference membersRef = db.child("communities").child(session.getCommunityId()).child("members");
        membersRef.keepSynced(true);
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullMemberList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member m = data.getValue(Member.class);
                    if (m != null) fullMemberList.add(m);
                }
                renderList(fullMemberList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        if (inputSearch == null) return;
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
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
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void renderList(List<Member> listToRender) {
        if (membersContainer == null) return;
        membersContainer.removeAllViews();

        for (Member member : listToRender) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_member, membersContainer, false);
                ((TextView) view.findViewById(R.id.tvMemberName)).setText(member.name);
                ((TextView) view.findViewById(R.id.tvMemberId)).setText(member.id + " | " + member.phone);
                ((TextView) view.findViewById(R.id.tvMemberDonation)).setText("Donated: ৳" + member.totalDonated);

                view.setOnClickListener(v -> showSafeMemberProfile(member));
                membersContainer.addView(view);
            } catch (Exception e) {
                TextView fallbackText = new TextView(this);
                fallbackText.setText("👤 " + member.name + " (" + member.id + ")\nDonated: ৳" + member.totalDonated);
                fallbackText.setPadding(40, 40, 40, 40);
                fallbackText.setTextSize(16f);
                fallbackText.setOnClickListener(v -> showSafeMemberProfile(member));
                membersContainer.addView(fallbackText);
            }
        }
    }

    private void showSafeMemberProfile(Member member) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("👤 " + member.name);
            
            String phone = member.phone != null ? member.phone : "N/A";
            String gotra = member.gotra != null && !member.gotra.isEmpty() ? member.gotra : "Not specified";
            String bloodGroup = member.bloodGroup != null && !member.bloodGroup.isEmpty() ? member.bloodGroup : "Not specified";

            String details = "SB-ID: " + member.id + "\n"
                           + "Phone: " + phone + "\n"
                           + "Gotra: " + gotra + "\n"
                           + "Blood Group: " + bloodGroup + "\n\n"
                           + "Total Donated: ৳" + member.totalDonated;

            builder.setMessage(details);
            builder.setPositiveButton("📄 Export PDF", (dialog, which) -> {
                PdfReportService.generateMemberProfile(MemberActivity.this, session.getCommunityName(), member);
            });
            builder.setNegativeButton("Close", null);
            builder.show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open profile.", Toast.LENGTH_SHORT).show();
        }
    }
}
