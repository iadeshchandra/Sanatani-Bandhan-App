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

        findViewById(R.id.btnExportAllPolls).setOnClickListener(v -> triggerAllPollsReport());
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
                        if (poll != null && poll.id != null) {
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
            TextView tvExpiry = view.findViewById(R.id.tvPollExpiry);
            TextView tvLiveCounts = view.findViewById(R.id.tvLiveCounts);
            
            Button btnOptA = view.findViewById(R.id.btnOptionA); 
            Button btnOptB = view.findViewById(R.id.btnOptionB);
            Button btnOptC = view.findViewById(R.id.btnOptionC); 
            Button btnOptD = view.findViewById(R.id.btnOptionD);
            
            TextView tvStatus = view.findViewById(R.id.tvPollStatus);
            LinearLayout layoutVoting = view.findViewById(R.id.layoutVoting);
            LinearLayout layoutVoting2 = view.findViewById(R.id.layoutVoting2);
            TextView tvOfficialComment = view.findViewById(R.id.tvOfficialComment);
            Button btnAddComment = view.findViewById(R.id.btnAddComment);
            Button btnDownload = view.findViewById(R.id.btnDownloadPollData);
            
            tvQuestion.setText(poll.question);
            btnOptA.setText(poll.optionA); 
            btnOptB.setText(poll.optionB);
            
            if (poll.optionC != null && !poll.optionC.isEmpty()) { btnOptC.setVisibility(View.VISIBLE); btnOptC.setText(poll.optionC); layoutVoting2.setVisibility(View.VISIBLE); }
            if (poll.optionD != null && !poll.optionD.isEmpty()) { btnOptD.setVisibility(View.VISIBLE); btnOptD.setText(poll.optionD); layoutVoting2.setVisibility(View.VISIBLE); }

            if (poll.officialComment != null && !poll.officialComment.isEmpty()) {
                tvOfficialComment.setVisibility(View.VISIBLE);
                tvOfficialComment.setText(poll.officialComment);
            }

            long currentTime = System.currentTimeMillis();
            boolean isPollClosed = currentTime > poll.endTimestamp;
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            
            if (isPollClosed) {
                tvExpiry.setText("🛑 POLL CLOSED (Ended " + sdf.format(new java.util.Date(poll.endTimestamp)) + ")");
                layoutVoting.setVisibility(View.GONE); layoutVoting2.setVisibility(View.GONE);
                tvStatus.setVisibility(View.VISIBLE); tvStatus.setText("Final Results Locked");
            } else {
                tvExpiry.setText("🟢 ACTIVE (Closes " + sdf.format(new java.util.Date(poll.endTimestamp)) + ")");
            }

            int cA = 0, cB = 0, cC = 0, cD = 0;
            if (poll.votes != null) {
                for (String choice : poll.votes.values()) {
                    if ("A".equals(choice)) cA++; else if ("B".equals(choice)) cB++; else if ("C".equals(choice)) cC++; else if ("D".equals(choice)) cD++;
                }
            }
            
            String tally = poll.optionA + ": " + cA + " | " + poll.optionB + ": " + cB;
            if (poll.optionC != null && !poll.optionC.isEmpty()) tally += " | " + poll.optionC + ": " + cC;
            if (poll.optionD != null && !poll.optionD.isEmpty()) tally += " | " + poll.optionD + ": " + cD;
            tvLiveCounts.setText("Live Tally ➔ " + tally);

            String userId = session.getUserId() != null && !session.getUserId().isEmpty() ? session.getUserId() : session.getUserName();
            boolean hasVoted = poll.votes != null && poll.votes.containsKey(userId);

            if (hasVoted && !isPollClosed) {
                layoutVoting.setVisibility(View.GONE); layoutVoting2.setVisibility(View.GONE);
                tvStatus.setVisibility(View.VISIBLE); 
                String uChoice = poll.votes.get(userId);
                String voteStr = uChoice.equals("A") ? poll.optionA : (uChoice.equals("B") ? poll.optionB : (uChoice.equals("C") ? poll.optionC : poll.optionD));
                tvStatus.setText("Your Vote Recorded: " + voteStr);
            } else if (!isPollClosed) {
                btnOptA.setOnClickListener(v -> submitVote(poll.id, userId, "A"));
                btnOptB.setOnClickListener(v -> submitVote(poll.id, userId, "B"));
                btnOptC.setOnClickListener(v -> submitVote(poll.id, userId, "C"));
                btnOptD.setOnClickListener(v -> submitVote(poll.id, userId, "D"));
            }

            if (isAdminOrManager && btnAddComment != null) {
                btnAddComment.setVisibility(View.VISIBLE);
                btnAddComment.setOnClickListener(v -> showAddCommentDialog(poll.id));
            }

            btnDownload.setOnClickListener(v -> PdfReportService.generatePollReport(this, session.getCommunityName(), poll, isSuperAdmin));

            pollsContainer.addView(view, 0);
        } catch (Exception e) {}
    }

    private void submitVote(String pollId, String userId, String choice) {
        db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).child("votes").child(userId).setValue(choice)
          .addOnSuccessListener(aVoid -> Toast.makeText(this, "Vote cast!", Toast.LENGTH_SHORT).show());
    }

    private void showCreatePollDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Community Poll");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final EditText inputQuestion = new EditText(this); inputQuestion.setHint("Question *"); layout.addView(inputQuestion);
        final EditText inputOptA = new EditText(this); inputOptA.setHint("Option 1 *"); layout.addView(inputOptA);
        final EditText inputOptB = new EditText(this); inputOptB.setHint("Option 2 *"); layout.addView(inputOptB);
        final EditText inputOptC = new EditText(this); inputOptC.setHint("Option 3 (Optional)"); layout.addView(inputOptC);
        final EditText inputOptD = new EditText(this); inputOptD.setHint("Option 4 (Optional)"); layout.addView(inputOptD);

        final long[] selectedEndTimestamp = {System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)}; // Default 7 days
        Button btnDate = new Button(this); btnDate.setText("Set End Date (Default: 7 Days)");
        btnDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                cal.set(y, m, d, 23, 59, 59);
                selectedEndTimestamp[0] = cal.getTimeInMillis();
                btnDate.setText("Ends on: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime()));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(btnDate);

        builder.setView(layout);
        builder.setPositiveButton("PUBLISH", (dialog, which) -> {
            String q = inputQuestion.getText().toString().trim();
            String a = inputOptA.getText().toString().trim(); String b = inputOptB.getText().toString().trim();
            String c = inputOptC.getText().toString().trim(); String d = inputOptD.getText().toString().trim();
            
            if (q.isEmpty() || a.isEmpty() || b.isEmpty()) { Toast.makeText(this, "Question and 2 options required", Toast.LENGTH_SHORT).show(); return; }
            if (selectedEndTimestamp[0] <= System.currentTimeMillis()) { Toast.makeText(this, "End time must be in future", Toast.LENGTH_SHORT).show(); return; }

            String pollId = db.child("communities").child(session.getCommunityId()).child("polls").push().getKey();
            String signature = "ADMIN".equals(session.getRole()) ? "Super Admin - " + session.getUserName() : "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";
            
            Poll newPoll = new Poll(pollId, q, a, b, c, d, System.currentTimeMillis(), selectedEndTimestamp[0], signature);
            db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).setValue(newPoll);
            Toast.makeText(this, "Poll Published", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
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
                String strictSignature = "ADMIN".equals(session.getRole()) ? "Super Admin - " + session.getUserName() : "Manager - " + session.getUserName() + " (" + session.getUserId() + ")";
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
                long startTs = startCal.getTimeInMillis(); long endTs = endCal.getTimeInMillis();
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
