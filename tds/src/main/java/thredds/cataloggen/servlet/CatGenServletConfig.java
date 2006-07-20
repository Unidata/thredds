package thredds.cataloggen.servlet;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.input.*;

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

  private final File resultPath;
  private final File configPath;
  private final String servletConfigDocName;

  private final ConfigDocumentParser configDocParser;

  /** The collection of configuration tasks. */
  // @GuardedBy("this")
  private List configTasks;
  // @GuardedBy("this")
  private HashMap configTaskHash = new HashMap();
  // @GuardedBy("this")
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

    this.configDocParser = new ConfigDocumentParser();

    // Make sure paths exist.
    if ( ! this.resultPath.exists() )
    {
      if ( ! this.resultPath.mkdirs())
      {
        String tmpMsg = "Creation of results directory failed";
        log.error( "CatGenServletConfig(): " + tmpMsg + " <" + this.resultPath.getAbsolutePath() + ">" );
        throw new IOException( tmpMsg );
      }
    }
    if ( !this.configPath.exists() )
    {
      if ( ! this.configPath.mkdirs() )
      {
        String tmpMsg = "Creation of config directory failed";
        log.error( "CatGenServletConfig(): " + tmpMsg + " <" + this.configPath.getAbsolutePath() + ">" );
        throw new IOException( tmpMsg );
      }
    }

    // Make sure config file exists.
    File configFile = new File( this.configPath, this.servletConfigDocName);
    log.debug( "CatGenServletConfig(): reading config file <" + configFile.getPath() + ">." );
    if ( configFile.createNewFile() )
    {
      // Write an empty config file.
      log.debug( "CatGenServletConfig(): no config file exists, writing empty config file <" + configFile.getAbsolutePath() + ">.");
      configDocParser.writeXML( configFile, Collections.EMPTY_LIST );
    }

    // Read config file.
    configTasks = configDocParser.parseXML( configFile );

    if ( this.configTasks == null )
    {
      log.error( "CatGenServletConfig(): invalid config file." );
      this.configTasks = Collections.EMPTY_LIST;
    }
    else if ( this.configTasks.isEmpty() )
    {
      log.debug( "CatGenServletConfig(): task list empty");
    }
    else
    {
      log.debug( "CatGenServletConfig(): at least one task in config file.");

      for ( Iterator it = this.configTasks.iterator(); it.hasNext(); )
      {
        CatGenTimerTask curTask =  (CatGenTimerTask) it.next();
        curTask.init( this.resultPath, this.configPath);
        this.configTaskHash.put( curTask.getName(), curTask);
        this.configTaskHashByConfigDocName.put( curTask.getConfigDocName(), curTask);
        log.debug( "CatGenServletConfig(): task name = " + curTask.getName());

        StringBuffer msg = new StringBuffer();
        if ( ! curTask.isValid( msg ))
        {
          log.warn( "ctor(): not scheduling invalid task <" + curTask.getName() + ">: " + msg.toString());
        }
        else
        {
          // Schedule the current task.
          this.scheduleTask( curTask);
        }
      }
    }
  }

  public void cancelTimer()
  {
    this.timer.cancel();
  }

  /** Return the filename for the configuration file. */
  public String getServletConfigDocName() { return( this.servletConfigDocName); }

  public synchronized List getUnmodTasks()
  {
    return Collections.unmodifiableList( this.configTasks );
  }
  /** Return an iterator of the config tasks in this config. */
  public synchronized java.util.Iterator getUnmodTaskIterator()
  {
    List unModList = Collections.unmodifiableList( this.configTasks);
    return( unModList.iterator());
  }

  /** Find a task with the given name. */
  public synchronized CatGenTimerTask findTask( String taskName)
  {
    return( (CatGenTimerTask) configTaskHash.get( taskName));
  }

  /** Find a task with the given config file name. */
  public synchronized CatGenTimerTask findTaskByConfigDocName( String configDocName)
  {
    return( (CatGenTimerTask) configTaskHashByConfigDocName.get( configDocName));
  }

  /** Add the given task unless it is a duplicate. */
  public synchronized boolean addTask( CatGenTimerTask task)
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

    log.debug( "addTask(): added task <" + task.getName() + "> to config." );
    StringBuffer msg = new StringBuffer();
    if ( ! task.isValid( msg ) )
    {
      log.warn( "addTask(): invalid task <" + task.getName() + ">, not scheduling: " + msg.toString() );
    }
    else
    {
      this.scheduleTask( task );
    }

    return( true);
  }

  /** Remove the given task. */
  public synchronized boolean removeTask( CatGenTimerTask task)
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
  public synchronized boolean removeTask( String taskName)
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
  public synchronized void notifyNewConfigDoc( String configDocName)
  {
    log.debug( "notifyNewConfigDoc(): start." );
    CatGenTimerTask task = this.findTaskByConfigDocName( configDocName);
    if ( task != null )
    {
      this.unScheduleTask( task );

      CatGenTimerTask newTask = new CatGenTimerTask( task );
      newTask.init( this.resultPath, this.configPath );
      StringBuffer msg = new StringBuffer();
      if ( ! newTask.isValid( msg ) )
      {
        log.warn( "notifyNewConfigDoc(): invalid task <" + newTask.getName() + ">, not scheduling: " + msg.toString() );
      }
      else
      {
        this.scheduleTask( newTask );
      }

      log.debug( "notifyNewConfigDoc(): done." );
    }
    return;
  }

  private synchronized boolean scheduleTask( CatGenTimerTask task)
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

      this.timer.scheduleAtFixedRate( task.getTimerTask(), date, periodInMillis);
      log.debug( "scheduleTask(): task scheduled.");

      return( true);
    }
  }

  private synchronized boolean unScheduleTask( CatGenTimerTask task)
  {
    log.debug( "unScheduleTask(): start: (" + task.getName() + ").");
    return( task.getTimerTask().cancel());
  }

  /* Write the configuration to the XMLStore. */
  public void writeConfig()
          throws java.io.IOException
  {
    log.debug( "writeConfig(): start.");

    // Make sure config file exists.
    File configFile = new File( this.configPath, this.servletConfigDocName );
    if ( ! configFile.canWrite() )
    {
      // Write an empty config file.
      log.error( "CatGenServletConfig(): cannot write to config file <" + configFile.getPath() + ">." );
      return; // @todo throw exception?
    }

    this.configDocParser.writeXML( configFile, this.getUnmodTasks());
  }

  /* Write the configuration to the XMLStore. */
  public void writeConfig( OutputStream os )
          throws java.io.IOException
  {
    log.debug( "writeConfig(): writing config to XMLStore (OutputStream)." );
    this.configDocParser.writeXML( os, this.getUnmodTasks() );
  }

