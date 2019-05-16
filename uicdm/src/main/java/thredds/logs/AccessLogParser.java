/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.logs;

import java.util.regex.*;
import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Read TDS access logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class AccessLogParser implements LogReader.LogParser {

  ///////////////////////////////////////////////////////
  // log reading

  // sample
  // 1                2   3                           4                                                                                                5   6  7     8               9
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null" "Java/1.6.0_05" 21
  // 24.18.236.132 - - [04/Feb/2011:17:49:03 -0700] "GET /thredds/fileServer//nexrad/level3/N0R/YUX/20110205/Level3_YUX_N0R_20110205_0011.nids "       200 10409 "-" "-" 17

  // an access log without the extra fields at the end
  // 1           2  3                           4                        5   6                                                                      5   6  7     8               9
  // 127.0.0.1 - - [17/Jun/2015:13:48:32 -0600] "GET /thredds/ HTTP/1.1" 302 -

  // 30/Sep/2009:23:50:47 -0600
  private SimpleDateFormat formatFrom = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

  private static Pattern regPattern =
                         // 1                             2       3         4      5      6             7        8      9                                                         5   6  7     8               9
          Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

  // pattern without the extra fields at the end         1                             2       3         4      5      6                                                                  5   6  7     8               9
  private static Pattern regPattern2 = Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)] \"(.*)\" (\\d+) ([\\-\\d]+)");

  public LogReader.Log nextLog(BufferedReader dataIS) throws IOException {
    String line = dataIS.readLine();
    if (line == null) return null;
    LogReader.Log resultLog = parseLog(line);
    if (resultLog != null) {
      return resultLog;
    } else {
      // todo: FIX ME...no likey recursy.
      return nextLog(dataIS);
    }
    /* try {
      //System.out.println("\n"+line);
      Matcher m = regPattern.matcher(line);
      if (m.matches()) {
        LogReader.Log log = new LogReader.Log();
        log.ip = m.group(1);
        log.date = convertDate( m.group(3));
        String request = m.group(4);
        log.returnCode = parse(m.group(5));
        log.sizeBytes = parseLong(m.group(6));
        log.referrer = m.group(7);
        log.client = m.group(8);
        log.msecs = parseLong(m.group(9));

        String[] reqss = request.split(" ");
        if (reqss.length == 3) {
          log.verb = reqss[0].intern();
          log.path = reqss[1];
          log.http = reqss[2].intern();
        }

        return log;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Cant parse " + line);
    }
    return null;  */
  }

  LogReader.Log parseLog(String line) throws IOException {
    try {
      // the enhanced log
      Matcher m = regPattern.matcher(line);
      if (m.matches()) {
        LogReader.Log log = new LogReader.Log();
        log.ip = m.group(1);
        log.date = convertDate( m.group(3));
        String request = m.group(4);
        log.returnCode = parse(m.group(5));
        log.sizeBytes = parseLong(m.group(6));
        log.referrer = m.group(7);
        log.client = m.group(8);
        log.msecs = parseLong(m.group(9));

        String[] reqss = request.split(" ");
        if (reqss.length == 3) {
          log.verb = reqss[0].intern();
          log.path = reqss[1];
          log.http = reqss[2].intern();

        } else if (reqss.length == 2) { // no HTTP/1.x
          log.verb = reqss[0].intern();
          log.path = reqss[1];
        }
        return log;

      } else {
        // the non-enhanced log
         m = regPattern2.matcher(line);
         if (m.matches()) {
           LogReader.Log log = new LogReader.Log();
           log.ip = m.group(1);
           log.date = convertDate(m.group(3));
           String request = m.group(4);
           log.returnCode = parse(m.group(5));
           log.sizeBytes = parseLong(m.group(6));

           log.referrer = "-";
           log.client = "-";
           log.msecs = -1;

           String[] reqss = request.split(" ");
           if (reqss.length == 3) {
             log.verb = reqss[0].intern();
             log.path = reqss[1];
             log.http = reqss[2].intern();

           } else if (reqss.length == 2) { // no HTTP/1.x
             log.verb = reqss[0].intern();
             log.path = reqss[1];
           }
           return log;
         }
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Cant parse " + line);
    }
    return null;
  }

  private long convertDate(String accessDateFormat) {
    // 30/Sep/2009:23:50:47 -0600
    try {
      Date d = formatFrom.parse(accessDateFormat);
      return d.getTime(); // formatTo.toDateTimeStringISO(d);
    } catch (Throwable t) {
      System.out.printf("Bad date format = %s err = %s%n", accessDateFormat, t.getMessage());
    }
    return -1;
  }

  private int parse(String s) {
    if (s.equals("-")) return 0;
    return Integer.parseInt(s);
  }

  private long parseLong(String s) {
    if (s.equals("-")) return 0;
    return Long.parseLong(s);
  }

  // try problem logs
  public static void main(String[] args) throws IOException {
    AccessLogParser p = new AccessLogParser();
    String line = "24.18.236.132 - - [04/Feb/2011:17:49:03 -0700] \"GET /thredds/fileServer//nexrad/level3/N0R/YUX/20110205/Level3_YUX_N0R_20110205_0011.nids \" 200 10409 \"-\" \"-\" 17";
    Matcher m = regPattern.matcher(line);
    System.out.printf("%s %s%n", m.matches(), m);
    for (int i=0; i<m.groupCount(); i++) {
      System.out.println(" "+i+ " "+m.group(i));
    }

    LogReader.Log log = p.parseLog(line);
    System.out.printf("%s%n", log);
  }

}
