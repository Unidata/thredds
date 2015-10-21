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
// $Id: CollectionLevelScanner.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.catalog.*;
import thredds.crawlabledataset.*;

import static ucar.nc2.util.URLnaming.escapePathForURL;
import java.util.*;
import java.io.IOException;

/**
 * CollectionLevelScanner maps between the CrawlableDataset realm and the
 * InvCatalog/InvDataset realm. It scans a single level of a dataset collection
 * and generates a catalog. The generated catalog contains InvCatalogRef
 * objects for all contained collection datasets.
 *
 * <p>
 * Three different levels of the dataset collection must be provided to to
 * properly map from CrawlableDataset to InvCatalog/InvDataset:
 * <ol>
 * <li> the collection level is the top of the data collection (the data root);</li>
 * <li> the catalog level is the level in the collection for which a catalog
 * is to be constructed; and</li>
 * <li> the current level (only different from catalog level when the resulting
 * single-level catalog will be used in the construction of a
 * <a href="#multiLevel">multi-level catalog</a> ) is the level in the
 * collection for which a catalog is currently being constructed.</li>
 * </ol>
 *
 * <p> Besides the three CrawlableDatasets that define the collection to be
 * cataloged, there are a variety of ways to modify or enhance the resulting
 * catalog. For more details, see the documentation for the various setters
 * ({@link #setCollectionId(String) setCollectionId()},
 * {@link #setIdentifier(CrawlableDatasetLabeler) setIdentifier()},
 * {@link #setNamer(CrawlableDatasetLabeler) setNamer()},
 * {@link #setDoAddDataSize(boolean) setDoAddDataSize()},
 * {@link #setSorter(CrawlableDatasetSorter) setSorter()} ,
 * {@link #setProxyDsHandlers(Map) setProxyDsHandlers()},
 * {@link #addChildEnhancer(thredds.cataloggen.DatasetEnhancer)
 * addChildEnhancer()}.
 *
 * <a name="example"></a><h4>Example</h4>
 *
 * <p> Here we'll look at the parameters used to construct a
 * CollectionLevelScanner and to generate a catalog for the following
 * request:
 *
 * <pre>http://my.server:8080/thredds/ncep/nam/80km/catalog.xml</pre>
 *
 * <p>In the constuctor, we have:
 * <ul>
 * <li> collection ID: "ncep";</li>
 * <li> collectionLevel.getPath(): "/my/data/collection/model/ncep";</li>
 * <li> catalogLevel.getPath(): "/my/data/collection/model/ncep/nam/80km";</li>
 * <li> currentLevel = null (so the catalogLevel is used);</li>
 * <li> filter and service: not really important for this example, so we'll
 * ignore them for now.</li>
 * </ul>
 * <p> The two datasets we'll use in the example are:
 * <ul>
 * <li> childAtomicCrDs.getPath(): "/my/data/collection/model/ncep/nam/80km/20060208_1200_nam80km.grib"</li>
 * <li> childCollectionCrDs.getPath(): "/my/data/collection/model/ncep/nam/80km/2000archive"</li>
 * </ul>
 *
 * <p>Following are the details on how the resulting InvDataset and
 * InvCatalogRef objects are created.
 *
 * <ul>
 * <li> The name of a dataset element (and the xlink:title of a catalogRef
 * element) is the name of the corresponding CrawlableDataset. Example:
 * <pre>
 * &lt;dataset name="20060208_1200_nam80km.grib"/&gt;
 * &lt;catalogRef xlink:title="2000archive"/&gt;
 * </pre
 * where the values were determined as follows:
 * <ul>
 * <li> name = childAtomicCrDs.getName()</li>
 * <li> xlink:title = childCollectionCrDs.getName()</li>
 * </ul>
 * </li>
 *
 * <li> The ID of a catalog dataset element is the ID of the parent dataset
 * and the name of the corresponding CrawlableDataset seperated by a "/".
 * So, it ends up being the path of the corresponding CrawlableDataset from
 * the point where the collection CrawlableDataset path ends then prefixed
 * by the collectionId which is set using the setCollectionId() string. Example:
 * <pre>
 * &lt;dataset name="20060208_1200_nam80km.grib" ID="NCEP/nam/80km/20060208_1200_nam80km.grib"/&gt;
 * &lt;catalogRef xlink:title="2000archive" ID="NCEP/nam/80km/2000archive" /&gt;
 * </pre>
 * where the values were determined as follows:
 * <ul>
 * <li> ID = collectionId + childAtomicCrDs.getPath().substring( collectionLevel.getPath().length + 1)</li>
 * <li> ID = collectionId + childCollectionCrDs.getPath().substring( collectionLevel.getPath().length + 1)</li>
 * </ul>
 * </li>
 *
 * <li> The urlPath of a dataset element is the collectionPath plus the path of
 * the corresponding CrawlableDataset starting at the point where the
 * collection CrawlableDataset path ends. Example:
 * <pre>
 * &lt;dataset name="20060208_1200_nam80km.grib" ID="NCEP/nam/80km/20060208_1200_nam80km.grib"
 * urlPath="ncep/nam/80km/20060208_1200_nam80km.grib" /&gt;
 * </pre>
 * where the values were determined as follows:
 * <ul>
 * <li> urlPath = collectionPath + "/" + childAtomicCrDs.getPath().substring( collectionLevel.getPath().length + 1)</li>
 * </ul>
 * </li>
 *
 * <li> The xlink:href of a catalogRef element is the path of the corresponding
 * CrawlableDataset starting at the point where the catalogLevel
 * CrawlableDataset ends plus "/catalog.xml". Example:
 *
 * <pre>
 * &lt;catalogRef xlink:title="2000archive" xlink:href="2000archive/catalog.xml"/&gt;
 * </pre>
 * where the values were determined as follows:
 * <ul>
 * <li> xlink:href = childCollectionCrDs.getPath().substring( catalogLevel.getPath().length() + 1 ) + "/catalog.xml"</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>See {@link DatasetScanCatalogBuilder} for more details on how a
 * THREDDS server config file (catalog.xml) and the contained datasetScan
 * elements map into CollectionLevelScanner.
 *
 * <a name="multiLevel"></a><h4>Multi-level Catalogs</h4>
 *
 * <p>Resulting single level catalogs can be used to construct multi-level
 * catalogs by replacing InvCatalogRef objects with the catalogs generated for
 * the corresponding CrawlableDataset objects. Construction of multi-level
 * catalogs is supported in several ways:</p>
 *
 * <ul>
 * <li> The getCatRefInfo() method provides access to the list of InvCatalogRef
 * objects and their corresponding CrawlableDataset objects.</li>
 * <li> In the CollectionLevelScanner constructor, the currentLevel parameter
 * indicates the level for which the current single-level catalog is to be
 * created rather than the top-level of the resulting multi-level catalog.</li>
 * </ul>
 *
 * <p>NOTE: The {@link StandardCatalogBuilder} class is an example of using
 * ColletionLevelScanner to construct multi-level catalogs.</p>
 *
 * @author edavis
 * @since Jun 14, 2005T12:41:23 PM
 */
