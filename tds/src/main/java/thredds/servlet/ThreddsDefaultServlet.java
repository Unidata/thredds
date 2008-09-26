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

import org.apache.log4j.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.util.DiskCache;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.nc2.util.cache.FileCacheRaf;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.ncml.AggregationFmrc;
import ucar.nc2.dataset.NetcdfDataset;
import thredds.catalog.InvDatasetScan;

/**
 * THREDDS default servlet - handles everything not explicitly mapped.
 * You should map this servlet to "/*" in web.xml.
 *
 * @author caron
 */
public class ThreddsDefaultServlet extends AbstractServlet {
  static String version = null;

  protected String getPath() {
    return "";
  }

  protected String getContextName() {
    return this.getServletContext().getServletContextName();
  }

  protected String getDocsPath() {
    return "http://www.unidata.ucar.edu/projects/THREDDS/tech/TDS.html";
  }
  //protected String getDocsPath() { return "docs/"; }

  protected String getTdsPageCssPath() { return "tds.css"; }
  protected String getTdsCatalogCssPath() { return "tdsCat.css"; }

  protected String getContextLogoPath() {
    return "thredds.jpg";
  }

  protected String getContextLogoAlt() {
    return "thredds";
  }

  protected String getInstituteLogoPath() {
    return "unidataLogo.gif";
  }

  protected String getInstituteLogoAlt() {
    return "unidata";
  }

  protected String getFolderIconPath() {
    return "folder.gif";
  }

  protected String getFolderIconAlt() {
    return "folder";
  }

  protected DataRootHandler catHandler;  // singleton

  // cache scouring
  private Timer timer;
  private org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");
  private DiskCache2 aggCache;
  private FileCacheRaf fileCacheRaf;

