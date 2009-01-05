package thredds.server.catalogservice;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import thredds.servlet.DataRootHandler;
import thredds.servlet.HtmlWriter;
import thredds.servlet.ServletUtil;
import thredds.server.config.TdsContext;
import thredds.server.views.FileView;
import thredds.server.views.InvCatalogXmlView;
import thredds.catalog.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handle requests for catalog service on remote catalogs.
 *
 * Includes validation, subsetting, and HTML view.
 *
 * Currently, handles the following requests:
 * <ul>
 *   <li>Mapping="/remoteCatalogService"</li>
 * </ul>
 *
 * @author edavis
 * @since 4.0
 */
public class RemoteCatalogServiceController extends AbstractController
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private boolean catalogSupportOnly;
  private boolean htmlView;

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

  public boolean isHtmlView()
  {
    return htmlView;
  }

  public void setHtmlView( boolean htmlView )
  {
    this.htmlView = htmlView;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    // Bind HTTP request to a LocalCatalogRequest.
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidateRemoteCatalogRequest( request );

    // If any binding or validation errors, return BAD_REQUEST.
    if ( bindingResult.hasErrors() )
    {
      StringBuilder msg = new StringBuilder( "Bad request" );
      List<ObjectError> oeList = bindingResult.getAllErrors();
      for ( ObjectError e : oeList )
        msg.append( ": " ).append( e.toString() );
      log.error( "handleRequestInternal(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
      return null;
    }

    // Retrieve the resulting CatalogServiceRequest.
    RemoteCatalogRequest catalogServiceRequest = (RemoteCatalogRequest) bindingResult.getTarget();

    // Determine path and catalogPath
    URI uri = catalogServiceRequest.getCatalogUri();

    // Check for matching catalog.
    InvCatalog catalog = null;
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    try
    {
      catalog = fac.readXML( uri );
    }
    catch ( Throwable t )
    {
      String msg = "Bad catalog [" + uri + "]: " + t.getMessage();
      log.error( "handleRequestInternal(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
      return null;
    }

    // If no catalog found, handle as a publicDoc request.
    if ( catalog == null )
    {
      String msg = "No catalog found [" + uri + "].";
      log.error( "handleRequestInternal(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
      return null;
    }

    ///////////////////////////////////////////
    // Otherwise, handle catalog as indicated by "command".
    if ( catalogServiceRequest.getCommand().equals( Command.SHOW))
    {
      if ( this.htmlView )
      {
        HtmlWriter.getInstance().writeCatalog( response, (InvCatalogImpl) catalog, true );
        return null;
        // return constructModelForCatalogView( catalog, this.htmlView );
      }
      else
        return new ModelAndView( new InvCatalogXmlView(), "catalog", catalog );
    }
    else if ( catalogServiceRequest.getCommand().equals( Command.SUBSET ))
    {
      String datasetId = catalogServiceRequest.getDataset();
      InvDataset dataset = catalog.findDatasetByID( datasetId );
      if ( dataset == null )
      {
        String msg = "Did not find dataset [" + datasetId + "] in catalog [" + uri + "].";
        log.error( "handleRequestInternal(): " + msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
        return null;
      }

      if ( this.htmlView)
      {
        HtmlWriter.getInstance().showDataset( uri.toString(), (InvDatasetImpl) dataset, request, response );
        return null;
      }
      else
      {
        catalog.subset( dataset ); // subset the catalog
        return new ModelAndView( new InvCatalogXmlView(), "catalog", catalog );
      }
    }
    return null;
  }

  private ModelAndView handlePublicDocumentRequest( HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    String path )
          throws IOException, ServletException
  {
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

  private ModelAndView constructModelForCatalogView( InvCatalog cat )
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