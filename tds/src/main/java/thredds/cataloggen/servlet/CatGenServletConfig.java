// $Id: CatGenServletConfig.java 51 2006-07-12 17:13:13Z caron $

package thredds.cataloggen.servlet;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import java.io.*;
import java.util.*;

/**
 * The CatGenServletConfig class keeps track of the configuration
 * of this Catalog Generator. The configuration is made up of all
 * the tasks being handled by this Catalog Generator.
 *
 */
public class CatGenServletConfig
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( CatGenServletConfig.class);

  private File resultPath, configPath;
  private String servletConfigDocName;

  /** The XMLStore for the config file. */
  private XMLStore configStore = null;

  /** The preferences stored in the config file. */
  private PreferencesExt configPrefs = null;

  /** The collection of configuration tasks. */
  private Collection configTasks = null;
  private HashMap configTaskHash = new HashMap();
  private HashMap configTaskHashByConfigDocName = new HashMap();

  /** Timer for scheduling tasks. */
  // private static Timer timer = new Timer(); // Why static?
  private Timer timer = new Timer();

  /**
   * Constructor. Read config file and initialize any tasks contained therein.
   *
   * @param configPath
   * @param servletConfigDocName
   */
  public CatGenServletConfig( File resultPath, File configPath, String servletConfigDocName)
          throws IOException
  {
    this.resultPath = resultPath;
    this.configPath = configPath;
    this.servletConfigDocName = servletConfigDocName;

    // Make sure paths exist.
    if ( ! this.resultPath.exists() )
    {
      if ( ! this.resultPath.mkdirs())
      {
        String tmpMsg = "Creation of results directory failed";
        log.debug( "CatGenServletConfig(): " + tmpMsg + " <" + this.resultPath.getAbsolutePath() + ">" );
        throw new IOException( tmpMsg );
      }
    }
    if ( !this.configPath.exists() )
    {
      if ( ! this.configPath.mkdirs() )
      {
        String tmpMsg = "Creation of config directory failed";
        log.debug( "CatGenServletConfig(): " + tmpMsg + " <" + this.configPath.getAbsolutePath() + ">" );
        throw new IOException( tmpMsg );
      }
    }

    // Make sure config file exists.
    File configFile = new File( this.configPath, this.servletConfigDocName);
    if ( configFile.createNewFile() )
    {
      // Write an empty XMLStore file.
      log.debug( "CatGenServletConfig(): no config file exists, writing empty config file <"
                    + configFile.getAbsolutePath() + ">.");
      OutputStream out = new BufferedOutputStream( new FileOutputStream( configFile ) );
      XMLStore store = new XMLStore();
      store.save( out );
      out.close();
    }

    // Try opening ucar.util.prefs XML store.
    log.debug( "CatGenServletConfig(): reading config (" + configFile.toString() + ")" );
    try
    {
      // @todo Use createFromInputStream instead.
      this.configStore = XMLStore.createFromFile( configFile.toString(), null);
    }
    catch ( java.io.IOException e)
    {
      log.error( "CatGenServletConfig(): XMLStore creation failed for " + this.servletConfigDocName);
      // @todo throw exception and handle in CatGenServlet
    }

    // Get preferences, i.e., the collection of Beans named "config".
    this.configPrefs = this.configStore.getPreferences();
    if ( this.configPrefs == null)
    {
      log.warn( "CatGenServletConfig(): null preferences from the config file (???)." );
    }
    this.configTasks = (Collection) this.configPrefs.getBean( "config", new ArrayList());
    if ( this.configTasks.isEmpty())
    {
      log.debug( "CatGenServletConfig(): task list empty");
    }

    // If configTasks is not empty, initialize and schedule each task.
    if ( ! this.configTasks.isEmpty())
    {
      log.debug( "CatGenServletConfig(): at least one task in config file.");

      CatGenTimerTask curTask = null;
      java.util.Iterator iter = this.configTasks.iterator();
      while ( iter.hasNext())
      {
        curTask =  (CatGenTimerTask) iter.next();
        curTask.init( this.resultPath, this.configPath);
        this.configTaskHash.put( curTask.getName(), curTask);
        this.configTaskHashByConfigDocName.put( curTask.getConfigDocName(), curTask);
        log.debug( "CatGenServletConfig(): task name = " + curTask.getName());

        // ??? Check that config file is read/write/etc-able?
        // ??? Check that config file is valid CatalogGenConfig?

        // Schedule the current task.
        this.scheduleTask( curTask);
      }
    }
    else
    {
      log.debug( "CatGenServletConfig(): no tasks in config file.");
    }
  }

  public void cancelTimer()
  {
    this.timer.cancel();
  }

  /** Return the filename for the configuration file. */
  public String getServletConfigDocName() { return( this.servletConfigDocName); }

  /** Return an iterator of the config tasks in this config. */
  public java.util.Iterator getTaskIterator()
  { return( this.configTasks.iterator()); }

  /** Find a task with the given name. */
  public CatGenTimerTask findTask( String taskName)
  {
    return( (CatGenTimerTask) configTaskHash.get( taskName));
  }

  /** Find a task with the given config file name. */
  public CatGenTimerTask findTaskByConfigDocName( String configDocName)
  {
    return( (CatGenTimerTask) configTaskHashByConfigDocName.get( configDocName));
  }

  /** Add the given task unless it is a duplicate. */
  public boolean addTask( CatGenTimerTask task)
          throws IOException
  {
    log.debug( "addTask(): start.");

    // If the task is null, do not add task and return false.
    if ( task == null)
    {
      log.debug( "addTask(): task to add is null.");
      return( false);
    }

    // If there is an existing task with the same name,
    // do not add task and return false.
    if ( this.findTask( task.getName()) != null)
    {
      log.debug( "addTask(): task with same name alread exists (" + task.getName() + ").");
      return( false);
    }
    // If there is an existing task with the same config doc name,
    // do not add task and return false.
    if ( this.findTaskByConfigDocName( task.getConfigDocName()) != null)
    {
      log.debug( "addTask(): task with same config doc alread exists (" + task.getConfigDocName() + ").");
      return( false);
    }

    task.init( this.resultPath, this.configPath);
    this.configTasks.add( task);
    this.configTaskHash.put( task.getName(), task);
    this.configTaskHashByConfigDocName.put( task.getConfigDocName(), task);
    this.writeConfig();
    this.scheduleTask( task);

    log.debug( "addTask(): task added (" + task.getName() + ").");

    return( true);
  }

  /** Remove the given task. */
  public boolean removeTask( CatGenTimerTask task)
          throws IOException
 {
    log.debug( "removeTask(): start.");

    if ( this.findTask( task.getName()) != null )
    {
      this.unScheduleTask( task);
      this.configTaskHashByConfigDocName.remove( task.getConfigDocName());
      this.configTaskHash.remove( task.getName());
      this.configTasks.remove( task);
      this.writeConfig();
      log.debug( "removeTask(): task removed (" + task.getName() + ").");
      return( true);
    }
//    if ( this.configTasks.remove( task))
//    {
//      this.configTaskHash.remove( task.getName());
//      this.configTaskHashByConfigDocName.remove( task.getConfigDocName());
//      this.unScheduleTask( task);
//      this.writeConfig();
//      return( true);
//    }
    log.debug( "removeTask(): task not in list.");
    return( false);
  }

  /** Remove the task with the given name. */
  public boolean removeTask( String taskName)
          throws IOException
 {
    return( this.removeTask( this.findTask( taskName)));
  }

  /**
   * Provides a way to notify this servlet configuration that a CatGen
   * config document has been written. If there is a task that uses that
   * config document, that task is started re-scheduled to run.
   *
   * @param configDocName - the name of the CatGen config doc.
   */
  public void notifyNewConfigDoc( String configDocName)
  {
    log.debug( "notifyNewConfigDoc(): start." );
    CatGenTimerTask task = this.findTaskByConfigDocName( configDocName);
    if ( task != null )
    {
      this.unScheduleTask( task );

      CatGenTimerTask newTask = new CatGenTimerTask( task );
      newTask.init( this.resultPath, this.configPath );
      this.scheduleTask( newTask );

      log.debug( "notifyNewConfigDoc(): done." );
    }
    return;
  }

  private boolean scheduleTask( CatGenTimerTask task)
  {
    log.debug( "scheduleTask(): start.");
    if ( task == null) return( false);
    log.debug( "scheduleTask(): start (" + task.getName() + ")." );

    int periodInMillis = task.getPeriodInMinutes() * 60 * 1000; // minutes to milliseconds
    int delayInMins = task.getDelayInMinutes();

    log.debug( "periodInMillis=" + periodInMillis + " - delayInMins=" + delayInMins + "" );
    if ( periodInMillis == 0)
    {
      log.debug( "scheduleTask(): period set to zero, do not schedule.");
      return( false);
    }
    else
    {
      log.debug( "scheduleTask(): scheduling task." );
      Calendar cal = Calendar.getInstance();
      if (delayInMins != 0)
      {
        cal.add( Calendar.MINUTE, delayInMins);
      }
      Date date = cal.getTime();

      this.timer.scheduleAtFixedRate( task, date, periodInMillis);
      log.debug( "scheduleTask(): task scheduled.");

      return( true);
    }
  }

  private boolean unScheduleTask( CatGenTimerTask task)
  {
    log.debug( "unScheduleTask(): start: (" + task.getName() + ").");
    return( task.cancel());
  }

  /* Write the configuration to the XMLStore. */
  public void writeConfig()
          throws java.io.IOException
  {
    log.debug( "writeConfig(): start.");

    this.configPrefs.putBeanCollection( "config", configTasks);
    //try
    //{
      this.configStore.save();
    //}
    //catch (java.io.IOException e)
    //{
    //  log.debug( "writeConfig(): config file written (" + this.servletConfigDocName + ").");
    //  // @todo throw exception and deal ith it in CatGenServlet.
    //}
  }

  /* Write the configuration to the XMLStore. */
  public void writeConfig( OutputStream os )
          throws java.io.IOException
  {
    log.debug( "writeConfig(): writing config to XMLStore (OutputStream)." );

    this.configPrefs.putBeanCollection( "config", configTasks );
    this.configStore.save( os );
  }

