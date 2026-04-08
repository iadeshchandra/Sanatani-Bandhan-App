package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private List<Member> fullMemberList = new ArrayList<>();
    private HashMap<String, String> lastDonationTracker = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member); 

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        if (session.getCommunityId() == null) { finish(); return; }

        membersContainer = findViewById(R.id.membersContainer);
        inputSearch = findViewById(R.id.inputSearch);
        
        findViewById(R.id.btnAddNew).setOnClickListener(v -> startActivity(new Intent(this, AddMemberActivity.class)));
        findViewById(R.id.btnSharePdf).setOnClickListener(v -> {
            if (!fullMemberList.isEmpty()) PdfReportService.generateMemberDirectory(this, session.getCommunityName(), fullMemberList);
        });

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
                        lastDonationTracker.put(name, "Last: ৳" + amt + " on " + sdf.format(new Date(ts)));
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
            ((TextView) view.findViewById(R.id.tvMemberDonation)).setText("Lifetime: ৳" + member.totalDonated);
            ((TextView) view.findViewById(R.id.tvMemberIdPhone)).setText(member.id + "  |  📞 " + (member.phone != null ? member.phone : "N/A"));
            
            ((TextView) view.findViewById(R.id.tvMemberJoined)).setText("Joined: " + (member.timestamp > 0 ? sdfJoined.format(new Date(member.timestamp)) : "N/A"));
            
            TextView tvLast = view.findViewById(R.id.tvLastDonation);
            if (lastDonationTracker.containsKey(member.name + " [Member]")) {
                tvLast.setText(lastDonationTracker.get(member.name + " [Member]"));
            } else {
                tvLast.setText("No donations recorded yet.");
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
