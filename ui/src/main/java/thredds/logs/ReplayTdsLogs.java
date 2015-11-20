/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.util.IO;
import ucar.nc2.util.EscapeStrings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Read TDS access logs off of disk and replay the requests on a
 * new server
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class ReplayTdsLogs {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReplayTdsLogs.class);
  private static AtomicInteger reqno = new AtomicInteger(0);
  private static Formatter out, out2;
  private static final String USER_AGENT = "ReplayTdsLogs testing utility from toolsUI";
  private static final String URI_DEFAULT_SCHEME_SPECIFIC_PART = "http://";
  ///////////////////////////////////////////////////////
  // multithreading
  final int nthreads = 2;

  ExecutorService executor;
  ExecutorCompletionService<SendRequestTask> completionService;
  //ArrayBlockingQueue<Future<SendRequestTask>> completionQ;
  Thread resultProcessingThread;

  AtomicLong regProcess = new AtomicLong();
  //AtomicLong total_requests = new AtomicLong();
  AtomicLong total_sendRequest_time = new AtomicLong();
  AtomicLong total_expected_time = new AtomicLong();

  final String server;
  boolean dump = false;

  ReplayTdsLogs(String server) throws FileNotFoundException, HTTPException {
    this.server = server;
    // httpClient = HttpClientManager.init(null, "ReplayTdsLogs");

    executor = Executors.newFixedThreadPool(nthreads); // number of threads
    //completionQ = new ArrayBlockingQueue<Future<SendRequestTask>>(30); // bounded, threadsafe
    completionService = new ExecutorCompletionService<SendRequestTask>(executor);

    // out = new Formatter(new FileOutputStream("C:/TEMP/readTdsLogs.txt"));

    resultProcessingThread = new Thread(new ResultProcessor());
    resultProcessingThread.start();
  }

  public class SendRequestTask implements Callable<SendRequestTask> {
    Log log;

    long statusCode;
    long bytesRead;
    boolean failed = false;
    String failMessage;
    long msecs;
    long reqnum;

    SendRequestTask(Log log) {
      this.log = log;
    }

    public SendRequestTask call() throws Exception {
      long start = System.nanoTime();

      reqnum = reqno.incrementAndGet();
      if (reqnum % 100 == 0)
        System.out.println(reqnum + " request= " + log);

      try {
        send();

        long took = System.nanoTime() - start;
        msecs = took / 1000 / 1000;

        total_sendRequest_time.getAndAdd(msecs);
        total_expected_time.getAndAdd((long) log.msecs);

      } catch (Throwable t) {
        t.printStackTrace();
        failed = true;
        failMessage = t.getMessage();
      }

      return this;
    }

    void send() throws IOException {
      String unescapedForm = EscapeStrings.unescapeURL(log.path); // make sure its unescaped
      //if (!unescapedForm.equals(log.path))
      //  System.out.printf("org=%s unescaped=%s%n", log.path, unescapedForm);
      String urlencoded = server + unescapedForm;
      // String urlencoded = server + URLnaming.escapeQuery(unescapedForm);
      try (HTTPMethod method = HTTPFactory.Get(urlencoded)) {  // escape the query part
        method.setRequestHeader(HTTPSession.HEADER_USERAGENT, USER_AGENT);
        //out2.format("send %s %n", method.getPath());
        statusCode = method.execute();

        InputStream is = method.getResponseBodyAsStream();
        if (is != null) {
          bytesRead = IO.copy2null(is, 10 * 1000); // read data and throw away
        }
      }
    }
  }

  private class ResultProcessor implements Runnable {
    private boolean cancel = false;

    public void run() {
      while (true) {
        try {
          Future<SendRequestTask> f = completionService.poll(); // see if ones ready
          if (f == null) {
            if (cancel) break; // check if interrupted
            f = completionService.take(); // block until ready
          }

          //long reqno = total_requests.getAndIncrement();
          SendRequestTask itask = f.get();
          Log log = itask.log;
          String urlString = server + log.path;
          //out.format("%d,\"%s\",%d,%d", reqno, urlString, log.sizeBytes, log.msecs);
          if (dump) System.out.printf("\"%s\",%d,%d", urlString, log.sizeBytes, log.msecs);
          float speedup = (itask.msecs > 0) ? ((float) log.msecs) / itask.msecs : 0;

          //out.format(",%d,%f,%s%n", itask.msecs, speedup, itask.failed);
          if (itask.failed)
            System.out.printf("***FAIL %s %s %n", log.path, itask.failMessage);

          else if ((itask.statusCode != log.returnCode) && (log.returnCode != 304) && (log.returnCode != 302)) {
            if (!compareAgainstLive(itask)) {
              if (out != null) out.format("%5d: status=%d was=%d %s  %n", itask.reqnum, itask.statusCode, log.returnCode, log.path);
              out2.format("%5d: status=%d was=%d %s  %n", itask.reqnum, itask.statusCode, log.returnCode, log.path);
            }

          } else if ((itask.statusCode == 200) && (itask.bytesRead != log.sizeBytes)) {
            if (out != null) out.format("%5d: bytes=%d was=%d %s%n", itask.reqnum, itask.bytesRead, log.sizeBytes, log.path);
            // out2.format("%5d: bytes=%d was=%d %s%n", reqno, itask.bytesRead, log.sizeBytes, log.path);
          }

          if (dump) System.out.printf(",%d,%f,%s%n", itask.msecs, speedup, itask.failed);

        } catch (InterruptedException e) {
          cancel = true;

        } catch (Exception e) {
          logger.error("ResultProcessor ", e);
        }
      }
      System.out.println("exit ResultProcessor");
    }

    private boolean compareAgainstLive(SendRequestTask itask) throws IOException {
      if (serverLive == null) return true;
      try (HTTPMethod method = HTTPFactory.Get(serverLive + itask.log.path)) {
        out2.format("send %s %n", method.getPath());
        method.setRequestHeader(HTTPSession.HEADER_USERAGENT, USER_AGENT);
        int statusCode = method.execute();

        InputStream is = method.getResponseBodyAsStream();
        if (is != null)
          IO.copy2null(is, 10 * 1000); // read data and throw away

        // out2.format("%5d: test status=%d live=%d %n", itask.reqnum, itask.statusCode, statusCode);
        return statusCode == itask.statusCode;

      }

    }
  }

  public void exit(int secs) throws IOException {

    executor.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait for existing tasks to terminate
      if (!executor.awaitTermination(secs, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // Cancel currently executing tasks                       
        // Wait a while for tasks to respond to being cancelled
        if (!executor.awaitTermination(secs, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      System.out.println("exit interrupted");
      // (Re-)Cancel if current thread also interrupted
      executor.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
    System.out.println("executor terminated");

    System.out.println("total requests= " + reqno.get());
    System.out.println("total_sendRequest_time= " + total_sendRequest_time.get() / 1000 + " secs");
    System.out.println("total_expected_time= " + total_expected_time.get() / 1000 + " secs");

    float speedup = ((float) total_expected_time.get()) / total_sendRequest_time.get();
    System.out.println("speedup= " + speedup);
    //out.close();
  }

  ///////////////////////////////////////////////////////
  // log reading

  // sample
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null" "Java/1.6.0_05" 21
  //

  private static Pattern regPattern =
          Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

  private int maxLines = -1; // -1 for all

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

  private class Log {
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


  static private int total_submit = 0;
  static private int skip_submit = -1;
  static private int max_submit = Integer.MAX_VALUE;

  void sendRequests(String filename, int max) throws IOException {
    int submit = 0;
    long skip = 0;
    long total = 0;

    InputStream ios = new FileInputStream(filename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    while ((max < 0) || (submit < max)) {
      String line = dataIS.readLine();
      if (line == null) break;
      Log log = parseLine(regPattern, line);
      if (log == null) continue;
      if (log.path == null) continue;
      total++;

      if ((total + total_submit < skip_submit) || (total + total_submit > max_submit)) {
        skip++;
        continue;
      }

      if (log.verb != null && log.verb.equals("POST")) {
        skip++;
        // System.out.println(" *** skip POST " + log);
        continue;
      }

     /* if (!(log.path.indexOf("wcs") > 0) && !(log.path.indexOf("wms") > 0))  {    // wcs/wms only
       skip++;
       continue;
     }   */

      /* if (!log.path.contains("ncss")) {
        // System.out.println(" *** skip fmrc " + log);
        skip++;
        continue;
      }

      if (log.path.indexOf("fileServer") > 0) {
        // System.out.println(" *** skip fmrc " + log);
        skip++;
        continue;
      }  */

      if (log.path.indexOf("manager") > 0) {
        skip++;
        continue;
      }

      if (log.path.indexOf("admin") > 0) {
        skip++;
        continue;
      }

      if (log.path.indexOf("/dts/") > 0) {  // skip dts
        skip++;
        continue;
      }

      /* if (log.path.indexOf("fmrc") > 0)  {  // exclude fmrc
        // System.out.println(" *** skip fmrc " + log);
        skip++;
        continue;
      } */

      if (log.returnCode != 200) {
        skip++;
        // System.out.println(" *** skip failure " + log);
        continue;
      }

      completionService.submit(new SendRequestTask(log));
      submit++;
    }
    ios.close();
    System.out.println("total requests= " + total + " skip= " + skip + " submit = " + submit);
    total_submit += submit;
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
      if (!filtered) {
        out.println(log.toCSV());
        unknown.accum(log);
      }

      count++;
    }

    ios.close();
    System.out.println(" " + filename + " requests= " + count + " bad=" + bad);
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
      out.format(" %20s: %10d %10.3f %10.3f%n", name, nreqs, secs, mb);
    }
  }

  static class All extends Filter {
    All() {
      super("All");
    }

    boolean isMine(Log log) {
      return true;
    }
  }

  static class Unknown extends Filter {
    Unknown() {
      super("Unknown");
    }

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
    JUnitReqs() {
      super("JUnitReqs");
    }

    boolean isMine(Log log) {
      return log.ip.equals("128.117.140.75");
    }
  }

  static class Idv extends Filter {
    Idv() {
      super("IDV");
    }

    boolean isMine(Log log) {
      return log.client.startsWith("IDV") || log.client.startsWith("Jakarta Commons-HttpClient");
    }
  }

  static class PostProbe extends Filter {
    PostProbe() {
      super("PostProbe");
    }

    boolean isMine(Log log) {
      return log.request.startsWith("POST /proxy");
    }
  }

  static class FileServer extends Filter {
    FileServer() {
      super("FileServer");
    }

    boolean isMine(Log log) {
      return log.path.startsWith("/thredds/fileServer/") &&
              log.client.startsWith("Wget") || log.client.startsWith("curl") || log.client.startsWith("Python-urllib");
    }
  }

  static class Datafed extends Filter {
    Datafed() {
      super("Datafed");
    }

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

  static String serverLive = null;

  public static void main(String args[]) throws IOException {
    int EXIT_NORMAL = 0;
    int USAGE_ERROR = 64;
    int HOST_ERROR = 68;
    int IO_ERROR = 74;

    //todo out appears to be useless in terms of the utility of this class. look at removing
    //  to clean up code.
    out = null; // new Formatter(new FileOutputStream("C:/TEMP/readTdsLogs.txt"));
    out2 = new Formatter(System.out);

    // Basic usage message
    List<String> message = Arrays.asList(" ",
            "ReplayTdsLogs takes two arguments.%n",
            "  The first argument is the URL of the server to test (e.g. http://my.thredds.edu)%n",
            "  The second argument is the path of the directory which holds the log files you wish " +
                    "to replay against the test server. (e.g. /my/testlogs/live/here)%n",
            "%n",
            "  Example usage:%n",
            "java -classpath toolsUI.jar thredds.logs.ReplayTdsLogs http://my.thredds.edu /my/testlogs/live/here %n");

    // Check command line arguments for basic issues (too many, to few, none)
    if (args.length == 0) {
      for (String line : message) {
        System.out.format(line);
      }
      System.exit(EXIT_NORMAL);
    } else if (args.length != 2) {
      for (String line : message) {
        System.err.format(line);
      }
      System.exit(USAGE_ERROR);
    }

    // The command line args for this should be strings
    String serverToTest = args[0].toString();
    String accessLogsToTestWith = args[1].toString();


    // Check testServer url for potential issues with the first command line arg
    if (!serverToTest.startsWith(URI_DEFAULT_SCHEME_SPECIFIC_PART)) {
      serverToTest = URI_DEFAULT_SCHEME_SPECIFIC_PART + serverToTest;
    }

    try (HTTPMethod method = HTTPFactory.Head(serverToTest)) {
      // Grab url string after it's been cleaned up by HTTPfactory.Head...the string verion of the
      // url is used elsewhere in this code.
      // todo - replace string version of the url with something more robust, like a URL object.
      serverToTest =  method.getSession().getSessionURL();
      method.setRequestHeader(HTTPSession.HEADER_USERAGENT, USER_AGENT);
      method.execute();
      method.close();
    } catch (HTTPException httpe) {
      String errorMessage = httpe.getLocalizedMessage();
      System.err.format("Error related to test server %s:%n", serverToTest);
      System.err.format("%n");
      if (errorMessage.toLowerCase().contains("unknownhostexception")) {
        System.err.format("The hostname %s is incorrect, or the host may be down.%n", serverToTest);
        System.exit(HOST_ERROR);
      } else {
        System.err.format("%s%n", httpe.getMessage());
      }
      System.exit(USAGE_ERROR);
    }

    // use Path object to do more specific testing on second command line arg
    Path pathToTestDir = Paths.get(accessLogsToTestWith);
    try {
      pathToTestDir = pathToTestDir.toRealPath();
      if (!Files.isDirectory(pathToTestDir)) {
        System.err.format("The second argument %s is not a directory.", pathToTestDir.toString());
        System.exit(USAGE_ERROR);
      }

      try (Stream<Path> potential_log_files = Files.list(pathToTestDir)) {
        if (!potential_log_files.iterator().hasNext()) {
          System.err.format("The directory %s appears to be empty.", pathToTestDir.toString());
          System.exit(EXIT_NORMAL);
        }
      }
    } catch (NoSuchFileException nsfe) {
      System.err.format("The directory %s does not exists.", pathToTestDir.toString());
      System.exit(EXIT_NORMAL);
    } catch (IOException ioe) {
      System.err.format("%s%n", ioe);
      System.exit(IO_ERROR);
    }


    // sendRequests
    final ReplayTdsLogs reader = new ReplayTdsLogs(serverToTest);
    long startElapsed = System.nanoTime();

    System.out.format("%ntest server=%s sending request for log files in the directory %s %n%n", serverToTest, accessLogsToTestWith);

    read(accessLogsToTestWith, new MClosure() {
      public void run(String filename) throws IOException {
        reader.sendRequests(filename, -1);
      }
    });
    System.out.println("total_submit= " + total_submit);
    reader.exit(24 * 3600); // 24 hours

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.format("elapsed= %d secs%n", elapsedTime / (1000 * 1000 * 1000));

    if (out != null) out.close();
    out2.close();

    System.exit(EXIT_NORMAL);
  }
}
