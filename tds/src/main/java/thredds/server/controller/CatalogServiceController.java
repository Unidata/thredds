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

  public static enum XmlHtmlOrEither { XML, HTML, EITHER }

  public CatalogServiceController() { }

  public void setTdsContext( TdsContext tdsContext) { this.tdsContext = tdsContext; }
  public TdsContext getTdsContext() { return this.tdsContext; }

  public void setLocalCatalog( boolean localCatalog ) { this.localCatalog = localCatalog; }
  public boolean isLocalCatalog() { return localCatalog; }

  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    // Bind and validate the request.
    BindingResult bindingResult = CatalogServiceUtils.bindAndValidate( request, this.localCatalog, CatalogServiceUtils.XmlHtmlOrEither.EITHER );

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

    // Retrieve the resulting request.
    CatalogServiceRequest catalogServiceRequest = (CatalogServiceRequest) bindingResult.getTarget();

    // Check for matching catalog.
    DataRootHandler drh = DataRootHandler.getInstance();
    String path = catalogServiceRequest.getCatalog();
    if ( path.endsWith( ".html" ))
    {
      if ( ! this.localCatalog )
      {
        String msg = "Bad catalog URI [" + path + "].";
        log.error( "handleRequestInternal(): " + msg );
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

    if ( cat == null )
    {
      String msg = "Did not find requested catalog [" + path + "].";
      log.error( "handleRequestInternal(): " + msg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, msg.length() );
      response.sendError( HttpServletResponse.SC_NOT_FOUND, msg );
    }

    Map model = new HashMap();
    model.put( "path", request.getPathInfo() );
    model.put( "catalog", catalogServiceRequest.getCatalog() );
    model.put( "dataset", catalogServiceRequest.getDataset() );
    model.put( "command", catalogServiceRequest.getCommand() );
    model.put( "view", catalogServiceRequest.isHtmlView() );
    model.put( "verbose", catalogServiceRequest.isVerbose() );

    return new ModelAndView( "catServiceReq", model );

  }
}
