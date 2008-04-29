package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import ucar.nc2.NetcdfFileCache;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.unidata.io.FileCache;
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
public class ContentController extends AbstractController
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
  }

  public void destroy()
  {
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response ) throws Exception
  {
    return null;
  }
}