public class CollectionLevelScanner
{
  // @todo Combine DatasetSource and DirectoryScanner into CollectionLevelScanner


  private final String collectionPath;
  private String collectionName;
  private String collectionId = null;

  private final CrawlableDataset collectionLevel;
  private final CrawlableDataset catalogLevel;
  private final CrawlableDataset currentLevel;

  private final InvService service;

  private final CrawlableDatasetFilter filter;

  private CrawlableDatasetSorter sorter;
  private Map proxyDsHandlers;

  private CrawlableDatasetLabeler identifier;
  private CrawlableDatasetLabeler namer;
  private boolean doAddDataSize = false;

  private boolean addLastModified = true;

  private List childEnhancerList;

  private InvDatasetImpl topLevelMetadataContainer;

  //private List allDsInfo;
  private List catRefInfo;
  private List atomicDsInfo;
  private List proxyDsInfo;

  private InvCatalogImpl genCatalog;

  // Allowed states:
  //   0 - constructed, setup OK;
  //   1 - currently scanning collection level;
  //   2 - scan completed and catalog generated;
  private int state;

  /**
   * Construct a CollectionLevelScanner.
   *
   * <p> The collectionLevel and catalogLevel parameters are used to properly
   * determine the dataset urlPath. The catalogLevel must either be the
   * collectionLevel or be a decendent of the collectionLevel. The
   * currentLevel, if not null, must either be the catalogLevel or be a
   * decendent of the catalogLevel.</p>
   * <ul>
   * <li> If the service base is relative to the catalog (i.e., an empty
   * string), the urlPath needs to be relative to the catalog as well.
   * Therefore, the urlPath thus becomes the dataset path minus the
   * catalogLevel path. </li>
   * <li> If the service base is relative to the collection (absolute? relative
   * to the server?), e.g., "/thredds/dodsC/", the urlPath needs to be relative
   * to the collection as well. Therefore, the urlPath thus becomes the dataset
   * path minus the collectionLevel path. </li>
   * </ul>
   * <p> The currentLevel parameter indicates what level is to be scanned. It
   * is the same as the catalogLevel except for the case when catalogRefs are
   * not used for all collection levels. (The urlPath is still determined as
   * described above. Only the location of the datasets is changed.) </p>
   *
   * @param collectionPath the path of the collection, used as the base of all resulting dataset@urlPath values (may be "", if null, "" is used).
   * @param collectionLevel the root of the collection to be cataloged (must not be a CrawlableDatasetAlias).
   * @param catalogLevel the location, within the collection, for which a catalog is being generated.
   * @param currentLevel the location, at or below the catalog level, which is to be scanned for datasets. Only necessary when multiple catalogs are to be aggregated. May be null. If null, assumed to be same as catalog level.
   * @param filter determines which CrawlableDatasets are accepted as part of the collection.
   * @param service the default service of all InvDatasets in the generated catalog.
   *
   * @throws IllegalArgumentException
   */
  public CollectionLevelScanner( String collectionPath,
                                 CrawlableDataset collectionLevel,
                                 CrawlableDataset catalogLevel,
                                 CrawlableDataset currentLevel,
                                 CrawlableDatasetFilter filter,
                                 InvService service )
  {
    this.collectionPath = ( collectionPath != null ? collectionPath : "");
    if ( collectionLevel == null ) throw new IllegalArgumentException( "The collection root must not be null.");
    // @todo Remove alias until sure how to handle things like ".scour*" being a regular file.
//    if ( CrawlableDatasetAlias.isAlias( collectionLevel.getPath() ) ) throw new IllegalArgumentException( "The collection root <" + collectionLevel.getPath() + "> must not be a CrawlableDatasetAlias.");
    if ( catalogLevel == null ) throw new IllegalArgumentException( "The catalog root must not be null.");
    if ( service == null )
      throw new IllegalArgumentException( "The service must not be null.");

    if ( ! catalogLevel.getPath().startsWith( collectionLevel.getPath() ) )
      throw new IllegalArgumentException( "The catalog root <" + catalogLevel.getPath() + "> must be under the collection root <" + collectionLevel.getPath() + ">." );
    if ( ! collectionLevel.isCollection() )
      throw new IllegalArgumentException( "The collection root <" + collectionLevel.getPath() + "> must be a collection." );
    if ( ! catalogLevel.isCollection() )
      throw new IllegalArgumentException( "The catalog root <" + catalogLevel.getPath() + "> must be a collection." );

    this.collectionLevel = collectionLevel;
    this.catalogLevel = catalogLevel;

    if ( currentLevel != null )
    {
      if ( ! currentLevel.isCollection() )
        throw new IllegalArgumentException( "The current root <" + currentLevel.getPath() + "> must be a collection." );
      if ( ! currentLevel.getPath().startsWith( catalogLevel.getPath() ) )
        throw new IllegalArgumentException( "The current root <" + currentLevel.getPath() + "> must be under the catalog root <" + catalogLevel.getPath() + ">." );
      this.currentLevel = currentLevel;
    }
    else
    {
      this.currentLevel = catalogLevel;
    }

    this.filter = filter;         // @todo Thread safe: copy instead of reference
    this.service = service;       // @todo Thread safe: copy instead of reference

    childEnhancerList = new ArrayList();

    // @todo Maybe postpone creation of list till know approx size
    catRefInfo = new ArrayList();
    atomicDsInfo = new ArrayList();
    proxyDsInfo = new ArrayList();

    collectionId = null;
    collectionName = null;

    state = 0;
  }

//  public CollectionLevelScanner( CrawlableDataset accessPath, CrawlableDataset accessPathHeader,
//                            CrawlableDatasetFilter filter, InvService service )
//  {
//    if ( accessPath == null ) throw new IllegalArgumentException( "The access path must not be null." );
//    if ( accessPathHeader == null ) throw new IllegalArgumentException( "The access path header must not be null." );
//    if ( service == null ) throw new IllegalArgumentException( "The service must not be null." );
//
//    if ( ! accessPath.getPath().startsWith( accessPathHeader.getPath() ) )
//      throw new IllegalArgumentException( "The accessPath <" + accessPath.getPath() + "> must start with the accessPathHeader <" + accessPathHeader.getPath() + ">." );
//    if ( ! accessPathHeader.isCollection() )
//      throw new IllegalArgumentException( "The accessPathHeader <" + accessPathHeader.getPath() + "> must be a collection." );
//    if ( ! accessPath.isCollection() )
//      throw new IllegalArgumentException( "The accessPath <" + accessPath.getPath() + "> must be a collection." );
//
//    this.collectionPath = ""; //accessPath.getPath().substring( accessPathHeader.getPath().length() );
//    this.collectionLevel = accessPathHeader;
//    this.catalogLevel = accessPath;
//
//    this.filter = matcher;         // @todo Thread safe: copy instead of reference
//    this.service = service;       // @todo Thread safe: copy instead of reference
//  }

