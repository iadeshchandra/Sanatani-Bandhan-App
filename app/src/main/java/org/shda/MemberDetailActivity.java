package org.shda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemberDetailActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private Member currentMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);

        String memberId = getIntent().getStringExtra("MEMBER_ID");
        if (memberId == null) { finish(); return; }

        loadMemberData(memberId);

        findViewById(R.id.btnDownloadProfile).setOnClickListener(v -> {
            if (currentMember != null) {
                PdfReportService.generateMemberProfile(this, session.getCommunityName(), currentMember);
            }
        });
    }

    private void loadMemberData(String memberId) {
        db.child("communities").child(session.getCommunityId()).child("members").child(memberId)
          .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentMember = snapshot.getValue(Member.class);
                if (currentMember != null) {
                    ((TextView) findViewById(R.id.tvDetailId)).setText(currentMember.id);
                    ((TextView) findViewById(R.id.tvDetailName)).setText(currentMember.name);
                    ((TextView) findViewById(R.id.tvDetailPhone)).setText("📞 " + currentMember.phone);
                    ((TextView) findViewById(R.id.tvDetailGotra)).setText("🕉 Gotra: " + currentMember.gotra);
                    ((TextView) findViewById(R.id.tvDetailBlood)).setText("🩸 Blood Group: " + currentMember.bloodGroup);
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    ((TextView) findViewById(R.id.tvDetailJoinDate)).setText("Joined: " + sdf.format(new Date(currentMember.timestamp)));
                    ((TextView) findViewById(R.id.tvDetailTotal)).setText("Total Donated: ৳" + currentMember.totalDonated);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}
