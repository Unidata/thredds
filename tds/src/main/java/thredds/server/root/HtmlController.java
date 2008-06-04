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

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class HtmlController extends AbstractController
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( HtmlController.class );

  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    String path = req.getPathInfo();
    DataRootHandler drh = DataRootHandler.getInstance();
    String catPath = path.replaceAll( ".html$", ".xml" );
    InvCatalog cat = drh.getCatalog( catPath, null );

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

    Map<String,Object> model = new HashMap<String,Object>();
    model.put( "catalog", cat);
    model.put( "webappVersion", tdsContext.getWebappVersion());
    model.put( "webappName", this.getServletContext().getServletContextName());
    model.put( "docsPath", "http://someserver/thredds/");
    return new ModelAndView( "thredds/server/catalog/catalog", model);
  }
}