package org.shda;

public class Member {
    public String id;
    public String name;
    public String phone;
    public String gotra;
    public String bloodGroup;
    public long timestamp;
    public float totalDonated;

    // CRITICAL: Firebase needs this empty constructor to read data!
    public Member() {
    }

    // Constructor for creating a new member
    public Member(String id, String name, String phone, String gotra, String bloodGroup, long timestamp) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.gotra = gotra;
        this.bloodGroup = bloodGroup;
        this.timestamp = timestamp;
        this.totalDonated = 0.0f; // Starts at zero for new members
    }
}
