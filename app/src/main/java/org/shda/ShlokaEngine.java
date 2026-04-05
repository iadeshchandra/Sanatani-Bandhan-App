package org.shda;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShlokaEngine {
    
    private static final List<String> uiShlokaList = new ArrayList<>();
    private static final List<String> pdfShlokaList = new ArrayList<>();
    private static final Random random = new Random();
    private static boolean isLoaded = false;

    private static final String FALLBACK_UI = "\"Dharmo Rakshati Rakshitah\"\n(Dharma protects those who protect it)\n— Manu Smriti 8.15\n\n\"ধর্মো রক্ষতি রক্ষিতঃ\"\n(যে ধর্মকে রক্ষা করে, ধর্মও তাকে রক্ষা করে)";
    private static final String FALLBACK_PDF = "\"Dharmo Rakshati Rakshitah\"\n(Dharma protects those who protect it)\n— Manu Smriti 8.15";

    private static void init(Context context) {
        if (isLoaded) return;
        try {
            InputStream is = context.getAssets().open("shlokas.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject rootObj = new JSONObject(jsonStr);
            JSONArray jsonArray = rootObj.getJSONArray("shlokas");
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String source = obj.optString("source", "Vedic Wisdom");
                String original = obj.optString("original", "");
                
                JSONObject enObj = obj.getJSONObject("en");
                String enQuote = enObj.optString("quote", "");
                
                JSONObject bnObj = obj.getJSONObject("bn");
                String bnQuote = bnObj.optString("quote", "");
                String bnMeaning = bnObj.optString("meaning", "");

                // 1. Build UI Shloka (Bilingual)
                StringBuilder uiSb = new StringBuilder();
                if (!original.isEmpty()) { uiSb.append("\"").append(original).append("\"\n(").append(enQuote).append(")\n"); } 
                else { uiSb.append("\"").append(enQuote).append("\"\n"); }
                uiSb.append("— ").append(source).append("\n\n\"").append(bnQuote).append("\"\n");
                if (!bnMeaning.isEmpty()) uiSb.append("(").append(bnMeaning).append(")");
                uiShlokaList.add(uiSb.toString());

                // 2. Build PDF-Safe Shloka (English/Sanskrit Only to avoid font crash)
                StringBuilder pdfSb = new StringBuilder();
                if (!original.isEmpty()) { pdfSb.append("\"").append(original).append("\"\n(").append(enQuote).append(")\n"); } 
                else { pdfSb.append("\"").append(enQuote).append("\"\n"); }
                pdfSb.append("— ").append(source);
                pdfShlokaList.add(pdfSb.toString());
            }
            isLoaded = true;
        } catch (Exception e) {}
    }

    public static String getRandomShloka(Context context) {
        if (!isLoaded) init(context);
        return uiShlokaList.isEmpty() ? FALLBACK_UI : uiShlokaList.get(random.nextInt(uiShlokaList.size()));
    }

    // ✨ NEW: Call this specifically for PDFs!
    public static String getPdfSafeShloka(Context context) {
        if (!isLoaded) init(context);
        return pdfShlokaList.isEmpty() ? FALLBACK_PDF : pdfShlokaList.get(random.nextInt(pdfShlokaList.size()));
    }
}
