package thredds.server.catalogservice;

import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogImpl;
import thredds.servlet.UsageLog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceUtils
{
  private CatalogServiceUtils() {}

  public static BindingResult bindAndValidateRemoteCatalogRequest( HttpServletRequest request )
  {
    // Bind and validate the request to a RemoteCatalogRequest.
    RemoteCatalogRequest rcr = new RemoteCatalogRequest();
    RemoteCatalogRequestDataBinder db = new RemoteCatalogRequestDataBinder( rcr, "request" );
    db.setAllowedFields( new String[]{"catalogUri", "command", "dataset", "verbose", "htmlView"} );
    db.bind( request );
    
    BindingResult bindingResult = db.getBindingResult();
    ValidationUtils.invokeValidator( new RemoteCatalogRequestValidator(), bindingResult.getTarget(), bindingResult );

    return bindingResult;
  }

  public static BindingResult bindAndValidateLocalCatalogRequest( HttpServletRequest request,
                                                                  boolean htmlView )
  {
    // Bind and validate the request to a LocalCatalogRequest.
    LocalCatalogRequest rcr = new LocalCatalogRequest();
    LocalCatalogRequestDataBinder db = new LocalCatalogRequestDataBinder( rcr, "request" );
    db.setAllowedFields( new String[]{"path", "command", "dataset"} );
    db.bind( request );

    BindingResult bindingResult = db.getBindingResult();
    LocalCatalogRequestValidator validator = new LocalCatalogRequestValidator();
    validator.setHtmlView( htmlView );
    ValidationUtils.invokeValidator( validator, bindingResult.getTarget(), bindingResult );

    return bindingResult;
  }

}
