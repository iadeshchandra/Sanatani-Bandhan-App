package org.shda;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class AuditLogger {
    
    public static void logAction(String communityId, String managerName, String actionType, String description) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        
        HashMap<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("managerName", managerName);
        log.put("actionType", actionType); // e.g., "CHANDA_COLLECTED", "MEMBER_ADDED"
        log.put("description", description);

        db.child("communities").child(communityId).child("audit_logs").push().setValue(log);
    }
}
