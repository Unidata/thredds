package thredds.catalog.util;

import thredds.catalog.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.2
 */
public class DeepCopyUtils
{
  private DeepCopyUtils() {}

  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( DeepCopyUtils.class );

  public static InvCatalog copyCatalog( InvCatalog catalog )
  {
    if ( catalog == null ) throw new IllegalArgumentException( "Catalog may not be null." );

    InvCatalogImpl resultCatalog = new InvCatalogImpl( catalog.getName(), "1.0", catalog.getExpires(), ((InvCatalogImpl) catalog).getBaseURI() );

    List<InvService> copiedServices = copyServicesIntoCopiedCatalog( catalog, resultCatalog );

    for ( InvDataset curDs : catalog.getDatasets()) {
      resultCatalog.addDataset( (InvDatasetImpl) DeepCopyUtils.copyDataset( curDs, copiedServices, false ) );
    }

    resultCatalog.finish();
    return resultCatalog;
  }

  private static List<InvService> copyServicesIntoCopiedCatalog( InvCatalog catalog, InvCatalogImpl resultCatalog )
  {
    List<InvService> services = catalog.getServices();
    for ( InvService service : services ) {
      resultCatalog.addService( DeepCopyUtils.copyService( service ) );
    }
    List<InvService> copiedServices = resultCatalog.getServices();
    return copiedServices;
  }

  public static InvCatalog subsetCatalogOnDataset( InvCatalog catalog, String datasetId )
  {
    if ( catalog == null ) throw new IllegalArgumentException( "Catalog may not be null." );
    if ( datasetId == null ) throw new IllegalArgumentException( "Dataset ID may not be null." );
    InvDataset ds = catalog.findDatasetByID( datasetId );
    if ( ds == null )
      throw new IllegalArgumentException( "The dataset ID [" + datasetId + "] does not match the ID of a dataset in the catalog." );
    return subsetCatalogOnDataset( catalog, ds );
  }

  public static InvCatalog subsetCatalogOnDataset( InvCatalog catalog, InvDataset dataset)
  {
    if ( catalog == null ) throw new IllegalArgumentException( "Catalog may not be null." );
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset may not be null." );
    if ( dataset.getParentCatalog() != catalog )
      throw new IllegalArgumentException( "Catalog must contain the dataset." );

    URI docBaseUri = formDocBaseUriForSubsetCatalog( catalog, dataset );

    InvCatalogImpl resultCatalog = new InvCatalogImpl( dataset.getName(), "1.0", docBaseUri );

    List<InvService> copiedServices = copyServicesIntoCopiedCatalog( catalog, resultCatalog );

    InvDataset topDs = DeepCopyUtils.copyDataset( dataset, copiedServices, true );

    resultCatalog.addDataset( (InvDatasetImpl) topDs );

    resultCatalog.finish();
    return resultCatalog;
  }

  private static URI formDocBaseUriForSubsetCatalog( InvCatalog catalog, InvDataset dataset )
  {
    String catDocBaseUri = catalog.getUriString();
    String subsetDocBaseUriString = catDocBaseUri + "/" + ( dataset.getID() != null ? dataset.getID() : dataset.getName() );
    URI thisDocBaseUri = ((InvCatalogImpl) catalog ).getBaseURI();
    try {
//      String uriPath = thisDocBaseUri.getPath() + "/" + ( dataset.getID() != null ? dataset.getID() : dataset.getName() );
//      URI subsetDocBaseUri = new URI( thisDocBaseUri.getScheme(), thisDocBaseUri.getUserInfo(), thisDocBaseUri.getHost(),
//                                      thisDocBaseUri.getPort(), uriPath, null, null );
      URI subsetDocBaseUri = new URI( subsetDocBaseUriString);
      return subsetDocBaseUri;
    }
    catch ( URISyntaxException e ) {
      // This shouldn't happen. But just in case ...
      throw new IllegalStateException( "Bad document Base URI for new catalog [" + catalog.getUriString() + "/" + (dataset.getID() != null ? dataset.getID() : dataset.getName()) + "].", e );
    }
  }

//  public static InvDataset copyDatasetAddingInheritedMetadata( InvDataset dataset, List<ThreddsMetadata> inheritedMd, List<InvService> availableServices)
//  {
//
//  }

