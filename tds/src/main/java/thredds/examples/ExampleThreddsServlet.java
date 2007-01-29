// $Id: ExampleThreddsServlet.java 51 2006-07-12 17:13:13Z caron $
package thredds.examples;

import thredds.servlet.*;
import thredds.catalog.InvDatasetScan;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import org.apache.log4j.*;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 15, 2006 2:52:54 PM
 */
public class ExampleThreddsServlet extends HttpServlet
{
  protected org.slf4j.Logger log;
  protected String rootPath;    // Path to location war file was unpacked.
  protected String contentPath; // Path to ${tomcat_home}/content/thredds
  // @todo Is this allowed in all servlet engines?

  protected DataRootHandler dataRootHandler;

  protected String getVersion()
  {
    return "ETS version 0.1";
  }

  protected String getDocsPath()
  {
    return "docs/";
  }

  protected String getUserCssPath()
  {
    return "upc.css";
  }

  protected String getContextLogoPath()
  {
    return "thredds.jpg";
  }

  protected String getInstituteLogoPath()
  {
    return "unidataLogo.gif";
  }


  public void init() throws javax.servlet.ServletException
  {
    ServletUtil.initDebugging( this ); // read debug flags
    rootPath = ServletUtil.getRootPath( this );
    contentPath = ServletUtil.getContentPath( this );

    // init logging
    ServletUtil.initLogging( this );
    ServletUtil.logServerSetup( this.getClass().getName() + ".init()" );
    log = org.slf4j.LoggerFactory.getLogger( getClass() );

    log.info( "servlet context name= " + this.getServletContext().getServletContextName() );
    log.info( "servlet context path= " + ServletUtil.getContextPath( this) );
    log.info( "rootPath= " + rootPath );
    log.info( "contentPath= " + contentPath );

    // first time, create content directory
    String initialContentPath = ServletUtil.getInitialContentPath( this );
    File initialContentFile = new File( initialContentPath );
    if ( initialContentFile.exists() )
    {
      try
      {
        if ( ServletUtil.copyDir( initialContentPath, contentPath ) )
        {
          log.info( "copyDir " + initialContentPath + " to " + contentPath );
        }
      }
      catch ( IOException ioe )
      {
        log.error( "failed to copyDir " + initialContentPath + " to " + contentPath, ioe );
      }
    }

    // handles all catalogs, including ones with DatasetScan elements, ie dynamic
    InvDatasetScan.setContext( ServletUtil.getContextPath( this) );
    InvDatasetScan.setCatalogServletName( "");
    DataRootHandler.init( contentPath, ServletUtil.getContextPath( this ) );
    dataRootHandler = DataRootHandler.getInstance();
    try
    {
      dataRootHandler.initCatalog( "catalog.xml" );
      dataRootHandler.initCatalog( "extraCatalog.xml" );
    }
    catch ( Throwable e )
    {
      log.error( "Error initializing catalog: " + e.getMessage(), e );
    }

    this.makeDebugActions();
    dataRootHandler.makeDebugActions();
    DatasetHandler.makeDebugActions();

    HtmlWriter.init( ServletUtil.getContextPath( this ),
                      this.getServletContext().getServletContextName(),
                      this.getVersion(), this.getDocsPath(),
                      this.getUserCssPath(), this.getContextLogoPath(), this.getInstituteLogoPath());

    log.info( "--- initialized " + getClass().getName() );
  }

