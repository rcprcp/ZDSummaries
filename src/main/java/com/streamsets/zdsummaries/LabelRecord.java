package com.streamsets.zdsummaries;

public class LabelRecord {

    String id;
    String labelText;

    public LabelRecord(String id, String labelText
    ) {

        this.id = id;
        this.labelText = labelText;

    }

    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }
}
