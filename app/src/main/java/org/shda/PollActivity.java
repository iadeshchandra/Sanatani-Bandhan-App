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
    // ... [Keep your exact existing variables, onCreate, loadPolls, and UI rendering logic from the previous step] ...

    // ✨ CRUD: Admin Options to Edit/Delete
    private void renderPolls() {
        pollsContainer.removeAllViews();
        long now = System.currentTimeMillis();

        for (Poll poll : pollList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_poll, pollsContainer, false);
                // ... [Keep exact existing UI rendering for Status, Votes, Progress Bars] ...

                // ✨ The New CRUD Hook
                if (isManagerOrAdmin) {
                    view.setOnLongClickListener(v -> {
                        showPollManagerDialog(poll);
                        return true;
                    });
                }
                pollsContainer.addView(view);
            } catch (Exception e) {}
        }
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
