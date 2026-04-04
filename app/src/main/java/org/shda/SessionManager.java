package org.shda;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private static final String PREF_NAME = "SanataniSession";
    private static final String KEY_COMM_ID = "communityId";
    private static final String KEY_ROLE = "role";
    private static final String KEY_COMM_NAME = "communityName";
    private static final String KEY_USER_NAME = "userName"; 
    private static final String KEY_USER_ID = "userId";     

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createLoginSession(String commId, String role, String commName, String userName, String userId) {
        editor.putString(KEY_COMM_ID, commId);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_COMM_NAME, commName);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    // NEW: Instantly update the community name in local memory
    public void updateCommunityName(String newName) {
        editor.putString(KEY_COMM_NAME, newName);
        editor.apply();
    }

    public String getCommunityId() { return prefs.getString(KEY_COMM_ID, null); }
    public String getRole() { return prefs.getString(KEY_ROLE, "MEMBER"); }
    public String getCommunityName() { return prefs.getString(KEY_COMM_NAME, "Sanatani Bandhan"); }
    public String getUserName() { return prefs.getString(KEY_USER_NAME, "Unknown User"); }
    public String getUserId() { return prefs.getString(KEY_USER_ID, ""); }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
