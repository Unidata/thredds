package thredds.server.catalogservice;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.servlet.HtmlWriter;
import thredds.servlet.ServletUtil;
import thredds.server.config.TdsContext;
import thredds.server.views.InvCatalogXmlView;
import thredds.catalog.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.URI;

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

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
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
    InvCatalogImpl catalog = null;
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    try
    {
      catalog = fac.readXML( uri );
    }
    catch ( Throwable t )
    {
      String msg = "Error reading catalog [" + uri + "]: " + t.getMessage();
      log.error( "handleRequestInternal(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
      return null;
    }

    // Check whether a catalog was found.
    if ( catalog == null )
    {
      String msg = "Failed to read catalog [" + uri + "].";
      log.error( "handleRequestInternal(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
      return null;
    }

    // Check catalog validity.
    StringBuilder validateMess = new StringBuilder();
    boolean verbose = catalogServiceRequest.isVerbose();
    catalog.check( validateMess, verbose );
    if ( catalog.hasFatalError() )
    {
      Map<String, Object> model = new HashMap<String,Object>();
      model.put( "catalogUrl", uri );
      model.put( "message", validateMess.toString() );
      model.put( "siteLogoPath", HtmlWriter.getInstance().getContextPath() + HtmlWriter.getInstance().getContextLogoPath() );
      model.put( "siteLogoAlt", HtmlWriter.getInstance().getContextLogoAlt() );
      model.put( "serverName", HtmlWriter.getInstance().getContextName() );
      return new ModelAndView( "/thredds/server/catalogservice/validationError", model );
    }

    ///////////////////////////////////////////
    // Otherwise, handle catalog as indicated by "command".
    if ( catalogServiceRequest.getCommand().equals( Command.SHOW))
    {
      HtmlWriter.getInstance().writeCatalog( response, (InvCatalogImpl) catalog, true );
      return null;
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

      if ( catalogServiceRequest.isHtmlView() )
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
    else if ( catalogServiceRequest.getCommand().equals( Command.VALIDATE ) )
    {
      Map<String, Object> model = new HashMap<String, Object>();
      model.put( "catalogUrl", uri );
      model.put( "message", validateMess.toString() );
      model.put( "siteLogoPath", HtmlWriter.getInstance().getContextPath() + HtmlWriter.getInstance().getContextLogoPath() );
      model.put( "siteLogoAlt", HtmlWriter.getInstance().getContextLogoAlt() );
      model.put( "serverName", HtmlWriter.getInstance().getContextName() );
      return new ModelAndView( "/thredds/server/catalogservice/validationMessage", model );
    }
    else
    {
      String msg = "Unsupported request command [" + catalogServiceRequest.getCommand() + "].";
      log.error( "handleRequestInternal(): " + msg + " -- NOTE: Should have been caught on input validation." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
      return null;
    }
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