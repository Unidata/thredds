/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.cataloggen.servlet;

import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.io.File;
import java.io.IOException;

import thredds.cataloggen.*;
import ucar.nc2.units.DateType;

/**
 * Describes a task to be run by the CatalogGenerator Servlet.
 *
 * <tt>CatGenTimerTask</tt> is a subclass of <tt>TimerTask</tt>
 * so that it can be run by a <tt>Timer</tt>. It is also a Bean
 * so that it can be stored using the ucar.util.prefs package.
 *
 * @deprecated Instead see {@link thredds.server.cataloggen.CatGenTaskScheduler} which is used by {@link thredds.server.cataloggen.CatGenController}.
 */
class CatGenTimerTask
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
  CatGenTimerTask( String name,
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

  CatGenTimerTask( CatGenTimerTask task )
  {
    this.name = task.getName();
    this.configDocName = task.getConfigDocName();
    this.resultFileName = task.getResultFileName();
    this.periodInMinutes = task.getPeriodInMinutes();
    this.delayInMinutes = task.getDelayInMinutes();
  }

  /** Return the value of name. */
  String getName() { return( this.name); }

  /** Return the value of configDocName. */
  String getConfigDocName() { return( this.configDocName); }

  /** Return the value of resultFileName. */
  String getResultFileName() { return( this.resultFileName); }

  /** Return the value of periodInMinutes. */
  int getPeriodInMinutes() { return( this.periodInMinutes); }

  /** Return the value of delayInMinutes. */
  int getDelayInMinutes() { return( this.delayInMinutes); }

  synchronized TimerTask getTimerTask()
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
  synchronized void init( File resultPath, File configDocPath)
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
  synchronized boolean isValid( StringBuilder messages)
  {
    if ( this.timerTask == null )
    {
      logger.error( "isValid(): CatGenTimerTask <" + this.name + "> has not been initialized.");
      return false;
    }

    boolean isValid = true;

    // Skip most validation if period set to zero.
    if ( this.getPeriodInMinutes() == 0 )
    {
      messages.append( "CatGenTimerTask.isValid() - period set to zero, skipping all but \"task name\" validity tests.\n");
    }
    // Otherwise, do full validation.
    else
    {
      // Check that catalog is valid.
      CatalogGen catGen = new CatalogGen( this.configDocURL);
      if ( ! catGen.isValid( messages) )
      {
        logger.debug( "isValid(): config doc <" + this.configDocURL + "> is not valid." );
        isValid = false;
      }

      // If the result file already exists, make sure we can write to it.
      if ( this.resultFile.exists())
      {
        if ( ! this.resultFile.canWrite())
        {
          messages.append( "CatGenTimerTask.isValid() - result file not writeable.\n");
          logger.warn( "isValid(): Result file is not writable.");
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

    if ( isValid )
    {
      logger.debug( "Config doc valid (" + this.configDoc.toString() + "): " + messages.toString());
    }
    else
    {
      logger.debug( "Invalid config doc (" + this.configDoc.toString() + "): " + messages.toString());
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
      StringBuilder messages = new StringBuilder();
      if ( catGen.isValid( messages ) )
      {
        catGen.expand();

        // Set the catalog expires date.
        long expireMillis = System.currentTimeMillis() + ( this.periodInMinutes * 60 * 1000 );
        Date expireDate = new Date( expireMillis );
        DateType expireDateType = new DateType( false, expireDate );
        catGen.setCatalogExpiresDate( expireDateType );

        // Write the catalog
        try
        {
          catGen.writeCatalog( this.resultFile.toString() );
        }
        catch ( IOException e )
        {
          logger.error( "run(): couldn't write catalog: " + e.getMessage() );
          return;
        }

        logger.debug( "run(): Catalog written (" + this.resultFile.toString() + ")." );
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