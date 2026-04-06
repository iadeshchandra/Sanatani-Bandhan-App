package org.shda;

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

        loadPolls();
    }

    private void loadPolls() {
        DatabaseReference pollsRef = db.child("communities").child(session.getCommunityId()).child("polls");
        // ✨ THE OFFLINE FIX: Forces Firebase to keep this list in local memory
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
        for (Poll poll : pollList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_poll, pollsContainer, false);
                ((TextView) view.findViewById(R.id.tvPollQuestion)).setText("📊 " + poll.question);
                
                String status = System.currentTimeMillis() > poll.endTimestamp ? "🔴 CLOSED" : "🟢 ACTIVE";
                ((TextView) view.findViewById(R.id.tvPollStatus)).setText(status);

                Button btnVote = view.findViewById(R.id.btnVote);
                if (System.currentTimeMillis() > poll.endTimestamp) {
                    btnVote.setText("View Results");
                    btnVote.setOnClickListener(v -> showResultsDialog(poll));
                } else {
                    btnVote.setText("Cast Vote");
                    btnVote.setOnClickListener(v -> showVotingDialog(poll));
                }

                Button btnDownload = view.findViewById(R.id.btnDownloadPollPdf);
                if (!isManagerOrAdmin) btnDownload.setVisibility(View.GONE);
                else btnDownload.setOnClickListener(v -> PdfReportService.generatePollReport(this, session.getCommunityName(), poll, "ADMIN".equals(session.getRole())));

                pollsContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showCreatePollDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Panchayat Poll");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        final EditText inputQ = new EditText(this); inputQ.setHint("Question / Topic");
        final EditText inputA = new EditText(this); inputA.setHint("Option A");
        final EditText inputB = new EditText(this); inputB.setHint("Option B");
        final EditText inputC = new EditText(this); inputC.setHint("Option C (Optional)");
        final EditText inputD = new EditText(this); inputD.setHint("Option D (Optional)");
        final EditText inputDays = new EditText(this); inputDays.setHint("Duration (Days)"); inputDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        layout.addView(inputQ); layout.addView(inputA); layout.addView(inputB); layout.addView(inputC); layout.addView(inputD); layout.addView(inputDays);
        builder.setView(layout);

        builder.setPositiveButton("CREATE", null);
        builder.setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // ✨ THE FIX: Prevent Double Clicks offline
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String q = inputQ.getText().toString().trim();
            String a = inputA.getText().toString().trim();
            String b = inputB.getText().toString().trim();
            String daysStr = inputDays.getText().toString().trim();

            if (q.isEmpty() || a.isEmpty() || b.isEmpty() || daysStr.isEmpty()) {
                Toast.makeText(this, "Required fields missing", Toast.LENGTH_SHORT).show(); return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Creating...");

            long days = Long.parseLong(daysStr);
            long endTs = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000);
            String pollId = db.child("communities").child(session.getCommunityId()).child("polls").push().getKey();

            Poll newPoll = new Poll(pollId, q, a, b, inputC.getText().toString().trim(), inputD.getText().toString().trim(), "", session.getUserName(), System.currentTimeMillis(), endTs, new HashMap<>());
            
            db.child("communities").child(session.getCommunityId()).child("polls").child(pollId).setValue(newPoll);
            Toast.makeText(this, "Poll Created Locally!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void showVotingDialog(Poll poll) {
        // Keeping your standard voting logic here...
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(poll.question);
        String[] options = {poll.optionA, poll.optionB, poll.optionC, poll.optionD};
        List<String> validOptions = new ArrayList<>();
        for(String o : options) if (o != null && !o.isEmpty()) validOptions.add(o);

        builder.setItems(validOptions.toArray(new String[0]), (dialog, which) -> {
            String selectedOption = "A";
            if (which == 1) selectedOption = "B"; else if (which == 2) selectedOption = "C"; else if (which == 3) selectedOption = "D";
            
            db.child("communities").child(session.getCommunityId()).child("polls").child(poll.id).child("votes").child(session.getUserId()).setValue(selectedOption);
            Toast.makeText(this, "Vote Recorded!", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void showResultsDialog(Poll poll) {
        Toast.makeText(this, "Poll Closed. Download PDF for full insight.", Toast.LENGTH_LONG).show();
    }
}
