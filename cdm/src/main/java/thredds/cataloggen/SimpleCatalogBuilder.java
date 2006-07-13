// $Id: SimpleCatalogBuilder.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.catalog.*;

import java.io.IOException;

import org.jdom.Document;

/**
 * Provide a simple interface for building single-level catalogs at any level
 * of the specified collection.
 *
 * @author edavis
 * @since Nov 30, 2005 3:17:23 PM
 */
public class SimpleCatalogBuilder implements CatalogBuilder
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( SimpleCatalogBuilder.class );

  private String collectionPath;

  private CrawlableDataset collectionCrDs;
  private InvService service;

  /**
   * Construct a SimpleCatalogBuilder for a given collection.
   *
   * @param collectionPath an path for the collection to be cataloged (used as the base of the generated URL, can be an empty string or null). If null, the collectionLevel final path segment is used.
   * @param collectionCrDs the root level of the collection to be cataloged.
   * @param serviceName specifies the name of the service to be used in the resulting catalogs.
   * @param serviceTypeName specifies the THREDDS service type, e.g., "OPENDAP"
   * @param serviceURL the base URL to the service
   */
  public SimpleCatalogBuilder( String collectionPath, CrawlableDataset collectionCrDs, String serviceName, String serviceTypeName, String serviceURL )
  {

    this.collectionPath = ( collectionPath == null
                            ? collectionCrDs.getName()
                            : collectionPath );
    this.collectionCrDs = collectionCrDs;

    service = new InvService( serviceName, serviceTypeName, serviceURL, null, null );
  }

  public CrawlableDataset requestCrawlableDataset( String path ) throws IOException
  {
    return CatalogBuilderHelper.verifyDescendentDataset( collectionCrDs, path, null );
  }

  public InvCatalogImpl generateCatalog( CrawlableDataset catalogCrDs ) throws IOException
  {
    CollectionLevelScanner scanner = new CollectionLevelScanner( collectionPath, collectionCrDs, catalogCrDs, null, null, service );

    scanner.scan();
    return scanner.generateCatalog();
  }

  /** Not supported by SimpleCatalogBuilder. */
  public InvCatalogImpl generateProxyDsResolverCatalog( CrawlableDataset catalogCrDs, ProxyDatasetHandler pdh ) throws IOException
  {
    throw new java.lang.UnsupportedOperationException( "This method not supported by SimpleCatalogBuilder.");
  }

  public Document generateCatalogAsDocument( CrawlableDataset catalogCrDs ) throws IOException
  {
    return CatalogBuilderHelper.convertCatalogToDocument( generateCatalog( catalogCrDs ) );
  }

  public String generateCatalogAsString( CrawlableDataset catalogCrDs ) throws IOException
  {
    return CatalogBuilderHelper.convertCatalogToString( generateCatalog( catalogCrDs ) );
  }
}
