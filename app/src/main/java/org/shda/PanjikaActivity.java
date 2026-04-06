package org.shda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PanjikaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panjika);

        TextView tvEnglishDate = findViewById(R.id.tvEnglishDate);
        TextView tvBengaliDate = findViewById(R.id.tvBengaliDate);
        TextView tvTodayTithi = findViewById(R.id.tvTodayTithi);
        LinearLayout container = findViewById(R.id.panjikaListContainer);

        // 1. Set English Date
        tvEnglishDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()));

        // 2. Set Tithi Alert from your existing engine
        tvTodayTithi.setText(PanchangEngine.getTodayTithiAlert());

        // 3. Calculate and Set Bengali Date (BD Standard)
        tvBengaliDate.setText(calculateBengaliDate(Calendar.getInstance()));

        // 4. Load the Full Year Ekadashi List
        loadUpcomingVratas(container);
    }

    private String calculateBengaliDate(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1; // 1-12
        int year = cal.get(Calendar.YEAR);

        int bYear = year - 593;
        String bMonth = "";
        int bDay = 0;

        // BD Standard Algorithm (Mid-month shifts)
        if (month == 4) { if (day >= 14) { bMonth = "বৈশাখ (Boishakh)"; bDay = day - 13; } else { bMonth = "চৈত্র (Chaitra)"; bDay = day + 17; bYear--; } }
        else if (month == 5) { if (day >= 15) { bMonth = "জ্যৈষ্ঠ (Joishtho)"; bDay = day - 14; } else { bMonth = "বৈশাখ (Boishakh)"; bDay = day + 17; } }
        else if (month == 6) { if (day >= 15) { bMonth = "আষাঢ় (Ashar)"; bDay = day - 14; } else { bMonth = "জ্যৈষ্ঠ (Joishtho)"; bDay = day + 17; } }
        else if (month == 7) { if (day >= 16) { bMonth = "শ্রাবণ (Shrabon)"; bDay = day - 15; } else { bMonth = "আষাঢ় (Ashar)"; bDay = day + 16; } }
        else if (month == 8) { if (day >= 16) { bMonth = "ভাদ্র (Bhadro)"; bDay = day - 15; } else { bMonth = "শ্রাবণ (Shrabon)"; bDay = day + 16; } }
        else if (month == 9) { if (day >= 16) { bMonth = "আশ্বিন (Ashwin)"; bDay = day - 15; } else { bMonth = "ভাদ্র (Bhadro)"; bDay = day + 16; } }
        else if (month == 10) { if (day >= 16) { bMonth = "কার্তিক (Kartik)"; bDay = day - 15; } else { bMonth = "আশ্বিন (Ashwin)"; bDay = day + 15; } }
        else if (month == 11) { if (day >= 15) { bMonth = "অগ্রহায়ণ (Agrahayon)"; bDay = day - 14; } else { bMonth = "কার্তিক (Kartik)"; bDay = day + 16; } }
        else if (month == 12) { if (day >= 15) { bMonth = "পৌষ (Poush)"; bDay = day - 14; } else { bMonth = "অগ্রহায়ণ (Agrahayon)"; bDay = day + 16; } }
        else if (month == 1) { if (day >= 14) { bMonth = "মাঘ (Magh)"; bDay = day - 13; bYear--; } else { bMonth = "পৌষ (Poush)"; bDay = day + 17; bYear--; } }
        else if (month == 2) { if (day >= 13) { bMonth = "ফাল্গুন (Falgun)"; bDay = day - 12; bYear--; } else { bMonth = "মাঘ (Magh)"; bDay = day + 18; bYear--; } }
        else if (month == 3) { if (day >= 15) { bMonth = "চৈত্র (Chaitra)"; bDay = day - 14; bYear--; } else { bMonth = "ফাল্গুন (Falgun)"; bDay = day + 16; bYear--; } }

        return bDay + " " + bMonth + " " + bYear + " বঙ্গাব্দ";
    }

    private void loadUpcomingVratas(LinearLayout container) {
        // Mock list replicating your PanchangEngine data for the UI
        String[] dates = {"14 Jan", "29 Jan", "13 Feb", "27 Feb", "15 Mar", "29 Mar", "13 Apr", "27 Apr"};
        String[] names = {"Shattila Ekadashi", "Bhaimi Ekadashi", "Vijaya Ekadashi", "Amalaki Vrata", "Papmochani", "Kamada", "Varuthini", "Mohini"};

        for (int i = 0; i < dates.length; i++) {
            TextView tv = new TextView(this);
            tv.setText("🕉️ " + dates[i] + " - " + names[i]);
            tv.setTextSize(16f);
            tv.setPadding(0, 16, 0, 16);
            tv.setTextColor(android.graphics.Color.parseColor("#424242"));
            container.addView(tv);
            
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
            container.addView(divider);
        }
    }
}
