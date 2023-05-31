package com.streamsets.zdsummaries;

public class SummaryRecord {

    String id;
    String theDate;
    String adjustedVersion;
    String version;
    String title;
    String summaryBody;
    String productBoardLink;
    String troubleshooting;

    public SummaryRecord(String id, String theDate,
                         String adjustedVersion, String version, String title,
                         String summaryBody,
                         String productBoardLink, String troubleshooting) {

        this.id = id;
        this.theDate = theDate;
        this.adjustedVersion = adjustedVersion;
        this.version = version;
        this.title = title;
        this.summaryBody = summaryBody;
        this.productBoardLink = productBoardLink;
        this.troubleshooting = troubleshooting;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }


    public String getTheDate() {
        return theDate;
    }

    public void setTheDate(String theDate) {
        this.theDate = theDate;
    }


    public String getVersion() {return version;}

    public void setVersion(String version) {
        this.version = version;
    }
    public String getAdjustedVersion() {return adjustedVersion;}

    public void setAdjustedVersion(String adjustedVersion) {
        this.adjustedVersion = adjustedVersion;
    }

    public String getTitle() {return title;}

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummaryBody() {
        return summaryBody;
    }

    public void setSummaryBody(String summaryBody) {
        this.summaryBody = summaryBody;
    }

    public String getProductBoardLink() {
        return productBoardLink;
    }

    public void setProductBoardLink(String productBoardLink) {
        this.productBoardLink = productBoardLink;

    }

    public String getTroubleshooting() {
        return troubleshooting;
    }

    public void setTroubleshooting(String troubleshooting) {
        this.troubleshooting = troubleshooting;

    }

    }

