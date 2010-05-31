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

import ucar.unidata.util.StringUtil;

import java.io.BufferedReader;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Superclass to read TDS logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class LogReader {
  private static SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  public interface LogParser {
    public Log nextLog(BufferedReader reader) throws IOException;
  }

  static public class Log {
    public String getIp() {
      return ip;
    }

    public String getDate() {
      return df.format(new Date(date));
    }

    public String getReferrer() {
      return referrer;
    }

    public String getClient() {
      return client;
    }

    public int getStatus() {
      return returnCode;
    }

    public long getMsecs() {
      return msecs;
    }

    public long getBytes() {
      return sizeBytes;
    }

    public String getPath() {
      return (path == null) ? null : StringUtil.unescape(path);
    }

    public long date;
    String verb, referrer, client;
    int returnCode;
    long msecs, sizeBytes;
    String ip, path, http;

    public String toCSV() {
      //return ip + "," + date + ",\"" + verb + "\","+ path + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
      return ip + "," + getDate() + "," + verb + ",\"" + getPath() + "\"," + returnCode + "," + sizeBytes + ",\"" + referrer + "\",\"" + client + "\"," + msecs;
    }

    public String toString() {
      return ip + " [" + getDate() + "] " + verb + " " + getPath() + " " + http + " " + returnCode + " " + sizeBytes + " " + referrer + " " + client + " " + msecs;
    }

  }

  public interface Closure {
    void process(Log log) throws IOException;
  }


  public static class Stats {
    public long total;
    public long passed;
  }

  ////////////////////////////////////////////////////////////

  public interface LogFilter {
    boolean pass(Log log);
  }

  public static class DateFilter implements LogFilter {
    long start, end;
    LogReader.LogFilter chain;

    public DateFilter(long start, long end, LogReader.LogFilter chain) {
      this.start = start;
      this.end = end;
      this.chain = chain;
    }

    public boolean pass(LogReader.Log log) {
      if (chain != null && !chain.pass(log))
        return false;

      if ((log.date < start) || (log.date > end))
        return false;

      return true;
    }
  }

  public static class IpFilter implements LogFilter {
    String[] match;
    LogReader.LogFilter chain;

    public IpFilter(String[] match, LogReader.LogFilter chain) {
      this.match = match;
      this.chain = chain;
    }

    public boolean pass(LogReader.Log log) {
      if (chain != null && !chain.pass(log))
        return false;

      for (String s : match)
        if (log.getIp().startsWith(s))
          return false;

      return true;
    }
  }

  public static class ErrorOnlyFilter implements LogFilter {
    LogReader.LogFilter chain;

    public ErrorOnlyFilter(LogReader.LogFilter chain) {
      this.chain = chain;
    }

    public boolean pass(LogReader.Log log) {
      if (chain != null && !chain.pass(log))
        return false;

      int status = log.getStatus();
      if ((status < 400) || (status >= 1000)) return false;

      return true;
    }
  }

  public static class FilterNoop implements LogFilter {
    public boolean pass(LogReader.Log log) {
      return true;
    }
  }
  /////////////////////////////////////////////////////////////////////

  private int maxLines = -1;
  private LogParser parser;

  public LogReader(LogParser parser)  {
    this.parser = parser;
  }

  /**
   * Read all the files in a directory and process them. Files are sorted by filename.
   * @param dir read from this directory
   * @param ff files must pass this filter (may be null)
   * @param closure send each Log to this closure
   * @param logf filter out these Logs (may be null)
   * @param stat accumulate statitistics (may be null)
   * @throws IOException on read error
   */
  public void readAll(File dir, FileFilter ff, Closure closure, LogFilter logf, Stats stat) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      System.out.printf("Dir has no files= %s%n", dir);
      return;
    }
    List list = Arrays.asList(files);
    Collections.sort(list);

    for (int i = 0; i < list.size(); i++) {
      File f = (File) list.get(i);
      if ((ff != null) && !ff.accept(f)) continue;

      if (f.isDirectory())
        readAll(f, ff, closure, logf, stat);
      else
        scanLogFile(f, closure, logf, stat);
    }
  }

  /**
   * Read a log file.
   * @param file file to read
   * @param closure send each Log to this closure
   * @param logf filter out these Logs (may be null)
   * @param stat accumulate statitistics (may be null)
   * @throws IOException on read error
   */
  public void scanLogFile(File file, Closure closure, LogFilter logf, Stats stat) throws IOException {
    InputStream ios = new FileInputStream(file);
    System.out.printf("-----Reading %s %n", file.getPath());

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios), 40 * 1000);
    int total = 0;
    int count = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      Log log = parser.nextLog(dataIS);
      if (log == null) break;
      total++;

      if ((logf != null) && !logf.pass(log)) continue;

      closure.process( log);
      count++;
    }

    if (stat != null) {
      stat.total += total;
      stat.passed += count;
    }

    ios.close();
    System.out.printf("----- %s total requests=%d passed=%d %n", file.getPath(), total, count);
  }


  ////////////////////////////////////////////////////////

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
    final LogReader reader = new LogReader( new AccessLogParser());

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
