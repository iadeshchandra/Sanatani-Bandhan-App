package org.shda;

public class Member {
    public String id;
    public String name;
    public String phone;
    public long joinDate;
    public float totalDonated;

    // Default constructor required for Firebase
    public Member() {} 

    public Member(String id, String name, String phone, long joinDate) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.joinDate = joinDate;
        this.totalDonated = 0f; // Starts at zero when they join
    }
}
