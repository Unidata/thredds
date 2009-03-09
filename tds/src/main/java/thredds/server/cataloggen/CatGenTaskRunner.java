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
package thredds.server.cataloggen;

import thredds.cataloggen.CatalogGen;
import thredds.servlet.UsageLog;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.net.URL;
import java.net.MalformedURLException;

import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenTaskRunner implements Runnable
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenTaskRunner.class );

  private final CatGenTaskConfig taskConfig;
  private final File configFile;
  private final File resultFile;


  CatGenTaskRunner( CatGenTaskConfig taskConfig, File configDir, File resultDir, File altResultsDir )
  {
    this.taskConfig = taskConfig;
    this.configFile = new File( configDir, taskConfig.getConfigDocName());
    String resultFileName = taskConfig.getResultFileName();
    if ( resultFileName.startsWith( "/" ))
      this.resultFile = new File( altResultsDir, resultFileName.substring( 1 ));
    else
      this.resultFile = new File( resultDir, resultFileName);
  }

  /**
   * Runs the given task.
   */
  public void run()
  {
    log.info( "run(): generating catalog [" + this.resultFile.toString()
              + "] from config doc [" + this.taskConfig.getConfigDocName() + "] - "
              + UsageLog.setupNonRequestContext() );
    URL configFileURL = null;
    try
    {
      configFileURL = this.configFile.toURI().toURL();
    }
    catch ( MalformedURLException e )
    {
      log.error( "run(): Config file path [" + this.configFile + "] gives malformed URL: " + e.getMessage());
      log.info( "run(): done -" + UsageLog.closingMessageNonRequestContext() );
      return;
    }
    CatalogGen catGen = new CatalogGen( configFileURL );

    StringBuilder messages = new StringBuilder();
    if ( catGen.isValid( messages ) )
    {
      catGen.expand();

      // Set the catalog expires date.
      long expireMillis = System.currentTimeMillis() + ( taskConfig.getPeriodInMinutes() * 60 * 1000 );
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
        log.error( "run(): couldn't write catalog: " + e.getMessage() );
        log.info( "run(): done -" + UsageLog.closingMessageNonRequestContext() );
        return;
      }

      log.debug( "run(): Catalog written (" + this.resultFile.toString() + ")." );
    }
    else
    {
      // Invalid CatGen, write to log.
      log.error( "run(): Tried running CatalogGen with invalid config doc, " + taskConfig.getConfigDocName() +
                    "\n" + messages.toString() );
    }
    log.info( "run(): done -" + UsageLog.closingMessageNonRequestContext() );
    return;
  }
}
