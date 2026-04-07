package org.shda;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {
    // ... [Keep existing variables and loadEvents logic] ...

    private void renderEvents() {
        eventsContainer.removeAllViews();
        long now = System.currentTimeMillis();

        for (Event event : eventList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_event, eventsContainer, false);
                // ... [Keep existing UI logic for countdowns, descriptions, buttons] ...

                // ✨ CRUD Hook
                if (isManagerOrAdmin) {
                    view.setOnLongClickListener(v -> {
                        showEventManagerDialog(event);
                        return true;
                    });
                }
                eventsContainer.addView(view);
            } catch (Exception e) {}
        }
    }

    private void showEventManagerDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Event").setMessage("Event: " + event.title);

        builder.setPositiveButton("EDIT NOTES", (d, w) -> {
            AlertDialog.Builder editBuilder = new AlertDialog.Builder(this);
            final EditText inputNote = new EditText(this); inputNote.setText(event.adminComment);
            editBuilder.setView(inputNote).setPositiveButton("SAVE", (d2, w2) -> {
                String newNote = inputNote.getText().toString();
                db.child("communities").child(session.getCommunityId()).child("events").child(event.id).child("adminComment").setValue(newNote);
                logAudit("EVENT_EDITED", "Updated notes for event: " + event.title);
                Toast.makeText(this, "Event Updated", Toast.LENGTH_SHORT).show();
            }).show();
        });

        builder.setNegativeButton("DELETE EVENT", (d, w) -> {
            new AlertDialog.Builder(this).setTitle("Confirm Deletion").setMessage("Erase this scheduled event?")
                .setPositiveButton("YES", (d2, w2) -> {
                    db.child("communities").child(session.getCommunityId()).child("events").child(event.id).removeValue();
                    logAudit("EVENT_DELETED", "Deleted event: " + event.title);
                    Toast.makeText(this, "Event Erased", Toast.LENGTH_SHORT).show();
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
