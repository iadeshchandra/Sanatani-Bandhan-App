package org.shda;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class SanataniApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // ✨ ACTIVATES FIREBASE OFFLINE ENGINE
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            
            // Optional: Tell the app to aggressively keep the Mandir logs synced offline
            // FirebaseDatabase.getInstance().getReference("communities").keepSynced(true);
        } catch (Exception e) {
            // Catches error if persistence is called twice
        }
    }
}
