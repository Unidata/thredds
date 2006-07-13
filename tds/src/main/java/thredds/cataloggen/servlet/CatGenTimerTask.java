// $Id: CatGenTimerTask.java 51 2006-07-12 17:13:13Z caron $

package thredds.cataloggen.servlet;

import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.io.File;

import thredds.cataloggen.*;
import thredds.datatype.DateType;

/**
 * Describes a task to be run by the CatalogGenerator Servlet.
 *
 * <tt>CatGenTimerTask</tt> is a subclass of <tt>TimerTask</tt>
 * so that it can be run by a <tt>Timer</tt>. It is also a Bean
 * so that it can be stored using the ucar.util.prefs package.
 *
 */
public class CatGenTimerTask
  extends TimerTask
  implements java.lang.Cloneable
{
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( CatGenTimerTask.class);

  /** The name of this task. */
  private String name = null;

  /** The path (i.e., minus the name) of the configuration document
   *  to be used by this task. (Should be private to servlet.) */
  private File configDocPath = null;

  /** The name of the configuration document to be used by this task. */
  private String configDocName = null;

  private File configDoc = null;
  private URL configDocURL = null;

  /** The servlets real path. */
  private File resultPath = null;

  /** The filename in which to store the results of this task. */
  private String resultFileName = null;
  private File resultFile = null;

  /** The time in minutes between runs of this task. */
  private int periodInMinutes = 0;

  /** The time in minutes to delay the initial run of this task. */
  private int delayInMinutes = 0;

  /** Constructor required by ucar.util.prefs (or to be a Bean?). */
  public CatGenTimerTask() { super(); }

  /**
   * Constructor
   *
   * @param name - the name of this task.
   * @param configDocName - the name of the config doc
   * @param resultFileName - name of the resulting file
   * @param periodInMinutes - the time in minutes between runs of this task
   * @param delayInMinutes - the time to wait before the first run of this task
   */
  public CatGenTimerTask( String name,
                          String configDocName,
                          String resultFileName,
                          int periodInMinutes,
                          int delayInMinutes)
  {
    super();
    this.name = name;
    this.configDocName = configDocName;
    this.resultFileName = resultFileName;
    this.periodInMinutes = periodInMinutes;
    this.delayInMinutes = delayInMinutes;
  }

  public CatGenTimerTask( CatGenTimerTask task )
  {
    super();
    this.name = task.getName();
    this.configDocName = task.getConfigDocName();
    this.resultFileName = task.getResultFileName();
    this.periodInMinutes = task.getPeriodInMinutes();
    this.delayInMinutes = task.getDelayInMinutes();
  }

  /**
   * Initialize with resultPath and configDocPath.
   *
   * @param configDocPath - the URL for the config file.
   */
  public void init( File resultPath, File configDocPath)
  {
    this.resultPath = resultPath;
    this.configDocPath = configDocPath;

    this.configDoc = new File( this.configDocPath, this.configDocName);
    try
    {
      URI tmpURI = this.configDoc.toURI();
      this.configDocURL = tmpURI.toURL();
    }
    catch ( MalformedURLException e )
    {
      logger.error( "init(): Config Doc to URL caused MallformedURLException");
      logger.error( e.getMessage());
      // @todo throw this exception and handle in CatGenServletConfig
      // throw( e);
    }

    this.resultFile = new File( this.resultPath, this.resultFileName);
    // Check that the result file exists and if not check if need to create subdirectory(ies).
    if ( ! this.resultFile.exists())
    {
      if ( ! this.resultFile.getParentFile().exists())
      {
        if ( this.resultFile.getParentFile().mkdirs())
        {
          logger.debug( "init(): Created directory \"" + this.resultFile.getParentFile().getAbsolutePath() + "\".");
        }
        else
        {
          logger.warn( "init(): Could not creat directory \"" +
                       this.resultFile.getParentFile().getAbsolutePath() + "\", result file " +
                       "(" + this.resultFile.getAbsolutePath() + ")invalid.");
          // @todo Throw an IOException here.
        }
      }
    }

    logger.debug( "init(): result path is " + this.resultPath.toString());
    logger.debug( "init(): config doc path is " + this.configDocPath.toString());
    logger.debug( "init(): config doc URL is " + this.configDocURL.toString());
    logger.debug( "init(): config doc is " + this.configDoc.toString());
    logger.debug( "init(): result file is " + this.resultFile.toString());
  }

  /**
   * Test whether this task is valid, including the validity of the config file.
   * @param messages - StringBuffer containing error and warning messages.
   * @return - true if task is valid, false if invalid (errors)
   */
  public boolean isValid( StringBuffer messages)
  {
    boolean isValid = true;
    String tmpMsg = null;

    // Check that task name is valid.
    logger.debug( "isValid(): Check name <" + this.getName() + "> for validity." );
    if ( this.getName() == null)
    {
      tmpMsg = "Tasks name not set (null).";
      logger.debug( "isValid(): " + tmpMsg);
      messages.append( "CatGenTimerTask.isValid(): " ).append( tmpMsg );
      isValid = false;
    }
    if ( this.getName().equals( ""))
    {
      tmpMsg = "Task name is empty (\"\").";
      logger.debug( "isValid(): " + tmpMsg);
      messages.append( "CatGenTimerTask.isValid(): " ).append( tmpMsg );
      isValid = false;
    }

    // Only check for validity if period is not zero (if zero task not run).
    logger.debug( "isValid(): If period is not zero <" + this.getPeriodInMinutes() + "> check validity.");
    if ( this.getPeriodInMinutes() != 0)
    {
      // Check that catalog is valid.
      CatalogGen catGen = new CatalogGen( this.configDocURL);
      if ( ! catGen.isValid( messages) )
      {
        logger.debug( "isValid(): config doc <" + this.configDocURL + "> is not valid." );
        isValid = false;
      }

      // Check that result file exists and is writeable.
      if ( this.resultFile.exists())
      {
        if ( ! this.resultFile.canWrite())
        {
          messages.append( "CatGenTimerTask.isValid() - result file not writeable.");
          logger.warn( "isValid(): Result file is not writable.");
          isValid = false;
        }
      }
      else
      {
        try
        {
          if ( ! this.resultFile.createNewFile())
          {
            // @todo Test to find out when this might happen. Perhaps when direcotry is not writable.
            messages.append( "CatGenTimerTask.isValid() - result file (" )
                    .append( this.resultFile.getPath() )
                    .append( ") doesn't exist and can't be created (1)." );
            isValid = false;
          }
        }
        catch (java.io.IOException e)
        {
          messages.append( "CatGenTimerTask.isValid() - result file (" )
                  .append( this.resultFile.getPath() )
                  .append( ") doesn't exist and can't be created (2)." );
          isValid = false;
        }
      }

      // Check that period is valid.
      if ( this.getPeriodInMinutes() < 0)
      {
        messages.append( "CatGenTimerTask.isValid() - period must be zero or above.");
        isValid = false;
      }

      // Check that delay is valid.
      if ( this.getDelayInMinutes() < 0)
      {
        messages.append( "CatGenTimerTask.isValid() - delay must be zero or above.");
        isValid = false;
      }
    }
    else
    {
      messages.append( "CatGenTimerTask.isValid() - period set to zero, skipping all but \"task name\" validity tests.");
    }

    if ( isValid )
    {
      logger.debug( "Config doc valid (" + this.configDoc.toString() + "):");
      logger.debug( messages.toString());
    }
    else
    {
      logger.debug( "Invalid config doc (" + this.configDoc.toString() + "):");
      logger.debug( messages.toString());
    }

    return( isValid);
  }

  //public boolean cancel() // from TimerTask
  //public long scheduledExecutionTime() // from TimerTask
  /** Implementation of the <tt>TimerTask</tt> abstract method, run(). */
  public void run()
  {
    logger.info( "run(): generating catalog <" + this.resultFile.toString() + "> from config doc, " + this.configDocURL.toString());
    CatalogGen catGen = new CatalogGen( this.configDocURL);
    StringBuffer messages = new StringBuffer();
    if ( catGen.isValid( messages))
    {
      catGen.expand();

      // Set the catalog expires date.
      long expireMillis = System.currentTimeMillis() + ( this.periodInMinutes * 60 * 1000);
      Date expireDate = new Date( expireMillis );
      DateType expireDateType = new DateType( false, expireDate);
      catGen.setCatalogExpiresDate( expireDateType);

      // Write the catalog
      if ( catGen.writeCatalog( this.resultFile.toString()))
      {
        logger.debug( "run(): Catalog written (" + this.resultFile.toString() + ").");
      }
      else
      {
        logger.error( "run(): catalog not written (" + this.resultFile.toString() + ").");
      }
    }
    else
    {
      // Invalid CatGen, write to log.
      logger.error( "run(): Tried running CatalogGen with invalid config doc, " + this.configDocURL.toString() +
                    "\n" + messages.toString() );
      // @todo unschedule task by setting period to zero, removing and
      //   adding this task from/to servlet config. How affect servlet config?*/
    }

    return;
  }

  /** Set the value of name. */
  public void setName( String name) { this.name = name; }
  /** Return the value of name. */
  public String getName() { return( this.name); }

  /** Set the value of configDocName. */
  public void setConfigDocName( String configDocName)
  { this.configDocName = configDocName; }
  /** Return the value of configDocName. */
  public String getConfigDocName() { return( this.configDocName); }

//  /** Set the value of configDocURLPath. */
//  public void setConfigDocURLPath( String configDocURLPath)
//  { this.configDocURLPath = configDocURLPath; }
//  /** Return the value of configDocURLPath. */
//  public String getConfigDocURLPath() { return( this.configDocName); }
//                                  //  { return( "*** Server Dependent ***"); }
//                                  //  { return( this.configDocName); }

  /** Set the value of resultFileName. */
  public void setResultFileName( String resultFileName)
  { this.resultFileName = resultFileName; }
  /** Return the value of resultFileName. */
  public String getResultFileName() { return( this.resultFileName); }

  /** Set the value of periodInMinutes. */
  public void setPeriodInMinutes( int periodInMinutes)
  { this.periodInMinutes = periodInMinutes; }
  /** Return the value of periodInMinutes. */
  public int getPeriodInMinutes() { return( this.periodInMinutes); }

  /** Set the value of delayInMinutes. */
  public void setDelayInMinutes( int delayInMinutes)
  { this.delayInMinutes = delayInMinutes; }
  /** Return the value of delayInMinutes. */
  public int getDelayInMinutes() { return( this.delayInMinutes); }

}
/*
 * $Log: CatGenTimerTask.java,v $
 * Revision 1.9  2006/06/08 23:08:41  edavis
 * 1) Use catalog "expires" attribute to determine if TDS static catalog cache is stale:
 *     a) Setup CatalogGen tasks to add "expires" attribute to generated catalog.
 *     b) Add checking for expired catalog and re-reading of catalog from disk in DataRootHandler2.getCatalog()
 * 2) Fix DataRootHandler2 singleton init() and getInstance().
 *
 * Revision 1.8  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.7  2005/12/16 23:19:35  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.6  2005/07/13 22:48:06  edavis
 * Improve server logging, includes adding a final log message
 * containing the response time for each request.
 *
 * Revision 1.5  2005/04/05 22:37:02  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.4  2004/05/11 20:07:52  edavis
 * Hand init() the path for the result files rather than the path of the
 * servlet root. Add some logging.
 *
 * Revision 1.3  2003/09/20 16:16:10  edavis
 * Create subdirectories if needed for result catalog.
 *
 * Revision 1.2  2003/08/29 21:41:47  edavis
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
 * Revision 1.1  2003/03/04 23:09:37  edavis
 * Added for 0.7 release (addition of CatGenServlet).
 *
 *
 */