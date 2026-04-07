package org.shda;

import java.util.HashMap;

public class Poll {
    public String id, question, optionA, optionB, optionC, optionD, createdBy, adminComment;
    public long timestamp, endTimestamp;
    public HashMap<String, String> votes;

    public Poll() {}
}