  /** Copy constructor */
  public CollectionLevelScanner( CollectionLevelScanner cs )
  {
    this( cs.collectionPath, cs.collectionLevel, cs.catalogLevel, cs.currentLevel, cs.filter, cs.service );

    this.setSorter( cs.getSorter() );
    this.setProxyDsHandlers( cs.getProxyDsHandlers() );
    this.setCollectionId( cs.getCollectionId() );
    this.setCollectionName( cs.getCollectionName() );
    this.setIdentifier( cs.getIdentifier() );
    this.setNamer( cs.getNamer() );
    this.setDoAddDataSize( cs.getDoAddDataSize() );

    this.childEnhancerList.add( cs.getChildEnhancerList() );
  }

  public CrawlableDatasetSorter getSorter()
  {
    return sorter;
  }

  /**
   * Set the sorter with which to sort the list of child CrawlableDatasets.
   *
   * @param sorter the CrawlableDatasetSorter that will be used to sort the list of child CrawlableDatasets.
   */
  public void setSorter( CrawlableDatasetSorter sorter )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed." );
    this.sorter = sorter;
  }

  public Map getProxyDsHandlers()
  {
    if ( proxyDsHandlers.isEmpty() )
      return Collections.EMPTY_MAP;
    else
      return Collections.unmodifiableMap( proxyDsHandlers );
  }
  public void setProxyDsHandlers( Map proxyDsHandlers )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed." );
    if ( proxyDsHandlers == null )
      this.proxyDsHandlers = Collections.EMPTY_MAP;
    else
      this.proxyDsHandlers = new HashMap( proxyDsHandlers);
  }

  /**
   * Set the value of the base dataset ID. The value is used to construct
   * the value of the dataset@ID attribute for all datasets.
   *
   * @param collectionId
   */
  public void setCollectionId( String collectionId )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed.");
    this.collectionId = collectionId;
  }

  protected String getCollectionId()
  {
    return( this.collectionId );
  }

  /**
   * Set the value of the collection Name. The value is used to name the
   * top-level dataset in the top-level collection catalog (that is, only when
   * the catalog level is the same as the collection level).
   *
   * @param collectionName
   */
  public void setCollectionName( String collectionName )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed.");
    this.collectionName = collectionName;
  }

  protected String getCollectionName()
  {
    return( this.collectionName );
  }

  /**
   * Set the CrawlableDatasetLabeler used to determine the ID of the InvDataset
   * built during catalog generation. The labeler is applied to the
   * CrawlableDataset that corresponds to each InvDataset built.
   *
   * @param identifier
   */
  public void setIdentifier( CrawlableDatasetLabeler identifier )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed." );
    this.identifier = identifier;
  }

  protected CrawlableDatasetLabeler getIdentifier()
  {
    return ( this.identifier );
  }

  /**
   * Set the CrawlableDatasetLabeler used to determine the name of each
   * InvDataset built during catalog generation. The labeler is applied to the
   * CrawlableDataset that corresponds to each InvDataset built.
   *
   * @param namer
   */
  public void setNamer( CrawlableDatasetLabeler namer )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed." );
    this.namer = namer;
  }

  protected CrawlableDatasetLabeler getNamer()
  {
    return( this.namer );
  }

  /**
   * Determines if datasetSize metadata will be added to each InvDataset built
   * during catalog generation. The CrawlableDataset.length() method is used to
   * determine the size of the dataset.
   *
   * @param doAddDataSize
   */
  public void setDoAddDataSize( boolean doAddDataSize )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed." );
    this.doAddDataSize = doAddDataSize;
  }

  protected boolean getDoAddDataSize()
  {
    return ( this.doAddDataSize );
  }

  /**
   * Add the given DatasetEnhancer to the list that will be applied to each of
   * the child datasets. The DatasetEnhancer only modify InvDataset objects but
   * can use the corresponding CrawlableDataset for information.
   *
   * @param childEnhancer
   */
  public void addChildEnhancer( DatasetEnhancer childEnhancer )
  {
    if ( state != 0 ) throw new IllegalStateException( "No further setup allowed, scan in progress or completed." );
    this.childEnhancerList.add( childEnhancer );
  }

  List getChildEnhancerList()
  {
    return Collections.unmodifiableList( childEnhancerList );
  }

  /**
   * Set the InvDatasetImpl that contains the metadata for the top level dataset.
   *
   * @param topLevelMetadataContainer
   */
  public void setTopLevelMetadataContainer( InvDatasetImpl topLevelMetadataContainer )
  {
    this.topLevelMetadataContainer = topLevelMetadataContainer;
  }

  /**
   * Scan the collection and gather information on contained datasets.
   *
   * @throws IOException if an I/O error occurs while locating the contained datasets.
   */
  public void scan() throws IOException
  {
    if ( state == 1 ) throw new IllegalStateException( "Scan already underway." );
    if ( state >= 2 ) throw new IllegalStateException( "Scan has already been generated." );
    state = 1;

    // Make sure proxyDsHandlers Map is not null.
    if ( proxyDsHandlers == null ) proxyDsHandlers = Collections.EMPTY_MAP;

    // Create a skeleton catalog.
    genCatalog = createSkeletonCatalog( currentLevel );
    InvDatasetImpl topInvDs = (InvDatasetImpl) genCatalog.getDatasets().get( 0 );

    // Get the datasets in this collection.
    List crDsList = currentLevel.listDatasets( this.filter );

    // Sort the datasets in this collection.
    // @todo Should we move sort to end of this method? As is, we can't use naming or enhancements to determine sort order.
    if ( sorter != null ) sorter.sort( crDsList );

    // Add the datasets to the catalog.
    for ( int i = 0; i < crDsList.size(); i++ )
    //for ( Iterator it = crDsList.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCrDs = (CrawlableDataset) crDsList.get( i );

      InvDatasetImpl curInvDs = (InvDatasetImpl) createInvDatasetFromCrawlableDataset( curCrDs, topInvDs, null );

      // Add dataset info to appropriate lists.
      InvCrawlablePair dsInfo = new InvCrawlablePair( curCrDs, curInvDs );
      //allDsInfo.add( dsInfo );
      if ( curCrDs.isCollection() )
        catRefInfo.add( dsInfo );
      else
        atomicDsInfo.add( dsInfo );

      // Add current InvDataset to top dataset.
      topInvDs.addDataset( curInvDs );
    }

    // Tie up any loose ends in catalog with finish().
    ( (InvCatalogImpl) genCatalog ).finish();

    // Add proxy datasets to list (only if some atomic datasets in this collection).
    if ( atomicDsInfo.size() > 0 )
    {
      boolean anyProxiesAdded = false;
      for ( Iterator it = proxyDsHandlers.values().iterator(); it.hasNext(); )
      {
        // Get current
        ProxyDatasetHandler curProxy = (ProxyDatasetHandler) it.next();

        InvService proxyService = curProxy.getProxyDatasetService( currentLevel );
        if ( proxyService != null )
        {
          // Create proxy CrawlableDataset and corresponding InvDataset
          CrawlableDataset crDsToAdd = curProxy.createProxyDataset( currentLevel );
          InvDatasetImpl invDsToAdd = createInvDatasetFromCrawlableDataset( crDsToAdd, topInvDs, proxyService );

          // Add dataset info to appropriate lists.
          InvCrawlablePair dsInfo = new InvCrawlablePair( crDsToAdd, invDsToAdd );
          proxyDsInfo.add( dsInfo );

          // Add dataset to catalog
          int index = curProxy.getProxyDatasetLocation( currentLevel, topInvDs.getDatasets().size() );
          topInvDs.addDataset( index, (InvDatasetImpl) invDsToAdd );

          genCatalog.addService( proxyService );
          anyProxiesAdded = true;
        }
      }

      // Tie up any proxy dataset loose ends.
      if ( anyProxiesAdded ) ( (InvCatalogImpl) genCatalog ).finish();
    }

    // Add any top-level metadata.
    this.addTopLevelMetadata( genCatalog, true );

    state = 2;
    return;
  }

