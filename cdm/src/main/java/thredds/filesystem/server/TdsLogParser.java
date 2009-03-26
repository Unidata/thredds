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

package thredds.filesystem.server;

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
public class TdsLogParser {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TdsLogParser.class);

  class Log {
    String ip, date, request, referrer, client;
    int returnCode;
    long msecs, sizeBytes;
    String verb, path, http;

    public String toCSV() {
      //return ip + "," + date + ",\"" + verb + "\","+ path + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
      return ip + "," + date + "," + verb + ",\"" + path + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
    }

    public String toString() {
      return ip + " [" + date + "] " + verb + " " + path + " " + http + " " + returnCode + " " + sizeBytes + " " + referrer + " " + client + " " + msecs;
    }
  }

  public interface Closure {
    void process(Log log) throws IOException;
  }

  public interface LogFilter {
    boolean pass(Log log);
  }

  public static class Stats {
    long total;
    long passed;
  }

  ///////////////////////////////////////////////////////
  // log reading

  // sample
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null" "Java/1.6.0_05" 21
  //

  private static Pattern regPattern =
          Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

  private int maxLines = -1;

  Log parseLine(Pattern p, String line) {
    try {
      //System.out.println("\n"+line);
      Matcher m = p.matcher(line);
      int groupno = 1;
      if (m.matches()) {
        Log log = new Log();
        log.ip = m.group(1);
        log.date = m.group(3);
        log.request = m.group(4);
        log.returnCode = parse(m.group(5));
        log.sizeBytes = parseLong(m.group(6));
        log.referrer = m.group(7);
        log.client = m.group(8);
        log.msecs = parseLong(m.group(9));

        String[] reqss = log.request.split(" ");
        if (reqss.length == 3) {
          log.verb = reqss[0];
          log.path = reqss[1];
          log.http = reqss[2];
        }

        return log;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Cant parse " + line);
    }
    return null;
  }

  private int parse(String s) {
    if (s.equals("-")) return 0;
    return Integer.parseInt(s);
  }

  private long parseLong(String s) {
    if (s.equals("-")) return 0;
    return Long.parseLong(s);
  }

  /////////////////////////////////////////////////////////////////////


  void readAll(File dir, FileFilter ff, Closure closure, LogFilter logf, Stats stat) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      System.out.printf("Dir has no files= %s%n", dir);
      return;
    }
    List list = Arrays.asList(files);
    Collections.sort(list);

    for (int i = 0; i < list.size(); i++) {
      File f = (File) list.get(i);
      if (!ff.accept(f)) continue;

      if (f.isDirectory())
        readAll(f, ff, closure, logf, stat);
      else
        scanLogFile(f.getPath(), closure, logf, stat);
    }
  }

  void scanLogFile(String filename, Closure closure, LogFilter filter, Stats stat) throws IOException {
    InputStream ios = new FileInputStream(filename);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int total = 0;
    int count = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      total++;

      Log log = parseLine(regPattern, line);
      if (log == null) continue;
      if ((filter != null) && !filter.pass(log)) continue;

      closure.process( log);
      count++;
    }
    stat.total += total;
    stat.passed += count;

    ios.close();
    System.out.printf("%s total requests=%d passed=%d %n", filename, total, count);
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

  static class MyFilter implements LogFilter {

    public boolean pass(Log log) {
      return log.path.startsWith("/thredds/catalog/");
    }
  }

  static class MyFF implements FileFilter {

    public boolean accept(File f) {
      return f.getPath().endsWith(".log");
    }
  }

  public static void main(String args[]) throws IOException {
    // test
    final TdsLogParser reader = new TdsLogParser();

    long startElapsed = System.nanoTime();
    Stats stats = new Stats();

    reader.readAll(new File("d:/motherlode/logs/all/"), new MyFF(), new Closure() {
      long count = 0;
      public void process(Log log) throws IOException {
        if (count % 1000 == 0) System.out.printf("%s %s %s%n", log.path, log.client, log.ip);
        count++;
      }
    }, new MyFilter(), stats);

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%d secs%n", elapsedTime / (1000 * 1000 * 1000));
  }
}
