package thredds.server.catalogservice;

import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogImpl;
import thredds.servlet.ServletUtil;

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

  public static BindingResult bindAndValidate( HttpServletRequest request, boolean localCatalog, XmlHtmlOrEither xmlHtmlOrEither )
  {
    // Bind and validate the request to a CatalogServiceRequest.
    CatalogServiceRequest csr = new CatalogServiceRequest();
    CatalogServiceRequestDataBinder db = new CatalogServiceRequestDataBinder( csr, "request", localCatalog, xmlHtmlOrEither );
    db.setAllowedFields( new String[]{"catalog", "verbose", "command", "htmlView", "dataset"} );
    db.bind( request );
    
    BindingResult bindingResult = db.getBindingResult();
    ValidationUtils.invokeValidator( new CatalogServiceRequestValidator(), bindingResult.getTarget(), bindingResult );

    return bindingResult;
  }

  public static InvDataset subsetCatalog(  InvCatalogImpl catalog, String datasetID)
  {
    InvDataset dataset = catalog.findDatasetByID( datasetID );
    if ( dataset == null )
    {
      //log.warn( "Cant find dataset=" + datasetID + " in catalog=" + catalog.getBaseURI() );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
      //response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Cant find dataset=" + datasetID );
      return null;
    }
    return dataset;

  }
}