  public void init() throws ServletException {
    super.init();
    super.initContent(); // first time, create content directory

    //URL r = getClass().getResource("spring/applic-config.xml"); 

    // Removed because causes this to fail with the following exception:
    //      org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'mbeanExporter' defined in class path resource [resources/spring/applic-config.xml]: Invocation of init method failed; nested exception is org.springframework.jmx.export.UnableToRegisterMBeanException: Unable to register MBean [thredds.monitor.OpenRAFMonitorImpl@1c6f48c] with key 'thredds:name=OpenRAF'; nested exception is javax.management.InstanceAlreadyExistsException: thredds:name=OpenRAF
//    ApplicationContext springContext =
//       new ClassPathXmlApplicationContext("classpath:resources/spring/applic-config.xml");
//       // new FileSystemXmlApplicationContext("C:/dev/tds/thredds/tds/src/main/resources/spring/applic-config.xml");
//    Object bean = springContext.getBean("openRAFMonitor");

    // get the URL context :  URLS must be context/catalog/...
    // cannot be overridded in ThreddsConfig
    String contextPath = ServletUtil.getContextPath();
    InvDatasetScan.setContext(contextPath);
    InvDatasetScan.setCatalogServletName("/catalog");

    // read in persistent user-defined params from threddsConfog.xml
    ThreddsConfig.init(this.getServletContext(), contentPath + "/threddsConfig.xml", log);
    // maybe change the logging
    // ( String datePattern, long maxFileSize, int maxFiles )
    if (ThreddsConfig.hasElement("Logging")) {
      String datePattern = ThreddsConfig.get("Logging.DatePattern", null);
      long maxFileSize = ThreddsConfig.getBytes("Logging.MaxFileSize", -1);
      int maxFiles = ThreddsConfig.getInt("Logging.MaxFiles", 5);
      changeLogs(datePattern, maxFileSize, maxFiles);
    }

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

    // HTTP file access : // allow 20 - 40 open datasets, cleanup every 10 minutes
    min = ThreddsConfig.getInt("HTTPFileCache.minFiles", 25);
    max = ThreddsConfig.getInt("HTTPFileCache.maxFiles", 40);
    secs = ThreddsConfig.getSeconds("HTTPFileCache.scour", 10 * 60);
    if (max > 0) {
      fileCacheRaf = new FileCacheRaf(min, max, secs);
      ServletUtil.setFileCache( fileCacheRaf);
    }

    // turn off Grib extend indexing; indexes are automatically done every 10 minutes externally
    //boolean extendIndex = ThreddsConfig.getBoolean("GribIndexing.setExtendIndex", false);
    //ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex(extendIndex);

    boolean alwaysUseCache = ThreddsConfig.getBoolean("GribIndexing.alwaysUseCache", false);
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache(alwaysUseCache);

    // optimization: netcdf-3 files can only grow, not have metadata changes
    ucar.nc2.NetcdfFile.setProperty("syncExtendOnly", "true");

    // persist joinNew aggregations. default every 24 hours, delete stuff older than 30 days
    String dir = ThreddsConfig.get("AggregationCache.dir", contentPath + "cacheAged/");
    int scourSecs = ThreddsConfig.getSeconds("AggregationCache.scour", 24 * 60 * 60);
    int maxAgeSecs = ThreddsConfig.getSeconds("AggregationCache.maxAge", 30 * 24 * 60 * 60);
    aggCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    Aggregation.setPersistenceCache(aggCache);

    // how to choose the typical dataset ?
    String typicalDataset = ThreddsConfig.get("Aggregation.typicalDataset", null);
    if (null != typicalDataset)
      Aggregation.setTypicalDatasetMode(typicalDataset);

    // some paths cant be set otherwise
    String defPath = ServletUtil.getRootPath()+"WEB-INF/altContent/idd/thredds/modelInventory";
    AggregationFmrc.setDefinitionDirectory(defPath);

    // handles all catalogs, including ones with DatasetScan elements, ie dynamic
//    DataRootHandler.init(contentPath, contextPath);
//    catHandler = DataRootHandler.getInstance();
//    catHandler.registerConfigListener(new RestrictedAccessConfigListener());
//    initCatalogs();
//
//    catHandler.makeDebugActions();
//    DatasetHandler.makeDebugActions();

    // Make sure the version info gets calculated.
    getVersion();

    // Nj22 disk cache
    dir = ThreddsConfig.get("DiskCache.dir", contentPath + "cache/");
    boolean alwaysUse = ThreddsConfig.getBoolean("DiskCache.alwaysUse", false);
    scourSecs = ThreddsConfig.getSeconds("DiskCache.scour", 60 * 60);
    long maxSize = ThreddsConfig.getBytes("DiskCache.maxSize", (long) 1000 * 1000 * 1000);
    DiskCache.setRootDirectory(dir);
    DiskCache.setCachePolicy(alwaysUse);

    Calendar c = Calendar.getInstance(); // contains current startup time
    c.add(Calendar.SECOND, scourSecs / 2); // starting in half the scour time
    timer = new Timer();
    timer.scheduleAtFixedRate(new CacheScourTask(maxSize), c.getTime(), (long) 1000 * scourSecs);

    HtmlWriter.init(contextPath, this.getContextName(), this.getVersion(), this.getDocsPath(),
        this.getTdsPageCssPath(), this.getTdsCatalogCssPath(),
        this.getContextLogoPath(), this.getContextLogoAlt(),
        this.getInstituteLogoPath(), this.getInstituteLogoAlt(),
        this.getFolderIconPath(), this.getFolderIconAlt());

    cacheLog.info("Restarted");
    // Checking for double init seeing in intellij debug
    log.info("init(): done initializing <context= " + contextPath + ">.");
  }

  public void destroy() {
    timer.cancel();
    NetcdfDataset.shutdown();
    aggCache.exit();
  }

  void initCatalogs() {
    ArrayList<String> catList = new ArrayList<String>();
    catList.add("catalog.xml"); // always first
    getExtraCatalogs(catList);
    catHandler.initCatalogs(catList);
  }

