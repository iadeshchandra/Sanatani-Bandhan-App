package org.shda;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout eventsContainer;
    private EditText inputSearch;
    
    private List<Event> fullEventList = new ArrayList<>();
    private List<Event> currentlyDisplayedList = new ArrayList<>();
    private boolean isManagerOrAdmin;
    
    private Long filterStartTs = null;
    private Long filterEndTs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event); 

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        eventsContainer = findViewById(R.id.eventsContainer);
        inputSearch = findViewById(R.id.inputSearch);
        isManagerOrAdmin = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        if (session.getCommunityId() == null) { finish(); return; }

        View btnAddEvent = findViewById(R.id.btnAddEvent);
        View btnExportMaster = findViewById(R.id.btnExportMaster);
        View btnFilterDates = findViewById(R.id.btnFilterDates);

        if (btnAddEvent != null) {
            if (!isManagerOrAdmin) btnAddEvent.setVisibility(View.GONE);
            else btnAddEvent.setOnClickListener(v -> showAddEventDialog());
        }

        if (btnFilterDates != null) {
            btnFilterDates.setOnClickListener(v -> showGlobalDateFilterDialog());
        }

        if (btnExportMaster != null) {
            btnExportMaster.setOnClickListener(v -> {
                if (currentlyDisplayedList.isEmpty()) { Toast.makeText(this, "No events to export", Toast.LENGTH_SHORT).show(); return; }
                // ✨ FIX: Triggers the Master PDF, respecting the Date Filters!
                String title = filterStartTs != null ? "Filtered Event Calendar" : "Master Event Calendar";
                try {
                    PdfReportService.generateMasterEventReport(this, session.getCommunityName(), currentlyDisplayedList, title);
                } catch (Exception e) {
                    Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
                }
            });
        }

        setupSearch();
        loadEvents();
    }

    private void loadEvents() {
        DatabaseReference eventsRef = db.child("communities").child(session.getCommunityId()).child("events");
        eventsRef.keepSynced(true);
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullEventList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Event e = data.getValue(Event.class);
                    if (e != null) fullEventList.add(e);
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        currentlyDisplayedList.clear();
        String query = inputSearch != null ? inputSearch.getText().toString().toLowerCase().trim() : "";

        for (Event e : fullEventList) {
            boolean matchesSearch = query.isEmpty() || (e.title != null && e.title.toLowerCase().contains(query)) || (e.location != null && e.location.toLowerCase().contains(query));
            boolean matchesDate = true;
            
            if (filterStartTs != null && filterEndTs != null) {
                matchesDate = (e.timestamp >= filterStartTs && e.timestamp <= filterEndTs);
            }
            
            if (matchesSearch && matchesDate) currentlyDisplayedList.add(e);
        }
        Collections.sort(currentlyDisplayedList, (a, b) -> Long.compare(a.timestamp, b.timestamp));
        renderEvents();
    }

    private void setupSearch() {
        if (inputSearch == null) return;
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showGlobalDateFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Events by Date");
        builder.setItems(new String[]{"Select Specific Date Range", "Clear Filters (Show All Time)"}, (dialog, which) -> {
            if (which == 0) {
                pickDateRange((startTs, endTs) -> {
                    filterStartTs = startTs; filterEndTs = endTs; applyFilters();
                    Toast.makeText(this, "Dates Filtered Successfully", Toast.LENGTH_SHORT).show();
                });
            } else {
                filterStartTs = null; filterEndTs = null; applyFilters();
            }
        });
        builder.show();
    }

    private void pickDateRange(DateRangeCallback callback) {
        final Calendar startCal = Calendar.getInstance();
        new DatePickerDialog(this, (view1, y1, m1, d1) -> {
            startCal.set(y1, m1, d1, 0, 0, 0);
            final Calendar endCal = Calendar.getInstance();
            new DatePickerDialog(this, (view2, y2, m2, d2) -> {
                endCal.set(y2, m2, d2, 23, 59, 59);
                if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                else callback.onSelected(startCal.getTimeInMillis(), endCal.getTimeInMillis());
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            Toast.makeText(this, "Now select End Date", Toast.LENGTH_SHORT).show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }
    private interface DateRangeCallback { void onSelected(long start, long end); }

    private void renderEvents() {
        eventsContainer.removeAllViews();
        long now = System.currentTimeMillis();

        for (Event event : currentlyDisplayedList) {
            try {
                View view = LayoutInflater.from(this).inflate(R.layout.item_event, eventsContainer, false);
                
                ((TextView) view.findViewById(R.id.tvEventTitle)).setText("🪔 " + event.title);
                ((TextView) view.findViewById(R.id.tvEventDate)).setText("When: " + event.dateStr + " at " + event.timeStr);
                ((TextView) view.findViewById(R.id.tvEventLocation)).setText("Where: " + event.location);
                
                TextView tvCountdown = view.findViewById(R.id.tvEventCountdown);
                long diff = event.timestamp - now;
                if (diff > 0) {
                    long days = diff / (1000 * 60 * 60 * 24);
                    long hours = (diff / (1000 * 60 * 60)) % 24;
                    tvCountdown.setText("⏳ Starts in: " + days + " Days, " + hours + " Hours");
                    tvCountdown.setTextColor(android.graphics.Color.parseColor("#E65100")); 
                } else {
                    tvCountdown.setText("✅ Event Started / Concluded");
                    tvCountdown.setTextColor(android.graphics.Color.parseColor("#388E3C")); 
                }

                String fullDesc = event.description;
                if (event.adminComment != null && !event.adminComment.trim().isEmpty()) {
                    fullDesc += "\n\n[Management Note]: " + event.adminComment;
                }
                ((TextView) view.findViewById(R.id.tvEventDescription)).setText(fullDesc);

                view.findViewById(R.id.btnDownloadItinerary).setOnClickListener(v -> {
                    PdfReportService.generateEventItinerary(this, session.getCommunityName(), event, session.getUserName());
                });

                view.findViewById(R.id.btnShareEvent).setOnClickListener(v -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    String shareBody = "🙏 Namaskar! Join us for " + event.title + "\n📅 Date: " + event.dateStr + " at " + event.timeStr + "\n📍 Location: " + event.location + "\n\n" + event.description + "\n\n- Shared via Sanatani Bandhan";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(shareIntent, "Share Event"));
                });

                Button btnResend = view.findViewById(R.id.btnResendNotification);
                if (isManagerOrAdmin && diff > 0) {
                    btnResend.setVisibility(View.VISIBLE);
                    btnResend.setText("🔔 RESEND (" + event.notificationCount + ")");
                    btnResend.setOnClickListener(v -> {
                        notifyAllMembers(event.title, event.dateStr, event.timeStr);
                        int newCount = event.notificationCount + 1;
                        db.child("communities").child(session.getCommunityId()).child("events").child(event.id).child("notificationCount").setValue(newCount);
                        Toast.makeText(this, "Alert Sent to All Members!", Toast.LENGTH_SHORT).show();
                    });
                }

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

    private void showAddEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Schedule Mandir Event");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        final EditText inputTitle = new EditText(this); inputTitle.setHint("Event / Puja Name");
        final EditText inputLocation = new EditText(this); inputLocation.setHint("Location (e.g., Haritola Mandir)");
        final EditText inputDesc = new EditText(this); inputDesc.setHint("About the Event");
        final EditText inputAdminComment = new EditText(this); inputAdminComment.setHint("Admin Note (Optional)");
        
        final Button btnSelectDateTime = new Button(this);
        btnSelectDateTime.setText("Select Date & Time");
        
        final Calendar calendar = Calendar.getInstance();
        final String[] selectedDateStr = {""};
        final String[] selectedTimeStr = {""};

        btnSelectDateTime.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, day);
                selectedDateStr[0] = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(calendar.getTime());
                
                new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); calendar.set(Calendar.MINUTE, minute);
                    selectedTimeStr[0] = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
                    btnSelectDateTime.setText(selectedDateStr[0] + " at " + selectedTimeStr[0]);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
                
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        layout.addView(inputTitle); layout.addView(inputLocation); layout.addView(inputDesc); layout.addView(inputAdminComment); layout.addView(btnSelectDateTime);
        builder.setView(layout);

        builder.setPositiveButton("SCHEDULE", null); 
        builder.setNegativeButton("CANCEL", null); 
        
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = inputTitle.getText().toString().trim();
            if (title.isEmpty() || selectedDateStr[0].isEmpty()) {
                Toast.makeText(this, "Title and Date/Time are required", Toast.LENGTH_SHORT).show(); return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Saving...");

            String eventId = db.child("communities").child(session.getCommunityId()).child("events").push().getKey();
            Event newEvent = new Event(eventId, title, selectedDateStr[0], selectedTimeStr[0], inputLocation.getText().toString(), inputDesc.getText().toString(), inputAdminComment.getText().toString(), session.getUserName(), calendar.getTimeInMillis(), 1);
            
            db.child("communities").child(session.getCommunityId()).child("events").child(eventId).setValue(newEvent);

            notifyAllMembers(title, selectedDateStr[0], selectedTimeStr[0]);
            Toast.makeText(this, "Event Scheduled!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
    }

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
                        AppNotification notif = new AppNotification(notifId, "🛕 Mandir Event Alert", message, ts, "EVENT");
                        db.child("communities").child(session.getCommunityId()).child("notifications").child(memberId).child(notifId).setValue(notif);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    public static class Event {
        public String id, title, dateStr, timeStr, location, description, adminComment, createdBy;
        public long timestamp; public int notificationCount = 1;
        public Event() {}
        public Event(String id, String t, String dStr, String timeStr, String loc, String desc, String comment, String creator, long ts, int notifCount) {
            this.id=id; this.title=t; this.dateStr=dStr; this.timeStr=timeStr; this.location=loc; this.description=desc; this.adminComment=comment; this.createdBy=creator; this.timestamp=ts; this.notificationCount=notifCount;
        }
    }
}
