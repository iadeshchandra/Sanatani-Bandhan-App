package org.shda;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PollActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout pollsContainer;
    private boolean isAdminOrManager;
    private boolean isSuperAdmin;
    private List<Poll> allPolls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        pollsContainer = findViewById(R.id.pollsContainer);

        if (session.getCommunityId() == null) { finish(); return; }

        String role = session.getRole();
        if (role == null) role = "MEMBER"; 
        
        isAdminOrManager = role.equals("ADMIN") || role.equals("MANAGER");
        isSuperAdmin = role.equals("ADMIN");

        Button btnCreatePoll = findViewById(R.id.btnCreatePoll);
        if (isAdminOrManager && btnCreatePoll != null) {
            btnCreatePoll.setVisibility(View.VISIBLE);
            btnCreatePoll.setOnClickListener(v -> showCreatePollDialog());
        }

        View btnExport = findViewById(R.id.btnExportAllPolls);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> triggerAllPollsReport());
        }

        loadPolls();
    }

    private void loadPolls() {
        db.child("communities").child(session.getCommunityId()).child("polls").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(pollsContainer == null) return;
                pollsContainer.removeAllViews();
                allPolls.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Poll poll = data.getValue(Poll.class);
                        if (poll != null && poll.id != null && poll.question != null) {
                            allPolls.add(poll);
                            renderPollCard(poll);
                        }
                    } catch (Exception e) {}
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderPollCard(Poll poll) {
        try {
            View view = LayoutInflater.from(this).inflate(R.layout.item_poll, pollsContainer, false);
            
            TextView tvQuestion = view.findViewById(R.id.tvPollQuestion);
            TextView tvLiveCounts = view.findViewById(R.id.tvLiveCounts);
            Button btnOptA = view.findViewById(R.id.btnOptionA);
            Button btnOptB = view.findViewById(R.id.btnOptionB);
            TextView tvStatus = view.findViewById(R.id.tvPollStatus);
            LinearLayout layoutVoting = view.findViewById(R.id.layoutVoting);
            TextView tvOfficialComment = view.findViewById(R.id.tvOfficialComment);
            Button btnAddComment = view.findViewById(R.id.btnAddComment);
            Button btnDownload = view.findViewById(R.id.btnDownloadPollData);

            if(tvQuestion != null) tvQuestion.setText(poll.question);
            if(btnOptA != null) btnOptA.setText(poll.optionA != null ? poll.optionA : "Option A");
            if(btnOptB != null) btnOptB.setText(poll.optionB != null ? poll.optionB : "Option B");

            int countA = 0, countB = 0;
            if (poll.votes != null) {
                for (String choice : poll.votes.values()) {
                    if ("A".equals(choice)) countA++;
                    else if ("B".equals(choice)) countB++;
                }
            }
            if(tvLiveCounts != null) tvLiveCounts.setText("Live Tally ➔ " + (poll.optionA != null ? poll.optionA : "A") + ": " + countA + " | " + (poll.optionB != null ? poll.optionB : "B") + ": " + countB);

            if (poll.officialComment != null && !poll.officialComment.isEmpty() && tvOfficialComment != null) {
                tvOfficialComment.setVisibility(View.VISIBLE);
                tvOfficialComment.setText(poll.officialComment);
            }

            String userId = session.getUserId() != null && !session.getUserId().isEmpty() ? session.getUserId() : session.getUserName();
            boolean hasVoted = poll.votes != null && poll.votes.containsKey(userId);

            if (hasVoted) {
                if(layoutVoting != null) layoutVoting.setVisibility(View.GONE);
                if(tvStatus != null) {
                    tvStatus.setVisibility(View.VISIBLE);
                    String userChoice = poll.votes.get(userId).equals("A") ? poll.optionA : poll.optionB;
                    tvStatus.setText("Your Vote Recorded: " + userChoice);
                }
            } else {
                if(btnOptA != null) btnOptA.setOnClickListener(v -> submitVote(poll.id, userId, "A"));
                if(btnOptB != null) btnOptB.setOnClickListener(v -> submitVote(poll.id, userId, "B"));
            }

            if (isAdminOrManager && btnAddComment != null) {
                btnAddComment.setVisibility(View.VISIBLE);
                btnAddComment.setOnClickListener(v -> showAddCommentDialog(poll.id));
            }

            if(btnDownload != null) btnDownload.setOnClickListener(v -> PdfReportService.generatePollReport(this, session.getCommunityName(), poll, isSuperAdmin));

            pollsContainer.addView(view, 0);
        } catch (Exception e) {}
    }

    private void submitVote(String pollId, String userId, String choice) {
        if (pollId == null) return;
        db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).child("votes").child(userId).setValue(choice)
          .addOnSuccessListener(aVoid -> Toast.makeText(this, "Vote securely cast!", Toast.LENGTH_SHORT).show());
    }

    private void showCreatePollDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Community Poll");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final EditText inputQuestion = new EditText(this); inputQuestion.setHint("Question"); layout.addView(inputQuestion);
        final EditText inputOptA = new EditText(this); inputOptA.setHint("Option 1"); layout.addView(inputOptA);
        final EditText inputOptB = new EditText(this); inputOptB.setHint("Option 2"); layout.addView(inputOptB);

        builder.setView(layout);
        builder.setPositiveButton("PUBLISH", (dialog, which) -> {
            String q = inputQuestion.getText().toString().trim();
            String a = inputOptA.getText().toString().trim();
            String b = inputOptB.getText().toString().trim();
            if (q.isEmpty() || a.isEmpty() || b.isEmpty()) { Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show(); return; }

            String pollId = db.child("communities").child(session.getCommunityId()).child("polls").push().getKey();
            
            // 🛡️ DYNAMIC STRICT SIGNATURE
            String strictSignature;
            if ("ADMIN".equals(session.getRole())) {
                strictSignature = "Super Admin - " + session.getUserName();
            } else {
                strictSignature = "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";
            }
            
            Poll newPoll = new Poll(pollId, q, a, b, System.currentTimeMillis(), strictSignature);
            db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).setValue(newPoll);
            
            AuditLogger.logAction(session.getCommunityId(), session.getUserName(), "POLL_CREATED", "Created poll: " + q);
            Toast.makeText(this, "Poll Published Live", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAddCommentDialog(String pollId) {
        if (pollId == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Official Remark");
        final EditText inputComment = new EditText(this); inputComment.setHint("Write remark here...");
        
        LinearLayout layout = new LinearLayout(this); layout.setPadding(50, 20, 50, 0); layout.addView(inputComment);
        builder.setView(layout);

        builder.setPositiveButton("POST", (dialog, which) -> {
            String comment = inputComment.getText().toString().trim();
            if (!comment.isEmpty()) {
                // 🛡️ DYNAMIC STRICT SIGNATURE
                String strictSignature;
                if ("ADMIN".equals(session.getRole())) {
                    strictSignature = "Super Admin - " + session.getUserName();
                } else {
                    strictSignature = "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";
                }
                
                String signature = "✍️ [" + strictSignature + "]: " + comment;
                db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).child("officialComment").setValue(signature);
                Toast.makeText(this, "Remark added", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void triggerAllPollsReport() {
        Calendar startCal = Calendar.getInstance(); Calendar endCal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                
                long startTs = startCal.getTimeInMillis();
                long endTs = endCal.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String range = "Period: " + sdf.format(startCal.getTime()) + " to " + sdf.format(endCal.getTime());

                List<Poll> filtered = new ArrayList<>();
                for (Poll p : allPolls) {
                    if (p.timestamp >= startTs && p.timestamp <= endTs) { filtered.add(p); }
                }
                
                if (filtered.isEmpty()) { Toast.makeText(this, "No polls found in date range", Toast.LENGTH_SHORT).show(); } 
                else { PdfReportService.generateMultiplePollsReport(this, session.getCommunityName(), filtered, range, isSuperAdmin); }
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
