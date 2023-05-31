package com.streamsets.zdsummaries;

public class ComponentRecord {

    String id;
    String component;

    public ComponentRecord(String id, String component
    ) {

        this.id = id;
        this.component = component;


    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }
}