/**
   * Build the HTML returned from a hit on the servlet with path "/admin/config"
   *
   * @return
   */
  public String toHtml()
  {
    StringBuffer tmpString = new StringBuffer();
    log.debug( "toHtml(): start.");

    tmpString.append( "<html>");
    tmpString.append( "<head><title>Catalog Generator Servlet Config</title></head>");
    tmpString.append( "<body>");
    tmpString.append( "<h1>Catalog Generator Servlet Config</h1>");
    tmpString.append( "<hr>");

    tmpString.append( "<h2>Currently Scheduled Tasks</h2>");
    tmpString.append( "<table border=\"1\">");
    tmpString.append( "<tr>");
    tmpString.append( "<th> Task Name</th>");
    tmpString.append( "<th> Config Filename</th>");
    tmpString.append( "<th>");
    tmpString.append( "Results Filename");
    tmpString.append( "</th>");
    tmpString.append( "<th> Period (minutes)</th>");
    tmpString.append( "<th> Initial Delay (minutes)</th>");
    tmpString.append( "<th> Edit/Delete Task</th>");
    tmpString.append( "</tr>");

    CatGenTimerTask curTask = null;
    java.util.Iterator iter = this.configTasks.iterator();
    while ( iter.hasNext())
    {
      curTask =  (CatGenTimerTask) iter.next();
      tmpString.append( "<tr>");
      tmpString.append( "<td>" + curTask.getName() + "</td>");
      tmpString.append( "<td> <a href=\"./" +
                        curTask.getConfigDocName() + "\">" +
                        curTask.getConfigDocName() + "</a></td>");
      tmpString.append( "<td>");
      tmpString.append( curTask.getResultFileName());
      tmpString.append( "</td>");
      tmpString.append( "<td>" + curTask.getPeriodInMinutes() + "</td>");
      tmpString.append( "<td>" + curTask.getDelayInMinutes() + "</td>");
      tmpString.append( "<td>");
      tmpString.append( "[<a href=\"./editTask-" + curTask.getConfigDocName() + "\">Edit</a>]");
      tmpString.append( "[<a href=\"./deleteTask-" + curTask.getConfigDocName() + "\">Delete</a>]");
      tmpString.append( "</td>");
      tmpString.append( "</tr>");
    }
    tmpString.append( "</table>");
    tmpString.append( "<a href=\"./addTask\">Add a new task</a>");

    //<form method="POST" action="http://...">
    //</form>

    tmpString.append( "<hr>");
    tmpString.append( "</body>");
    tmpString.append( "</html>");

    log.debug( "toHtml(): start.");
    return( tmpString.toString());
  }

}
/*
 * $Log: CatGenServletConfig.java,v $
 * Revision 1.9  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.8  2005/04/05 22:37:02  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.7  2004/05/11 20:21:04  edavis
 * Update init() so that it gets the directory for the resulting catalogs
 * rather than the directory to the servlet root. Add some more logging. Add
 * some functionality to allow tasks to be updated when config files are PUT.
 *
 * Revision 1.6  2003/09/05 22:05:05  edavis
 * Minor change to logging message.
 *
 * Revision 1.5  2003/08/29 21:41:47  edavis
 * The following changes where made:
 *
 *  1) Added more extensive logging (changed from thredds.util.Log and
 * thredds.util.Debug to using Log4j).
 *
 * 2) Improved existing error handling and added additional error
 * handling where problems could fall through the cracks. Added some
 * catching and throwing of exceptions but also, for problems that aren't
 * fatal, added the inclusion in the resulting catalog of datasets with
 * the error message as its name.
 *
 * 3) Change how the CatGenTimerTask constructor is given the path to the
 * config files and the path to the resulting files so that resulting
 * catalogs are placed in the servlet directory space. Also, add ability
 * for servlet to serve the resulting catalogs.
 *
 * 4) Switch from using java.lang.String to using java.io.File for
 * handling file location information so that path seperators will be
 * correctly handled. Also, switch to java.net.URI rather than
 * java.io.File or java.lang.String where necessary to handle proper
 * URI/URL character encoding.
 *
 * 5) Add handling of requests when no path ("") is given, when the root
 * path ("/") is given, and when the admin path ("/admin") is given.
 *
 * 6) Fix the PUTting of catalogGenConfig files.
 *
 * 7) Start adding GDS DatasetSource capabilities.
 *
 * Revision 1.4  2003/08/20 17:48:21  edavis
 * Import statments optimized.
 *
 * Revision 1.3  2003/05/01 23:43:18  edavis
 * Added a few comments.
 *
 * Revision 1.2  2003/04/30 18:29:53  edavis
 * Added main() for testing.
 *
 * Revision 1.1  2003/03/04 23:09:37  edavis
 * Added for 0.7 release (addition of CatGenServlet).
 *
 *
 */