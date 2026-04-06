package org.shda;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PanchangEngine {
    
    // Hardcoded major 2026 Tithis for high accuracy offline
    private static final HashMap<String, String> tithiMap = new HashMap<>();
    
    static {
        // April 2026
        tithiMap.put("07 Apr 2026", "Papmochani Ekadashi");
        tithiMap.put("12 Apr 2026", "Amavasya (Somvati)");
        tithiMap.put("22 Apr 2026", "Kamada Ekadashi");
        tithiMap.put("27 Apr 2026", "Chaitra Purnima / Hanuman Jayanti");
        // May 2026
        tithiMap.put("07 May 2026", "Varuthini Ekadashi");
        tithiMap.put("12 May 2026", "Vaishakha Amavasya");
        tithiMap.put("22 May 2026", "Mohini Ekadashi");
        tithiMap.put("26 May 2026", "Buddha Purnima");
        // June 2026
        tithiMap.put("06 Jun 2026", "Apara Ekadashi");
        tithiMap.put("20 Jun 2026", "Nirjala Ekadashi");
        // July 2026
        tithiMap.put("05 Jul 2026", "Yogini Ekadashi");
        tithiMap.put("20 Jul 2026", "Devshayani Ekadashi");
        // August 2026
        tithiMap.put("04 Aug 2026", "Kamika Ekadashi");
        tithiMap.put("19 Aug 2026", "Shravana Putrada Ekadashi");
        // Add more as needed!
    }

    public static String getTodayTithiAlert() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String today = sdf.format(new Date());
        
        // Check for Tomorrow
        long tomorrowMillis = System.currentTimeMillis() + (1000 * 60 * 60 * 24);
        String tomorrow = sdf.format(new Date(tomorrowMillis));

        if (tithiMap.containsKey(today)) {
            return "🕉️ Today is " + tithiMap.get(today) + "!";
        } else if (tithiMap.containsKey(tomorrow)) {
            return "⏳ Reminder: Tomorrow is " + tithiMap.get(tomorrow) + ".";
        }
        
        return "✨ Shubh Din (Auspicious Day)";
    }
}
