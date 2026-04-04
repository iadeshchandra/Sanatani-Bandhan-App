package org.shda;

public class Member {
    public String id;
    public String name;
    public String phone;
    public String gotra;
    public String bloodGroup;
    public long timestamp;
    public float totalDonated;
    
    // Security & Roles
    public String role; 
    public String password; 

    public Member() {}

    public Member(String id, String name, String phone, String gotra, String bloodGroup, long timestamp, String role, String password) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.gotra = gotra;
        this.bloodGroup = bloodGroup;
        this.timestamp = timestamp;
        this.totalDonated = 0.0f;
        this.role = role;
        this.password = password;
    }
}
