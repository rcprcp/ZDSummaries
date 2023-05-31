package com.streamsets.zdsummaries;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ZDdb {
    private static final String URL = "jdbc:postgresql://localhost:5432/";
    private static final String DB_NAME = "zddb";
    private static final String T_SUMMARY = "summaryinfo";
    private static final String T_LABEL = "labels";
    private static final String T_JIRA = "Jiras";
    private static final String T_ESC = "Escalations";
    private static final String T_COMP = "Components";
    private static final String USER_NAME = "postgres";
    private static final String PASSWORD = "postgres";
    private static String adjustedVersion= "1";

    private Connection conn;
    private Statement stmt;

    public ZDdb() {
        try {
            conn = DriverManager.getConnection(URL + DB_NAME, USER_NAME, PASSWORD);
            conn.setAutoCommit(true);
            stmt = conn.createStatement();
        } catch (SQLException ex) {
            System.out.println("can't get db connection " + ex.getMessage());
            ex.printStackTrace();
            System.exit(3);
        }
        return;
    }

    public void createTables() {
        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }
     // create summary table
        String ZDTable = "Create table "
                + T_SUMMARY
                + " (id Varchar(10) primary key, thedate VarChar(10), "
                + "adjustedversion Varchar(80), version Varchar(80), "
                + "title Varchar(200), summaryBody Varchar(20000), "
                + "productboardlink Varchar(300), troubleshooting Varchar(20000))";

        String ZDix1 = "Create unique index " + T_SUMMARY + "_ix1  "
                + " on " + T_SUMMARY + " (version, id)";
        String ZDix2 = "Create unique index " + T_SUMMARY + "_ix2  "
                + " on " + T_SUMMARY + " (adjustedversion desc, id)";
        String ZDix3 = "Create unique index " + T_SUMMARY + "_ix3  "
                + " on " + T_SUMMARY + " (theDate, id)";

        try {
            stmt.executeUpdate(ZDTable);
        } catch (Exception e) {
            System.out.println("error creating table " + ZDTable
                    + " " + e);
        }

        try {
            stmt.executeUpdate(ZDix1);
        } catch (Exception e) {
            System.out.println("error creating index " + ZDix1
                    + " " + e);
        }
        try {
            stmt.executeUpdate(ZDix2);
        } catch (Exception e) {
            System.out.println("error creating index " + ZDix2
                    + " " + e);
        }
        try {
            stmt.executeUpdate(ZDix3);
        } catch (Exception e) {
            System.out.println("error creating index " + ZDix3
                    + " " + e);
        }

        String LabelTable = "Create table "
                + T_LABEL
                + " (id Varchar(10), foreign key(id) references  " + T_SUMMARY + " (id) on delete cascade,  "
                + " labeltext Varchar(300))";

        try {
            stmt.executeUpdate(LabelTable);
        } catch (Exception e) {
            System.out.println("error creating table " + LabelTable
                    + " " + e);
        }


        String Labelix1 = "Create unique index " + T_LABEL+ "_ix1  "
                + " on " + T_LABEL + " (id, labeltext)";

        try {
            stmt.executeUpdate(Labelix1);
        } catch (Exception e) {
            System.out.println("error creating index " + Labelix1
                    + " " + e);
        }

        String ComponentTable = "Create table "
                + T_COMP
                + " (id Varchar(10), foreign key(id) references " + T_SUMMARY + " (id) on delete cascade, "
                + " component Varchar(1000))";

        try {
            stmt.executeUpdate(ComponentTable);
        } catch (Exception e) {
            System.out.println("error creating table " + ComponentTable
                    + " " + e);
        }

        String Compix1 = "Create unique index " + T_COMP + "_ix1  "
                + " on " + T_COMP + " (id, component)";

        try {
            stmt.executeUpdate(Compix1);
        } catch (Exception e) {
            System.out.println("error creating index " + Compix1
                    + " " + e);
        }
        String JiraTable = "Create table "
                + T_JIRA
                + " (id Varchar(10), foreign key(id) references  " + T_SUMMARY + " (id) on delete cascade, jiranum Varchar(200), "
                + " jirabody Varchar(30000))";

        try {
            stmt.executeUpdate(JiraTable);
        } catch (Exception e) {
            System.out.println("error creating table " + JiraTable
                    + " " + e);
        }
        String Jiraix1 = "Create unique index " + T_JIRA + "_ix1  "
                + " on " + T_JIRA + " (id, jiranum)";

        try {
            stmt.executeUpdate(Jiraix1);
        } catch (Exception e) {
            System.out.println("error creating index " + Jiraix1
                    + " " + e);
        }

        String EscalationTable = "Create table "
                + T_ESC
                + " (id Varchar(10), foreign key(id) references " + T_SUMMARY + " (id) on delete cascade,  escalationnum varchar(200), "
                + " escalationbody Varchar(30000))";

        try {
            stmt.executeUpdate(EscalationTable);
        } catch (Exception e) {
            System.out.println("error creating table " + EscalationTable
                    + " " + e);
        }
        String Escix1 = "Create unique index " + T_ESC + "_ix1  "
                + " on " + T_ESC + " (id, escalationnum)";

        try {
            stmt.executeUpdate(Escix1);
        } catch (Exception e) {
            System.out.println("error creating index " + Escix1
                    + " " + e);
        }

    }

    public List<SummaryRecord> getSummaryRecords() {
        List<SummaryRecord> ans = new ArrayList<>();
        if (conn == null) {
            System.out.println("connection is not available");
            return ans;
        }

        String sql = "select id, theDate, adjustedversion, version,  title, summarybody, "
                + "productboardlink, troubleshooting "
                + " from "
                + T_SUMMARY + " order by adjustedversion desc, id  ";

        System.out.println(sql);
        ResultSet c ;
        try {
            c = stmt.executeQuery(sql);
            while (c.next()) {
                SummaryRecord sr = new SummaryRecord(
                        c.getString("id"),
                        c.getString("thedate"),
                        c.getString("adjustedversion"),
                        c.getString("version"),
                        c.getString("title"),
                        c.getString("summarybody"),
                        c.getString ("productboardlink"),
                        c.getString ("troubleshooting")
                );

                ans.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("getSummaryRecords() -- query failed." + e);
            return ans;
        }

        try {
               c.close();
            //stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException " + ex.getMessage());
            ex.printStackTrace();
            System.exit(4);
        }

        return ans;
    }
    public List<JiraRecord> getJiraRecords(String id) {
        List<JiraRecord> ans = new ArrayList<>();
        if (conn == null) {
            System.out.println("connection is not available");
            return ans;
        }

        String sql = "select id, jiranum, jirabody "
                + " from "
                + T_JIRA + " where id =  " + "'" + id + "'" ;

        System.out.println(sql);
        ResultSet c ;
        try {
            c = stmt.executeQuery(sql);
            while (c.next()) {
                JiraRecord sr = new JiraRecord(
                        c.getString("id"),
                        c.getString("jiranum"),
                        c.getString("jirabody")
                );

                ans.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("getJiraRecords() -- query failed." + e + " " + sql );
            return ans;
        }

        try {
            c.close();
            //stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException " + ex.getMessage());
            ex.printStackTrace();
            System.exit(4);
        }

        return ans;
    }


    public List<EscalationRecord> getEscalationRecords(String id) {
        List<EscalationRecord> ans = new ArrayList<>();
        if (conn == null) {
            System.out.println("connection is not available");
            return ans;
        }

        String sql = "select id, escalationnum, escalationbody "
                + " from "
                + T_ESC + " where id =  " + "'" + id + "'" ;

        System.out.println(sql);
        ResultSet c ;
        try {
            c = stmt.executeQuery(sql);
            while (c.next()) {
                EscalationRecord sr = new EscalationRecord(
                        c.getString("id"),
                        c.getString("escalationnum"),
                        c.getString("escalationbody")
                        );

                ans.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("getEscalationRecords() -- query failed." + e + " " + sql );
            return ans;
        }

        try {
            c.close();
            //stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException " + ex.getMessage());
            ex.printStackTrace();
            System.exit(4);
        }

        return ans;
    }


    public List<LabelRecord> getLabelRecords(String id) {
        List<LabelRecord> ans = new ArrayList<>();
        if (conn == null) {
            System.out.println("connection is not available");
            return ans;
        }

        String sql = "select id, labeltext "
                + " from "
                + T_LABEL + " where id =  " + "'" + id + "'" ;

        System.out.println(sql);
        ResultSet c ;
        try {
            c = stmt.executeQuery(sql);
            while (c.next()) {
                LabelRecord sr = new LabelRecord(
                        c.getString("id"),
                        c.getString("labeltext")
                        );

                ans.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("getLabelRecords() -- query failed." + e + " " + sql );
            return ans;
        }

        try {
            c.close();
            //stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException " + ex.getMessage());
            ex.printStackTrace();
            System.exit(4);
        }

        return ans;
    }


    public List<ComponentRecord> getComponentRecords(String id) {
        List<ComponentRecord> ans = new ArrayList<>();
        if (conn == null) {
            System.out.println("connection is not available");
            return ans;
        }

        String sql = "select id, component "
                + " from "
                + T_COMP + " where id =  " + "'" + id + "'"   ;

        System.out.println(sql);
        ResultSet c ;
        try {
            c = stmt.executeQuery(sql);
            while (c.next()) {
                ComponentRecord sr = new ComponentRecord(
                        c.getString("id"),
                        c.getString("component")
                        );

                ans.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("getComponentecords() -- query failed." + e + " " + sql );
            return ans;
        }

        try {
            c.close();
            //stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException " + ex.getMessage());
            ex.printStackTrace();
            System.exit(4);
        }

        return ans;
    }




    public List<SummaryRecord> getSummaryRecordsByDate(String startDate) {
        List<SummaryRecord> ans = new ArrayList<>();
        if (conn == null) {
            System.out.println("connection is not available");
            return ans;
        }

        System.out.println("the date " +startDate);
        String sql = "select id, thedate, adjustedversion, version,  title, summarybody, "
                + "productboardlink,troubleshooting "
                + " from "
                + T_SUMMARY + " where thedate >  " + "'"+ startDate + "'" + " order by thedate desc, adjustedversion desc, id ";

       System.out.println(sql);

        ResultSet c ;
        try {
            c = stmt.executeQuery(sql);
            while (c.next()) {
                SummaryRecord sr = new SummaryRecord(
                        c.getString("id"),
                        c.getString("thedate"),
                        c.getString("adjustedversion"),
                        c.getString("version"),
                        c.getString("title"),
                        c.getString("summarybody"),
                        c.getString ("productboardlink"),
                        c.getString("troubleshooting"));

                ans.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("getSummaryRecords() -- query failed." + e);
            return ans;
        }

        try {
            c.close();
            //stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException " + ex.getMessage());
            ex.printStackTrace();
            System.exit(4);
        }

        return ans;
    }

    public void insertSummary(SummaryRecord sumRecord)
     {

        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "INSERT INTO " + T_SUMMARY
                + " (id,  thedate, adjustedversion, version, summarybody,  title ,"
                + "productboardlink, troubleshooting "
                + ") VALUES ( "
                + " '" + sumRecord.getId()               + "' , "
                + " '" + sumRecord.getTheDate()          + "' , "
                + " '" + sumRecord.getAdjustedVersion()  + "' , "
                + " '" + sumRecord.getVersion()          + "' , "
                + " '" + sumRecord.getSummaryBody()      + "' , "
                + " '" + sumRecord.getTitle()            + "' , "
                + " '" + sumRecord.getProductBoardLink() + "' , "
                + " '" + sumRecord.getTroubleshooting()  + "'"
                + ")" ;



         try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("Summary: INSERT failed: " + e + "\n " + "sql " +sql);
        }
    }
    public void InsertJira(JiraRecord JR)
    {

        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "INSERT INTO " + T_JIRA
                + " (id,  jiranum, jirabody "
                + ") VALUES ( "
                + " '" + JR.getId()               + "' , "
                + " '" + JR.getJiraNum()          + "' , "
                + " '" + JR.getJiraBody()             + "'"     + " ) " ;


        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("Summary: INSERT failed: " + e + "\n " + "sql " +sql);
        }
    }
    public void InsertEscalation(EscalationRecord ESC)
    {

        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "INSERT INTO " + T_ESC
                + " (id,  escalationnum, escalationbody "
                + ") VALUES ( "
                + " '" + ESC.getId()               + "' , "
                + " '" + ESC.getEscalationNum()         + "' , "
                + " '" + ESC.getEscalationBody()             + "'"     + " ) " ;


        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("escalation: INSERT failed: " + e + "\n " + "sql " +sql);
        }
    }
    public void InsertLabel(LabelRecord l)
    {

        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "INSERT INTO " + T_LABEL
                + " (id,  labeltext "
                + ") VALUES ( "
                + " '" + l.getId()               + "' , "
                + " '" + l.getLabelText()             + "'"     + " ) " ;


        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("label : INSERT failed: " + e + "\n " + "sql " +sql);
        }
    }
    public void InsertComponent(ComponentRecord c)
    {

        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "INSERT INTO " + T_COMP
                + " (id,  component "
                + ") VALUES ( "
                + " '" + c.getId()               + "' , "
                + " '" + c.getComponent()             + "'"     + " ) " ;


        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("component: INSERT failed: " + e + "\n " + "sql " +sql);
        }
    }

    public void deleteAll() {
        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "DELETE FROM " + T_SUMMARY;
        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("deleteAll-- SQL failed -- "
                    + stmt + " " + e + " sql " + sql);
        }
    }
    public void deletebyId(String id) {
        if (conn == null) {
            System.out.println("connection is not available");
            return;
        }

        String sql = "DELETE FROM " + T_SUMMARY + " where id = " + "'" +id + "'";
        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("deletebyId-- SQL failed -- "
                    + stmt + " " + e + "\n  sql " + sql) ;
        }
    }

}
