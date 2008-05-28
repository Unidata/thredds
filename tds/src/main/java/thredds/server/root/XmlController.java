package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.servlet.DataRootHandler;
import thredds.server.config.TdsContext;
import thredds.server.views.InvCatalogXmlView;
import thredds.catalog.InvCatalog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class XmlController extends AbstractController
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( XmlController.class );

  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    String path = req.getServletPath();
    DataRootHandler drh = DataRootHandler.getInstance();
    InvCatalog cat = drh.getCatalog( path, null );

    if ( cat == null )
    {
      tdsContext.getDefaultRequestDispatcher().forward( req, res);
    }

    return new ModelAndView( new InvCatalogXmlView(), "catalog", cat);
  }
}