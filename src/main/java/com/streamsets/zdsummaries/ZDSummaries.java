package com.streamsets.zdsummaries;

import com.streamsets.supportlibrary.aws.AWSSecret;
import com.streamsets.supportlibrary.zendesk.ZendeskAPI;
import com.sun.tools.corba.se.idl.InterfaceGen;
import org.apache.commons.lang3.StringUtils;
import org.zendesk.client.v2.model.Comment;
import org.zendesk.client.v2.model.CustomFieldValue;
import org.zendesk.client.v2.model.Status;
import org.zendesk.client.v2.model.Ticket;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ZDSummaries {

  // the cutoff version
  private static  final int SDC_VERSION_4 = 4;
  private static  final int SDC_VERSION_3 = 3;
  private static  final int SDC_VERSION_3_11 = 11;

  private static boolean dont_Process = false;
  private static final List<String> jiraList = new ArrayList<>();
  private static final List<String> escalations = new ArrayList<>();
  private static final List<String> labels = new ArrayList<>();
  private static final List<String> components = new ArrayList<>();
  private static final boolean inDebug = true;
  private static String productBoardLink = "";
  private static final SummaryRecord sumRecord = new SummaryRecord(" ", "", "", "", "", "", "", "");



  public static void main(String... args) throws ParseException {

    ZDSummaries zds = new ZDSummaries();
    zds.run();
    System.exit(0);
  }

  private void run() {
    //gather the credentials
    AWSSecret secret = new AWSSecret();
    Map<String, String> creds = secret.getSecret(System.getenv("AWS_SECRET"), System.getenv("AWS_REGION"));

    ZDdb z = new ZDdb();
    // if needed, create tables, mostly this will fail on second and subsequent times.
    z.createTables();

    // we are going to accumulate the tickets so don't delete all, we will delete each zendesk entry before insert in
    // case
    // a case was reopened and changed
    //z.deleteAll();

    Date limit = new Date();
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      limit = sdf.parse("2020-01-01");
    } catch(ParseException ex) {
      System.out.println("Exception: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(5);
    }

    // tracker file logs every zendesk processed to verify we processed everything
    PrintWriter tracker = null;
    try {
      tracker = new PrintWriter("Tracker.html");
    } catch (Exception e) {
      System.out.println("error opening tracker file <br> ");
    }

    try (ZendeskAPI zdAPI = new ZendeskAPI(creds)) {
      Components c = new Components();
      c.updateComponents(zdAPI, z);

      Iterable<Ticket> tickets = zdAPI.getAllTickets();
      for (Ticket t : tickets) {
        dont_Process = false;
        jiraList.clear();
        escalations.clear();
        labels.clear();
        components.clear();
        productBoardLink = "";
        clearSumRecord();


        if (t.getStatus() == Status.SOLVED || t.getStatus() == Status.CLOSED) {
          if (t.getCreatedAt().compareTo(limit) < 0) {
            System.out.println("skip " + t.getCreatedAt());
            tracker.println(t.getId() + " not in date range  " + t.getCreatedAt() + "<br>");
            continue;
          }

          System.out.println("*****************start***************");
          System.out.println("ticket number " + t.getId());

          String version = ticketVersion(t);

          if (ticketSelected(version)) {
            if (dont_Process) {
              // don't process customer not responding
              tracker.println(t.getId() + " customer not responding <br>");
            } else {
              tracker.println(t.getId() + " processed ticket **** <br>");
              addToDBTicketInfo(t, zdAPI, z);
              addToDBJira(creds, z);
              addToDBProductBoard();
              addToDBEscalations(creds, z);
              addToDBLabels(z);
              addToDBComponents(z);
            }
          } else {
            tracker.println(t.getId() + " ticket version out of range " + version + "<br>");
          }
        } else {
          tracker.println(t.getId() + "ticket not solved or closed " + t.getStatus().toString() + "<br>");
        }
      }
    }
    tracker.close();

    printSummary(z);
    PrintSmallSummary(z);

  }


  static String ticketVersion(Ticket t) {

    //todo find product and version than make a limit statement
    String version = "";

    sumRecord.setId(removeLink(Long.toString(t.getId())));
    sumRecord.setTitle(printableString(t.getSubject().trim()));

    for (CustomFieldValue c : t.getCustomFields()) {
      try {
        for (String item : c.getValue()) {
          // sdc components listed
          if (!StringUtils.isEmpty(item) && c.getId() == 360009237153L) {
            if (!item.isEmpty()) {
              String[] parts = item.split("(,| )");
              for (String p : parts) {
                components.add(p.trim());
                //                System.out.println("***components " + p);

              }
            }
          }
          // get version
          if (!StringUtils.isEmpty(item) && c.getId() == 80670167) {
            if (!item.isEmpty()) {
              version = item;
              //            System.out.println("custom id" + c.getId() + "VERSION " + item);
            }
          }
          // lets skip zen desk ticket that have customer_not_repsonsive
          if (!StringUtils.isEmpty(item) && c.getId() == 360000347207L) {
            if (!item.isEmpty()) {
              if (item.equals("customer_not_responsive")) {
                dont_Process = true;
              }
            }
          }

          // product board link && jiras
          if (!StringUtils.isEmpty(item) && c.getId() == 360015275973L) {
            if (!item.isEmpty()) {
              if (item.contains("productboard")) {
                productBoardLink = item;
              } else {
                String[] parts = item.split("(,| )");
                for (String p : parts) {
                  if (p.contains("SDC") || p.contains("DOC-") || p.contains("DPM-") || p.contains("ESC-")) {
                    jiraList.add(p.trim());
                    //              System.out.println("***Jira " + p);
                  }
                }
              }
            }
          }
          //escalations
          if (!StringUtils.isEmpty(item) && c.getId() == 360015203274L) {
            if (!item.isEmpty()) {
              String[] parts = item.split("(,| )");
              for (String p : parts) {
                if (p.contains("SDC-") || p.contains("DOC-") || p.contains("DPM-") || p.contains("ESC-")) {
                  escalations.add(p.trim());
                  //            System.out.println("***escalations " + p);
                }
              }
            }
          }
        }
      } catch (Exception ex) {
        // System.out.println("eror in ticketinrange");

      }
    }
    return version;

  }

  static boolean ticketSelected(String version) {
    String sdc_version = "";
    String control_hub_version = "";
    int pos = 0;

    int i = 0;
    if (version.isEmpty()) {
      control_hub_version = "";
      sdc_version = "";
      return false;
    }

    if (version.equals("sch__sch_cloud")) {
      control_hub_version = "sch__sch_cloud";
      sumRecord.setVersion("Control hub Cloud");
      sumRecord.setAdjustedVersion("Control hub Cloud");
      return true;
    }
    // split the version in pieces compare to minversion and majorversion
    String[] parts = version.split("_");
    if (parts.length > -1) {
      switch (parts[0]) {
        case "data":
        case "streamsets":
          sdc_version = version;
          sumRecord.setVersion("SDC: " + version);
          break;
        default:
          control_hub_version = version;
          sumRecord.setVersion("Control hub:" + version);
          break;

      }
    } else {
      //    System.out.println("empty version nor processing " + version );
      return false;
    }

    // System.out.println("before processing version " + version + " in record " + sumRecord.getVersion() );

    // adjustedvesion to have the first two parts of the version always be two digits
    // 3.1.0   becomes 03.02.0 this is for sorting and comparing
    String adjustedVersion = "";
    //   System.out.println("sdc version " + sdc_version + "   control hub version " + control_hub_version);
    String[] pieces = parts[parts.length - 1].split("\\.");
    if (pieces.length > 1) {

      // FIXME:
      if((Integer.parseInt(pieces[0]) == SDC_VERSION_4) || (Integer.parseInt(pieces[0]) == SDC_VERSION_3 && Integer.parseInt(pieces[1]) > SDC_VERSION_3_11)) {
        // find the adjusted version for proper sorting
        if (Integer.parseInt(pieces[0]) < 9) {
          adjustedVersion = "0";
        }
        adjustedVersion = adjustedVersion + pieces[0] + ".";
        if (Integer.parseInt(pieces[1]) < 9) {
          adjustedVersion = adjustedVersion + "0";
        }
        adjustedVersion = adjustedVersion + pieces[1];
        if (pieces.length > 2) {
          adjustedVersion = adjustedVersion + "." + pieces[2];
        }
        sumRecord.adjustedVersion = adjustedVersion;
        return true;
      } else {
        //     System.out.println(" *****===== version out of range ===== *** pos = " + version);
      }
    } else {
      //  System.out.println("***** ERROR PROCESSING VERSION " +  version);
    }


    //System.out.println("***** ERROR PROCESSING VERSION to few parts" + " VERSION "+ version);


    return false;

  }


  static void addToDBTicketInfo(Ticket t, ZendeskAPI zd, ZDdb z) {

    Iterable<Comment> comments = zd.getComments(t.getId());
    for (Comment c : comments) {
      if (c.getBody().contains("ISSUE") && c.getBody().contains("RESOLUTION")) {
        // I think this may be the ticket closed date. *** please verify
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        sumRecord.setTheDate(sdf.format(c.getCreatedAt()));
        int stripname = c.getBody().indexOf(",");
        int stripassigned = c.getBody().lastIndexOf(",");

        // subtract 6 for "Thanks"

        String newBody = "";

        if (stripassigned != -1 && stripassigned > stripname + 1) {
          newBody = c.getBody().substring(stripname + 1, (stripassigned - 6));
        } else {
          newBody = c.getBody().substring(stripname + 1);
        }

        sumRecord.setSummaryBody(printableString(newBody).trim());

        if (c.getBody().contains("**TROUBLESHOOTING")) {
          sumRecord.setTroubleshooting(printableString(c.getBody()).trim());
        }

        z.deletebyId(sumRecord.getId());
        z.insertSummary(sumRecord);

        //      System.out.println("new Body " + newBody);

        for (String s : t.getTags()) {
          //            System.out.println("*****  getting Tags " + s);
          labels.add(printableString(s));
        }
      }
    }
  }

  static void addToDBJira(Map<String, String> creds, ZDdb z) {
    int i = 1;
    for (String J : jiraList) {
      //      System.out.println("**** jira " + J + "******");
      getJiraInfo(creds, J, "Jira", i++, z);


    }
  }

  static void addToDBEscalations(Map<String, String> creds, ZDdb z) {
    int i = 1;
    for (String J : escalations) {
      //      System.out.println("**** Escalations " + J);
      getJiraInfo(creds, J, "Escalations", i++, z);
    }
  }

  static void getJiraInfo(Map<String, String> creds, String J, String whichType, int i, ZDdb z) {
    String fixedVersion = "";
    String description = "";
    String status = "";
    Jira jira = new Jira(creds, J);
    if (Jira.getError()) {
      return;
    }

    if (jira.getJiraStatus() == null) {
      status = "Status Open";
    } else {
      status = "Status " + printableString(jira.getJiraStatus());
    }
    description = printableString(jira.getIssueDescription()).trim();

    if (whichType.contains("Jira")) {
      String jiraBody = "Jira: " + J + " " + "Status: " + printableString(status.trim()) + " \n " + printableString(
          description).trim() + " \n " + " link " + printableString(jira.getLink()).trim();
      JiraRecord JR = new JiraRecord(sumRecord.getId(), J, jiraBody);
      z.InsertJira(JR);
    } else {
      String escalationBody =
          "Jira: " + J + " " + "Status: " + printableString(status.trim()) + " \n " + printableString(
          description).trim() + " \n " + " link " + printableString(jira.getLink()).trim();

      EscalationRecord JR = new EscalationRecord(sumRecord.getId(), J, escalationBody);
      z.InsertEscalation(JR);

    }
  }

  static void addToDBComponents(ZDdb z) {
    int i = 1;
    for (String J : components) {
      //    System.out.println("**** components " + J);

      ComponentRecord CP = new ComponentRecord(sumRecord.getId(), J);
      z.InsertComponent(CP);
    }
  }

  static void addToDBLabels(ZDdb z) {

    for (String J : labels) {
      if (sumRecord.getId().equals("11677")) {
        //          System.out.println("debug");

        //    System.out.println("**** labels " + J);

        LabelRecord CP = new LabelRecord(sumRecord.getId(), J);
        z.InsertLabel(CP);
      }
    }
  }


  static void addToDBProductBoard() {
    //System.out.println("product board link " + productBoardLink);
    if (!productBoardLink.isEmpty()) {
      sumRecord.setProductBoardLink("Product Board " + printableString(productBoardLink).trim());
    }

  }

  static void clearSumRecord() {
    sumRecord.setId("");
    sumRecord.setTheDate("");
    sumRecord.setVersion("");
    sumRecord.setTitle("");
    sumRecord.setVersion("");
    sumRecord.setSummaryBody("");
    sumRecord.setAdjustedVersion("");
    sumRecord.setProductBoardLink("");
    sumRecord.setTroubleshooting("");


  }

  static void printSummary(ZDdb z) {
    //open files, one for 2 weeks
    //open for all
    Boolean firstTime = true;
    int theRow = 0;
    try {
      PrintWriter bigFile = new PrintWriter("SummaryZenD.html");

      List<SummaryRecord> sr = z.getSummaryRecords();

      for (SummaryRecord r : sr) {
        theRow++;
        String printRecord;
        printThem(r, bigFile, firstTime, theRow, z);
        firstTime = false;
      }
      bigFile.println("</body></head>");
      bigFile.close();
    } catch (Exception ex) {
      System.out.println("error printing");
    }

  }

  static void PrintSmallSummary(ZDdb z) {

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -14);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    String cutOffDate = sdf.format(calendar.getTime());

    Boolean firstTime = true;
    try {
      PrintWriter smallFile = new PrintWriter("SummaryZenD14.html");
      List<SummaryRecord> sr = z.getSummaryRecordsByDate(cutOffDate);
      int theRow = 0;
      for (SummaryRecord r : sr) {
        String printRecord;
        theRow++;
        printThem(r, smallFile, firstTime, theRow, z);
        firstTime = false;

      }
      smallFile.println("</body></head>");
      smallFile.close();
    } catch (Exception ex) {
      System.out.println("error writing 14 day file");
    }
  }

  static void printThem(SummaryRecord r, PrintWriter pw, boolean firstTime, int theRow, ZDdb z) {

    String printRecord;
    if (firstTime) {

      pw.println("<!DOCTYPE html ><html ><body >");
    }

    pw.println(theRow + "<br>");
    printRecord = "<h1 font-size:40px;>" + "Zendesk Ticket " + r.getId() + " </h1>";
    pw.println(printRecord);

    printRecord = "<p style=font-size:20px > Version: " + r.getVersion() + "          " + " (Closed : " + r.getTheDate() + ") " + "</p>";
    pw.println(printRecord);

    printRecord = "<p style=font-size:22px >" + r.getTitle() + " </p><br>";

    pw.println(printRecord);

    int part1 = r.getSummaryBody().indexOf("Summary of the ticket");
    int part2 = r.getSummaryBody().indexOf("**ISSUE");
    if (part2 == -1) {
      part2 = r.getSummaryBody().indexOf("ISSUE");
    }
    int part3 = r.getSummaryBody().indexOf("**RESOLUTION");

    if (part3 == -1) {
      part3 = r.getSummaryBody().indexOf("RESOLUTION");
    }
    if (part1 == -1 || part2 == -1 || part3 == -1) {
      printRecord = "<p style=font-size:20px> " + fixLink(r.getSummaryBody()) + "<br></p>";
      pw.println(printRecord);
    } else {
      printRecord = "<p style=font-size:20px> " + fixLink(r.getSummaryBody().substring(0, part1)) + "<br><br></p>";
      pw.println(printRecord);
      printRecord = "<p style=font-size:20px> " + fixLink(r.getSummaryBody().substring(part1, part2)) + "<br><br></p>";
      pw.println(printRecord);
      printRecord = "<p style=font-size:20px> " + fixLink(r.getSummaryBody().substring(part2, part3)) + "<br><br></p>";
      pw.println(printRecord);
      printRecord = "<p style=font-size:20px> " + fixLink(r.getSummaryBody()
          .substring(part3)) + "<br><br></p>";
      pw.println(printRecord);
    }

    //troubleshooting
    if (!r.getTroubleshooting().isEmpty()) {
      printRecord = "<p style=font-size:20px> " + fixLink(r.getTroubleshooting()) + "<br><br></p>";
      pw.println(printRecord);
    } else {
      //  printRecord = "<p style=font-size:20px> " + "**TROUBLESHOOTING  goes here" + "<br><br></p>";
      //  pw.println(printRecord);
    }

    List<JiraRecord> JRS = z.getJiraRecords(r.getId());
    for (JiraRecord JR : JRS) {
      if (!JR.getJiraNum().isEmpty()) {
        printRecord = "<h2 style=font-size:20px> Jira : " + JR.getJiraNum() + "<br> </h2>";
        pw.println(printRecord);
      }

      if (!JR.getJiraBody().isEmpty()) {
        printRecord = "<p style=font-size:18px>" + "       " + fixLink(JR.getJiraBody()
            .replaceAll(" \n ", "<BR>")) + " <br></p>";
        pw.print(printRecord);

      }
    }
    List<EscalationRecord> es = z.getEscalationRecords(r.getId());
    for (EscalationRecord e : es) {
      if (!e.getEscalationNum().isEmpty()) {
        printRecord = "<h2 style=font-size:20px> Escalation : " + e.getEscalationNum() + "<br>  </h2>";
        pw.println(printRecord);
      }
      if (!e.getEscalationBody().isEmpty()) {
        printRecord = "<p style=font-size:18px>" + "       " + fixLink(e.getEscalationBody()
            .replaceAll(" \n ", "<BR>")) + "<br>  </p>";
        pw.print(printRecord);

      }
    }

    List<ComponentRecord> cs = z.getComponentRecords(r.getId());
    for (ComponentRecord c : cs) {
      if (!c.getComponent().isEmpty()) {
        printRecord = "<h2 style=font-size:20px> Component : " + c.getComponent() + "<br>  </h2>";
        pw.println(printRecord);
      }
    }
    if (!r.getProductBoardLink().isEmpty()) {
      printRecord = "<p font-size:25px> other " + fixLink(r.getProductBoardLink()) + "<br></p>";
      pw.println(printRecord);
    }


  }

  static String fixLink(String s) {

    String newString = "";
    int i = 0;
    int part = 0;
    part = s.indexOf("http");
    // initial part before http
    if (part > 0) {
      newString = s.substring(i, part);
    }
    //
    while (part != -1) {
      for (int j = part; j < s.length(); j++) {
        if (s.charAt(j) == ' ') {
          i = j;

          break;
        }
        i = j;
      }
      // this is when it is at the end of the field. i = was not incremented and since it is either a space or the
      // end of the field it's
      // ok to add one
      if (i == s.length() - 1) {
        i++;
      }
      // make the link
      newString = newString + "<a href=" + s.substring(part, i) + "> " + s.substring(part, i) + " </a>";
      // are there any more
      part = s.indexOf("http", i);
    }

    if (i < s.length()) {
      newString = newString + s.substring(i);
    }
    return newString;


  }

  static String removeLink(String st) {
    String[] parts = st.split("/");
    if (parts.length == 1) {
      return st;
    } else {
      return parts[parts.length - 1];
    }
  }


  static String printableString(String s) {

    s = s.replaceAll("'", "");
    s = s.replaceAll("\"", " ");
    s = s.replaceAll("<", " ");
    s = s.replaceAll(">", " ");

    return s;
  }

}