  protected void doGet( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {
    // Setup logging for this request.
    ServletUtil.logServerAccessSetup( req );

    // Get the request path.
    String path = req.getPathInfo(); // ServletUtil.getRequestPath( req );

    // Permanent redirect to "/" (HTTP status code 301)
    if ( path == null || path.equals( "" ) )
    {
      String newPath = req.getRequestURL().append( "/" ).toString();
      ServletUtil.sendPermanentRedirect( newPath, req, res );
      return;
    }

    // Handle requests for files in content directory (authorization required).
    else if ( path.startsWith( "/content/" ) )
    {
      ServletUtil.handleRequestForContentFile( path, this, req, res );
      return;
    }

    // Handle requests for files in root directory (authorization required).
    else if ( path.startsWith( "/root/" ) )
    {
      ServletUtil.handleRequestForRootFile( path, this, req, res );
      return;
    }

    // debugging
    else if ( path.equals( "/debug" ) || path.equals( "/debug/" ) )
    {
      DebugHandler.doDebug( this, req, res );
      return;
    }

//    else if ( path.endsWith( "/"))
//    {
//      // Handle data_dir request.
//    }

    // Handle static and dynamic catalog requests (including those ending with "/").
    else if ( dataRootHandler.processReqForCatalog( req, res ) )
    {
      return;
    }

    // Handle dataset requests.
    else if ( dataRootHandler.hasDataRootMatch( path ) )
    {
      // @todo Don't create new DSP for each request.
      dataRootHandler.handleRequestForDataset( path, new MockOpendapDSP(), req, res );
      return;
    }

    // If none of the above, try to handle as a request for a regular file.
    ServletUtil.handleRequestForRawFile( path, this, req, res );
  }


  protected void makeDebugActions()
  {
    DebugHandler debugHandler = DebugHandler.get( "General" );
    DebugHandler.Action act;

    act = new DebugHandler.Action( "showVersion", "Show Build Version" )
    {
      public void doAction( DebugHandler.Event e )
      {
        try
        {
          thredds.util.IO.copyFile( rootPath + "README.txt", e.pw );
        }
        catch ( Exception ioe )
        {
          e.pw.println( ioe.getMessage() );
        }
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showRuntime", "Show Runtime info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        Runtime runt = Runtime.getRuntime();
        double scale = 1.0 / ( 1000.0 * 1000.0 );
        e.pw.println( " freeMemory= " + scale * runt.freeMemory() + " Mb" );
        e.pw.println( " totalMemory= " + scale * runt.totalMemory() + " Mb" );
        e.pw.println( " maxMemory= " + scale * runt.maxMemory() + " Mb" );
        e.pw.println( " availableProcessors= " + runt.availableProcessors() );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showFlags", "Show Debugging Flags" )
    {
      public void doAction( DebugHandler.Event e )
      {
        showFlags( e.req, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "toggleFlag", null )
    {
      public void doAction( DebugHandler.Event e )
      {
        if ( e.target != null )
        {
          String flag = e.target;
          Debug.set( flag, !Debug.isSet( flag ) );
        }
        else
          e.pw.println( " Must be toggleFlag=<flagName>" );

        showFlags( e.req, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showLoggers", "Show Log4J info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        showLoggers( e.req, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "setLogger", null )
    {
      public void doAction( DebugHandler.Event e )
      {
        if ( e.target == null )
        {
          e.pw.println( " Must be setLogger=loggerName" );
          return;
        }

        StringTokenizer stoker = new StringTokenizer( e.target, "&=" );
        if ( stoker.countTokens() < 3 )
        {
          e.pw.println( " Must be setLogger=loggerName&setLevel=levelName" );
          return;
        }

        String loggerName = stoker.nextToken();
        stoker.nextToken(); // level=
        String levelName = stoker.nextToken();

        boolean isRootLogger = loggerName.equals( "root" );
        if ( !isRootLogger && LogManager.exists( loggerName ) == null )
        {
          e.pw.println( " Unknown logger=" + loggerName );
          return;
        }

        if ( Level.toLevel( levelName, null ) == null )
        {
          e.pw.println( " Unknown level=" + levelName );
          return;
        }

        Logger log = isRootLogger ? LogManager.getRootLogger() : LogManager.getLogger( loggerName );
        log.setLevel( Level.toLevel( levelName ) );
        e.pw.println( loggerName + " set to " + levelName );
        showLoggers( e.req, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showRequest", "Show HTTP Request info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        e.pw.println( ServletUtil.showRequestDetail( ExampleThreddsServlet.this, e.req ) );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showServerInfo", "Show Server info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        ServletUtil.showServerInfo( ExampleThreddsServlet.this, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showServletInfo", "Show Servlet info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        ServletUtil.showServletInfo( ExampleThreddsServlet.this, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showSession", "Show HTTP Session info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        ServletUtil.showSession( e.req, e.res, e.pw );
      }
    };
    debugHandler.addAction( act );

    act = new DebugHandler.Action( "showSecurity", "Show Security info" )
    {
      public void doAction( DebugHandler.Event e )
      {
        e.pw.println( ServletUtil.showSecurity( e.req, "admin" ) );
      }
    };
    debugHandler.addAction( act );
  }

  void showFlags( HttpServletRequest req, PrintStream pw )
  {
    for (Object o : Debug.keySet()) {
      String key = (String) o;
      String url = req.getRequestURI() + "?toggleFlag=" + key;
      pw.println("  <a href='" + url + "'>" + key + " = " + Debug.isSet(key) + "</a>");
    }
  }

  void showLoggers( HttpServletRequest req, PrintStream pw )
  {
    Logger root = LogManager.getRootLogger();
    showLogger( req, root, pw );

    Enumeration logEnums = LogManager.getCurrentLoggers();
    List loggersSorted = Collections.list( logEnums );
    Collections.sort( loggersSorted, new LoggerComparator() );
    Iterator loggers = loggersSorted.iterator();
    while ( loggers.hasNext() )
    {
      Logger logger = (Logger) loggers.next();
      showLogger( req, logger, pw );
    }
  }

  private void showLogger( HttpServletRequest req, Logger logger, PrintStream pw )
  {
    pw.print( " logger = " + logger.getName() + " level= " );
    String url = req.getRequestURI() + "?setLogger=" + logger.getName() + "&level=";
    showLevel( url, Level.ALL, logger.getEffectiveLevel(), pw );
    showLevel( url, Level.DEBUG, logger.getEffectiveLevel(), pw );
    showLevel( url, Level.INFO, logger.getEffectiveLevel(), pw );
    showLevel( url, Level.WARN, logger.getEffectiveLevel(), pw );
    showLevel( url, Level.ERROR, logger.getEffectiveLevel(), pw );
    showLevel( url, Level.FATAL, logger.getEffectiveLevel(), pw );
    showLevel( url, Level.OFF, logger.getEffectiveLevel(), pw );
    pw.println();

    Enumeration appenders = logger.getAllAppenders();
    while ( appenders.hasMoreElements() )
    {
      Appender app = (Appender) appenders.nextElement();
      pw.println( "  appender= " + app.getName() + " " + app.getClass().getName() );
      if ( app instanceof AppenderSkeleton )
      {
        AppenderSkeleton skapp = (AppenderSkeleton) app;
        if ( skapp.getThreshold() != null )
          pw.println( "    threshold=" + skapp.getThreshold() );
      }
      if ( app instanceof FileAppender )
      {
        FileAppender fapp = (FileAppender) app;
        pw.println( "    file=" + fapp.getFile() );
      }
    }
  }

  private void showLevel( String baseUrl, Level show, Level current, PrintStream pw )
  {
    if ( show.toInt() != current.toInt() )
      pw.print( " <a href='" + baseUrl + show + "'>" + show + "</a>" );
    else
      pw.print( " " + show );
  }

  private class LoggerComparator implements Comparator
  {
    public int compare( Object o1, Object o2 )
    {
      Logger l1 = (Logger) o1;
      Logger l2 = (Logger) o2;
      return l1.getName().compareTo( l2.getName() );
    }

    public boolean equals( Object o )
    {
      return this == o;
    }
  }

}
/*
 * $Log: ExampleThreddsServlet.java,v $
 * Revision 1.6  2006/06/14 22:26:28  edavis
 * THREDDS Servlet Framework (TSF) changes:
 * 1) Allow developer to specify the logo files to be used by in HTML responses.
 * 2) Allow developer to specify the servlet path to be used for catalog requests.  
 * 3) Improve thread safety in DataRootHandler.
 *
 * Revision 1.5  2006/05/19 19:23:05  edavis
 * Convert DatasetInserter to ProxyDatasetHandler and allow for a list of them (rather than one) in
 * CatalogBuilders and CollectionLevelScanner. Clean up division between use of url paths (req.getPathInfo())
 * and translated (CrawlableDataset) paths.
 *
 * Revision 1.4  2006/04/28 21:45:15  edavis
 * Clean up some logging stuff.
 *
 * Revision 1.3  2006/04/18 20:39:54  edavis
 * Change context parameter "ContextPath" to "ContentPath".
 *
 * Revision 1.2  2006/04/03 23:05:16  caron
 * add DLwriterServlet, StationObsCollectionServlet
 * rename various servlets, CatalogRootHandler -> DataRootHandler
 *
 * Revision 1.1  2006/03/30 23:22:10  edavis
 * Refactor THREDDS servlet framework, especially CatalogRootHandler and ServletUtil.
 *
 * Revision 1.1  2006/03/07 23:45:33  edavis
 * Remove hardwiring of "/thredds" as the context path in TDS framework.
 * Start refactoring URL mappings in TDS framework, use ExampleThreddsServlet as test servlet.
 *
 */