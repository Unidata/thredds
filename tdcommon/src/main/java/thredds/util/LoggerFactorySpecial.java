package thredds.util;

import org.apache.log4j.*;
import org.slf4j.Logger;
import ucar.nc2.util.log.LoggerFactory;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A LoggerFactory that uses log4j to create and configure a special RollingFileAppender
 * specific to this name.
 * used by InvDatasetFeatureCollection to create a log for each feature collection.
 * This duplicates thredds.util.LoggerFactorySpecial in tds module
 *
 * @author caron
 * @since 3/27/13
 */
public class LoggerFactorySpecial implements LoggerFactory {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private String dir = "./";
  private long maxSize;
  private int maxBackupIndex;
  private Level level = Level.INFO;

  public LoggerFactorySpecial(long maxSize, int maxBackupIndex, String levels) {
    String p = System.getProperty("tds.log.dir");
    if (p != null) dir = p;

    this.maxSize =  maxSize;
    this.maxBackupIndex =  maxBackupIndex;
    try {
      Level tlevel = Level.toLevel(levels);
      if (tlevel != null) level = tlevel;
    } catch (Exception e) {
      startupLog.error("Illegal Logger level="+levels);
    }
  }

  private static Map<String, Logger> map = new HashMap<String, Logger>();

  @Override
  public Logger getLogger(String name) {
    name = StringUtil2.replace(name.trim(), ' ', "_");
    Logger result = map.get(name);
    if (result != null) return result;

    try {
      String fileName = dir + "/" + name + ".log";

      //create logger in log4j
      Layout layout = new PatternLayout("%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} %-5p - %m%n");

      RollingFileAppender app = new RollingFileAppender(layout, fileName);
      app.setMaxBackupIndex(maxBackupIndex);
      app.setMaximumFileSize(maxSize);
      app.setFile(fileName);
      app.activateOptions();

      org.apache.log4j.Logger log4j = LogManager.getLogger(name);
      log4j.addAppender(app);
      log4j.setLevel(level);
      log4j.setAdditivity(false); // otherwise, it also gets sent to root logger (threddsServlet.log)

      startupLog.info("LoggerFactorySpecial add logger= {} file= {}", name, fileName);

      result = org.slf4j.LoggerFactory.getLogger(name); // get wrapper in slf4j
      map.put(name, result);
      return result;

    } catch (IOException ioe) {
      startupLog.error("LoggerFactorySpecial failed on " + name, ioe);

      // standard slf4j - rely on external configuration
      return org.slf4j.LoggerFactory.getLogger(name);
    }
  }
}

