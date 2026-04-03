package org.shda;

public class Member {
    public String id;
    public String name;
    public String phone;
    public String gotra;      // New: For Puja and Family tracking
    public String bloodGroup; // New: For community emergencies
    public long joinDate;
    public float totalDonated;

    // Default constructor required for Firebase
    public Member() {} 

    public Member(String id, String name, String phone, String gotra, String bloodGroup, long joinDate) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.gotra = gotra;
        this.bloodGroup = bloodGroup;
        this.joinDate = joinDate;
        this.totalDonated = 0f;
    }
}
