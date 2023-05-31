package com.streamsets.zdsummaries;

public class EscalationRecord {
    String id;
    String escalationNum;
    String escalationBody;


    public EscalationRecord(String id, String escalationNum, String escalationBody
    ) {

        this.id = id;
        this.escalationNum = escalationNum;
        this.escalationBody = escalationBody;

    }
    public String getId() {return id;}

    public void setId(String id) { this.id = id;}

    public String getEscalationNum() {
        return escalationNum;
    }

    public void setEscalationNum(String esclationNum) {
        this.escalationNum = escalationNum;
    }

    public String getEscalationBody() {
        return escalationBody;
    }


}
