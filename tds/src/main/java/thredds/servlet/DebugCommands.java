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

package thredds.servlet;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.IO;
import ucar.nc2.util.cache.FileCacheRaf;

import java.util.*;
import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;

/**
 * A Singleton class instantiated by Spring, to populate the Debug methods in the
 * DebugHandler class.
 *
 * @author caron
 * @since Jan 15, 2009
 */
public class DebugCommands {

  public DebugCommands() {
    makeCacheActions();
    makeDebugActions();
  }

  protected void makeCacheActions() {
    // NetcdfFileCache : default is allow 200 - 400 open files, cleanup every 10 minutes
    int min = ThreddsConfig.getInt("NetcdfFileCache.minFiles", 200);
    int max = ThreddsConfig.getInt("NetcdfFileCache.maxFiles", 400);
    int secs = ThreddsConfig.getSeconds("NetcdfFileCache.scour", 10 * 60);
    if (max > 0) {
      NetcdfDataset.initNetcdfFileCache(min, max, secs);
    }

    /* NetcdfDatasetCache: // allow 100 - 200 open datasets, cleanup every 10 minutes
    min = ThreddsConfig.getInt("NetcdfDatasetCache.minFiles", 100);
    max = ThreddsConfig.getInt("NetcdfDatasetCache.maxFiles", 200);
    secs = ThreddsConfig.getSeconds("NetcdfDatasetCache.scour", 10 * 60);
    if (max > 0) {
      NetcdfDataset.initDatasetCache(min, max, secs);
    } */

    // LOOK : fileCacheRaf must be set
    // HTTP file access : // allow 20 - 40 open datasets, cleanup every 10 minutes
    min = ThreddsConfig.getInt("HTTPFileCache.minFiles", 25);
    max = ThreddsConfig.getInt("HTTPFileCache.maxFiles", 40);
    secs = ThreddsConfig.getSeconds("HTTPFileCache.scour", 10 * 60);
    if (max > 0) {
      FileCacheRaf fileCacheRaf = new FileCacheRaf(min, max, secs);
      ServletUtil.setFileCache( fileCacheRaf);
    }

    DebugHandler debugHandler = DebugHandler.get("Caches");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showCaches", "Show All Caches") {
      public void doAction(DebugHandler.Event e) {
        Formatter f = new Formatter(e.pw);
        f.format("NetcdfFileCache contents\n");
        NetcdfDataset.getNetcdfFileCache().showCache(f);

        FileCacheRaf fileCacheRaf = ServletUtil.getFileCache();
        f.format("\nRAF Cache contents\n");
        for (Object cacheElement : fileCacheRaf.getCache())
          f.format(" %s\n",cacheElement);
        e.pw.flush();
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("clearCache", "Clear Caches") {
      public void doAction(DebugHandler.Event e) {
        NetcdfDataset.getNetcdfFileCache().clearCache(false);
        ServletUtil.getFileCache().clearCache(false);
        e.pw.println("  ClearCache ok");
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("forceNCCache", "Force clear NetcdfFileCache Cache") {
      public void doAction(DebugHandler.Event e) {
        NetcdfDataset.getNetcdfFileCache().clearCache(true);
        e.pw.println("  NetcdfFileCache force clearCache done");
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("forceRAFCache", "Force clear RAF FileCache Cache") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.getFileCache().clearCache(true);
        e.pw.println("  RAF FileCache force clearCache done ");
      }
    };
    debugHandler.addAction(act);

  }

  protected void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("General");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showVersion", "Show Build Version") {
      public void doAction(DebugHandler.Event e) {
        try {
          IO.copyFile(ServletUtil.getRootPath() + "docs/README.txt", e.pw);
        } catch (Exception ioe) {
          e.pw.println(ioe.getMessage());
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showRuntime", "Show Runtime info") {
      public void doAction(DebugHandler.Event e) {
        Runtime runt = Runtime.getRuntime();
        double scale = 1.0 / (1000.0 * 1000.0);
        e.pw.println(" freeMemory= " + scale * runt.freeMemory() + " Mb");
        e.pw.println(" totalMemory= " + scale * runt.totalMemory() + " Mb");
        e.pw.println(" maxMemory= " + scale * runt.maxMemory() + " Mb");
        e.pw.println(" availableProcessors= " + runt.availableProcessors());
        e.pw.println();
        ServletUtil.showThreads(e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showFlags", "Show Debugging Flags") {
      public void doAction(DebugHandler.Event e) {
        showFlags(e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("toggleFlag", null) {
      public void doAction(DebugHandler.Event e) {
        if (e.target != null) {
          String flag = e.target;
          Debug.set(flag, !Debug.isSet(flag));
        } else
          e.pw.println(" Must be toggleFlag=<flagName>");

        showFlags(e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    /* act = new DebugHandler.Action("showLoggers", "Show Log4J info") {
      public void doAction(DebugHandler.Event e) {
        showLoggers(e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("setLogger", null) {
      public void doAction(DebugHandler.Event e) {
        if (e.target == null) {
          e.pw.println(" Must be setLogger=loggerName");
          return;
        }

        StringTokenizer stoker = new StringTokenizer(e.target, "&=");
        if (stoker.countTokens() < 3) {
          e.pw.println(" Must be setLogger=loggerName&setLevel=levelName");
          return;
        }

        String loggerName = stoker.nextToken();
        stoker.nextToken(); // level=
        String levelName = stoker.nextToken();

        boolean isRootLogger = loggerName.equals("root");
        if (!isRootLogger && LogManager.exists(loggerName) == null) {
          e.pw.println(" Unknown logger=" + loggerName);
          return;
        }

        if (Level.toLevel(levelName, null) == null) {
          e.pw.println(" Unknown level=" + levelName);
          return;
        }

        Logger log = isRootLogger ? LogManager.getRootLogger() : LogManager.getLogger(loggerName);
        log.setLevel(Level.toLevel(levelName));
        e.pw.println(loggerName + " set to " + levelName);
        showLoggers(e.req, e.pw);
      }
    };
    debugHandler.addAction(act); */

    /* act = new DebugHandler.Action("showRequest", "Show HTTP Request info") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println(ServletUtil.showRequestDetail(ThreddsDefaultServlet.this, e.req));
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showServerInfo", "Show Server info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showServerInfo(ThreddsDefaultServlet.this, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showServletInfo", "Show Servlet info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showServletInfo(ThreddsDefaultServlet.this, e.pw);
      }
    };
    debugHandler.addAction(act);  */

    act = new DebugHandler.Action("showSession", "Show HTTP Session info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showSession(e.req, e.res, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showSecurity", "Show Security info") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println(ServletUtil.showSecurity(e.req, "admin"));
      }
    };
    debugHandler.addAction(act);

    /* debugHandler = DebugHandler.get("catalogs");
    act = new DebugHandler.Action("reinit", "Reinitialize") {
      public void doAction(DebugHandler.Event e) {
        // TODO The calls to reinit() and initCatalogs() are synchronized but should be atomic.
        // TODO Should change this to build config data structure and synch only when replacing the old with the new structure.
        catHandler.reinit();
        ThreddsConfig.readConfig(log);
        initCatalogs();
        e.pw.println("reinit ok");
      }
    };
    debugHandler.addAction(act);   */
  }

  void showFlags(HttpServletRequest req, PrintStream pw) {
    for (String key : Debug.keySet()) {
      String url = req.getRequestURI() + "?toggleFlag=" + key;
      pw.println("  <a href='" + url + "'>" + key + " = " + Debug.isSet(key) + "</a>");
    }
  }


  /* private void changeLogs(String datePattern, long maxFileSize, int maxFiles) {
    // get the existing appender
    Logger logger = LogManager.getLogger("thredds");
    FileAppender fapp = (FileAppender) logger.getAppender("threddsServlet");
    PatternLayout playout = (PatternLayout) fapp.getLayout();
    String filename = fapp.getFile();

    // create a new one
    Appender newAppender = null;

    try {
      if (null != datePattern) {
        newAppender = new DailyRollingFileAppender(playout, filename, datePattern);
      } else if (maxFileSize > 0) {
        RollingFileAppender rapp = new RollingFileAppender(playout, filename);
        rapp.setMaximumFileSize(maxFileSize);
        rapp.setMaxBackupIndex(maxFiles);
        newAppender = rapp;
      } else {
        return;
      }
    } catch (IOException ioe) {
      log.error("Error changing the logger", ioe);
    }

    // replace wherever you find it
    Logger root = LogManager.getRootLogger();
    replaceAppender(root, "threddsServlet", newAppender);

    Enumeration logEnums = LogManager.getCurrentLoggers();
    while (logEnums.hasMoreElements()) {
      Logger log = (Logger) logEnums.nextElement();
      replaceAppender(log, "threddsServlet", newAppender);
    }
  }  */

  /* private void replaceAppender(Logger logger, String want, Appender replaceWith) {
    Enumeration appenders = logger.getAllAppenders();
    while (appenders.hasMoreElements()) {
      Appender app = (Appender) appenders.nextElement();
      if (app.getName().equals(want)) {
        logger.removeAppender(app);
        logger.addAppender(replaceWith);
      }
    }
  }

  void showLoggers(HttpServletRequest req, PrintStream pw) {
    Logger root = LogManager.getRootLogger();
    showLogger(req, root, pw);

    Enumeration logEnums = LogManager.getCurrentLoggers();
    List<Logger> loggersSorted = Collections.list(logEnums);
    Collections.sort(loggersSorted, new LoggerComparator());
    for (Logger logger : loggersSorted) {
      showLogger(req, logger, pw);
    }
  }

  private void showLogger(HttpServletRequest req, Logger logger, PrintStream pw) {
    pw.print(" logger = " + logger.getName() + " level= ");
    String url = req.getRequestURI() + "?setLogger=" + logger.getName() + "&level=";
    showLevel(url, Level.ALL, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.DEBUG, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.INFO, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.WARN, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.ERROR, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.FATAL, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.OFF, logger.getEffectiveLevel(), pw);
    pw.println();

    Enumeration appenders = logger.getAllAppenders();
    while (appenders.hasMoreElements()) {
      Appender app = (Appender) appenders.nextElement();
      pw.println("  appender= " + app.getName() + " " + app.getClass().getName());
      Layout layout = app.getLayout();
      if (layout instanceof PatternLayout) {
        PatternLayout playout = (PatternLayout) layout;
        pw.println("    layout pattern= " + playout.getConversionPattern());
      }
      if (app instanceof AppenderSkeleton) {
        AppenderSkeleton skapp = (AppenderSkeleton) app;
        if (skapp.getThreshold() != null)
          pw.println("    threshold=" + skapp.getThreshold());
      }
      if (app instanceof FileAppender) {
        FileAppender fapp = (FileAppender) app;
        pw.println("    file=" + fapp.getFile());
      }
    }
  }

  private void showLevel(String baseUrl, Level show, Level current, PrintStream pw) {
    if (show.toInt() != current.toInt())
      pw.print(" <a href='" + baseUrl + show + "'>" + show + "</a>");
    else
      pw.print(" " + show);
  }


  private class LoggerComparator implements Comparator<Logger> {
    public int compare(Logger log1, Logger log2) {
      return log1.getName().compareTo(log2.getName());
    }

    public boolean equals(Object o) {
      return this == o;
    }
  }  */

  /* private class CacheScourTask extends TimerTask {
    long maxBytes;

    CacheScourTask(long maxBytes) {
      this.maxBytes = maxBytes;
    }

    public void run() {
      StringBuilder sbuff = new StringBuilder();
      DiskCache.cleanCache(maxBytes, sbuff); // 1 Gbyte
      sbuff.append("----------------------\n");
      cacheLog.info(sbuff.toString());
    }
  } */
}
