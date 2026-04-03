package org.shda;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences("SanataniSaaS", Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // Saves the user's SaaS data when they log in
    public void createSession(String uid, String communityId, String role, String communityName) {
        editor.putString("uid", uid);
        editor.putString("communityId", communityId);
        editor.putString("role", role);
        editor.putString("communityName", communityName);
        editor.apply();
    }

    // Fetch these anywhere in the app to route data correctly
    public String getCommunityId() { return prefs.getString("communityId", null); }
    public String getCommunityName() { return prefs.getString("communityName", "Sanatani Portal"); }
    public String getRole() { return prefs.getString("role", "MEMBER"); } // ADMIN, MANAGER, or MEMBER
    
    public void logout() { 
        editor.clear().apply(); 
    }
}
