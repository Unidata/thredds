/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package timing;

import java.io.BufferedReader;
import java.io.*;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class ReadTdsLogs {

  private static Pattern regPattern =
     Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");
     //Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) (\\d+) \"(.*)\" \"(.*)\" (\\d+)");
  int maxLines = -1;

   void makeCSV(String filename, PrintWriter out) throws IOException {
    InputStream ios = new FileInputStream(filename);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    int filterCount = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      Log log = parseLine(regPattern, line);
      if (log != null) {
        out.println(log.toCSV());
      }
      count++;
    }
    ios.close();
    System.out.println("total requests= "+count+" filter="+filterCount);
  }


  void scanTime(String filename, int secs) throws IOException {
    InputStream ios = new FileInputStream(filename);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    int filterCount = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      Log log = parseLine(regPattern, line);
      if (log != null) {
        if (log.msecs/1000 > secs) {
          System.out.println("TIME "+log.msecs/1000+": "+log);
          filterCount++;
        }
      }
      count++;
    }
    ios.close();
    System.out.println("total requests= "+count+" filter="+filterCount);
  }

  void scanRequest(String filename) throws IOException {
    InputStream ios = new FileInputStream(filename);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    int filterCount = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      Log log = parseLine(regPattern, line);
      if (log != null) {
        if (!log.request.startsWith("GET") && !log.request.startsWith("HEAD")) {
          System.out.println("REQUEST: "+log);
          filterCount++;
        }
      }
      count++;
    }
    ios.close();
    System.out.println("total requests= "+count+" filter="+filterCount);
  }


  Log parseLine(Pattern p, String line) {
    //System.out.println("\n"+line);
    Matcher m = p.matcher(line);
    int groupno = 1;
    if (m.matches()) {
      Log log = new Log();
      log.ip = m.group(1);
      log.date = m.group(3);
      log.request = m.group(4);
      log.returnCode = parse( m.group(5));
      log.sizeBytes = parse( m.group(6));
      log.referrer = m.group(7);
      log.client = m.group(8);
      log.msecs = parse( m.group(9));
      return log;
      //int ngroups = m.groupCount();
      //for (int i=1;i<=ngroups; i++)
      //  System.out.println(i+"= [" + m.group( i) + "]");
    }
    System.out.println("Cant parse "+line);
    return null;
  }

  private int parse(String s) {
    if (s.equals("-")) return 0;
    return Integer.parseInt( s);
  }

  private class Log {
    String ip,date,request,referrer,client;
    int returnCode, sizeBytes, msecs;

    public String toCSV() {
      return ip+","+date+",\""+request+"\","+returnCode+","+sizeBytes+",\""+referrer+"\",\""+client+"\","+msecs;
    }

    public String toString() {
      return ip+" ["+date+"] "+request+" "+returnCode+" "+sizeBytes+" "+referrer+" "+client+" "+msecs;
    }
  }

  private void test() {
    String line= "128.117.140.75 - - [01/Mar/2008:00:47:08 -0700] \"HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080229_1800.grib2.dds HTTP/1.1\" 200 - \"null\" \"Java/1.6.0_01\" 23";
    String line2 = "140.115.36.145 - - [01/Mar/2008:00:01:20 -0700] \"GET /thredds/dodsC/satellite/IR/NHEM-MULTICOMP_1km/20080229/NHEM-MULTICOMP_1km_IR_20080229_1500.gini.dods HTTP/1.0\" 200 134 \"null\" \"Wget/1.10.2 (Red Hat modified)\" 35";
    Pattern p =
       //Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"GET(.*)\" (\\d+) (\\d+) \"(.*)\" \"(.*)\" (\\d+)");
       Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

    Log log = parseLine(p, line);
    if (log != null)
      System.out.println("test= "+log);
  }

  private void makeCSV() throws IOException {
    ReadTdsLogs rl = new ReadTdsLogs();

    PrintWriter pw = new PrintWriter(new FileOutputStream("C:/temp/logs.csv"));
    rl.makeCSV("C:/TEMP/threddsLogs/access.2008-03.log", pw);
    pw.flush();
    pw.close();
  }

  static void testAllInDir(String dirName) {
    File dir = new File(dirName);
    List list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (int i=0; i<list.size(); i++) {
      File f = (File) list.get(i);
      if (!f.getName().endsWith("log")) continue;

      if (f.isDirectory())
        testAllInDir(f.getPath());
      else {
        try {
          //new ReadTdsLogs().scanRequest(f.getPath());
          new ReadTdsLogs().scanTime(f.getPath(), 120);
        } catch (Exception e) {
          System.out.println("Error on " + f + " (" + e.getMessage()+")\n");
          e.printStackTrace();
        }
      }
    }
  }


  public static void main(String args[]) throws IOException {
    ReadTdsLogs rl = new ReadTdsLogs();
    testAllInDir("C:/TEMP/threddsLogs/");
    //rl.scanRequest("C:/TEMP/threddsLogs/access.2008-03.log");
  }

}
