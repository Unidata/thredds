package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.PathMatcher;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
//import thredds.filesystem.server.LogReader;
//import thredds.filesystem.server.AccessLogParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handle the /admin/log interface
 *
 * @author caron
 * @since 4.0
 */
public class LogController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private File accessLogDirectory;
  private List<File> accessLogFiles = new ArrayList<File>(10);

  private TdsContext tdsContext;

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  public void setAccessLogDirectory(String accessLogDirectory) {
    this.accessLogDirectory = new File(accessLogDirectory);
    init();
  }

  private void init() {
    for (File f : accessLogDirectory.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("access.");
      }
    })) {

      accessLogFiles.add(f);
    }
  }

  /* private void read(String afterDate) throws IOException {
    LogReader reader = new LogReader(new AccessLogParser());

    ArrayList<LogReader.Log> completeLogs = new ArrayList<LogReader.Log>(30000);
    for (File f : accessLogFiles)
      reader.scanLogFile(f, new MyClosure(), new MyLogFilter(afterDate), null);
  } 

  private class MyLogFilter implements LogReader.LogFilter {
    String afterDate;

    MyLogFilter(String afterDate) {
      this.afterDate = afterDate;
    }

    public boolean pass(LogReader.Log log) {
      return log.getDate().compareTo(afterDate) > 0;
    }
  }

  private class MyClosure implements LogReader.Closure {

    public void process(LogReader.Log log) throws IOException {
      System.out.printf("%s%n", log.toString());
    }
  } */

  ////////
  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info(UsageLog.setupRequestContext(req));

    String path = req.getPathInfo();
    if (path == null) path = "";

    // Don't allow ".." directories in path.
    if (path.indexOf("/../") != -1
            || path.equals("..")
            || path.startsWith("../")
            || path.endsWith("/..")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Path cannot contain ..");
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return null;
    }

    File file = null;
    if (path.equals("/log/dataroots.txt")) {
      PrintWriter pw = new PrintWriter(res.getOutputStream());
      PathMatcher pathMatcher = DataRootHandler.getInstance().getPathMatcher();
      Iterator iter = pathMatcher.iterator();
      while (iter.hasNext()) {
        DataRootHandler.DataRoot ds = (DataRootHandler.DataRoot) iter.next();
        pw.format("%s%n", ds.toString2()); // path,dir
      }
      pw.flush();

    } else if (path.equals("/log/access/current")) {

      File dir = tdsContext.getTomcatLogDirectory();
      File[] files = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith("access");
        }
      });
      if ((files == null) || (files.length == 0)) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      List fileList = Arrays.asList(files);
      Collections.sort(fileList);
      file = (File) fileList.get(fileList.size() - 1); // last one

    } else if (path.equals("/log/access/")) {
      showFiles(tdsContext.getTomcatLogDirectory(), "access", res);

    } else if (path.startsWith("/log/access/")) {
      file = new File(tdsContext.getTomcatLogDirectory(), path.substring(12));
      ServletUtil.returnFile( req, res, file, "text/plain");
      return null;

    } else if (path.equals("/log/thredds/current")) {
      file = new File(tdsContext.getContentDirectory(), "logs/threddsServlet.log");

    } else if (path.equals("/log/thredds/")) {
      showFiles(new File(tdsContext.getContentDirectory(),"logs"), "thredds", res);

    } else if (path.startsWith("/log/thredds/")) {
      file = new File(tdsContext.getContentDirectory(), "logs/" + path.substring(13));
      ServletUtil.returnFile( req, res, file, "text/plain");
      return null;

    } else {
      PrintWriter pw = new PrintWriter(res.getOutputStream());
      pw.format("/log/access/current%n");
      pw.format("/log/access/%n");
      pw.format("/log/thredds/current%n");
      pw.format("/log/thredds/%n");
      pw.flush();
    }

    log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
    if (file != null)
      return new ModelAndView("threddsFileView", "file", file);
    else
      return null;
  }

  private void showFiles(File dir, final String filter, HttpServletResponse res) throws IOException {
    File[] files = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(filter);
      }
    });

    if ((files == null) || (files.length == 0)) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    List<File> fileList = Arrays.asList(files);
    Collections.sort(fileList);

    PrintWriter pw = new PrintWriter(res.getOutputStream());
    for (File f : fileList)
      pw.format("%s %d%n", f.getName(), f.length());
    pw.flush();
  }

}

/*

  protected ModelAndView save(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info("handleRequestInternal(): " + UsageLog.setupRequestContext(req));

    String path = req.getPathInfo();
    if (path == null) path = "";
    System.out.printf("path = %s %n", path);
    if (path.startsWith("/log/")) path = path.substring(5);
    System.out.printf("type = %s %n", path);

    String date = req.getParameter("since");

    try {
      DateFormatter df = new DateFormatter();
      Date d = df.getISODate(date);
      Formatter f = new Formatter();
      f.format("dateS = %s == %s == %s%n", date, d, df.toDateTimeStringISO(d));
      System.out.printf("%s", f.toString());
      res.getOutputStream().print(f.toString()); // LOOK whats easy ModelAndView ??
      read(df.toDateTimeStringISO(d));

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null; // ToDo
  }

*/