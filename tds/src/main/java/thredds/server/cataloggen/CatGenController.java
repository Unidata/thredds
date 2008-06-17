package thredds.server.cataloggen;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import thredds.server.config.TdsContext;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenController extends AbstractController
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenController.class );

  private String servletName = "cataloggen";
  private String catGenConfigDirName = "config";
  private String catGenConfigFileName = "config.xml";
  private String catGenResultCatalogsDirName = "catalogs";

  private CatGenConfig config;
  private CatGenConfigParser configParser;

  private CatGenTaskScheduler scheduler;

  private TdsContext tdsContext;
  private CatGenContext catGenContext;

  public void setTdsContext( TdsContext tdsContext ) { this.tdsContext = tdsContext; }
  public void setCatGenContext( CatGenContext catGenContext ) { this.catGenContext = catGenContext; }

  public void init()
  {
    WebApplicationContext webAppContext = this.getWebApplicationContext();
    ServletContext sc = webAppContext.getServletContext();
    catGenContext.init( tdsContext, servletName, catGenConfigDirName, catGenConfigFileName, catGenResultCatalogsDirName);

    // Some debug info.
    log.debug( "init(): CatGen content path = " + this.catGenContext.getContentDirectory() );
    log.debug( "init(): CatGen config path = " + this.catGenContext.getConfigDirectory() );
    log.debug( "init(): CatGen config file = " + this.catGenContext.getConfigFile() );
    log.debug( "init(): CatGen result path = " + this.catGenContext.getResultDirectory() );

    this.configParser = new CatGenConfigParser();
    if ( catGenContext.getConfigFile().exists())
    {
      try
      {
        this.config = configParser.parseXML( catGenContext.getConfigFile() );
      }
      catch ( IOException e )
      {
        log.error( "init(): Failed to close config file InputStream: " + e.getMessage() );
      }
    }
    else
    {
      log.error( "init(): Config file does not exist." );
      this.config = new CatGenConfig( "Config file does not exist." );
    }

    if ( catGenContext.getConfigDirectory().exists()
         && catGenContext.getResultDirectory().exists()
         && ! this.config.getTaskInfoList().isEmpty() )
    {
      this.scheduler = new CatGenTaskScheduler( this.config,
                                                catGenContext.getConfigDirectory(),
                                                catGenContext.getResultDirectory() );
      this.scheduler.start();
    }
    log.info( "init(): done." );
  }

  public void destroy()
  {
    log.debug( "destroy()" );
    if ( this.scheduler != null)
      this.scheduler.stop();
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res ) throws Exception
  {
    return new ModelAndView( "editTask", "config", "junk" );
    //return new ModelAndView( "thredds/server/cataloggen/index", "config", this.config );
  }
}
