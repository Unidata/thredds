package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.servlet.DataRootHandler;
import thredds.servlet.ServletUtil;
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
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( XmlController.class );

  private TdsContext tdsContext;
  private boolean catalogSupportOnly = true;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  public boolean isCatalogSupportOnly()
  {
    return catalogSupportOnly;
  }

  public void setCatalogSupportOnly( boolean catalogSupportOnly )
  {
    this.catalogSupportOnly = catalogSupportOnly;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    // Determine Path
    String path = req.getPathInfo();
    if ( path == null )
      path = req.getServletPath();
    if ( path.equals( "" ) )
    {
      String msg = "Root servlet mapping (\"/*\") not supported.";
      log.error( "handleRequestInternal(): " + msg );
      throw new IllegalStateException( msg );
    }

    // Check for matching catalog.
    DataRootHandler drh = DataRootHandler.getInstance();
    InvCatalog cat = null;
    String baseUriString = req.getRequestURI();
    try
    {
      cat = drh.getCatalog( path, new URI( baseUriString ) );
    }
    catch ( URISyntaxException e )
    {
      log.error( "handleRequestInternal(): bad URI syntax [" + baseUriString + "]: " + e.getMessage() );
      cat = null;
    }

    // If matching catalog found, hand to catalog XML view.
    if ( cat != null )
      return new ModelAndView( new InvCatalogXmlView(), "catalog", cat );

    // If not supporting access to public document files, send not found response.
    if ( this.catalogSupportOnly )
    {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError( HttpServletResponse.SC_NOT_FOUND );
      return null;
    }

    // If request doesn't match a known catalog, look for a public document.
    File publicFile = tdsContext.getPublicDocFileSource().getFile( path );
    if ( publicFile != null )
      return new ModelAndView( new FileView(), "file", publicFile );

    // If request doesn't match a public document, hand to default.
    tdsContext.getDefaultRequestDispatcher().forward( req, res);
    return null;
  }
}