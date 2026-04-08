package org.shda;

public class Member {
    public String id, name, phone, email, gotra, bloodGroup, role, address;
    public String fatherName, motherName, nid, addedBySignature;
    public float totalDonated = 0f;
    public long timestamp = 0L;
    
    // ✨ FIX: Added the tracking variable for the PDF Engine and UI cards
    public long lastDonationTimestamp = 0L;

    // Empty constructor required by Firebase
    public Member() {}
}
