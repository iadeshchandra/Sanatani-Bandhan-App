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
    
    private static final List<String> shlokaList = new ArrayList<>();
    private static final Random random = new Random();
    private static boolean isLoaded = false;

    // 🛡️ THE BULLETPROOF FALLBACK
    private static final String FALLBACK_SHLOKA = "\"Dharmo Rakshati Rakshitah\"\n(Dharma protects those who protect it)\n— Manu Smriti 8.15\n\n\"ধর্মো রক্ষতি রক্ষিতঃ\"\n(যে ধর্মকে রক্ষা করে, ধর্মও তাকে রক্ষা করে)";

    // Loads and parses the advanced JSON structure into memory
    private static void init(Context context) {
        if (isLoaded) return;
        try {
            InputStream is = context.getAssets().open("shlokas.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            
            // ✨ NEW: Read the root object, then grab the "shlokas" array
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

                StringBuilder sb = new StringBuilder();
                
                // 1. Format English / Sanskrit part
                if (!original.isEmpty()) {
                    sb.append("\"").append(original).append("\"\n");
                    sb.append("(").append(enQuote).append(")\n");
                } else {
                    sb.append("\"").append(enQuote).append("\"\n");
                }
                sb.append("— ").append(source).append("\n\n");
                
                // 2. Format Bengali part
                sb.append("\"").append(bnQuote).append("\"\n");
                if (!bnMeaning.isEmpty()) {
                    sb.append("(").append(bnMeaning).append(")");
                }

                // Add the beautifully formatted string to the active memory list
                shlokaList.add(sb.toString());
            }
            isLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
            // Will gracefully fail and rely on the FALLBACK_SHLOKA if JSON is missing
        }
    }

    public static String getRandomShloka(Context context) {
        if (!isLoaded) {
            init(context);
        }
        if (shlokaList.isEmpty()) {
            return FALLBACK_SHLOKA;
        }
        return shlokaList.get(random.nextInt(shlokaList.size()));
    }
}
