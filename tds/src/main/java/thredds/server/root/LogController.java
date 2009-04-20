package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.DebugHandler;
import thredds.filesystem.MCollection;
import thredds.filesystem.MFile;
import thredds.filesystem.server.LogReader;
import thredds.filesystem.server.AccessLogParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.units.DateFormatter;

import java.util.Date;
import java.util.Formatter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Handle the /admin/log interface
 *
 * @author caron
 * @since 4.0
 */
public class LogController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private MCollection collection;
  private File accessLogDirectory;
  private List<File> accessLogFiles = new ArrayList<File>(10);

  public void setAccessLogDirectory(String accessLogDirectory) {
    this.accessLogDirectory = new File(accessLogDirectory);
    init();
  }

    private void init() {
    for (File f : accessLogDirectory.listFiles( new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.startsWith("access.");
          }
        })) {

      accessLogFiles.add(f);
    }
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( req ) );

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



  private void read(String afterDate) throws IOException {
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
      return log.getDate().compareTo( afterDate) > 0;
    }
  }

  private class MyClosure implements LogReader.Closure {

    public void process(LogReader.Log log) throws IOException {
      System.out.printf("%s%n", log.toString());
    }
  }
}