package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MemberActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout membersContainer;
    private List<Member> memberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member);
        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        membersContainer = findViewById(R.id.membersContainer);

        if (session.getCommunityId() == null) { finish(); return; }

        loadMembers();

        findViewById(R.id.btnSharePdf).setOnClickListener(v -> generateAndSharePDF());
        findViewById(R.id.btnGoToAdd).setOnClickListener(v -> startActivity(new Intent(MemberActivity.this, AddMemberActivity.class)));
    }

    private void loadMembers() {
        db.child("communities").child(session.getCommunityId()).child("members").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersContainer.removeAllViews();
                memberList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Member member = data.getValue(Member.class);
                    if (member != null) {
                        memberList.add(member);
                        addMemberView(member);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addMemberView(Member member) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_member, membersContainer, false);
        TextView tvName = view.findViewById(R.id.tvMemberName);
        TextView tvId = view.findViewById(R.id.tvMemberId);
        TextView tvTotal = view.findViewById(R.id.tvTotalDonated);

        tvName.setText(member.name);
        tvId.setText(member.id + " | " + member.phone);
        tvTotal.setText("Donated: ৳" + member.totalDonated);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(MemberActivity.this, MemberDetailActivity.class);
            intent.putExtra("MEMBER_ID", member.id);
            startActivity(intent);
        });

        membersContainer.addView(view);
    }

    private void generateAndSharePDF() {
        if (memberList.isEmpty()) { Toast.makeText(this, "No members to export", Toast.LENGTH_SHORT).show(); return; }
        PdfReportService.generateMemberDirectory(this, session.getCommunityName(), memberList);
    }
}
