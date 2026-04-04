package org.shda;

public class Member {
    public String id, name, phone, gotra, bloodGroup, role, password;
    // NEW OPTIONAL FIELDS
    public String fatherName, motherName, nid, address, addedBySignature; 
    public long timestamp;
    public float totalDonated;

    public Member() {} // Required for Firebase

    public Member(String id, String name, String phone, String gotra, String bloodGroup, long timestamp, String role, String password, String fatherName, String motherName, String nid, String address, String addedBySignature) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.gotra = gotra;
        this.bloodGroup = bloodGroup;
        this.timestamp = timestamp;
        this.role = role;
        this.password = password;
        this.totalDonated = 0f;
        
        this.fatherName = fatherName;
        this.motherName = motherName;
        this.nid = nid;
        this.address = address;
        this.addedBySignature = addedBySignature;
    }
}
