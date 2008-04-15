package thredds.server.cataloggen;

import thredds.cataloggen.CatalogGen;

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


  CatGenTaskRunner( CatGenTaskConfig taskConfig, File configDir, File resultDir )
  {
    this.taskConfig = taskConfig;
    this.configFile = new File( configDir, taskConfig.getConfigDocName());
    this.resultFile = new File( resultDir, taskConfig.getResultFileName());
  }

  /**
   * Runs the given task.
   */
  public void run()
  {
    log.info( "run(): generating catalog <" + this.resultFile.toString() + "> from config doc, " + this.taskConfig.getConfigDocName() );
    URL configFileURL = null;
    try
    {
      configFileURL = this.configFile.toURI().toURL();
    }
    catch ( MalformedURLException e )
    {
      log.error( "runTask(): Config file path <" + this.configFile + "> gives malformed URL: " + e.getMessage());
      return;
    }
    CatalogGen catGen = new CatalogGen( configFileURL );

    StringBuffer messages = new StringBuffer();
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

    return;
  }
}
