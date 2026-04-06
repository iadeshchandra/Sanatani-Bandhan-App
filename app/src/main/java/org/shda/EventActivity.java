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
import java.util.List;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {

    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout eventsContainer;
    private List<Event> eventList = new ArrayList<>();
    private boolean isManagerOrAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event); 

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        eventsContainer = findViewById(R.id.eventsContainer);
        isManagerOrAdmin = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        if (session.getCommunityId() == null) { finish(); return; }

        View btnAddEvent = findViewById(R.id.btnAddEvent);
        if (btnAddEvent != null) {
            if (!isManagerOrAdmin) btnAddEvent.setVisibility(View.GONE);
            else btnAddEvent.setOnClickListener(v -> showAddEventDialog());
        }

        loadEvents();
    }

    private void loadEvents() {
        DatabaseReference eventsRef = db.child("communities").child(session.getCommunityId()).child("events");
        eventsRef.keepSynced(true);
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Event e = data.getValue(Event.class);
                    if (e != null) eventList.add(e);
                }
                Collections.sort(eventList, (a, b) -> Long.compare(a.timestamp, b.timestamp));
                renderEvents();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderEvents() {
        eventsContainer.removeAllViews();
        long now = System.currentTimeMillis();

        for (Event event : eventList) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_event, eventsContainer, false);
            
            ((TextView) view.findViewById(R.id.tvEventTitle)).setText("🪔 " + event.title);
            ((TextView) view.findViewById(R.id.tvEventDate)).setText("When: " + event.dateStr + " at " + event.timeStr);
            ((TextView) view.findViewById(R.id.tvEventLocation)).setText("Where: " + event.location);
            
            // ✨ Live Countdown Logic
            TextView tvCountdown = view.findViewById(R.id.tvEventCountdown); // Add this ID to your XML!
            long diff = event.timestamp - now;
            if (diff > 0) {
                long days = diff / (1000 * 60 * 60 * 24);
                long hours = (diff / (1000 * 60 * 60)) % 24;
                tvCountdown.setText("⏳ Starts in: " + days + " Days, " + hours + " Hours");
                tvCountdown.setTextColor(android.graphics.Color.parseColor("#E65100")); // Saffron
            } else {
                tvCountdown.setText("✅ Event Started / Concluded");
                tvCountdown.setTextColor(android.graphics.Color.parseColor("#388E3C")); // Green
            }

            // Description & Admin Notes
            TextView tvDesc = view.findViewById(R.id.tvEventDescription);
            String fullDesc = event.description;
            if (event.adminComment != null && !event.adminComment.trim().isEmpty()) {
                fullDesc += "\n\n[Management Note]: " + event.adminComment;
            }
            tvDesc.setText(fullDesc);

            // PDF Download
            view.findViewById(R.id.btnDownloadItinerary).setOnClickListener(v -> {
                PdfReportService.generateEventItinerary(this, session.getCommunityName(), event);
            });

            // ✨ Share Button (For everyone)
            view.findViewById(R.id.btnShareEvent).setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareBody = "🙏 Namaskar! Join us for " + event.title + "\n📅 Date: " + event.dateStr + " at " + event.timeStr + "\n📍 Location: " + event.location + "\n\n" + event.description + "\n\n- Shared via Sanatani Bandhan App";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(shareIntent, "Share Event"));
            });

            // ✨ Resend Notification Button (Admins/Managers ONLY, and only if event hasn't passed)
            Button btnResend = view.findViewById(R.id.btnResendNotification);
            if (isManagerOrAdmin && diff > 0) {
                btnResend.setVisibility(View.VISIBLE);
                btnResend.setText("🔔 Resend Alert (" + event.notificationCount + " sent)");
                btnResend.setOnClickListener(v -> {
                    notifyAllMembers(event.title, event.dateStr, event.timeStr);
                    int newCount = event.notificationCount + 1;
                    db.child("communities").child(session.getCommunityId()).child("events").child(event.id).child("notificationCount").setValue(newCount);
                    Toast.makeText(this, "Reminder Alert Sent to All Members!", Toast.LENGTH_SHORT).show();
                });
            } else {
                btnResend.setVisibility(View.GONE);
            }
            
            eventsContainer.addView(view);
        }
    }

    // ... (Keep your showAddEventDialog method exactly as I provided in the previous message, 
    // but ensure when creating 'newEvent', pass '1' for the notificationCount parameter).

    private void notifyAllMembers(String eventTitle, String dateStr, String timeStr) {
        DatabaseReference membersRef = db.child("communities").child(session.getCommunityId()).child("members");
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                long ts = System.currentTimeMillis();
                String message = "Reminder: " + eventTitle + " is scheduled on " + dateStr + " at " + timeStr;
                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    String memberId = memberSnap.getKey();
                    if (memberId != null) {
                        String notifId = db.child("communities").child(session.getCommunityId()).child("notifications").child(memberId).push().getKey();
                        AppNotification notif = new AppNotification(notifId, "🛕 Mandir Event Reminder", message, ts, "EVENT");
                        db.child("communities").child(session.getCommunityId()).child("notifications").child(memberId).child(notifId).setValue(notif);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static class Event {
        public String id, title, dateStr, timeStr, location, description, adminComment, createdBy;
        public long timestamp;
        public int notificationCount = 1; // Tracks how many times alert was sent
        public Event() {}
        public Event(String id, String t, String dStr, String timeStr, String loc, String desc, String comment, String creator, long ts, int notifCount) {
            this.id = id; this.title = t; this.dateStr = dStr; this.timeStr = timeStr; this.location = loc; this.description = desc; this.adminComment = comment; this.createdBy = creator; this.timestamp = ts; this.notificationCount = notifCount;
        }
    }
}