  private void getExtraCatalogs(List<String> extraList) {
    // if there are some roots in ThreddsConfig, then dont read extraCatalogs.txt
    ThreddsConfig.getCatalogRoots(extraList);
    if (extraList.size() > 0)
      return;

    // see if extraCatalogs.txt exists
    File file = new File(contentPath + "extraCatalogs.txt");
    if (file.exists()) {

      try {
        FileInputStream fin = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        while (true) {
          String line = reader.readLine();
          if (line == null) break;
          line = line.trim();
          if (line.length() == 0) continue;
          if (line.startsWith("#")) continue; // Skip comment lines.
          extraList.add(line);
        }
        fin.close();

      } catch (IOException e) {
        log.error("Error on getExtraCatalogs ", e);
      }
    }

  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    ServletUtil.logServerAccessSetup(req);

    try {
      String path = ServletUtil.getRequestPath(req);

      if (Debug.isSet("showRequest"))
        System.out.println("**ThreddsDefault GET req=" + ServletUtil.getRequest(req) + " path= " + path);
      if (Debug.isSet("showRequestDetail"))
        System.out.println("**ThreddsDefault GET req=" + ServletUtil.showRequestDetail(this, req));

      if ((path == null) || path.equals("/")) {
        String newPath = ServletUtil.getContextPath() + "/catalog.html";
        res.sendRedirect(newPath);
        return;
      }

      // these are the dataRoots, we allow raw access from the debug menu. must have tdsConfig rights
      if (path.startsWith("/dataDir/")) {
        path = path.substring(9);
        File file = getMappedFile(path);
        if ((file != null) && file.exists() && file.isDirectory())
          HtmlWriter.getInstance().writeDirectory(res, file, path);
        else
          res.sendError(HttpServletResponse.SC_NOT_FOUND);
        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
        return;
      }

      if (!path.startsWith("/content/")) {

        // see if its a catalog
        if (catHandler.processReqForCatalog(req, res)) {
          return;
        }

        if (path.endsWith("/") && (getStaticFile(req, true) == null)) {
          ServletUtil.forwardToCatalogServices(req, res);
          return;
        }
      }

//      if ( path.endsWith( "/latest.xml") ) {
//        catHandler.processReqForLatestDataset( this, req, res);
//        return;
//      }

      // debugging
      if (path.equals("/debug") || path.equals("/debug/")) {
        DebugHandler.doDebug(this, req, res);
        return;
      }

      // debugging
      if (path.equals("/catalogWait.xml")) {
        log.debug("sleep 10 secs");
        Thread.sleep(10000); // current thread sleeps
        path = "/catalog.xml";
      }

      // debugging
      if (path.equals("/testSessions") || path.equals("/testSecurity")) {
        System.out.println(ServletUtil.showRequestDetail(this, req));
        ServletUtil.showSession(req, System.out);
        return;
      }

      // see if its a static file
      File staticFile = getStaticFile(req, false);

      if (staticFile == null) {
        // TODO LOOK Yikes!!! Very dependent on servlet/jsp container implementation. 
        RequestDispatcher defaultRD = this.getServletContext().getNamedDispatcher( "jsp" );
        defaultRD.forward( req, res );
        return;
      }

      /* if (path.startsWith("/fileServer")) {
        ServletUtil.returnFile(this, req, res, staticFile, null);
        return;
      } */

      // directory
      if (staticFile.isDirectory()) {
        // gotta end with a "/" for sanity reasons
        if (!path.endsWith("/")) {
          String newPath = req.getRequestURI() + "/";
          res.sendRedirect(newPath);
          return;
        }

        // use an index.html file if it exists
        File indexFile = new File(staticFile, "index.html");
        if (indexFile.exists()) { // use it if it exists
          staticFile = indexFile;

        } else {
          // normal thing to do
          HtmlWriter.getInstance().writeDirectory(res, staticFile, path);
          return;
        }
      }

      ServletUtil.returnFile(this, req, res, staticFile, null);

    } catch (Throwable t) {
      t.printStackTrace();
      log.error("doGet req= " + ServletUtil.getRequest(req) + " got Exception", t);
      ServletUtil.handleException(t, res);
    }
  }

  /* private void doCatalogHtml(HttpServletRequest req, HttpServletResponse res, String path )
      throws IOException, ServletException {

    // dispatch to CatalogHtml servlet
    RequestDispatcher dispatch = req.getRequestDispatcher("/catalog.html?catalog="+path);
    if (dispatch != null)
      dispatch.forward(req, res);
    else
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
   } */

  // Servlets that support HTTP GET requests and can quickly determine their last modification time should
  // override this method. This makes browser and proxy caches work more effectively, reducing the load on
  // server and network resources.

  protected long getLastModified(HttpServletRequest req) {
    File staticFile = getStaticFile(req, true);
    if (staticFile == null)
      return -1;

    return staticFile.lastModified();
  }

