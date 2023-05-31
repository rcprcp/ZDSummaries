package com.streamsets.zdsummaries;

public class JiraRecord {
    String id;
    String jiraNum;
    String jiraBody;

    public JiraRecord(String id, String jiraNum, String jiraBody
    ) {
        this.id = id;
        this.jiraNum = jiraNum;
        this.jiraBody = jiraBody;


    }

    public String getId() {return id;}

    public void setId(String id) { this.id = id;}

    public String getJiraNum() {
        return jiraNum;
    }

    public void setJiraNum(String jiraNum) {
        this.jiraNum = jiraNum;
    }

    public String getJiraBody() {
        return jiraBody;
    }

    public void setJiraBody(String jiraBody) {
        this.jiraBody = jiraBody;
    }

}


