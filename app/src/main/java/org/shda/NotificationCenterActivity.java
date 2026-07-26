package org.shda;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCenterActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<AppNotification> notificationList = new ArrayList<>();
    private DatabaseReference globalRef, communityRef;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        session = new SessionManager(this);
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        // Update the 'Last Read' timestamp so the dashboard badge resets to 0
        SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        prefs.edit().putLong("last_read_ts", System.currentTimeMillis()).apply();

        // Initialize Firebase References
        globalRef = FirebaseDatabase.getInstance().getReference("global_notifications");
        communityRef = FirebaseDatabase.getInstance().getReference("communities").child(session.getCommunityId()).child("notifications");

        // ✨ OFFLINE CAPABILITY: Keep synced locally!
        globalRef.keepSynced(true);
        communityRef.keepSynced(true);

        fetchNotifications();
    }

    private void fetchNotifications() {
        // Fetch Global (App) Notifications
        globalRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                removeOldTypes("GLOBAL");
                for (DataSnapshot d : snapshot.getChildren()) {
                    AppNotification notif = parseSnapshot(d, "GLOBAL");
                    if (notif != null) notificationList.add(notif);
                }
                sortAndRefresh();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fetch Local (Mandir) Notifications
        communityRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                removeOldTypes("MANDIR");
                for (DataSnapshot d : snapshot.getChildren()) {
                    AppNotification notif = parseSnapshot(d, "MANDIR");
                    if (notif != null) notificationList.add(notif);
                }
                sortAndRefresh();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private AppNotification parseSnapshot(DataSnapshot d, String type) {
        String title = d.child("title").getValue(String.class);
        String message = d.child("message").getValue(String.class);
        Long timestamp = d.child("timestamp").getValue(Long.class);
        if (title != null && message != null && timestamp != null) {
            return new AppNotification(title, message, timestamp, type);
        }
        return null;
    }

    private void removeOldTypes(String type) {
        // Remove existing items of this type to avoid duplicates on data change
        for (int i = notificationList.size() - 1; i >= 0; i--) {
            if (notificationList.get(i).type.equals(type)) {
                notificationList.remove(i);
            }
        }
    }

    private void sortAndRefresh() {
        // Sort newest first
        Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
        adapter.notifyDataSetChanged();
    }

    // --- INNER CLASSES (Model & Adapter) ---

    public static class AppNotification {
        public String title, message, type;
        public long timestamp;

        public AppNotification(String title, String message, long timestamp, String type) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private List<AppNotification> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

        public NotificationAdapter(List<AppNotification> list) {
            this.list = list;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppNotification notif = list.get(position);
            holder.tvNotifTitle.setText(notif.title);
            holder.tvNotifMessage.setText(notif.message);
            holder.tvNotifTime.setText(sdf.format(new Date(notif.timestamp)));

            // Visual Signs based on source
            if ("GLOBAL".equals(notif.type)) {
                holder.tvSourceBadge.setText("APP");
                holder.tvSourceBadge.setBackgroundColor(Color.parseColor("#E65100")); // Orange
            } else {
                holder.tvSourceBadge.setText("MANDIR");
                holder.tvSourceBadge.setBackgroundColor(Color.parseColor("#2E7D32")); // Green
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNotifTitle, tvNotifMessage, tvNotifTime, tvSourceBadge;
            ViewHolder(View itemView) {
                super(itemView);
                tvNotifTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvNotifMessage = itemView.findViewById(R.id.tvNotifMessage);
                tvNotifTime = itemView.findViewById(R.id.tvNotifTime);
                tvSourceBadge = itemView.findViewById(R.id.tvSourceBadge);
            }
        }
    }
}
