/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
  static private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( StandardCatalogBuilder.class );

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

  public CrawlableDataset requestCrawlableDataset( String path )
          throws IOException
  {
    // Return requested dataset if it is the collection level dataset
    // or an allowed descendant.
    CrawlableDataset crDs = CatalogBuilderHelper.verifyDescendantDataset( collectionCrDs, path, this.filter );
    if ( crDs != null )
      return crDs;

    // Check if it is a proxy dataset request.
    String dsName;
    String parentPath;
    int indexLastSlash = path.lastIndexOf( "/" );
    if ( indexLastSlash != -1 )
    {
      dsName = path.substring( indexLastSlash + 1 );
      parentPath = path.substring( 0, indexLastSlash );
    }
    else
    {
      dsName = path;
      parentPath = "";
    }

    // Make sure parent is not filtered out and is a collection.
    CrawlableDataset parentCrDs = CatalogBuilderHelper.verifyDescendantDataset( collectionCrDs, parentPath, this.filter );
    if ( parentCrDs == null )
    {
      log.debug( "requestCrawlableDataset(): Parent dataset <" + parentPath + "> not allowed by filter." );
      return null;
    }
    if ( ! parentCrDs.isCollection() )
    {
      log.debug( "requestCrawlableDataset(): Parent dataset <" + parentPath + "> is not a collection dataset." );
      return null;
    }

    // Check if this is a request for a proxy dataset.
    ProxyDatasetHandler pdh = (ProxyDatasetHandler) this.proxyDatasetHandlers.get( dsName );
    if ( pdh == null )
    {
      log.debug( "requestCrawlableDataset(): Dataset name <" + dsName + "> has no corresponding proxy dataset handler." );
      return null;
    }
    if ( pdh.isProxyDatasetResolver() )
    {
      log.debug( "requestCrawlableDataset(): Proxy dataset <" + dsName + "> is a resolver, not valid dataset request." );
      return null;
    }

    // Get list of all atomic datasets in this collection.
    CollectionLevelScanner scanner = this.setupAndScan( parentCrDs, null );
    List atomicDsInfo = scanner.getAtomicDsInfo();

    // Determine which of the atomic datasets is the actual dataset.
    InvCrawlablePair dsInfo = pdh.getActualDataset( atomicDsInfo );

    return dsInfo.getCrawlableDataset();
  }

  public InvCatalogImpl generateCatalog( CrawlableDataset catalogCrDs )
          throws IOException
  {
    // Setup scanner
    CollectionLevelScanner scanner = setupAndScan( catalogCrDs, null );

    // Use scanner to generate catalog.
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
    CollectionLevelScanner scanner = setupAndScan( catalogCrDs, null );

    // Generate catalog.
    return scanner.generateProxyDsResolverCatalog( pdh );
  }

  private CollectionLevelScanner setupAndScan( CrawlableDataset catalogCrDs, CrawlableDataset currentCrDs )
          throws IOException
  {
    // Setup scanner.
    CollectionLevelScanner scanner = new CollectionLevelScanner( collectionPath, collectionCrDs, catalogCrDs, currentCrDs, filter, service );
    scanner.setCollectionId( collectionId );
    scanner.setCollectionName( collectionName );//*****
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
    scanner.setTopLevelMetadataContainer( topLevelMetadataContainer );

    // Scan the collection.
    scanner.scan();

    return scanner;
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
