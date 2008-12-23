package thredds.server.controller;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import thredds.server.config.TdsContext;
import thredds.util.TdsPathUtils;
import thredds.servlet.ServletUtil;
import thredds.servlet.DataRootHandler;
import thredds.catalog.InvCatalog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceController extends AbstractController
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatalogServiceController.class );

  private TdsContext tdsContext;
  private boolean localCatalog;
  private XmlHtmlOrEither xmlHtmlOrEither;
  private boolean catalogsOnly;

  public static enum XmlHtmlOrEither { XML, HTML, EITHER }

  public CatalogServiceController() { }

  public void setTdsContext( TdsContext tdsContext) { this.tdsContext = tdsContext; }
  public TdsContext getTdsContext() { return this.tdsContext; }

  public void setLocalCatalog( boolean localCatalog ) { this.localCatalog = localCatalog; }
  public boolean isLocalCatalog() { return localCatalog; }

  public void setXmlHtmlOrEither( XmlHtmlOrEither xmlHtmlOrEither )
  {
    this.xmlHtmlOrEither = xmlHtmlOrEither;
  }

  public void setCatalogsOnly( boolean catalogsOnly ) { this.catalogsOnly = catalogsOnly; }
  public boolean getCatalogsOnly() { return this.catalogsOnly; }

  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    // Bind and validate the request to a CatalogServiceRequest.
    CatalogServiceRequest csr = new CatalogServiceRequest();
    CatalogServiceRequestDataBinder db = new CatalogServiceRequestDataBinder( csr, "request", this.localCatalog, false );
    //db.registerCustomEditor( boolean.class, "htmlView", new CatalogServiceRequestDataBinder.ViewEditor() );
    db.setAllowedFields( new String[]{"catalog", "verbose", "command", "htmlView", "dataset"} );

    db.bind( request );

    BindingResult bindingResult = db.getBindingResult();
    ValidationUtils.invokeValidator( new CatalogServiceRequestValidator(), bindingResult.getTarget(), bindingResult );

    if ( bindingResult.hasErrors() )
    {
      StringBuilder msg = new StringBuilder( "Bad request");
      List<ObjectError> oeList = bindingResult.getAllErrors();
      for ( ObjectError e : oeList )
        msg.append( ": ").append( e.toString());
      log.error( "handle(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
      response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
    }

    // Check for matching catalog.
    DataRootHandler drh = DataRootHandler.getInstance();
    String path = csr.getCatalog();
    if ( path.endsWith( ".html" ))
    {
      if ( ! this.localCatalog )
      {
        String msg = "Bad catalog URI [" + path + "].";
        log.error( "handle(): " + msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
      }
    }
    String catPath = path.replaceAll( ".html$", ".xml" );

    InvCatalog cat = null;
    String baseUriString = request.getRequestURI();
    try
    {
      cat = drh.getCatalog( catPath, new URI( baseUriString ) );
    }
    catch ( URISyntaxException e )
    {
      log.error( "handleRequestInternal(): bad URI syntax [" + baseUriString + "]: " + e.getMessage() );
      cat = null;
    }

    Map model = new HashMap();
    model.put( "path", request.getPathInfo() );
    model.put( "catalog", csr.getCatalog() );
    model.put( "dataset", csr.getDataset() );
    model.put( "command", csr.getCommand() );
    model.put( "view", csr.isHtmlView() );
    model.put( "verbose", csr.isVerbose() );

    return new ModelAndView( "catServiceReq", model );

  }
}
