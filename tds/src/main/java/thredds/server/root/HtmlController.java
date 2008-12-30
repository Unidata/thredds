package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.servlet.DataRootHandler;
import thredds.servlet.HtmlWriter;
import thredds.servlet.ServletUtil;
import thredds.server.config.TdsContext;
import thredds.server.views.FileView;
import thredds.server.catalogservice.CatalogServiceUtils;
import thredds.server.catalogservice.CatalogServiceRequest;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handle all requests for HTML views of catalogs and any publicDoc HTML files.
 *
 * Currently, handles the following requests:
 * <table style="text-align: left; width: 80%;" border="1"
 cellpadding="2" cellspacing="2">

 * <ul>
 *   <li>Mapping="/catalog/*" -- ServletPath="/catalog" and PathInfo="/some/path/*.html"</li>
 *   <li>Mapping="*.html"     -- ServletPath="/some/path/*.html" and PathInfo=null</li>
 * </ul>
 *
 * @author edavis
 * @since 4.0
 */
public class HtmlController extends AbstractController
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( HtmlController.class );

  private TdsContext tdsContext;
  private boolean catalogSupportOnly;

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

  protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
          throws Exception
  {
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidate( request, true, CatalogServiceUtils.XmlHtmlOrEither.HTML );

    if ( bindingResult.hasErrors() )
    {
      StringBuilder msg = new StringBuilder( "Bad request" );
      List<ObjectError> oeList = bindingResult.getAllErrors();
      for ( ObjectError e : oeList )
        msg.append( ": " ).append( e.toString() );
      log.error( "handle(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
    }

    // Retrieve the resulting CatalogServiceRequest.
    CatalogServiceRequest catalogServiceRequest = (CatalogServiceRequest) bindingResult.getTarget();

    // Determine Path
    String path = catalogServiceRequest.getCatalog();

    // Check for matching catalog.
    DataRootHandler drh = DataRootHandler.getInstance();
    String catPath = path.replaceAll( ".html$", ".xml" );

    InvCatalog cat = null;
    String baseUriString = request.getRequestURI();
    try
    {
      cat = drh.getCatalog( catPath, new URI( baseUriString ) );
    }
    catch ( URISyntaxException e )
    {
      log.error( "handleRequestInternal(): bad URI syntax [" + baseUriString + "]: " + e.getMessage());
      cat = null;
    }

    // If matching catalog found, setup model and hand to catalog HTML view.
    if ( cat != null )
    {
      if ( true )
      {
        HtmlWriter.getInstance().writeCatalog( response, (InvCatalogImpl) cat, true );
        return null;
      }
      else
      {
        // Hand to catalog view.
        String catName = cat.getName();
        String catUri = cat.getUriString();
        if ( catName == null )
        {
          List childrenDs = cat.getDatasets();
          if ( childrenDs.size() == 1 )
          {
            InvDatasetImpl onlyChild = (InvDatasetImpl) childrenDs.get( 0 );
            catName = onlyChild.getName();
          }
          else
            catName = "";
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put( "catalog", cat );
        model.put( "catalogName", HtmlUtils.htmlEscape( catName ) );
        model.put( "catalogUri", HtmlUtils.htmlEscape( catUri ) );
        model.put( "webappName", this.getServletContext().getServletContextName() );
        model.put( "webappVersion", tdsContext.getWebappVersion() );
        model.put( "webappBuildDate", tdsContext.getWebappBuildDate() );
        model.put( "webappDocsPath", tdsContext.getTdsConfigHtml().getWebappDocsPath() );
        return new ModelAndView( "thredds/server/catalog/catalog", model );
      }
    }

    // If not supporting access to public document files, send not found response.
    if ( this.catalogSupportOnly )
    {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return null;
    }

    // If request doesn't match a known catalog, look for a public document.
    File publicFile = tdsContext.getPublicDocFileSource().getFile( path );
    if ( publicFile != null )
      return new ModelAndView( new FileView(), "file", publicFile );

    // If request doesn't match a public document, hand to default.
    tdsContext.getDefaultRequestDispatcher().forward( request, response );
    return null;
  }
}