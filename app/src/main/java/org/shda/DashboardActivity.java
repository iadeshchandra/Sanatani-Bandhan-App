package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseDatabase.getInstance().getReference();
        setupDynamicShloka();

        // 1. Members Directory
        findViewById(R.id.cardMembers).setOnClickListener(v -> 
            startActivity(new Intent(this, MemberActivity.class))
        );

        // 2. Smart Chanda
        findViewById(R.id.cardDonations).setOnClickListener(v -> 
            startActivity(new Intent(this, TransactionActivity.class))
        );

        // 3. Utsav & Seva
        findViewById(R.id.cardEvents).setOnClickListener(v -> 
            startActivity(new Intent(this, EventActivity.class))
        );

        // 4. Mass Sandesh (Successfully Linked!)
        findViewById(R.id.cardComms).setOnClickListener(v -> 
            startActivity(new Intent(this, CommsActivity.class))
        );

        // The Smart PDF Generator Logic
        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> {
            Toast.makeText(this, "Fetching data & Generating Report...", Toast.LENGTH_SHORT).show();
            
            db.child("logs").child("Donation").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> names = new ArrayList<>();
                    List<Float> amounts = new ArrayList<>();
                    List<String> notes = new ArrayList<>();
                    List<String> dates = new ArrayList<>();
                    float total = 0f;
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

                    for (DataSnapshot data : snapshot.getChildren()) {
                        String name = data.child("name").getValue(String.class);
                        Float amt = data.child("amount").getValue(Float.class);
                        String note = data.child("note").getValue(String.class);
                        Long ts = data.child("timestamp").getValue(Long.class);
                        
                        if(name != null && amt != null) {
                            names.add(name);
                            amounts.add(amt);
                            notes.add(note != null ? note : "No note provided");
                            dates.add(ts != null ? sdf.format(new Date(ts)) : "Unknown Date");
                            total += amt;
                        }
                    }
                    
                    // Hand the data over to the PDF Engine
                    PdfReportService.generateFinancialReport(DashboardActivity.this, dates, names, amounts, notes, total);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DashboardActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupDynamicShloka() {
        TextView tv = findViewById(R.id.shlokaText);
        String[] shlokas = {
            "“You have the right to work, but for the work's sake only. You have no right to the fruits of work.”\n\n- Bhagavad Gita 2.47",
            "“Whenever dharma declines and the purpose of life is forgotten, I manifest myself on earth.”\n\n- Bhagavad Gita 4.7",
            "“A person can rise through the efforts of his own mind; or draw himself down, in the same manner. Because each person is his own friend or enemy.”\n\n- Bhagavad Gita 6.5",
            "“The soul is neither born, and nor does it die.”\n\n- Bhagavad Gita 2.20",
            "“Truth is one, paths are many.”\n\n- Rig Veda",
            "“There is nothing lost or wasted in this life.”\n\n- Bhagavad Gita 2.40",
            "“Let noble thoughts come to us from every side.”\n\n- Rig Veda 1.89.1",
            "“He who has no attachments can really love others, for his love is pure and divine.”\n\n- Swami Vivekananda",
            "“Arise, awake, and stop not till the goal is reached.”\n\n- Katha Upanishad"
        };
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        tv.setText(shlokas[dayOfYear % shlokas.length]);
    }
}
