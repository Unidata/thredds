// $Id: StandardCatalogBuilder.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.catalog.*;
import thredds.crawlabledataset.*;

import java.io.IOException;
import java.util.*;

import org.jdom.Document;

/**
 * _more_
 *
 * @author edavis
 * @since Dec 6, 2005 1:45:07 PM
 */
public class StandardCatalogBuilder implements CatalogBuilder
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( StandardCatalogBuilder.class );

  private String collectionPath;
  private String collectionName;
  private CrawlableDataset collectionCrDs;
  private CrawlableDatasetFilter filter;
  private InvService service;

  private String collectionId;
  private CrawlableDatasetLabeler identifier;
  private CrawlableDatasetLabeler namer;
  private boolean doAddDatasetSize;
  private CrawlableDatasetSorter sorter;
  private Map proxyDatasetHandlers;
  private List childEnhancerList;

  private InvDatasetImpl topLevelMetadataContainer;

  private CatalogRefExpander catalogRefExpander;

  public StandardCatalogBuilder( String collectionPath, String collectionName,
                                 CrawlableDataset collectionCrDs, CrawlableDatasetFilter filter,
                                 InvService service,
                                 String collectionId, CrawlableDatasetLabeler identifier,
                                 CrawlableDatasetLabeler namer,
                                 boolean doAddDatasetSize,
                                 CrawlableDatasetSorter sorter,
                                 Map proxyDatasetHandlers,
                                 List childEnhancerList,
                                 InvDatasetImpl topLevelMetadataContainer,
                                 CatalogRefExpander catalogRefExpander )
  {
    this.collectionPath = collectionPath;
    this.collectionName = collectionName;
    this.collectionCrDs = collectionCrDs;
    this.filter = filter;
    this.service = service;
    this.collectionId = collectionId;
    this.identifier = identifier;
    this.namer = namer;
    this.doAddDatasetSize = doAddDatasetSize;
    this.sorter = sorter;
    this.proxyDatasetHandlers = proxyDatasetHandlers;
    this.childEnhancerList = childEnhancerList;
    this.topLevelMetadataContainer = topLevelMetadataContainer;
    this.catalogRefExpander = catalogRefExpander;
  }

  public CrawlableDataset requestCrawlableDataset( String path ) throws IOException
  {
      return CatalogBuilderHelper.verifyDescendentDataset( collectionCrDs, path, this.filter );
  }

  public InvCatalogImpl generateCatalog( CrawlableDataset catalogCrDs ) throws IOException
  {
    // Setup scanner
    CollectionLevelScanner scanner = new CollectionLevelScanner( collectionPath, collectionCrDs, catalogCrDs, null, filter, service );
    scanner.setCollectionId( collectionId );
    scanner.setCollectionName( collectionName );
    scanner.setIdentifier( identifier );
    scanner.setNamer( namer );
    scanner.setDoAddDataSize( doAddDatasetSize );
    scanner.setSorter( sorter );
    scanner.setProxyDsHandlers( proxyDatasetHandlers );
    if ( childEnhancerList != null )
    {
      for ( Iterator it = childEnhancerList.iterator(); it.hasNext(); )
      {
        scanner.addChildEnhancer( (DatasetEnhancer) it.next() );
      }
    }
    scanner.setTopLevelMetadataContainer( this.topLevelMetadataContainer );

    // Use scanner to generate catalog.
    scanner.scan();
    InvCatalogImpl catalog = scanner.generateCatalog();

    // Keep track of need for call to catalog.finish().
    boolean needFinish = false;

    // Expand any catalogRefs if applicable.
    if ( catalogRefExpander != null )
    {
      // @todo Adding and removing to this list. Perhaps should do recursively instead? Any thread issues here?
      List catRefInfoList = new ArrayList( scanner.getCatRefInfo());
      while ( catRefInfoList.size() > 0)
      {
        InvCrawlablePair curCatRefInfo = (InvCrawlablePair) catRefInfoList.get( 0);

        if ( catalogRefExpander.expandCatalogRef( curCatRefInfo ) )
        {
          // @todo  Deal with nested ScannerInfo (InvDatasetScans?) in this CollectionLevelScanner setup.
          CollectionLevelScanner curScanner = new CollectionLevelScanner( collectionPath, collectionCrDs, catalogCrDs, curCatRefInfo.getCrawlableDataset(), filter, service );
          curScanner.setCollectionId( collectionId );
          curScanner.setIdentifier( identifier );
          curScanner.setNamer( namer );
          curScanner.setDoAddDataSize( doAddDatasetSize );
          curScanner.setSorter( sorter );
          curScanner.setProxyDsHandlers( proxyDatasetHandlers );
          if ( childEnhancerList != null )
          {
            for ( Iterator it = childEnhancerList.iterator(); it.hasNext(); )
            {
              curScanner.addChildEnhancer( (DatasetEnhancer) it.next() );
            }
          }

          // Generate catalog for current location.
          curScanner.scan();
          InvCatalogImpl curCatalog = curScanner.generateCatalog();

          // Add any new catalogRefInfo to list.
          catRefInfoList.addAll( curScanner.getCatRefInfo() );

          // Replace the catalogRef with an expanded dataset.
          InvDataset curTopDs = curCatalog.getDataset();
          InvDataset targetDs = curCatRefInfo.getInvDataset();
          InvDataset targetParentDs = curCatRefInfo.getInvDataset().getParent();
          ((InvDatasetImpl) targetParentDs).removeDataset( (InvDatasetImpl) targetDs );
          ((InvDatasetImpl) targetParentDs).addDataset( (InvDatasetImpl) curTopDs );

          needFinish = true;
        }
        // Remove the catalogRefInfo that was just expanded from list.
        catRefInfoList.remove( 0);
      }
    }

    // Finish catalog if needed and return.
    if ( needFinish )
      catalog.finish();

    return catalog;
  }

  public InvCatalogImpl generateProxyDsResolverCatalog( CrawlableDataset catalogCrDs, ProxyDatasetHandler pdh )
          throws IOException
  {
    if ( catalogCrDs == null || pdh == null ) throw new IllegalArgumentException( "Null parameters not allowed.");
    if ( ! proxyDatasetHandlers.containsValue( pdh)) throw new IllegalArgumentException( "Unknown ProxyDatasetHandler.");
    // Setup scanner
    CollectionLevelScanner scanner = new CollectionLevelScanner( collectionPath, collectionCrDs, catalogCrDs, null, filter, service );
    scanner.setCollectionId( collectionId );
    scanner.setCollectionName( collectionName );
    scanner.setIdentifier( identifier );
    scanner.setNamer( namer );
    scanner.setDoAddDataSize( doAddDatasetSize );
    scanner.setSorter( sorter );
    scanner.setProxyDsHandlers( proxyDatasetHandlers );
    if ( childEnhancerList != null )
    {
      for ( Iterator it = childEnhancerList.iterator(); it.hasNext(); )
      {
        scanner.addChildEnhancer( (DatasetEnhancer) it.next() );
      }
    }
    scanner.setTopLevelMetadataContainer( this.topLevelMetadataContainer );

    // Use scanner to generate catalog.
    scanner.scan();
    InvCatalogImpl catalog = scanner.generateProxyDsResolverCatalog( pdh );

    return catalog;
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
