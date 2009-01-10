package thredds.server.catalogservice;

import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogImpl;
import thredds.servlet.ServletUtil;
import thredds.servlet.AccessLog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceUtils
{
  private CatalogServiceUtils() {}

  public static enum XmlHtmlOrEither { XML, HTML, EITHER }

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

  public static InvDataset subsetCatalog(  InvCatalogImpl catalog, String datasetID)
  {
    InvDataset dataset = catalog.findDatasetByID( datasetID );
    if ( dataset == null )
    {
      //log.warn( "Cant find dataset=" + datasetID + " in catalog=" + catalog.getBaseURI() );
      AccessLog.log.info( AccessLog.accessInfo( HttpServletResponse.SC_BAD_REQUEST, -1 ));
      //response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Cant find dataset=" + datasetID );
      return null;
    }
    return dataset;

  }
}
