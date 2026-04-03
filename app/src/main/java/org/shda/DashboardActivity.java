package org.shda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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
            Toast.makeText(this, "Utsav & Seva Module unlocking soon...", Toast.LENGTH_SHORT).show()
        );

        // 4. Mass Sandesh 
        findViewById(R.id.cardComms).setOnClickListener(v -> 
            Toast.makeText(this, "WhatsApp Integration unlocking soon...", Toast.LENGTH_SHORT).show()
        );

        // Generate Reports
        findViewById(R.id.btnGenerateReports).setOnClickListener(v -> {
            PdfReportService.generateReport(this);
            Toast.makeText(this, "Generating Sanatani Financial Report...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDynamicShloka() {
        TextView tv = findViewById(R.id.shlokaText);
        
        // Array of profound Sanatani Wisdom
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

        // Get the current day of the year (1 through 365)
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        // Select the Shloka based on the day. Modulo (%) prevents crashing if days exceed array size.
        int shlokaIndex = dayOfYear % shlokas.length;
        
        tv.setText(shlokas[shlokaIndex]);
    }
}
