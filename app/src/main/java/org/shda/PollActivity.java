package org.shda;

import android.graphics.Color;
import android.graphics.Typeface;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout pollsContainer;
    private List<Poll> pollList = new ArrayList<>();
    private boolean isManagerOrAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        pollsContainer = findViewById(R.id.pollsContainer);

        if (session.getCommunityId() == null) { finish(); return; }
        isManagerOrAdmin = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        View btnCreatePoll = findViewById(R.id.btnCreatePoll);
        if (btnCreatePoll != null) {
            if (!isManagerOrAdmin) btnCreatePoll.setVisibility(View.GONE);
            else btnCreatePoll.setOnClickListener(v -> showCreatePollDialog());
        }
        
        View btnExportPolls = findViewById(R.id.btnExportPolls);
        if (btnExportPolls != null) btnExportPolls.setOnClickListener(v -> {
            if (!pollList.isEmpty()) PdfReportService.generateMultiplePollsReport(this, session.getCommunityName(), pollList, "All Polls", "ADMIN".equals(session.getRole()));
        });

        loadPolls();
    }

    private void loadPolls() {
        DatabaseReference pollsRef = db.child("communities").child(session.getCommunityId()).child("polls");
        pollsRef.keepSynced(true);
        pollsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                pollList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Poll p = data.getValue(Poll.class);
                    if (p != null) pollList.add(p);
                }
                Collections.sort(pollList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                renderPolls();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderPolls() {
        pollsContainer.removeAllViews();
        long now = System.currentTimeMillis();

        for (Poll poll : pollList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_poll, pollsContainer, false);
                
                boolean isClosed = now > poll.endTimestamp;
                
                TextView tvStatus = view.findViewById(R.id.tvPollStatus);
                tvStatus.setText(isClosed ? "🔴 CLOSED" : "🟢 ACTIVE");
                tvStatus.setTextColor(Color.parseColor(isClosed ? "#D32F2F" : "#2E7D32"));
                tvStatus.setBackgroundColor(Color.parseColor(isClosed ? "#FFEBEE" : "#E8F5E9"));
                
                ((TextView) view.findViewById(R.id.tvPollCreator)).setText("Created by " + poll.createdBy);
                ((TextView) view.findViewById(R.id.tvPollQuestion)).setText("📊 " + poll.question);

                TextView tvCountdown = view.findViewById(R.id.tvPollCountdown);
                if (!isClosed) {
                    long diff = poll.endTimestamp - now;
                    long days = diff / (1000 * 60 * 60 * 24);
                    long hours = (diff / (1000 * 60 * 60)) % 24;
                    tvCountdown.setText("⏳ Closes in: " + days + "d " + hours + "h");
                    tvCountdown.setVisibility(View.VISIBLE);
                } else {
                    tvCountdown.setVisibility(View.GONE);
                }

                TextView tvAdminNote = view.findViewById(R.id.tvPollAdminComment);
                if (poll.adminComment != null && !poll.adminComment.trim().isEmpty()) {
                    tvAdminNote.setText("Note: " + poll.adminComment);
                    tvAdminNote.setVisibility(View.VISIBLE);
                }

                int totalVotes = poll.votes != null ? poll.votes.size() : 0;
                int countA = 0, countB = 0, countC = 0, countD = 0;
                boolean hasVoted = false; String myVote = "";

                if (poll.votes != null) {
                    for (Map.Entry<String, String> entry : poll.votes.entrySet()) {
                        if (entry.getValue().equals("A")) countA++; else if (entry.getValue().equals("B")) countB++; else if (entry.getValue().equals("C")) countC++; else if (entry.getValue().equals("D")) countD++;
                        if (entry.getKey().equals(session.getUserId())) { hasVoted = true; myVote = entry.getValue(); }
                    }
                }

                LinearLayout optionsContainer = view.findViewById(R.id.pollOptionsContainer);
                Button btnVote = view.findViewById(R.id.btnVote);

                if (isClosed || hasVoted) {
                    btnVote.setVisibility(View.GONE); 
                    addResultBar(optionsContainer, poll.optionA, countA, totalVotes, myVote.equals("A")); addResultBar(optionsContainer, poll.optionB, countB, totalVotes, myVote.equals("B"));
                    if (poll.optionC != null && !poll.optionC.isEmpty()) addResultBar(optionsContainer, poll.optionC, countC, totalVotes, myVote.equals("C"));
                    if (poll.optionD != null && !poll.optionD.isEmpty()) addResultBar(optionsContainer, poll.optionD, countD, totalVotes, myVote.equals("D"));
                    TextView tvTotal = new TextView(this); tvTotal.setText("Total Votes Cast: " + totalVotes); tvTotal.setTextSize(12f); tvTotal.setTextColor(Color.GRAY); tvTotal.setPadding(0, 16, 0, 0); optionsContainer.addView(tvTotal);
                } else {
                    btnVote.setVisibility(View.VISIBLE); btnVote.setOnClickListener(v -> showVotingDialog(poll));
                    addSimpleTextOption(optionsContainer, "• " + poll.optionA); addSimpleTextOption(optionsContainer, "• " + poll.optionB);
                    if (poll.optionC != null && !poll.optionC.isEmpty()) addSimpleTextOption(optionsContainer, "• " + poll.optionC);
                    if (poll.optionD != null && !poll.optionD.isEmpty()) addSimpleTextOption(optionsContainer, "• " + poll.optionD);
                }

                Button btnDownload = view.findViewById(R.id.btnDownloadPollPdf);
                if (!isManagerOrAdmin) btnDownload.setVisibility(View.GONE);
                else btnDownload.setOnClickListener(v -> PdfReportService.generatePollReport(this, session.getCommunityName(), poll, "ADMIN".equals(session.getRole())));

                if (isManagerOrAdmin) view.setOnLongClickListener(v -> { showPollManagerDialog(poll); return true; });
                pollsContainer.addView(view);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void addResultBar(LinearLayout container, String optionText, int votes, int totalVotes, boolean isMyVote) {
        int percentage = totalVotes == 0 ? 0 : Math.round(((float) votes / totalVotes) * 100);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(0, 12, 0, 12);
        LinearLayout textRow = new LinearLayout(this); textRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView tvOpt = new TextView(this); tvOpt.setText(optionText + (isMyVote ? " ✅" : "")); tvOpt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); tvOpt.setTextColor(Color.parseColor("#212121")); tvOpt.setTypeface(null, isMyVote ? Typeface.BOLD : Typeface.NORMAL);
        TextView tvPct = new TextView(this); tvPct.setText(percentage + "% (" + votes + ")"); tvPct.setTextColor(Color.parseColor("#757575")); tvPct.setTextSize(12f);
        textRow.addView(tvOpt); textRow.addView(tvPct);
        LinearLayout barRow = new LinearLayout(this); barRow.setOrientation(LinearLayout.HORIZONTAL); barRow.setPadding(0, 8, 0, 0); barRow.setWeightSum(100f);
        View fill = new View(this); fill.setBackgroundColor(Color.parseColor(isMyVote ? "#E65100" : "#9E9E9E")); fill.setLayoutParams(new LinearLayout.LayoutParams(0, 16, percentage > 0 ? percentage : 0.01f));
        View empty = new View(this); empty.setBackgroundColor(Color.parseColor("#E0E0E0")); empty.setLayoutParams(new LinearLayout.LayoutParams(0, 16, 100 - percentage > 0 ? 100 - percentage : 0.01f));
        barRow.addView(fill); barRow.addView(empty); row.addView(textRow); row.addView(barRow); container.addView(row);
    }

    private void addSimpleTextOption(LinearLayout container, String text) {
        TextView tv = new TextView(this); tv.setText(text); tv.setPadding(0, 8, 0, 8); tv.setTextColor(Color.DKGRAY); tv.setTextSize(14f); container.addView(tv);
    }

    private void showCreatePollDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle("Create Panchayat Poll");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final EditText inputQ = new EditText(this); inputQ.setHint("Question / Topic");
        final EditText inputA = new EditText(this); inputA.setHint("Option A");
        final EditText inputB = new EditText(this); inputB.setHint("Option B");
        final EditText inputC = new EditText(this); inputC.setHint("Option C (Optional)");
        final EditText inputD = new EditText(this); inputD.setHint("Option D (Optional)");
        final EditText inputDays = new EditText(this); inputDays.setHint("Duration (Days)"); inputDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText inputComment = new EditText(this); inputComment.setHint("Admin Comment (Optional)");

        layout.addView(inputQ); layout.addView(inputA); layout.addView(inputB); layout.addView(inputC); layout.addView(inputD); layout.addView(inputDays); layout.addView(inputComment);
        builder.setView(layout); builder.setPositiveButton("CREATE", null); builder.setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create(); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String q = inputQ.getText().toString().trim(); String a = inputA.getText().toString().trim(); String b = inputB.getText().toString().trim(); String daysStr = inputDays.getText().toString().trim();
            if (q.isEmpty() || a.isEmpty() || b.isEmpty() || daysStr.isEmpty()) { Toast.makeText(this, "Required fields missing", Toast.LENGTH_SHORT).show(); return; }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Creating...");
            long days = Long.parseLong(daysStr); long endTs = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000);
            String pollId = db.child("communities").child(session.getCommunityId()).child("polls").push().getKey();
            
            HashMap<String, Object> pollMap = new HashMap<>();
            pollMap.put("id", pollId); pollMap.put("question", q); pollMap.put("optionA", a); pollMap.put("optionB", b); pollMap.put("optionC", inputC.getText().toString().trim()); pollMap.put("optionD", inputD.getText().toString().trim()); 
            pollMap.put("adminComment", inputComment.getText().toString().trim()); 
            pollMap.put("createdBy", session.getUserName()); pollMap.put("timestamp", System.currentTimeMillis()); pollMap.put("endTimestamp", endTs);

            db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).setValue(pollMap);
            Toast.makeText(this, "Poll Created!", Toast.LENGTH_SHORT).show(); dialog.dismiss();
        });
    }

    private void showVotingDialog(Poll poll) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle(poll.question);
        String[] options = {poll.optionA, poll.optionB, poll.optionC, poll.optionD};
        List<String> validOptions = new ArrayList<>(); for(String o : options) if (o != null && !o.isEmpty()) validOptions.add(o);

        builder.setItems(validOptions.toArray(new String[0]), (dialog, which) -> {
            String selectedOption = "A"; if (which == 1) selectedOption = "B"; else if (which == 2) selectedOption = "C"; else if (which == 3) selectedOption = "D";
            db.child("communities").child(session.getCommunityId()).child("polls").child(poll.id).child("votes").child(session.getUserId()).setValue(selectedOption);
            Toast.makeText(this, "Vote Recorded!", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void showPollManagerDialog(Poll poll) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Poll").setMessage("Question: " + poll.question);

        builder.setPositiveButton("EDIT QUESTION", (d, w) -> {
            AlertDialog.Builder editBuilder = new AlertDialog.Builder(this);
            final EditText inputQ = new EditText(this); inputQ.setText(poll.question);
            editBuilder.setView(inputQ).setPositiveButton("SAVE", (d2, w2) -> {
                String newQ = inputQ.getText().toString();
                db.child("communities").child(session.getCommunityId()).child("polls").child(poll.id).child("question").setValue(newQ);
                logAudit("POLL_EDITED", "Edited poll question to: " + newQ);
                Toast.makeText(this, "Poll Updated", Toast.LENGTH_SHORT).show();
            }).show();
        });

        builder.setNegativeButton("DELETE POLL", (d, w) -> {
            new AlertDialog.Builder(this).setTitle("Confirm Deletion").setMessage("Erase this poll and all votes?")
                .setPositiveButton("YES", (d2, w2) -> {
                    db.child("communities").child(session.getCommunityId()).child("polls").child(poll.id).removeValue();
                    logAudit("POLL_DELETED", "Deleted poll: " + poll.question);
                    Toast.makeText(this, "Poll Erased", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("CANCEL", null).show();
        });
        builder.show();
    }

    private void logAudit(String actionType, String description) {
        String historyId = db.child("communities").child(session.getCommunityId()).child("audit_logs").push().getKey();
        HashMap<String, Object> auditMap = new HashMap<>();
        auditMap.put("managerName", session.getUserName()); auditMap.put("actionType", actionType);
        auditMap.put("description", description); auditMap.put("timestamp", System.currentTimeMillis());
        db.child("communities").child(session.getCommunityId()).child("audit_logs").child(historyId).setValue(auditMap);
    }
}
