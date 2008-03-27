package thredds.server.cataloggen;

import thredds.cataloggen.CatalogGen;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenTask implements Runnable
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenTask.class );

  private final int periodInMinutes;
  private final URL configDocURL;
  private final File resultFile;


  private CatGenTask( URL configDocURL, File resultFile,
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
    log.info( "run(): generating catalog <" + this.resultFile.toString() + "> from config doc, " + this.configDocURL.toString() );
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
      log.error( "run(): Tried running CatalogGen with invalid config doc, " + this.configDocURL.toString() +
                    "\n" + messages.toString() );
      //this.cancel();
    }

    return;
  }
}