/**
   * Build the HTML returned from a hit on the servlet with path "/admin/config"
   *
   * @return a String containing the "/admin/config" HTML document.
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

    for ( Iterator it = this.getUnmodTaskIterator(); it.hasNext(); )
    {
      CatGenTimerTask curTask =  (CatGenTimerTask) it.next();
      tmpString.append( "<tr>");
      tmpString.append( "<td>" ).append( curTask.getName() ).append( "</td>" );
      tmpString.append( "<td> <a href=\"./" )
              .append( curTask.getConfigDocName() ).append( "\">" )
              .append( curTask.getConfigDocName() ).append( "</a></td>" );
      tmpString.append( "<td>");
      tmpString.append( curTask.getResultFileName());
      tmpString.append( "</td>");
      tmpString.append( "<td>" ).append( curTask.getPeriodInMinutes() ).append( "</td>" );
      tmpString.append( "<td>" ).append( curTask.getDelayInMinutes() ).append( "</td>" );
      tmpString.append( "<td>");
      tmpString.append( "[<a href=\"./editTask-" )
              .append( curTask.getConfigDocName() ).append( "\">Edit</a>]" );
      tmpString.append( "[<a href=\"./deleteTask-" )
              .append( curTask.getConfigDocName() ).append( "\">Delete</a>]" );
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

  private class ConfigDocumentParser
  {
    private String rootElemName = "preferences";
    private String extXmlVerAttName = "EXTERNAL_XML_VERSION";
    private String extXmlVerAttVal = "1.0";

    private String rootUserElemName = "root";
    private String rootUserAttName = "type";
    private String rootUserAttVal = "user";

    private String mapElemName = "map";

    private String beanCollElemName = "beanCollection";
    private String beanCollKeyAttName = "key";
    private String beanCollKeyAttVal = "config";
    private String beanCollClassAttName = "class";
    private String beanCollClassAttVal = "thredds.cataloggen.servlet.CatGenTimerTask";

    private String beanElemName = "bean";
    private String beanNameAttName = "name";
    private String beanConfigDocNameAttName = "configDocName";
    private String beanResultFileNameAttName = "resultFileName";
    private String beanDelayAttName = "delayInMinutes";
    private String beanPeriodAttName = "periodInMinutes";

    /** Private default constructor. */
    private ConfigDocumentParser() {}

    /**
     * Parse the given config file for CatGenServlet.
     *
     * @param inFile the config file.
     * @return a List of CatGenTimerTask items (which may be empty), or null if config file is malformed.
     * @throws IOException if could not read File.
     */
    public List parseXML( File inFile )
            throws IOException
    {
      FileInputStream inStream = new FileInputStream( inFile );
      List config = parseXML( inStream, inFile.getPath() );
      inStream.close();
      return config;
    }

    /**
     * Parse the given config document for CatGenServlet.
     *
     * @param inStream an InputStream of the config document.
     * @return a List of CatGenTimerTask items (which may be empty), or null if config file is malformed.
     * @throws IOException if could not read InputStream.
     */
    public List parseXML( InputStream inStream, String docId )
            throws IOException
    {
      SAXBuilder builder = new SAXBuilder();
      Document doc;
      log.debug( "parseXML(): Parsing latest config doc \"" + docId + "\"." );
      try
      {
        doc = builder.build( inStream );
      }
      catch ( JDOMException e )
      {
        log.error( "parseXML(): Bad config doc <" + docId + ">: " + e.getMessage() );
        return null;
      }
      List config = readConfig( doc.getRootElement() );

      if ( config == null )
      {
        log.error( "parseXML(): Config doc <" + docId + "> not in valid format." );
      }
      else if ( config.isEmpty() )
      {
        log.warn( "parseXML(): Empty config file <" + docId + ">." );
      }

      return config;
    }

    /**
     * Read the contents of the config document and return a List of CatGenTimerTask objects.
     *
     * @param rootElem the root element in the config document.
     * @return a List (which may be empty), or null if the config document is malformed.
     */
    private List readConfig( Element rootElem )
    {
      if ( ! rootElem.getName().equals( rootElemName ) )
      {
        log.error( "readConfig(): Root element <" + rootElem.getName() + "> not as expected <" + rootElemName + ">." );
        return null;
      }
      Element rootUserElem = rootElem.getChild( rootUserElemName );
      Element mapElem = rootUserElem.getChild( mapElemName );
      Element beanColElem = mapElem.getChild( beanCollElemName );
      // Catch empty "map" elements that were possible with the
      // former ucar.util.prefs implementation.
      if ( beanColElem == null )
        return null;
      String keyAttVal = beanColElem.getAttributeValue( beanCollKeyAttName );
      String classAttVal = beanColElem.getAttributeValue( beanCollClassAttName );
      if ( ! keyAttVal.equals( beanCollKeyAttVal ))
      {
        log.error( "readConfig(): bean collection element key attribute <" + keyAttVal + "> not as expected <" + beanCollKeyAttVal + ">." );
        return null;
      }
      if ( ! classAttVal.equals( beanCollClassAttVal ))
      {
        log.error( "readConfig(): bean collection element class attribute <" + classAttVal + "> not as expected <" + beanCollClassAttVal + ">." );
        return null;
      }

      List configList = new ArrayList();
      java.util.List list = beanColElem.getChildren( beanElemName );
      for ( Iterator it = list.iterator(); it.hasNext(); )
      {
        Element curBeanElem = (Element) it.next();
        String name = curBeanElem.getAttributeValue( beanNameAttName );
        String configDocName = curBeanElem.getAttributeValue( beanConfigDocNameAttName );
        String resultFileName = curBeanElem.getAttributeValue( beanResultFileNameAttName );
        int delayInMinutes;
        int periodInMinutes;
        try
        {
          delayInMinutes = curBeanElem.getAttribute( beanDelayAttName ).getIntValue();
          periodInMinutes = curBeanElem.getAttribute( beanPeriodAttName ).getIntValue();
        }
        catch ( DataConversionException e )
        {
          log.error( "readConfig(): bean element delay or period attribute not an integer value: " + e.getMessage() );
          return null;
        }

        configList.add( new CatGenTimerTask( name, configDocName, resultFileName, periodInMinutes, delayInMinutes));
      }

      return configList;
    }

    public void writeXML( File outFile, List configList )
            throws IOException
    {
      FileOutputStream outStream = new FileOutputStream( outFile );
      writeXML( outStream, configList );
      outStream.close();
    }

    public void writeXML( OutputStream outStream, List configList )
            throws IOException
    {
      Element rootElem = new Element( rootElemName );
      rootElem.setAttribute( extXmlVerAttName, extXmlVerAttVal );
      Document doc = new Document( rootElem );

      Element rootUserElem = new Element( rootUserElemName );
      rootUserElem.setAttribute( rootUserAttName, rootUserAttVal );
      rootElem.addContent( rootUserElem );

      Element mapElem = new Element( mapElemName );
      rootUserElem.addContent( mapElem );

      Element beanCollElem = new Element( beanCollElemName );
      beanCollElem.setAttribute( beanCollKeyAttName, beanCollKeyAttVal );
      beanCollElem.setAttribute( beanCollClassAttName, beanCollClassAttVal );
      mapElem.addContent( beanCollElem );

      for ( Iterator it = configList.iterator(); it.hasNext(); )
      {
        CatGenTimerTask curItem = (CatGenTimerTask) it.next();
        Element curItemElem = new Element( beanElemName );
        curItemElem.setAttribute( beanNameAttName, curItem.getName() );
        curItemElem.setAttribute( beanConfigDocNameAttName, curItem.getConfigDocName() );
        curItemElem.setAttribute( beanResultFileNameAttName, curItem.getResultFileName() );
        curItemElem.setAttribute( beanDelayAttName, Integer.toString( curItem.getDelayInMinutes() ) );
        curItemElem.setAttribute( beanPeriodAttName, Integer.toString( curItem.getPeriodInMinutes() ) );

        beanCollElem.addContent( curItemElem );
      }

      XMLOutputter outputter = new XMLOutputter();
      outputter.output( doc, outStream );
    }
  }
}
