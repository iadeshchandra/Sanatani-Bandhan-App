package org.shda;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
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
    private boolean isAdminOrManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        eventsContainer = findViewById(R.id.eventsContainer);

        if (session.getCommunityId() == null) { finish(); return; }

        isAdminOrManager = "ADMIN".equals(session.getRole()) || "MANAGER".equals(session.getRole());

        Button btnAddEvent = findViewById(R.id.btnAddEvent);
        if (!isAdminOrManager) btnAddEvent.setVisibility(View.GONE);
        else btnAddEvent.setOnClickListener(v -> showEventDialog());

        loadEvents();
    }

    private void loadEvents() {
        db.child("communities").child(session.getCommunityId()).child("events").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventsContainer.removeAllViews();
                List<Event> eventList = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Event e = data.getValue(Event.class);
                    if (e != null) eventList.add(e);
                }
                
                // Sort by closest upcoming date first
                Collections.sort(eventList, (a, b) -> Long.compare(a.eventDateTs, b.eventDateTs));

                for (Event e : eventList) {
                    renderEventCard(e);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderEventCard(Event event) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24); card.setLayoutParams(params);
        card.setCardElevation(4f); card.setRadius(16f);

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this); tvTitle.setText("📅 " + event.title); tvTitle.setTextSize(18f); tvTitle.setTextColor(android.graphics.Color.parseColor("#7B1FA2")); tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView tvDate = new TextView(this); tvDate.setText("When: " + event.dateStr); tvDate.setTextSize(14f); tvDate.setPadding(0, 10, 0, 0);
        TextView tvLoc = new TextView(this); tvLoc.setText("Where: " + event.location); tvLoc.setTextSize(14f);
        TextView tvDesc = new TextView(this); tvDesc.setText(event.description); tvDesc.setPadding(0, 15, 0, 20);

        Button btnExport = new Button(this); btnExport.setText("Download Itinerary PDF"); btnExport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B1FA2")));
        btnExport.setOnClickListener(v -> PdfReportService.generateEventItinerary(this, session.getCommunityName(), event));

        layout.addView(tvTitle); layout.addView(tvDate); layout.addView(tvLoc); layout.addView(tvDesc); layout.addView(btnExport);
        card.addView(layout); eventsContainer.addView(card);
    }

    private void showEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Schedule Mandir Event");

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 20, 50, 0);

        final EditText inputTitle = new EditText(this); inputTitle.setHint("Event Title (e.g. Committee Meeting)"); layout.addView(inputTitle);
        final EditText inputLocation = new EditText(this); inputLocation.setHint("Location (e.g. Main Hall)"); layout.addView(inputLocation);
        final EditText inputDesc = new EditText(this); inputDesc.setHint("Agenda / Description"); layout.addView(inputDesc);

        final long[] selectedTs = {System.currentTimeMillis()};
        final String[] dateStr = {""};
        Button btnDate = new Button(this); btnDate.setText("Pick Date");
        btnDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                cal.set(y, m, d); selectedTs[0] = cal.getTimeInMillis();
                dateStr[0] = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
                btnDate.setText(dateStr[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(btnDate);

        builder.setView(layout);
        builder.setPositiveButton("PUBLISH", (dialog, which) -> {
            String title = inputTitle.getText().toString().trim();
            if (title.isEmpty() || dateStr[0].isEmpty()) { Toast.makeText(this, "Title and Date required", Toast.LENGTH_SHORT).show(); return; }

            String eventId = db.child("communities").child(session.getCommunityId()).child("events").push().getKey();
            Event newEvent = new Event(eventId, title, dateStr[0], selectedTs[0], inputDesc.getText().toString(), inputLocation.getText().toString(), System.currentTimeMillis(), session.getUserName());
            
            db.child("communities").child(session.getCommunityId()).child("events").child(eventId).setValue(newEvent);
            Toast.makeText(this, "Event Scheduled!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("CANCEL", null); builder.show();
    }
}