//  List getAllDsInfo()
//  {
//    if ( state != 2 ) throw new IllegalStateException( "Scan has not been performed." );
//    return Collections.unmodifiableList( allDsInfo );
//  }

  /**
   * Return a list of all the collection dataset objects (InvCatalogRef) and
   * their corresponding CrawlableDataset objects. Each item in the list is
   * an InvCrawlablePair.
   *
   * @return a list of InvCrawlablePairs
   */
  List getCatRefInfo()
  {
    if ( state != 2 ) throw new IllegalStateException( "Scan has not been performed." );
    return Collections.unmodifiableList( catRefInfo );
  }

  /**
   * Return a list of all the atomic dataset objects (InvDataset) and
   * their corresponding CrawlableDataset objects. Each item in the list is
   * an InvCrawlablePair.
   *
   * @return a list of InvCrawlablePairs
   */
  List getAtomicDsInfo()
  {
    if ( state != 2 ) throw new IllegalStateException( "Scan has not been performed." );
    return Collections.unmodifiableList( atomicDsInfo );
  }

  public InvCatalogImpl generateCatalog() throws IOException
  {
    if ( state != 2 ) throw new IllegalStateException( "Scan has not been performed.");
    return genCatalog;
  }

  /**
   * Generate the catalog for a resolver request of the given ProxyDatasetHandler.
   *
   * @param pdh the ProxyDatasetHandler corresponding to the resolver request.
   * @return the catalog for a resolver request of the given proxy dataset.
   * @throws IllegalStateException if this collection has not yet been scanned.
   * @throws IllegalArgumentException if the given ProxyDatasetHandler is not known by this CollectionLevelScanner.
   */
  public InvCatalogImpl generateProxyDsResolverCatalog( ProxyDatasetHandler pdh )
  {
    if ( state != 2 ) throw new IllegalStateException( "Scan has not been performed." );
    if ( ! proxyDsHandlers.containsValue( pdh )) throw new IllegalArgumentException( "Unknown ProxyDatasetHandler.");

    // Create a skeleton catalog.
    InvCatalogImpl catalog = createSkeletonCatalog( currentLevel );
    InvDatasetImpl topDs = (InvDatasetImpl) catalog.getDatasets().get( 0 );

    // Find actual dataset in the list of atomic dataset InvCrawlablePairs.
    InvCrawlablePair actualDsInfo = pdh.getActualDataset( atomicDsInfo );
    if ( actualDsInfo == null )
      return catalog; // TODO Test this case in TestDataRootHandler.
    InvDatasetImpl actualInvDs = (InvDatasetImpl) actualDsInfo.getInvDataset();

    actualInvDs.setName( pdh.getActualDatasetName( actualDsInfo, topDs.getName() ) );

    // Add current InvDataset to top dataset.
    catalog.removeDataset( topDs );
    catalog.addDataset( actualInvDs );
    // topDs.addDataset( actualInvDs );

    // Finish catalog.
    catalog.finish();

    // Add any top-level metadata.
    this.addTopLevelMetadata( catalog, false );

    return catalog;
  }

  private void addTopLevelMetadata( InvCatalog catalog, boolean isRegularCatalog )
  {
    if ( this.topLevelMetadataContainer == null )
      return;
    if ( ! catalogLevel.getPath().equals( currentLevel.getPath() ) )
      return;

    // Transfer all metadata from given metadata container to the top
    // InvDataset. This propagates any public metadata from the given
    // metadata container to all generated catalogs.
    InvDatasetImpl topInvDs = (InvDatasetImpl) catalog.getDataset();

    topInvDs.transferMetadata( this.topLevelMetadataContainer, true );

    // LOOK experimental datasetScan may have its own access elements
    for ( Iterator it = this.topLevelMetadataContainer.getAccess().iterator(); it.hasNext(); )
    {
      InvAccess invAccess = (InvAccess) it.next();
      topInvDs.addAccess( invAccess );
      InvService s = invAccess.getService();
      ( (InvCatalogImpl) catalog ).addService( s );
    }

    // If this is a collection level scan, set some special attributes
    // that transferMetadata() doesn't transfer.
    boolean isCollectionLevel = catalogLevel.getPath().equals( collectionLevel.getPath() );
    if ( isCollectionLevel && isRegularCatalog )
    {
      topInvDs.setHarvest( this.topLevelMetadataContainer.isHarvest() );
      topInvDs.setCollectionType( this.topLevelMetadataContainer.getCollectionType() );
    }

    // Finish catalog.
    ( (InvCatalogImpl) catalog ).finish();

  }

  private InvCatalogImpl createSkeletonCatalog( CrawlableDataset topCrDs )
  {
    InvCatalogImpl catalog = new InvCatalogImpl( null, null, null );

//    // Copy service to add to catalog.
//    InvService service = new InvService( this.service.getName(), this.service.getServiceType().toString(), this.service.getBase(), this.service.getSuffix(), this.service.getDescription() );
//    for ( Iterator it = this.service.getProperties().iterator(); it.hasNext(); )
//      service.addProperty( (InvProperty) it.next() );
//    for ( Iterator it = this.service.getServices().iterator(); it.hasNext(); )
//      service.addService( (InvService) it.next() );

    // Add service to catalog.
    catalog.addService( service );

    // Determine name of top-level dataset.
    String newName = null;
    if ( namer != null )
      newName = this.namer.getLabel( topCrDs );
    if ( newName == null )
      // If generating the collection level catalog, use the collection name.
      if ( collectionLevel.getPath().equals( catalogLevel.getPath() ) )
        newName = this.collectionName;
    if ( newName == null )
      newName = this.getName( topCrDs );

    // Create top-level dataset.
    InvDatasetImpl topDs = new InvDatasetImpl( null, newName, null, null, null );

    // Set ID of dataset.
    String newId = null;
    if ( identifier != null )
      newId = this.identifier.getLabel( topCrDs );
    else
      newId = this.getID( topCrDs );
    if ( newId != null )
      topDs.setID( newId );

    // Set the serviceName (inherited by all datasets) in top-level dataset.
    ThreddsMetadata tm = new ThreddsMetadata( false );
    tm.setServiceName( service.getName() );
    InvMetadata md = new InvMetadata( topDs, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm );
    ThreddsMetadata tm2 = new ThreddsMetadata( false );
    tm2.addMetadata( md );
    topDs.setLocalMetadata( tm2 );

    // Add top-level dataset to catalog.
    catalog.addDataset( topDs );

    return catalog;
  }

  private InvDatasetImpl createInvDatasetFromCrawlableDataset( CrawlableDataset crawlableDs, InvDatasetImpl parentInvDs, InvService service )
  {
    // Determine name of dataset.
    String newName = null;
    if ( namer != null )
      newName = this.namer.getLabel( crawlableDs );
    if ( newName == null )
      newName = this.getName( crawlableDs );

    InvDatasetImpl curInvDs;
    if ( crawlableDs.isCollection() )
    {
      // Collection dataset, create a catalogRef to be added to the catalog.
      curInvDs = new InvCatalogRef( parentInvDs, newName, this.getXlinkHref( crawlableDs ) );
    }
    else
    {
      // Atomic dataset, add a dataset to the catalog.
      curInvDs = new InvDatasetImpl( parentInvDs, newName, null,
                                     ( service != null ? service.getName() : null ),
                                     this.getUrlPath( crawlableDs, service ) );

      // Set dataSize of dataset (not applicable for collection datasets).
      if ( this.doAddDataSize && crawlableDs.length() != -1 )
      {
        curInvDs.setDataSize( crawlableDs.length() );
      }

      if ( this.addLastModified )
      {
        Date lastModDate = crawlableDs.lastModified();
        if ( lastModDate != null )
          curInvDs.setLastModifiedDate( lastModDate );
      }
    }

    // Set ID of dataset.
    String newId = null;
    if ( identifier != null )
      newId = this.identifier.getLabel( crawlableDs );
    else
      newId = this.getID( crawlableDs );
    if ( newId != null )
      curInvDs.setID( newId );

    // Enhance each child dataset.
    for ( Iterator it2 = childEnhancerList.iterator(); it2.hasNext(); )
    {
      ( (DatasetEnhancer) it2.next() ).addMetadata( curInvDs, crawlableDs );
    }

    return curInvDs;
  }

  // dataset name and xlink:title=
  //   - node.getName()
  private String getName( CrawlableDataset dataset )
  {
    // @todo Remove alias until sure how to handle things like ".scour*" being a regular file.
//    if ( ! CrawlableDatasetAlias.isAlias( currentLevel.getPath() ) )
//    {
      return ( dataset.getName() );
//    }
//    else
//    {
//      // @todo Move this functionality into CrawlableDatasetAlias
//      String path = dataset.getPath();
//      int curIndex = path.length();
//      for ( int i = 0; i < currentLevel.getName().split( "/" ).length; i++ )
//      {
//        curIndex = dataset.getPath().lastIndexOf( "/", curIndex - 1 );
//      }
//      return ( dataset.getPath().substring( curIndex + 1 ) );
//    }
  }

  // dataset ID=
  //   - parentId + "/" + dataset.getName() [when ID added after catalog built using dataset info]; or
  //   - baseID + "/" + node.getPath().substring( collectionLevel.length) [when ID added at catalog construction using node info]
  private String getID( CrawlableDataset dataset )
  {
    if ( dataset == null )
      return null;
    if ( collectionId == null )
      return null;

    int i = collectionLevel.getPath().length();
    String id = dataset.getPath().substring( i );
    if ( id.startsWith( "/" ))
      id = id.substring( 1 );
    if ( collectionId.equals( "" ) )
    {
      if ( id.equals( ""))
        return null;
      return id;
    }
    if ( id.equals( "") )
      return collectionId;

    return( collectionId + "/" + id );
  }

  // dataset urlPath=
  //   - node.getPath().substring( catalogLevel.length) [when service is relative to catalog URL]
  //   - collectionPath + "/" + node.getPath().substring( collectionLevel.length) [when service is absolute (i.e., relative to collection)]
  private String getUrlPath( CrawlableDataset dataset, InvService service )
  {
    InvService serviceForThisDs = service != null ? service : this.service;

    if ( serviceForThisDs.getBase().equals( "" )
         && ! serviceForThisDs.getServiceType().equals( ServiceType.COMPOUND ) )
    {
      // Service is relative to the catalog URL.
      String urlPath = dataset.getPath().substring( catalogLevel.getPath().length() );
      if ( urlPath.startsWith( "/" ))
        urlPath = urlPath.substring( 1 );
      return urlPath;
    }
    else
    {
      if ( serviceForThisDs.isRelativeBase() )
      {
        // Service is relative to the collection root.
        String relPath = dataset.getPath().substring( collectionLevel.getPath().length() );
        if ( relPath.startsWith( "/" ) )
          relPath = relPath.substring( 1 );
        return ( ( collectionPath.equals( "" ) ? "" : collectionPath + "/" ) + relPath );
      }
      else
      {
        // Service base URI is Absolute so don't use collectionPath.
        String relPath = dataset.getPath().substring( collectionLevel.getPath().length() );
        if ( relPath.startsWith( "/" ) )
          relPath = relPath.substring( 1 );
        return relPath;
      }
    }
  }

  // catalogRef href=
  //   - node.getPath().substring( catalogLevel.length) + "/catalog.xml"
  private String getXlinkHref( CrawlableDataset dataset )
  {
    // @todo Remove alias until sure how to handle things like ".scour*" being a regular file.
//    if ( ! CrawlableDatasetAlias.isAlias( catalogLevel.getPath()) )
//    {
    String path = dataset.getPath().substring( catalogLevel.getPath().length() );
    if ( path.startsWith( "/" ))
      path = path.substring( 1);
    if ( path.endsWith( "/"))
      path += "catalog.xml";
    else
      path += "/catalog.xml";
    return escapePathForURL(path);
//    }
//    else
//    {
//      // @todo Move this functionality into CrawlableDatasetAlias
//      String path = dataset.getPath();
//      int curIndex = path.length();
//      int numSegments = catalogLevel.getName().split( "/" ).length;
//      for ( int i = 0 ; i < numSegments; i++ )
//      {
//        curIndex = dataset.getPath().lastIndexOf( "/", curIndex - 1);
//      }
//      return( dataset.getPath().substring( curIndex + 1 ) + "/catalog.xml");
//    }
  }

}