  /**
   * look for paths of the form
   * /root/*
   * /content/*
   * <p/>
   * then look to see if the file exists at:
   * contentPath + path
   * rootPath + path
   *
   * @param req the request
   * @return File is found, else null
   */
  private File getStaticFile(HttpServletRequest req, boolean allowRoot) {
    String path = req.getPathInfo();
    if (path == null) return null;

    /* if (path.startsWith("/fileServer/")) {
     return getMappedFile(req.getPathInfo());
   } */

    boolean explicit = false;
    String filename;

    // special mapping to see top directories
    if (path.startsWith("/root/")) {
      explicit = true;
      path = path.substring(5);
      filename = ServletUtil.formFilename(ServletUtil.getRootPath(), path);

    } else if (path.startsWith("/content/")) {
      explicit = true;
      path = path.substring(8);
      filename = ServletUtil.formFilename(contentPath, path);

    } else {
      // general case, doesnt start with /root/ or /content/
      // we are getting content from the war file (webapps/thredds/) or from content/thredds/public
      // first see if it exists under content
      filename = ServletUtil.formFilename(contentPath + "public/", path);
      if (filename != null) {
        File file = new File(filename);
        if (file.exists())
          return file;
      }

      // otherwise try rootPath
      if ( allowRoot )
        filename = ServletUtil.formFilename(ServletUtil.getRootPath(), path);
    }

    if (filename == null)
      return null;

    // these are ok if its an explicit root or content, since those are password protected
    if (!explicit) {
      if (path.endsWith("catalog.html") || path.endsWith("catalog.xml"))
        return null;

//      String upper = filename.toUpperCase();
//      if (upper.indexOf("WEB-INF") != -1 || upper.indexOf("META-INF") != -1)
//        return null;
    }

    File file = new File(filename);
    if (file.exists())
      return file;

    return null;
  }

  private File getMappedFile(String path) {
    if (path == null) return null;
    File file = catHandler.getCrawlableDatasetAsFile(path);
    if ((file != null) && file.exists())
      return file;

    return null;
  }

  /**
   * ************************************************************************
   * PUT requests: save a file in the content directory. Path must start with content.
   * <p/>
   * Request must be of the form
   * config?server=serverName
   *
   * @param req The client's <code> HttpServletRequest</code> request object.
   * @param res The server's <code> HttpServletResponse</code> response object.
   */
  public void doPut(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    ServletUtil.logServerAccessSetup(req);

    String path = ServletUtil.getRequestPath(req);
    if (Debug.isSet("showRequest"))
      log.debug("ThreddsDefault PUT path= " + path);

    if (path.startsWith("/content"))
      path = path.substring(8);
    else {
      ServletUtil.logServerAccess(HttpServletResponse.SC_FORBIDDEN, 0);
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    if (path != null) {
      if (ServletUtil.saveFile(this, contentPath, path, req, res)) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, 0);
        return; // LOOK - could trigger reread of config file
      }
    }

    ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
    res.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  // lame
  static String getVersionStatic() {
    return version;
  }

  protected String getVersion() {
    if (version == null) {
      String readme;
      try {
        readme = IO.readFile(ServletUtil.getRootPath() + "docs/README.txt");
      } catch (IOException e) {
        return "unknown version";
      }

      int pos = readme.indexOf('\n');
      if (pos > 0)
        version = readme.substring(0, pos);
      else
        version = readme;
    }
    return version;
  }

  //////////////////////////////////////////////////////////////
  // debugging

  protected void makeCacheActions() {
    DebugHandler debugHandler = DebugHandler.get("Caches");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showCaches", "Show All Caches") {
      public void doAction(DebugHandler.Event e) {
        Formatter f = new Formatter(e.pw);
        f.format("NetcdfFileCache contents\n");
        NetcdfDataset.getNetcdfFileCache().showCache(f);
        
        if (fileCacheRaf != null) {
          f.format("\nRAF Cache contents\n");
          for (Object cacheElement : fileCacheRaf.getCache())
            f.format(" %s\n",cacheElement);
        }
        e.pw.flush();
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("clearCache", "Clear Caches") {
      public void doAction(DebugHandler.Event e) {
        NetcdfDataset.getNetcdfFileCache().clearCache(false);
        if (fileCacheRaf != null) fileCacheRaf.clearCache(false);
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
        fileCacheRaf.clearCache(true);
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

    act = new DebugHandler.Action("showLoggers", "Show Log4J info") {
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
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showRequest", "Show HTTP Request info") {
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
    debugHandler.addAction(act);

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

    makeCacheActions();

    debugHandler = DebugHandler.get("catalogs");
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
    debugHandler.addAction(act);
  }

  void showFlags(HttpServletRequest req, PrintStream pw) {
    for (String key : Debug.keySet()) {
      String url = req.getRequestURI() + "?toggleFlag=" + key;
      pw.println("  <a href='" + url + "'>" + key + " = " + Debug.isSet(key) + "</a>");
    }
  }


  private void changeLogs(String datePattern, long maxFileSize, int maxFiles) {
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
  }

  private void replaceAppender(Logger logger, String want, Appender replaceWith) {
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
  }

  private class CacheScourTask extends TimerTask {
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
  }

}