  public static InvDataset copyDataset( InvDataset dataset, List<InvService> availableServices,
                                        boolean copyInheritedMetadataFromParents )
  {
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset may not be null." );
    if ( availableServices == null ) throw new IllegalArgumentException( "List of available services may not be null.");

    InvDatasetImpl resultDs = null;
    // ToDo Deal with InvDatasetScan and its ilk.
    if ( dataset instanceof InvCatalogRef )
    {
      InvCatalogRef catRef = (InvCatalogRef) dataset;
      resultDs = new InvCatalogRef( null, catRef.getName(), catRef.getXlinkHref());
    } else {
      resultDs = new InvDatasetImpl( null, dataset.getName() );
    }
    resultDs.setID( dataset.getID() );

    resultDs.transferMetadata( (InvDatasetImpl) dataset, copyInheritedMetadataFromParents );

    // Only copy child InvAccess if the current dataset is not an InvCatalogRef.
    if ( !( dataset instanceof InvCatalogRef ) ) {
      String urlPath = ( (InvDatasetImpl) dataset ).getUrlPath();
      if ( urlPath != null )
        resultDs.setUrlPath( urlPath );
      else
      {
        for ( InvAccess curAccess : dataset.getAccess()) {
          InvAccess access = copyAccess( curAccess, resultDs, availableServices );
          if ( access != null ) resultDs.addAccess( access );
        }
      }
    }

    // Only recurse into child datasets if the current dataset is not an InvCatalogRef.
    if ( !( dataset instanceof InvCatalogRef ) ) {
      for ( InvDataset curDs : dataset.getDatasets() )
      {
        InvDatasetImpl curDsCopy = (InvDatasetImpl) copyDataset( curDs, availableServices, false );
        curDsCopy.setParent( resultDs );
        resultDs.addDataset( curDsCopy );
      }
    }

    return resultDs;
  }

  public static InvAccess copyAccess( InvAccess access, InvDataset parentDataset, List<InvService> availableServices )
  {
    if ( parentDataset == null ) throw new IllegalArgumentException( "Parent dataset may not be null.");
    String serviceName = access.getService().getName();
    InvService service = findServiceByName( serviceName, availableServices);
    if ( service == null ) {
      logger.warn( "Access service [" + serviceName + "] not in available service list.");
      return null;
      // ToDo Support copying service.
      //service = copyService( access.getService() );
    }
    DataFormatType dataFormatType = access.getDataFormatType();
    InvAccessImpl resultAccess = null;
    if ( dataFormatType == null ) {
      resultAccess = new InvAccessImpl( parentDataset, access.getUrlPath(), service );
      resultAccess.setSize( access.getDataSize() );
    }
    else
      resultAccess = new InvAccessImpl( parentDataset, access.getUrlPath(), service.getName(), null,
                                        dataFormatType.toString(), access.getDataSize() );

    return resultAccess;
  }

  static InvService findServiceByName( String name, List<InvService> servicePool)
  {
    if ( servicePool == null ) return null;

    for ( InvService curService : servicePool )
    {
      if (curService.getName().equals( name ))
        return curService;
      List<InvService> nestedServices = curService.getServices();
      InvService target = nestedServices != null ? findServiceByName( name, nestedServices ) : null;
      if ( target != null )
        return target;
    }
    return null;
  }

  public static InvService copyService( InvService service )
  {
    if ( service == null ) throw new IllegalArgumentException( "Service may not be null.");
    InvService resultService = new InvService( service.getName(), service.getServiceType().toString(),
                                               service.getBase(), service.getSuffix(), service.getDescription() );
    for ( InvService curService : service.getServices()) {
      resultService.addService( copyService( curService ) );
    }
    for ( InvProperty curProperty : service.getProperties()) {
      resultService.addProperty( copyProperty( curProperty ) );
    }
    for ( InvProperty curDatasetRoot : service.getDatasetRoots()) {
      resultService.addDatasetRoot( copyProperty( curDatasetRoot) );
    }

    return resultService;
  }

  public static InvProperty copyProperty( InvProperty property) {
    return new InvProperty( property.getName(), property.getValue());
  }
}
