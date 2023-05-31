package com.streamsets.zdsummaries;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Jira {
    private static final Logger LOG = LoggerFactory.getLogger(ZDSummaries.class);

    private int httpStatus;
    private String jiraStatus;
    private String issueSummary;
    private String fixVersion;
    private String id;
    private String link;
    private String issueDescription;
    static boolean  had_error= false;

    Jira(Map<String, String> creds, String id) {
        had_error = false;
        this.id = id;
        link = "https://issues.streamsets.com/rest/api/2/issue/" + id;

        String rawJson = "";
        try {
            URL url = new URL(link);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

           // String auth = System.getenv("JIRA_USERNAME") + ":" + System.getenv("JIRA_PASSWORD");
            String auth = creds.get("jira_user") + ":" + creds.get("jira_token");
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            con.setRequestProperty("Authorization", authHeaderValue);

            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            httpStatus = con.getResponseCode();

            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            rawJson = content.toString();
            in.close();
            con.disconnect();
        } catch (IOException ex) {
            System.out.println("Exception: {} " + ex.getMessage() + "  " + ex);
            ex.printStackTrace();
            // todo - remove debugging code:
            had_error=true;
            return;
        }

        JSONObject root = new JSONObject(rawJson);
        JSONObject f = root.getJSONObject("fields");
        JSONArray v = f.getJSONArray("fixVersions");
        for (int i = 0; i < v.length(); i++) {
            JSONObject o = v.getJSONObject(i);
            if (o.get("name") != null) {
                fixVersion = (String) o.get("name");
                break;
            }
        }

        JSONObject obj = f.getJSONObject("status");
        jiraStatus = obj.getString("name");
        try {
            issueDescription = f.getString("description");
        }
        catch(Exception ex) {
            issueDescription = "";
        }

        System.out.println("hello " + issueDescription);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getJiraStatus() {
        return jiraStatus;
    }

    public String getIssueSummary() {
        return issueSummary;
    }

    public String getIssueDescription() {
        return issueDescription;
    }


    public String getFixVersion() {
        return fixVersion;
    }

    public String getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    static  boolean getError() {return had_error;}
}

