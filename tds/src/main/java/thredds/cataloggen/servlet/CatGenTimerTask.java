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
{
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( CatGenTimerTask.class);

  /** The name of this task. */
  private final String name;

  /** The name of the configuration document to be used by this task. */
  private final String configDocName;

  /** The filename in which to store the results of this task. */
  private final String resultFileName;

  /** The time in minutes between runs of this task. */
  private final int periodInMinutes;

  /** The time in minutes to delay the initial run of this task. */
  private final int delayInMinutes;

  private File configDoc = null;
  private URL configDocURL = null;
  private File resultFile = null;

  // @GuardedBy("this")
  private MyTimerTask timerTask = null;

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
    if ( name == null || name.equals( "") )
    {
      logger.error( "ctor(): The name cannot be null or empty string." );
      throw new IllegalArgumentException( "The name cannot be null or empty string." );
    }
    if ( configDocName == null || configDocName.equals( "") )
    {
      logger.error( "ctor(): The config doc name cannot be null or empty string." );
      throw new IllegalArgumentException( "The config doc name cannot be null or empty string." );
    }
    if ( resultFileName == null || resultFileName.equals( "") )
    {
      logger.error("ctor(): The result file name cannot be null or empty string.");
      throw new IllegalArgumentException( "The result file name cannot be null or empty string.");
    }
    this.name = name;
    this.configDocName = configDocName;
    this.resultFileName = resultFileName;
    this.periodInMinutes = periodInMinutes;
    this.delayInMinutes = delayInMinutes;
  }

  public CatGenTimerTask( CatGenTimerTask task )
  {
    this.name = task.getName();
    this.configDocName = task.getConfigDocName();
    this.resultFileName = task.getResultFileName();
    this.periodInMinutes = task.getPeriodInMinutes();
    this.delayInMinutes = task.getDelayInMinutes();
  }

  /** Return the value of name. */
  public String getName() { return( this.name); }

  /** Return the value of configDocName. */
  public String getConfigDocName() { return( this.configDocName); }

  /** Return the value of resultFileName. */
  public String getResultFileName() { return( this.resultFileName); }

  /** Return the value of periodInMinutes. */
  public int getPeriodInMinutes() { return( this.periodInMinutes); }

  /** Return the value of delayInMinutes. */
  public int getDelayInMinutes() { return( this.delayInMinutes); }

  public synchronized TimerTask getTimerTask()
  {
    if ( this.timerTask == null )
    {
      logger.error( "getTimerTask(): CatGenTimerTask <" + this.name + "> has not been initialized." );
      throw new IllegalStateException( "Must call init() first.");
    }
    return this.timerTask;
  }

  /**
   * Initialize with resultPath and configDocPath.
   *
   * @param configDocPath - the URL for the config file.
   */
  public synchronized void init( File resultPath, File configDocPath)
  {
    if ( this.timerTask != null )
    {
      logger.error( "init(): CatGenTimerTask <" + this.name + "> has already been initialized." );
      throw new IllegalStateException( "init() has already been called.");
    }

    this.configDoc = new File( configDocPath, this.configDocName);
    try
    {
      URI tmpURI = this.configDoc.toURI();
      this.configDocURL = tmpURI.toURL();
    }
    catch ( MalformedURLException e )
    {
      logger.error( "init(): Bad URL for config File <" + this.configDoc.getPath() + ">: " + e.getMessage());
      throw new IllegalArgumentException( "init(): config file doesn't convert to valid URI: " + e.getMessage() );
    }

    this.resultFile = new File( resultPath, this.resultFileName);
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
          logger.error( "init(): Could not create directory \"" +
                       this.resultFile.getParentFile().getAbsolutePath() + "\", result file " +
                       "(" + this.resultFile.getAbsolutePath() + ")invalid.");
          throw new IllegalArgumentException( "Result file directory doesn't exist and couldn't be created." );
        }
      }
    }

    logger.debug( "init(): result path is " + resultPath.toString());
    logger.debug( "init(): config doc path is " + configDocPath.toString());
    logger.debug( "init(): config doc URL is " + this.configDocURL.toString());
    logger.debug( "init(): config doc is " + this.configDoc.toString());
    logger.debug( "init(): result file is " + this.resultFile.toString());

    this.timerTask = new MyTimerTask( this.configDocURL, this.resultFile, this.periodInMinutes );
  }

  /**
   * Test whether this task is valid, including the validity of the config file.
   * @param messages - StringBuffer for appending error and warning messages.
   * @return - true if task is valid, false if invalid (errors)
   */
  public synchronized boolean isValid( StringBuffer messages)
  {
    if ( this.timerTask == null )
    {
      logger.error( "isValid(): CatGenTimerTask <" + this.name + "> has not been initialized.");
      return false;
    }

    boolean isValid = true;

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
          messages.append( "CatGenTimerTask.isValid() - result file not writeable.\n");
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
                    .append( ") doesn't exist and can't be created (1).\n" );
            isValid = false;
          }
        }
        catch (java.io.IOException e)
        {
          messages.append( "CatGenTimerTask.isValid() - result file (" )
                  .append( this.resultFile.getPath() )
                  .append( ") doesn't exist and can't be created (2).\n" );
          isValid = false;
        }
      }

      // Check that period is valid.
      if ( this.getPeriodInMinutes() < 0)
      {
        messages.append( "CatGenTimerTask.isValid() - period must be zero or above.\n");
        isValid = false;
      }

      // Check that delay is valid.
      if ( this.getDelayInMinutes() < 0)
      {
        messages.append( "CatGenTimerTask.isValid() - delay must be zero or above.\n");
        isValid = false;
      }
    }
    else
    {
      messages.append( "CatGenTimerTask.isValid() - period set to zero, skipping all but \"task name\" validity tests.\n");
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

  private class MyTimerTask extends TimerTask
  {
    private final int periodInMinutes;
    private final URL configDocURL;
    private final File resultFile;


    private MyTimerTask( URL configDocURL, File resultFile,
                         int periodInMinutes )
    {
      this.configDocURL = configDocURL;
      this.resultFile = resultFile;
      this.periodInMinutes = periodInMinutes;
    }
    /**
     * Implementation of the <tt>TimerTask</tt> abstract method, run().
     */
    public void run()
    {
      logger.info( "run(): generating catalog <" + this.resultFile.toString() + "> from config doc, " + this.configDocURL.toString() );
      CatalogGen catGen = new CatalogGen( this.configDocURL );
      StringBuffer messages = new StringBuffer();
      if ( catGen.isValid( messages ) )
      {
        catGen.expand();

        // Set the catalog expires date.
        long expireMillis = System.currentTimeMillis() + ( this.periodInMinutes * 60 * 1000 );
        Date expireDate = new Date( expireMillis );
        DateType expireDateType = new DateType( false, expireDate );
        catGen.setCatalogExpiresDate( expireDateType );

        // Write the catalog
        if ( catGen.writeCatalog( this.resultFile.toString() ) )
        {
          logger.debug( "run(): Catalog written (" + this.resultFile.toString() + ")." );
        }
        else
        {
          logger.error( "run(): catalog not written (" + this.resultFile.toString() + ")." );
        }
      }
      else
      {
        // Invalid CatGen, write to log.
        logger.error( "run(): Tried running CatalogGen with invalid config doc, " + this.configDocURL.toString() +
                      "\n" + messages.toString() );
        this.cancel();
      }

      return;
    }
  }
}