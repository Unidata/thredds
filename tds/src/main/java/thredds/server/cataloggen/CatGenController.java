package thredds.server.cataloggen;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
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

  private TdsContext tdsContext;
  private CatGenContext catGenContext;

  public void setTdsContext( TdsContext tdsContext ) { this.tdsContext = tdsContext; }
  public void setCatGenContext( CatGenContext catGenContext ) { this.catGenContext = catGenContext; }

  public void init()
  {
    WebApplicationContext webAppContext = this.getWebApplicationContext();
    ServletContext sc = webAppContext.getServletContext();
    tdsContext.init( sc);
    catGenContext.init( tdsContext, servletName, catGenConfigDirName, catGenConfigFileName, catGenResultCatalogsDirName);

    // Some debug info.
    log.debug( "init(): CatGen content path = " + this.catGenContext.getContentDirectory() );
    log.debug( "init(): CatGen config path = " + this.catGenContext.getConfigDirectory() );
    log.debug( "init(): CatGen config file = " + this.catGenContext.getConfigFile() );
    log.debug( "init(): CatGen result path = " + this.catGenContext.getResultDirectory() );



    
//    // Setup the configuration information.
//    try
//    {
//      this.mainConfig = new CatGenServletConfig( this.catGenResultPath, this.catGenConfigPath, this.configFileName );
//    }
//    catch ( IOException e )
//    {
//      String tmpMsg = "Reading config file failed";
//      log.error( "init(): " + tmpMsg, e );
//      throw new ServletException( tmpMsg + ": " + e.getMessage(), e );
//    }
  }

  public void destroy()
  {
    log.debug( "destroy()" );
//    //this.mainConfig.writeConfig();
//    this.mainConfig.cancelTimer();
//    super.destroy();
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse ) throws Exception
  {
    return new ModelAndView( "thredds/server/cataloggen/index" );
  }
}
