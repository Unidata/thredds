/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.logs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;
import java.util.Formatter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.net.URLDecoder;

/**
 * Read TDS access logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class ServletLogParser implements LogReader.LogParser {
  static public class ServletLog extends LogReader.Log {
    public long getReqTime() {
      return reqTime;
    }

    public long getReqSeq() {
      return reqSeq;
    }

    public String getLevel() {
      return level;
    }

    public boolean isExtra() {
      return extra != null;
    }

    public boolean isDone() {
      return isDone;
    }

    public boolean isStart() {
      return isStart;
    }

    long reqTime;
    long reqSeq;
    String level, where;
    public StringBuilder extra;
    boolean isDone, isStart;

    public String toString() {
      Formatter f = new Formatter();
      f.format("%s [%d] [%d] %s %s: ", getDate(), reqTime, reqSeq, level, where);

      if (isStart)
        f.format(" (%s) %s %n", ip, getPath());
      else if (isDone)
        f.format(" %d %d %d %n", returnCode, sizeBytes, msecs);

      if (extra != null)
        f.format(" %s", extra);

      return f.toString();
    }

    void addExtra(String s) {
      if (extra == null) extra = new StringBuilder(300);
      extra.append(s);
      extra.append("\n");
    }

  }

  ///////////////////////////////////////////////////////
  // log reading

  // sample
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null" "Java/1.6.0_05" 21
  //           Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

  // 2009-03-10T16:08:55.184 -0600 [  16621850][  162233] INFO  - thredds.server.opendap.NcDODSServlet - Remote host: 128.117.140.71 - Request: "GET /thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20090309_0000.grib1.dods?Geopotential_height%5B7:1:7%5D%5B0:10:10%5D%5B0:1:64%5D%5B0:1:92%5D HTTP/1.1"
  // 2009-03-10T16:08:55.218 -0600 [  16621884][  162233] INFO  - thredds.server.opendap.NcDODSServlet - Request Completed - 200 - -1 - 34
  //
  /*
   1 2009-03-10T16:08:54.617 -0600
   2   16621283
   3   162230
   4 INFO
   5 thredds.server.opendap.NcDODSServlet
   6 Request Completed - 200 - -1 - 47
   */

  static private final Pattern donePattern = Pattern.compile("^Request Completed - (.*) - (.*) - (.*)");
  static private final Pattern startPattern = Pattern.compile("^Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");
  static private final Pattern commonPattern = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - (.*)");

  int count = 0, limit = 10;

  /*
  Difficult thing is to return the extra line assocated with the previous good log
  We do this by not returning until we get a match on the next log. We have to rewind.
   */
  public LogReader.Log nextLog(BufferedReader dataIS) throws IOException {

    ServletLog log = new ServletLog();
    boolean haveLog = false;

    while (true) {
      dataIS.mark(20 * 1000); // track where we are
      String line = dataIS.readLine();
      if (line == null) {
        return haveLog ? log : null;
      }
      // if (count++ < limit) System.out.println("\n" + line);

      try {
        Matcher m = commonPattern.matcher(line);
        if (m.matches()) {
          if (haveLog) { // have a log, next one matches, proceed
            try {
              dataIS.reset();
              return log;

            } catch (Throwable t) {
              System.out.println("Cant reset " + line);              
            }
          }
          haveLog = true; // next match will return the current log

          log.date = convertDate( m.group(1));
          log.reqTime = parseLong(m.group(2));
          log.reqSeq = parseLong(m.group(3));
          log.level = m.group(4).intern();
          log.where = m.group(5).intern();

          String rest = m.group(6);
          if (rest.indexOf("Request Completed") >= 0) {
            int pos = rest.indexOf("Request Completed");
            Matcher m2 = donePattern.matcher(rest.substring(pos));
            if (m2.matches()) {
              log.returnCode = parse(m2.group(1));
              log.sizeBytes = parseLong(m2.group(2));
              log.msecs = parseLong(m2.group(3));
              log.isDone = true;

            } else {
              System.out.println("Cant parse donePattern= " + rest);
              System.out.println(" line= " + line);
              log.addExtra(rest);
            }

          } else if (rest.indexOf("Remote host") >= 0) {
            int pos = rest.indexOf("Remote host");
            Matcher m2 = startPattern.matcher(rest.substring(pos));
            if (m2.matches()) {
              log.ip = m2.group(1).intern();
              log.verb = m2.group(2).intern();
              log.path = URLDecoder.decode(m2.group(3)).intern();
              log.http = m2.group(4).intern();
              log.isStart = true;

            } else {
              System.out.println("Cant parse startPattern= " + rest);
              System.out.println(" line= " + line);
              log.addExtra(rest);
            }

          } else { // a non-start, non-done log
            log.addExtra(rest);
          }

        } else { // the true extra line
          //System.out.println("No match on " + line);
          log.addExtra(line);
        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Cant parse " + line);
        log.addExtra(line);
      }
    }

  }

  // 2010-04-21T13:05:22.006 -0600
  private SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private long convertDate(String accessDateFormat) {
    // 30/Sep/2009:23:50:47 -0600
    try {
      Date d = df.parse(accessDateFormat);
      return d.getTime();
    } catch (Throwable t) {
      System.out.printf("Bad date format = %s err = %s%n", accessDateFormat, t.getMessage());
    }
    return -1;
  }


  private int parse(String s) {
    if (s.equals("-")) return 0;
    return Integer.parseInt(s.trim());
  }

  private long parseLong(String s) {
    if (s.equals("-")) return 0;
    return Long.parseLong(s.trim());
  }

  static class MyLogFilter implements LogReader.LogFilter {
    int count = 0;
    int limit;

    MyLogFilter(int limit) {
      this.limit = limit;
    }

    public boolean pass(LogReader.Log log) {
      return (limit < 0) || count++ < limit;
    }
  }

  public static void main1(String args[]) throws IOException {
    // test
    final LogReader reader = new LogReader(new ServletLogParser());

    long startElapsed = System.nanoTime();
    LogReader.Stats stats = new LogReader.Stats();

    reader.readAll(new File("D:/motherlode/logs/servlet/"), null, new LogReader.Closure() {
      public void process(LogReader.Log log) throws IOException {
        //if (count < limit) System.out.printf(" %s %n", log);
      }
    }, new MyLogFilter(-1), stats);

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%d secs%n", elapsedTime / (1000 * 1000 * 1000));
  }

  public static void main2(String args[]) {
    //           1                                2           3       4       5                                                   6                           7   8                                                                                                                                                    9
    String s1 = "2009-03-10T16:08:55.184 -0600 [  16621850][  162233] INFO  - thredds.server.opendap.NcDODSServlet - Remote host: 128.117.140.71 - Request: \"GET /thredds/dodsC/model/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20090309_0000.grib1.dods?Geopotential_height%5B7:1:7%5D%5B0:10:10%5D%5B0:1:64%5D%5B0:1:92%5D HTTP/1.1\"";
    //                             1                                              2       3      4             5                      6                    7      8    9
    Pattern p1 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");

    show(s1, p1);

    //           1                                2           3       4       5                                                           6    7    8                                                                                                                                                                      9
    String s2 = "2009-03-10T16:08:54.617 -0600 [  16621283][  162230] INFO  - thredds.server.opendap.NcDODSServlet - Request Completed - 200 - -1 - 47";
    //                             1                                               2       3     4              5                            6        7       8                                                                                                                                              9
    Pattern p2 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Request Completed - (\\d+) - (.*) - (.*)");
//  Pattern p1 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");

    show(s2, p2);

//  Pattern p2 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Request Completed - (\\d+) - (.*) - (.*)");
//  Pattern p1 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - Remote host: ([^-]+) - Request: \"(\\w+) (.*) (.*)");
    Pattern p3 = Pattern.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ -\\d+) \\[(.*)]\\[(.*)] (\\w+)[\\s]+- ([^-]+) - (.*)");

    show(s1, p3);
    show(s2, p3);

  }

  public static void main(String args[]) {

      //           1                                 2            3       4     5                                          6                           7   8                                                                                                                                                    9
    String test = "2009-04-03T21:48:54.000 -0600 [   4432114][    1184] INFO  - thredds.servlet.ServletUtil - Remote host: 128.117.140.75 - Request: \"GET /thredds/dodsC/fmrc/NCEP/NAM/CONUS_20km/surface/forecast/NCEP-NAM-CONUS_20km-surface_ConstantForecast_2009-04-04T15:00:00Z.dods?time,time_run,time_offset,Lambert_Conformal,pressure_difference_layer_bounds,depth_below_surface_layer_bounds,pressure_difference_layer1_bounds,depth_below_surface_layer1_bounds,depth_below_surface_layer2_bounds,pressure_difference_layer2_bounds,pressure_difference_layer3_bounds,pressure_difference_layer4_bounds,pressure_difference_layer5_bounds,height_above_ground_layer1_bounds,pressure_layer_bounds,height_above_ground_layer_bounds,y,x,height_above_ground1,height_above_ground2,pressure_difference_layer,depth_below_surface_layer,pressure_difference_layer1,depth_below_surface_layer1,depth_below_surface_layer2,height_above_ground,pressure_difference_layer2,height_above_ground3,pressure,depth_below_surface,pressure1,pressure_difference_layer3,pressure_difference_layer4,pressure_difference_layer5,height_above_ground_layer1,pressure2,pressure_layer,height_above_ground_layer HTTP/1.1\"";
    show(test, commonPattern);
    
    String test2 = "Remote host: 128.117.140.75 - Request: \"GET /thredds/dodsC/fmrc/NCEP/NAM/CONUS_20km/surface/forecast/NCEP-NAM-CONUS_20km-surface_ConstantForecast_2009-04-04T15:00:00Z.dods?time,time_run,time_offset,Lambert_Conformal,pressure_difference_layer_bounds,depth_below_surface_layer_bounds,pressure_difference_layer1_bounds,depth_below_surface_layer1_bounds,depth_below_surface_layer2_bounds,pressure_difference_layer2_bounds,pressure_difference_layer3_bounds,pressure_difference_layer4_bounds,pressure_difference_layer5_bounds,height_above_ground_layer1_bounds,pressure_layer_bounds,height_above_ground_layer_bounds,y,x,height_above_ground1,height_above_ground2,pressure_difference_layer,depth_below_surface_layer,pressure_difference_layer1,depth_below_surface_layer1,depth_below_surface_layer2,height_above_ground,pressure_difference_layer2,height_above_ground3,pressure,depth_below_surface,pressure1,pressure_difference_layer3,pressure_difference_layer4,pressure_difference_layer5,height_above_ground_layer1,pressure2,pressure_layer,height_above_ground_layer HTTP/1.1\"";
    show(test2, startPattern);

  }

  private static void show(String s, Pattern p) {
    System.out.println("==============================");
    Matcher m = p.matcher(s);
    System.out.printf(" match against %s = %s %n", m, m.matches());
    if (!m.matches()) return;
    for (int i = 1; i <= m.groupCount(); i++)
      System.out.println(" " + i + " " + m.group(i));

  }
}
