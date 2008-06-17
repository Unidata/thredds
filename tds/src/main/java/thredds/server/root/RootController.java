package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import thredds.servlet.ServletUtil;
import thredds.server.config.TdsContext;

import java.io.File;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class RootController extends AbstractController
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( RootController.class );

  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  public void init()
  {
    WebApplicationContext webAppContext = this.getWebApplicationContext();
    ServletContext sc = webAppContext.getServletContext();
    initContent();

    // setup logging
    // setup debug
    // setup InvDAtasetScan to know context and catalog servlet paths
    // setup ThreddsConfig
    // setup GRIB indexing
    // setup cacheing

    // optimization: netcdf-3 files can only grow, not have metadata changes
    ucar.nc2.NetcdfFile.setProperty( "syncExtendOnly", "true" );

    // setup aggregation cache and stuff
    // setup fmrc agg definition directory

    

  }

  private void initContent()
          //throws javax.servlet.ServletException
  {

    // first time, create content directory
    File initialContentDirectory = tdsContext.getInitialContentDirectory();
    if ( initialContentDirectory.exists() )
    {
      try
      {
        if ( ServletUtil.copyDir( initialContentDirectory.toString(),
                                  tdsContext.getContentDirectory().toString() ) );
          //log.info( "copyDir " + initialContentPath + " to " + contentPath );
      }
      catch ( IOException ioe )
      {
        //log.error( "failed to copyDir " + initialContentPath + " to " + contentPath, ioe );
      }
    }

  }

  public void destroy()
  {
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response ) throws Exception
  {
    return null;
  }
}
