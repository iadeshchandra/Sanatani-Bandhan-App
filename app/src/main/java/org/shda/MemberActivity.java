package org.shda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.firebase.database.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.File;
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

        if (session.getCommunityId() == null) {
            finish();
            return;
        }

        loadMembers();

        findViewById(R.id.btnSharePdf).setOnClickListener(v -> generateAndSharePDF());
        findViewById(R.id.btnGoToAdd).setOnClickListener(v -> startActivity(new Intent(MemberActivity.this, AddMemberActivity.class)));
    }

    private void loadMembers() {
        db.child("communities").child(session.getCommunityId()).child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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

        membersContainer.addView(view);
    }

    private void generateAndSharePDF() {
        if (memberList.isEmpty()) {
            Toast.makeText(this, "No members to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "Directory_" + System.currentTimeMillis() + ".pdf");

            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph(session.getCommunityName() + " - Member Directory").setBold().setFontSize(18));
            document.add(new Paragraph("Generated securely from SaaS Portal.\n\n"));

            for (Member m : memberList) {
                document.add(new Paragraph(m.id + " | " + m.name + " | Phone: " + m.phone + " | Total Donated: ৳" + m.totalDonated));
                document.add(new Paragraph("--------------------------------------------------"));
            }
            document.close();

            shareFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, session.getCommunityName() + " Member Directory");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Directory via..."));
    }
}
