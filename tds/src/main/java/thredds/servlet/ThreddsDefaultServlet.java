// $Id: ThreddsDefaultServlet.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import ucar.unidata.io.FileCache;
import ucar.nc2.NetcdfFileCache;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.dataset.NetcdfDatasetCache;
import thredds.catalog.InvDatasetScan;

/**
 * THREDDS default servlet - handles everything not explicitly mapped.
 * You should map this servlet to "/*" in web.xml.
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class ThreddsDefaultServlet extends AbstractServlet {
  static String version = null;

  protected String getPath() { return ""; }

  protected String getContextName() { return "THREDDS Data Server"; }
  protected String getDocsPath() { return "docs/"; }

  protected String getUserCssPath() { return "upc.css"; }

  protected String getContextLogoPath() { return "thredds.jpg"; }
  protected String getInstituteLogoPath() { return "unidataLogo.gif"; }


  protected DataRootHandler catHandler;

    // cache scouring
  private Timer timer;
  private org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");
  private DiskCache2 aggCache;

  public void init() throws ServletException {
    super.init();

    // get the URL context :  URLS must be context/catalog/...
    // cannot be overridded in ServletParams
    String contextPath = ServletUtil.getContextPath( this);
    InvDatasetScan.setContext( contextPath);
    InvDatasetScan.setCatalogServletName( "/catalog" );

    // persistent user-defined params
    ServletParams.init(this.getServletContext(), contentPath+"/params.xml", log);

    // turn off Grib extend indexing; indexes are automatically done every 10 minutes externally
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( false);

    // optimization: netcdf-3 files can only grow, not have metadata changes
    ucar.nc2.N3iosp.setProperty( "syncExtendOnly", "true");

    // cache initialization
    // set the cache directory
    String cache = ServletParams.getInitParameter("CachePath", contentPath + "cache/");
    DiskCache.setRootDirectory(cache);
    DiskCache.setCachePolicy(false); // allow to write into data directory if possible

    // DODS Server
    NetcdfFileCache.init(200, 300, 10 * 60);  // allow 200 - 300 open files, cleanup every 10 minutes
    // WCS Server
    NetcdfDatasetCache.init(200, 300, 10 * 60); // allow 200 - 300 open datasets, cleanup every 10 minutes
    // HTTP file access
    FileCache.init(50, 70, 2 * 60);  // allow 20 - 40 open datasets, cleanup every 10 minutes

    // for efficiency, persist aggregations. every 12 hours, delete stuff older than 10 days
    String cache2 = ServletParams.getInitParameter("CacheAged", contentPath + "cacheAged/");
    aggCache = new DiskCache2(cache2, false, 60 * 24 * 10, 60 * 12);
    Aggregation.setPersistenceCache( aggCache);  // */
    aggCache.setLogger( cacheLog);

    // handles all catalogs, including ones with DatasetScan elements, ie dynamic
    DataRootHandler.init(contentPath, contextPath);
    catHandler = DataRootHandler.getInstance();

    List catList = getExtraCatalogs();
    catList.add(0, "catalog.xml"); // always first
    for (int i = 0; i < catList.size(); i++) {
      String catFilename = (String) catList.get(i);
      try {
        catHandler.initCatalog(catFilename);
      } catch (Throwable e) {
        log.error( "Error initializing catalog "+catFilename+"; "+e.getMessage(), e);
      }
    }

    catHandler.makeDebugActions();
    DatasetHandler.makeDebugActions();

    // Make sure the version info gets calculated.
    getVersion();

    // cache scouring
    Calendar c = Calendar.getInstance(); // contains current startup time
    timer = new Timer();
    // each morning between 1 - 2 am
    /* c.set( Calendar.HOUR_OF_DAY, 1); // 1 am
    c.add( Calendar.DAY_OF_YEAR, 1); // tommorrow
    timer.scheduleAtFixedRate( new CacheScourTask(), c.getTime(), (long) 1000 * 60 * 60 * 24 ); */

    // each hour, starting in 30 minutes
    c.add( Calendar.MINUTE, 30);
    timer.scheduleAtFixedRate( new CacheScourTask(), c.getTime(), (long) 1000 * 60 * 60 );

    HtmlWriter.init( contextPath, this.getContextName(), this.getVersion(), this.getDocsPath(),
                      this.getUserCssPath(), this.getContextLogoPath(), this.getInstituteLogoPath() );

    cacheLog.info("Restarted");
  }

  public void destroy() {
    timer.cancel();
    NetcdfFileCache.exit();
    NetcdfDatasetCache.exit();
    FileCache.exit();
    aggCache.exit();
  }

  private List getExtraCatalogs() {
    ArrayList extraList = new ArrayList();
    try {
      FileInputStream fin = new FileInputStream(contentPath + "extraCatalogs.txt");
      BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
      while (true) {
        String line = reader.readLine();
        if (line == null) break;
        line = line.trim();
        if (line.length() == 0) continue;
        if ( line.startsWith( "#") ) continue; // Skip comment lines.
        extraList.add( line);
      }
      fin.close();

    } catch (FileNotFoundException e) {
      // its ok
    } catch (IOException e) {
      log.error("Error on getExtraCatalogs ",e);
    }

    return extraList;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    ServletUtil.logServerAccessSetup( req );

    try {
      String path = ServletUtil.getRequestPath( req);

      if (Debug.isSet("showRequest"))
        System.out.println("**ThreddsDefault GET req="+ServletUtil.getRequest(req)+" path= "+path);
      if (Debug.isSet("showRequestDetail"))
        System.out.println( "**ThreddsDefault GET req="+ServletUtil.showRequestDetail(this, req));

      if ( (path == null) || path.equals("/") ) {
        String newPath = ServletUtil.getContextPath(this) +"/catalog.html";
        res.sendRedirect( newPath);
        return;
      }

      // these are the dataRoots, we allow raw access from the debug menu. must have tdsConfig rights
      if (path.startsWith("/dataDir/")) {
        path = path.substring(9);
        File file =  getMappedFile(path);
        if ((file != null) && file.exists() && file.isDirectory())
          HtmlWriter.getInstance().writeDirectory( res, file, path);
        else
          res.sendError(HttpServletResponse.SC_NOT_FOUND);
          ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
        return;
      }

      if (!path.startsWith("/content/")) {

        // see if its a catalog
        if (catHandler.processReqForCatalog( req, res)) {
          return;
        }

        if (path.endsWith("/") && (getStaticFile(req) == null)) {
           ServletUtil.forwardToCatalogServices( req, res);
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
         System.out.println( ServletUtil.showRequestDetail(this, req));
         ServletUtil.showSession(req, System.out);
         return;
       }

       // see if its a static file
      File staticFile = getStaticFile(req);

      if (staticFile == null) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      /* if (path.startsWith("/fileServer")) {
        ServletUtil.returnFile(this, req, res, staticFile, null);
        return;
      } */

      // directory
      if (staticFile.isDirectory()) {
        // gotta end with a "/" for sanity reasons
        if ( !path.endsWith("/")) {
          String newPath = req.getRequestURI() +"/";
          res.sendRedirect( newPath);
          return;
        }

        // use an index.html file if it exists
        File indexFile = new File( staticFile, "index.html");
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
      log.error("doGet req= "+ServletUtil.getRequest(req)+" got Exception", t);
      ServletUtil.handleException( t,  res);
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
    File staticFile = getStaticFile( req);
    if (staticFile == null)
      return -1;

    return staticFile.lastModified();
  }

  /** look for paths of the form
   *  /root/*
   *  /content/*
   *
   * then look to see if the file exists at:
   *  contentPath + path
   *  rootPath + path
   *
   * @param req
   * @return File is found, else null
   */
  private File getStaticFile(HttpServletRequest req) {
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
      filename = ServletUtil.formFilename( rootPath, path);

    } else if (path.startsWith("/content/")) {
      explicit = true;
      path = path.substring(8);
      filename = ServletUtil.formFilename( contentPath, path);

    } else {
      // general case, doesnt start with /root/ or /content/
      // we are getting content from the war file (webapps/thredds/) or from content/thredds/
      // first see if it exists under content
      /*  remove this now
      filename = ServletUtil.formFilename( contentPath, path);
      if (filename != null) {
        File file = new File( filename);
        if (file.exists())
          return file;
      }  */

      // otherwise try rootPath
     filename = ServletUtil.formFilename( rootPath, path);
    }

    if (filename == null)
      return null;

    // these are ok if its an explicit root or content, since those are password protected
    if (!explicit) {
      if (path.endsWith("catalog.html") || path.endsWith("catalog.xml"))
        return null;

      String upper = filename.toUpperCase();
      if (upper.indexOf("WEB-INF") != -1 || upper.indexOf("META-INF") != -1)
        return null;
    }

    File file = new File( filename);
    if (file.exists())
      return file;

    return null;
  }

  private File getMappedFile(String path) {
    if (path == null) return null;
    String filePath = catHandler.translatePath( path);
    // @todo Should instead use ((CrawlableDatasetFile)catHandler.findRequestedDataset( path )).getFile();

    if (filePath == null) return null;
    File file = new File( filePath);
    if (file.exists())
        return file;

    return null;
  }

 /***************************************************************************
  * PUT requests: save a file in the content directory. Path must start with content.
  *
  * Request must be of the form
  *   config?server=serverName
  *
  * @param req The client's <code> HttpServletRequest</code> request object.
  * @param res The server's <code> HttpServletResponse</code> response object.
  */
  public void doPut(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    ServletUtil.logServerAccessSetup( req );

    String path = ServletUtil.getRequestPath( req);
    if (Debug.isSet("showRequest"))
      log.debug("ThreddsDefault PUT path= "+path);

    if (path.startsWith("/content"))
      path = path.substring(8);
    else {
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, 0 );
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
   }

    if (path != null) {
      if (ServletUtil.saveFile( this, contentPath, path, req, res)) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, 0 );
        return; // LOOK - could trigger reread of config file
      }
    }

    ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
    res.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  // lame
  static String getVersionStatic() { return version; }

  protected String getVersion() {
    if (version == null) {
      String readme;
      try {
        readme = thredds.util.IO.readFile(rootPath+"docs/README.txt");
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
    DebugHandler debugHandler = new DebugHandler("Caches");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showCaches", "Show All Caches") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("NetcdfFileCache contents\n");
        java.util.List cacheList = NetcdfFileCache.getCache();
        for (int i = 0; i < cacheList.size(); i++) {
          Object o = cacheList.get(i);
          e.pw.println(" " + o);
        }
        e.pw.println("\nNetcdfDatasetCache contents");
        cacheList = NetcdfDatasetCache.getCache();
        for (int i = 0; i < cacheList.size(); i++) {
          Object o = cacheList.get(i);
          e.pw.println(" " + o);
        }
        e.pw.println("\nRAF Cache contents");
        cacheList = ucar.unidata.io.FileCache.getCache();
        for (int i = 0; i < cacheList.size(); i++) {
          Object o = cacheList.get(i);
          e.pw.println(" " + o);
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("clearCache", "Clear Caches") {
      public void doAction(DebugHandler.Event e) {
        NetcdfFileCache.clearCache(false);
        NetcdfDatasetCache.clearCache(false);
        ucar.unidata.io.FileCache.clearCache(false);
        e.pw.println("  ClearCache ok");
      }
    };
    debugHandler.addAction( act);

    act = new DebugHandler.Action("forceNCCache", "Force clear NetcdfFileCache Cache") {
      public void doAction(DebugHandler.Event e) {
        NetcdfFileCache.clearCache(true);
        e.pw.println("  NetcdfFileCache force clearCache done");
      }
    };
    debugHandler.addAction( act);

    act = new DebugHandler.Action("forceDSCache", "Force clear NetcdfDatasetCache Cache") {
      public void doAction(DebugHandler.Event e) {
        NetcdfDatasetCache.clearCache(true);
        e.pw.println("  NetcdfDatasetCache force clearCache done");
      }
    };
    debugHandler.addAction( act);

    act = new DebugHandler.Action("forceRAFCache", "Force clear RAF FileCache Cache") {
      public void doAction(DebugHandler.Event e) {
        ucar.unidata.io.FileCache.clearCache(true);
        e.pw.println("  RAF FileCache force clearCache done ");
      }
    };
    debugHandler.addAction( act);

  }

  protected void makeDebugActions() {
    DebugHandler debugHandler = new DebugHandler("General");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showVersion", "Show Build Version") {
      public void doAction(DebugHandler.Event e) {
        try {
          thredds.util.IO.copyFile(rootPath+"docs/README.txt", e.pw);
        } catch (Exception ioe) {
          e.pw.println(ioe.getMessage());
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showRuntime", "Show Runtime info") {
      public void doAction(DebugHandler.Event e) {
        Runtime runt = Runtime.getRuntime();
        double scale = 1.0/(1000.0 * 1000.0);
        e.pw.println(" freeMemory= "+ scale * runt.freeMemory()+" Mb");
        e.pw.println(" totalMemory= "+scale * runt.totalMemory()+" Mb");
        e.pw.println(" maxMemory= "+scale * runt.maxMemory()+" Mb");
        e.pw.println(" availableProcessors= "+runt.availableProcessors());
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showFlags", "Show Debugging Flags") {
      public void doAction(DebugHandler.Event e) {
        showFlags( e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("toggleFlag", null) {
      public void doAction(DebugHandler.Event e) {
        if (e.target != null) {
          String flag  = e.target;
          Debug.set( flag, !Debug.isSet(flag));
        } else
          e.pw.println(" Must be toggleFlag=<flagName>");

        showFlags( e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showLoggers", "Show Log4J info") {
      public void doAction(DebugHandler.Event e) {
        showLoggers( e.req, e.pw);
      }
    };
    debugHandler.addAction( act);

    act = new DebugHandler.Action("setLogger", null) {
      public void doAction(DebugHandler.Event e) {
        if (e.target == null) {
          e.pw.println(" Must be setLogger=loggerName");
          return;
        }

        StringTokenizer stoker = new StringTokenizer( e.target,"&=");
        if (stoker.countTokens() < 3) {
          e.pw.println(" Must be setLogger=loggerName&setLevel=levelName");
          return;
        }

        String loggerName = stoker.nextToken();
        stoker.nextToken(); // level=
        String levelName = stoker.nextToken();

        boolean isRootLogger = loggerName.equals("root");
        if (!isRootLogger && LogManager.exists(loggerName) == null) {
          e.pw.println(" Unknown logger="+loggerName);
          return;
        }

        if (Level.toLevel(levelName, null) == null) {
          e.pw.println(" Unknown level="+levelName);
          return;
        }

        Logger log = isRootLogger ? LogManager.getRootLogger() : LogManager.getLogger(loggerName);
        log.setLevel( Level.toLevel(levelName));
        e.pw.println(loggerName+" set to "+levelName);
        showLoggers( e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showRequest", "Show HTTP Request info") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println(ServletUtil.showRequestDetail( ThreddsDefaultServlet.this, e.req));
      }
    };
    debugHandler.addAction( act);

    act = new DebugHandler.Action("showServerInfo", "Show Server info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showServerInfo( ThreddsDefaultServlet.this, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showServletInfo", "Show Servlet info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showServletInfo( ThreddsDefaultServlet.this, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showSession", "Show HTTP Session info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showSession( e.req, e.res, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showSecurity", "Show Security info") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println( ServletUtil.showSecurity( e.req));
      }
    };
    debugHandler.addAction(act);

    makeCacheActions();
  }

  void showFlags(HttpServletRequest req, PrintStream pw) {
    Iterator iter = Debug.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      String url = req.getRequestURI() + "?toggleFlag=" + key;
      pw.println("  <a href='" +url+"'>"+key+" = "+Debug.isSet(key)+"</a>");
    }
  }

  void showLoggers(HttpServletRequest req, PrintStream pw) {
    Logger root = LogManager.getRootLogger();
    showLogger( req, root, pw);

    Enumeration logEnums = LogManager.getCurrentLoggers();
    List loggersSorted = Collections.list( logEnums);
    Collections.sort( loggersSorted, new LoggerComparator());
    Iterator loggers = loggersSorted.iterator();
    while (loggers.hasNext()) {
      Logger logger =  (Logger) loggers.next();
      showLogger( req, logger, pw);
    }
  }

  private void showLogger(HttpServletRequest req, Logger logger, PrintStream pw) {
    pw.print(" logger = "+logger.getName()+" level= ");
    String url = req.getRequestURI() + "?setLogger=" + logger.getName()+"&level=";
    showLevel( url, Level.ALL, logger.getEffectiveLevel(), pw);
    showLevel( url, Level.DEBUG, logger.getEffectiveLevel(), pw);
    showLevel( url, Level.INFO, logger.getEffectiveLevel(), pw);
    showLevel( url, Level.WARN, logger.getEffectiveLevel(), pw);
    showLevel( url, Level.ERROR, logger.getEffectiveLevel(), pw);
    showLevel( url, Level.FATAL, logger.getEffectiveLevel(), pw);
    showLevel( url, Level.OFF, logger.getEffectiveLevel(), pw);
    pw.println();

    Enumeration appenders = logger.getAllAppenders();
    while (appenders.hasMoreElements()) {
      Appender app =  (Appender) appenders.nextElement();
      pw.println("  appender= "+app.getName()+" "+app.getClass().getName());
      if (app instanceof AppenderSkeleton) {
        AppenderSkeleton skapp = (AppenderSkeleton) app;
        if (skapp.getThreshold() != null)
          pw.println("    threshold="+skapp.getThreshold());
      }
      if (app instanceof FileAppender) {
        FileAppender fapp = (FileAppender) app;
        pw.println("    file="+fapp.getFile());
      }
    }
  }

  private void showLevel(String baseUrl, Level show, Level current, PrintStream pw) {
    if (show.toInt() != current.toInt())
      pw.print(" <a href='"+baseUrl+show+"'>"+show+"</a>");
    else
      pw.print(" "+show);
  }


  private class LoggerComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      Logger l1 = (Logger) o1;
      Logger l2 = (Logger) o2;
      return l1.getName().compareTo( l2.getName());
    }

    public boolean equals( Object o) {
      return this == o;
    }
  }

  private class CacheScourTask extends TimerTask {
    CacheScourTask( ) { }

    public void run() {
      StringBuffer sbuff = new StringBuffer();
      DiskCache.cleanCache(1000 * 1000 * 1000, sbuff); // 1 Gbyte
      sbuff.append("----------------------\n");
      cacheLog.info(sbuff.toString());
    }
  }
}
