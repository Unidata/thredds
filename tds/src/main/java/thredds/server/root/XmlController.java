package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.servlet.DataRootHandler;
import thredds.server.config.TdsContext;
import thredds.server.views.InvCatalogXmlView;
import thredds.server.views.FileView;
import thredds.catalog.InvCatalog;

import java.io.File;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;

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
    if ( path == null || path.equals( "" ) )
      path = req.getPathInfo();
    DataRootHandler drh = DataRootHandler.getInstance();

    InvCatalog cat = null;
    String baseUriString = req.getRequestURI();
    try
    {
      cat = drh.getCatalog( path, new URI( baseUriString ) );
    }
    catch ( URISyntaxException e )
    {
      logger.error( "handleRequestInternal(): bad URI syntax [" + baseUriString + "]: " + e.getMessage() );
      cat = null;
    }

    if ( cat == null )
    {
      File publicFile = tdsContext.getPublicDocFileSource().getFile( path );
      if ( publicFile == null )
      {
        tdsContext.getDefaultRequestDispatcher().forward( req, res);
        return null;
      }
      return new ModelAndView( new FileView(), "file", publicFile);
    }

    return new ModelAndView( new InvCatalogXmlView(), "catalog", cat);
  }
}