package org.shda;

import java.util.HashMap;

public class Poll {
    public String id, question, optionA, optionB, optionC, optionD, createdBy, officialComment;
    public long timestamp, endTimestamp;
    public HashMap<String, String> votes;

    public Poll() {} // Required for Firebase

    public Poll(String id, String question, String optionA, String optionB, String optionC, String optionD, long timestamp, long endTimestamp, String createdBy) {
        this.id = id;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.timestamp = timestamp;
        this.endTimestamp = endTimestamp;
        this.createdBy = createdBy;
        this.votes = new HashMap<>();
    }
}
