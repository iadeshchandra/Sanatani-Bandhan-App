package org.shda;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PanchangEngine {
    
    // Official 2026 ISKCON (Rangpur, BD) Ekadashi List
    private static final HashMap<String, String> tithiMap = new HashMap<>();
    
    static {
        // January
        tithiMap.put("14 Jan 2026", "Shattila Ekadashi (ষটতিলা একাদশী)");
        tithiMap.put("29 Jan 2026", "Bhaimi Ekadashi (ভৈমী একাদশী)");
        // February
        tithiMap.put("13 Feb 2026", "Vijaya Ekadashi (বিজয়া একাদশী)");
        tithiMap.put("27 Feb 2026", "Amalaki Vrata Ekadashi (আমলকীব্রত একাদশী)");
        // March
        tithiMap.put("15 Mar 2026", "Papmochani Ekadashi (পাপমোচনী একাদশী)");
        tithiMap.put("29 Mar 2026", "Kamada Ekadashi (কামদা একাদশী)");
        // April
        tithiMap.put("13 Apr 2026", "Varuthini Ekadashi (বরুথিনী একাদশী)");
        tithiMap.put("27 Apr 2026", "Mohini Ekadashi (মোহিনী একাদশী)");
        // May
        tithiMap.put("13 May 2026", "Apara Ekadashi (অপরা একাদশী)");
        tithiMap.put("27 May 2026", "Padmini Ekadashi (পদ্মিনী একাদশী)");
        // June
        tithiMap.put("11 Jun 2026", "Parama Ekadashi (পরমা একাদশী)");
        tithiMap.put("26 Jun 2026", "Pandava Nirjala Ekadashi (পাণ্ডবনির্জালা একাদশী)");
        // July
        tithiMap.put("11 Jul 2026", "Yogini Ekadashi (যোগিনী একাদশী)");
        tithiMap.put("25 Jul 2026", "Shayan Ekadashi (শয়ন একাদশী)");
        // August
        tithiMap.put("09 Aug 2026", "Kamika Ekadashi (কামিকা একাদশী)");
        tithiMap.put("24 Aug 2026", "Pavitraropana Ekadashi (পবিত্রারোপণ একাদশী)");
        // September
        tithiMap.put("07 Sep 2026", "Annada Ekadashi (অন্নদা একাদশী)");
        tithiMap.put("22 Sep 2026", "Parshva Ekadashi (পার্শ্ব একাদশী)");
        // October
        tithiMap.put("06 Oct 2026", "Indira Ekadashi (ইন্দিরা একাদশী)");
        tithiMap.put("22 Oct 2026", "Pashankusha Ekadashi (পাশাঙ্কুশা একাদশী)");
        // November
        tithiMap.put("05 Nov 2026", "Rama Ekadashi (রমা একাদশী)");
        tithiMap.put("21 Nov 2026", "Utthana Ekadashi (উত্থান একাদশী)");
        // December
        tithiMap.put("05 Dec 2026", "Utpanna Ekadashi (উত্পন্না একাদশী)");
        tithiMap.put("20 Dec 2026", "Mokshada Ekadashi (মোক্ষদা একাদশী)");
    }

    public static String getTodayTithiAlert() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String today = sdf.format(new Date());
        
        // Check for Tomorrow (Adds 24 hours in milliseconds)
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
