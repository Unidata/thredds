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

import ucar.nc2.util.IO;

import java.io.BufferedReader;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Read TDS access logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class ReadTdsLogs {

  // sample
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null" "Java/1.6.0_05" 21
  //

  private static Pattern regPattern =
          Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

  private int maxLines = -1;

  Log parseLine(Pattern p, String line) {
    //System.out.println("\n"+line);
    Matcher m = p.matcher(line);
    int groupno = 1;
    if (m.matches()) {
      Log log = new Log();
      log.ip = m.group(1);
      log.date = m.group(3);
      log.request = m.group(4);
      log.returnCode = parse(m.group(5));
      log.sizeBytes = parse(m.group(6));
      log.referrer = m.group(7);
      log.client = m.group(8);
      log.msecs = parse(m.group(9));
 
      String[] reqss = log.request.split(" ");
      if (reqss.length == 3) {
        log.verb = reqss[0];
        log.path = reqss[1];
        log.http = reqss[2];
      }

      return log;
    }
    System.out.println("Cant parse " + line);
    return null;
  }

  private int parse(String s) {
    if (s.equals("-")) return 0;
    return Integer.parseInt(s);
  }

  private class Log {
    String ip, date, request, referrer, client;
    int returnCode, sizeBytes, msecs;
    String verb, path, http;

    public String toCSV() {
      //return ip + "," + date + ",\"" + verb + "\","+ path + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
      return ip + "," + date + ","+ verb + ",\"" + path + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
    }

    public String toString() {
      return ip + " [" + date + "] " + verb + " " + path + " " + http + " " + returnCode + " " + sizeBytes + " " + referrer + " " + client + " " + msecs;
    }
  }

  /////////////////////////////////////////////////////////////////////

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
        if (log.msecs / 1000 > secs) {
          System.out.println("TIME " + log.msecs / 1000 + ": " + log);
          filterCount++;
        }
      }
      count++;
    }
    ios.close();
    System.out.println("total requests= " + count + " filter=" + filterCount);
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
          System.out.println("REQUEST: " + log);
          filterCount++;
        }
      }
      count++;
    }
    ios.close();
    System.out.println("total requests= " + count + " filter=" + filterCount);
  }

  static long total_sendRequest_time = 0;
  static long total_expected_time = 0;
  void sendRequests(String filename, String server, int max) throws IOException {
    int count = 0;
    long expected = 0;
    long actual = 0;

    InputStream ios = new FileInputStream(filename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    while ((max < 0) || (count < max)) {
      String line = dataIS.readLine();
      if (line == null) break;
      Log log = parseLine(regPattern, line);
      if (log == null) continue;
      count++;

      if (log.verb.equals("POST")) {
        System.out.println(" *** skip POST " + log);
        continue;
      }

      if (log.returnCode != 200) {
        System.out.println(" *** skip failure " + log);
        continue;
      }

      String urlString = server + log.path;
      System.out.print("\""+urlString+"\","+log.sizeBytes+ ","+log.msecs);
      long start = System.nanoTime();

      try {
        IO.copyUrlB(urlString, null, 10 * 1000); // read data and throw away
      } catch (Throwable t) {
        //t.printStackTrace();
        System.out.println("  FAILED ");
        continue;
      }

      long took = System.nanoTime() - start;
      total_expected_time += log.msecs;

      long msecs = took/1000/1000;
      float speedup = (msecs > 0) ? ((float)log.msecs)/msecs : 0;
      total_sendRequest_time += msecs;

      System.out.println("," + msecs+","+speedup);
    }
    ios.close();
    System.out.println("total requests= " + count);
  }


  ////////////////////////////////////////////////////////

  static int total_reqs = 0, total_bad = 0;

  void passFilter(String filename, PrintWriter out) throws IOException {
    InputStream ios = new FileInputStream(filename);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    int bad = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      Log log = parseLine(regPattern, line);
      if (log == null) {
        bad++;
        total_bad++;
        continue;
      }
      all.accum(log);
      total_reqs++;

      boolean filtered = false;
      for (Filter f : filters) {
        if (f.isMine(log)) {
          f.accum(log);
          filtered = true;
          break;
        }
      }
      if (!filtered){
        out.println(log.toCSV());
        unknown.accum(log);
      }

      count++;
    }

    ios.close();
    System.out.println(" "+filename+" requests= " + count + " bad=" + bad);
  }

  static Filter all = new All();
  static Filter unknown = new Unknown();
  static List<Filter> filters = new ArrayList<Filter>();
  static abstract class Filter {
    abstract boolean isMine(Log log);

    String name;
    int nreqs;
    long accumTime, accumBytes;

    Filter(String name) {
      this.name = name;
    }

    void accum(Log log) {
      nreqs++;
      accumTime += log.msecs;
      if (log.sizeBytes > 0)
        accumBytes += log.sizeBytes;
    }

    public void show(Formatter out) {
      double mb = .001 * .001 * accumBytes;
      double secs = .001 * accumTime;
      out.format(" %20s: %10d %10.3f %10.3f\n", name, nreqs, secs, mb);
    }
  }

  static class All extends Filter {
    All() { super("All"); }
    boolean isMine(Log log) {
      return true;
    }
  }

  static class Unknown extends Filter {
    Unknown() { super("Unknown"); }
    boolean isMine(Log log) {
      return false;
    }
  }

  static class Client extends Filter {
    String clientStartsWith;
    Client(String clientStartsWith) {
      super(clientStartsWith);
      this.clientStartsWith = clientStartsWith;
    }
    boolean isMine(Log log) {
      return log.client.startsWith(clientStartsWith);
    }
  }

  static class JUnitReqs extends Filter {
    JUnitReqs() { super("JUnitReqs"); }
    boolean isMine(Log log) {
      return log.ip.equals("128.117.140.75");
    }
  }

  static class Idv extends Filter {
    Idv() { super("IDV"); }
    boolean isMine(Log log) {
      return log.client.startsWith("IDV") || log.client.startsWith("Jakarta Commons-HttpClient");
    }
  }

  static class PostProbe extends Filter {
    PostProbe() { super("PostProbe"); }
    boolean isMine(Log log) {
      return log.request.startsWith("POST /proxy");
    }
  }

  static class FileServer extends Filter {
    FileServer() { super("FileServer"); }
    boolean isMine(Log log) {
      return log.path.startsWith("/thredds/fileServer/") &&
        log.client.startsWith("Wget") || log.client.startsWith("curl") || log.client.startsWith("Python-urllib");
    }
  }

  static class Datafed extends Filter {
    Datafed() { super("Datafed"); }
    boolean isMine(Log log) {
      return log.ip.equals("128.252.21.75") && log.path.startsWith("/thredds/wcs/");
    }
  }

  ////////////////////////////////////////////////////////

  private void test() {
    String line = "128.117.140.75 - - [01/Mar/2008:00:47:08 -0700] \"HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080229_1800.grib2.dds HTTP/1.1\" 200 - \"null\" \"Java/1.6.0_01\" 23";
    String line2 = "140.115.36.145 - - [01/Mar/2008:00:01:20 -0700] \"GET /thredds/dodsC/satellite/IR/NHEM-MULTICOMP_1km/20080229/NHEM-MULTICOMP_1km_IR_20080229_1500.gini.dods HTTP/1.0\" 200 134 \"null\" \"Wget/1.10.2 (Red Hat modified)\" 35";
    String line3 = "82.141.193.194 - - [01/May/2008:09:29:06 -0600] \"GET /thredds/wcs/galeon/testdata/sst.nc?REQUEST=GetCoverage&SERVICE=WCS&VERSION=1.0.0&COVERAGE=tos&CRS=EPSG:4326&BBOX=1,-174,359,184&WIDTH=128&HEIGHT=128&FORMAT=GeoTIFF HTTP/1.1\" 200 32441 \"null\" \"null\" 497";

    //Pattern p =
            //Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"GET(.*)\" (\\d+) (\\d+) \"(.*)\" \"(.*)\" (\\d+)");
   //         Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

    Log log = parseLine(regPattern, line3);
    if (log != null)
      System.out.println("test= " + log);

    String what = "GET /thredds/wcs/galeon/testdata/sst.nc?REQUEST=GetCoverage&SERVICE=WCS&VERSION=1.0.0&COVERAGE=tos&CRS=EPSG:4326&BBOX=1,-174,359,184&WIDTH=128&HEIGHT=128&FORMAT=GeoTIFF HTTP/1.1";
    String[] hell = what.split(" ");
  }

  static void readAllInDir(File dir, MClosure closure) {
    List list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (int i = 0; i < list.size(); i++) {
      File f = (File) list.get(i);
      if (!f.getName().endsWith("log")) continue;

      if (f.isDirectory())
        readAllInDir(f, closure);
      else {
        try {
          closure.run(f.getPath());
        } catch (Exception e) {
          System.out.println("Error on " + f + " (" + e.getMessage() + ")\n");
          e.printStackTrace();
        }
      }
    }
  }

  static void read(String filename, MClosure closure) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      System.out.println(filename + " does not exist");
      return;
    }
    if (f.isDirectory()) readAllInDir(f, closure);
    else closure.run(f.getPath());
  }

  interface MClosure {
    void run(String filename) throws IOException;
  }


  public static void main(String args[]) throws IOException {
    //new ReadTdsLogs().test();

    /* scanRequest
    read("d:/motherlode/logs/", new MClosure() {
      public void run(String filename) throws IOException {
        new ReadTdsLogs().scanRequest(filename);
      }
    }); // */

    /* scanTime
    read("d:/motherlode/logs/", new MClosure() {
      public void run(String filename) throws IOException {
        new ReadTdsLogs().scanTime(filename, 120);
      }
    });  // */

    /* passFilter
    filters.add(new Idv());
    filters.add(new Client("libdap"));
    filters.add(new Client("OpendapConnector"));
    filters.add(new Client("My World"));
    filters.add(new Client("ToolsUI"));
    filters.add(new Datafed());
    filters.add(new FileServer());
    filters.add(new JUnitReqs());
    filters.add(new Client("BigBrother"));
    filters.add(new Client("www.dlese.org"));
    filters.add(new PostProbe());
    unknown = new Unknown();
    all = new All();

    final PrintWriter pw = new PrintWriter(new FileOutputStream("C:/temp/logs.csv"));
    read("d:/motherlode/logs/access.2008-05-29.log", new MClosure() {
    //read("d:/motherlode/logs/", new MClosure() {
      public void run(String filename) throws IOException {
        new ReadTdsLogs().passFilter(filename, pw);
      }
    });
    pw.flush();
    pw.close();

    System.out.println("total bad requests= " + total_bad);
    Formatter out = new Formatter(System.out);
    out.format("\n                          nreqs       secs     Mbytes\n");
    all.show(out);
    out.format("\n");

    for (Filter f : filters)
      f.show(out);

    unknown.show(out);
    // */

    // sendRequests
    read("d:/motherlode/logs/access.2008-09-22.log", new MClosure() {
      public void run(String filename) throws IOException {
        new ReadTdsLogs().sendRequests(filename, "http://newmotherlode.ucar.edu:8080", -1);
      }
    });
    System.out.println("total_sendRequest_time= " + total_sendRequest_time/1000+" secs");
    System.out.println("total_expected_time= " + total_expected_time/1000+" secs");

    float speedup = ((float) total_expected_time)/total_sendRequest_time;
    System.out.println("speedup= " + speedup);
    // */

  }

}
