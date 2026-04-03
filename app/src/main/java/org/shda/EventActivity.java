package org.shda;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.*;

public class EventActivity extends AppCompatActivity {
    private DatabaseReference db;
    private SessionManager session;
    private LinearLayout eventsContainer;
    private EditText inputTitle, inputDate, inputDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        db = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        
        if (session.getCommunityId() == null) {
            finish();
            return;
        }

        eventsContainer = findViewById(R.id.eventsContainer);
        inputTitle = findViewById(R.id.inputEventTitle);
        inputDate = findViewById(R.id.inputEventDate);
        inputDesc = findViewById(R.id.inputEventDesc);
        Button btnSave = findViewById(R.id.btnSaveEvent);

        loadEvents();

        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void saveEvent() {
        String title = inputTitle.getText().toString().trim();
        String date = inputDate.getText().toString().trim();
        String desc = inputDesc.getText().toString().trim();

        if (title.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Title and Date are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String commId = session.getCommunityId();
        String eventId = db.child("communities").child(commId).child("events").push().getKey();
        Event newEvent = new Event(eventId, title, date, desc, System.currentTimeMillis());

        if (eventId != null) {
            db.child("communities").child(commId).child("events").child(eventId).setValue(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Utsav Scheduled!", Toast.LENGTH_SHORT).show();
                    inputTitle.setText("");
                    inputDate.setText("");
                    inputDesc.setText("");
                });
        }
    }

    private void loadEvents() {
        db.child("communities").child(session.getCommunityId()).child("events").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventsContainer.removeAllViews();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Event event = data.getValue(Event.class);
                    if (event != null) {
                        addEventCard(event);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addEventCard(Event event) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setCardElevation(4f);
        card.setRadius(16f);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🪔 " + event.title);
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(Color.parseColor("#C2185B"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);
        tvDate.setText("Date: " + event.date);
        tvDate.setTextColor(Color.DKGRAY);
        tvDate.setPadding(0, 8, 0, 8);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(event.description);
        tvDesc.setTextColor(Color.GRAY);

        layout.addView(tvTitle);
        layout.addView(tvDate);
        layout.addView(tvDesc);
        card.addView(layout);
        
        eventsContainer.addView(card, 0); 
    }
}
