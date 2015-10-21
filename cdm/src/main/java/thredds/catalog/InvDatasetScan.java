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
package thredds.catalog;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ucar.nc2.util.URLnaming.escapePathForURL;

import thredds.catalog.util.DeepCopyUtils;
import thredds.cataloggen.CatalogBuilder;
import thredds.cataloggen.CatalogRefExpander;
import thredds.cataloggen.DatasetEnhancer;
import thredds.cataloggen.DatasetScanCatalogBuilder;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.cataloggen.datasetenhancer.RegExpAndDurationTimeCoverageEnhancer;
import thredds.cataloggen.inserter.SimpleLatestProxyDsHandler;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDatasetLabeler;
import thredds.crawlabledataset.CrawlableDatasetSorter;
import thredds.crawlabledataset.filter.RegExpMatchOnNameFilter;
import thredds.crawlabledataset.sorter.LexigraphicByNameSorter;

/**
 * Represents server-side information on how to scan a collection of datasets
 * for catalog generation.
 *
 * <p>Used by the THREDDS Data Server (TDS) to automatically generate catalogs.</p>
 *
 * <p>Typically built from the information given by a datasetScan element in a
 * TDS config catalog.</p>
 *
 * <p>Usage notes:
 * <ol>
 * <li>The static methods setContext() and setCatalogServletName() should only
 * be called once per web application instance. For instance, in your
 * HttpServlet implementations init() method.</li>
 * <li>The method setScanLocation() should not be used; it is "public by accident".
 * </li>
 * </ol>
 *
 * <p>Should be thread safe except that the above two usage notes are not enforced.
 */
public class InvDatasetScan extends InvCatalogRef {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvDatasetScan.class);
  static private String context = "/thredds";
  static public void setContext( String c ) { context = c; }
  static private String catalogServletName = "/catalog";
  static public void setCatalogServletName( String catServletName ) { catalogServletName = catServletName; }
  static private String makeHref(String path)
  {
    return context + (catalogServletName == null ? "" : catalogServletName) + "/" + escapePathForURL(path) + "/catalog.xml";
  }

  ////////////////////////////////////////////////
  private final String rootPath;
  private String scanLocation;

  private CrawlableDataset scanLocationCrDs;

  private final String crDsClassName;
  private final Object crDsConfigObj;

  private final CrawlableDatasetFilter filter;

  private CrawlableDatasetLabeler identifier;
  private CrawlableDatasetLabeler namer;

  private CrawlableDatasetSorter sorter;

  private Map<String, ProxyDatasetHandler> proxyDatasetHandlers;

  private boolean addDatasetSize = true;

  private List<DatasetEnhancer> childEnhancerList;
  private CatalogRefExpander catalogRefExpander;

  private boolean isValid;
  private StringBuilder invalidMessage;

  public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanLocation, String id, InvDatasetScan from )  {
    this(parent, name, path , scanLocation,
         from.crDsClassName, from.crDsConfigObj, from.filter,
         from.identifier, from.namer, from.addDatasetSize,
         from.sorter, from.proxyDatasetHandlers, from.childEnhancerList,
         from.catalogRefExpander );

    setID( id);
  }

  /**
   * Constructor. Old stuff.
   *
   * @param catalog parent catalog
   * @param parent parent dataset
   * @param name dataset name
   * @param path url path
   * @param scanLocation scan this directory
   * @param filter RegExp match on name
   * @param addDatasetSize add a size element
   * @param addLatest add a latest element
   * @param sortOrderIncreasing sort
   * @param datasetNameMatchPattern dataset naming
   * @param startTimeSubstitutionPattern time range using the file name
   * @param duration  time range using the file name
   */
  public InvDatasetScan( InvCatalogImpl catalog, InvDatasetImpl parent, String name, String path, String scanLocation, String filter,
                         boolean addDatasetSize, String addLatest, boolean sortOrderIncreasing,
                         String datasetNameMatchPattern, String startTimeSubstitutionPattern, String duration ) {

    super(parent, name, makeHref(path));
    log.debug(  "InvDatasetScan(): parent="+ parent + ", name="+ name +" , path="+ path +" , scanLocation="+ scanLocation +" , filter="+ filter +" , addLatest="+ addLatest +" , sortOrderIncreasing="+ sortOrderIncreasing +" , datasetNameMatchPattern="+ datasetNameMatchPattern +" , startTimeSubstitutionPattern= "+ startTimeSubstitutionPattern +", duration="+duration);

    this.rootPath = path;
    this.scanLocation = scanLocation;
    this.crDsClassName = null;
    this.crDsConfigObj = null;
    this.scanLocationCrDs = createScanLocationCrDs();
    this.isValid = true;
    if ( this.scanLocationCrDs == null )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=").append( path)
              .append( "; scanLocation=" ).append( scanLocation )
              .append(">: could not create CrawlableDataset for scanLocation." );
    }
    else if ( ! this.scanLocationCrDs.exists() )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=").append( scanLocation )
              .append(">: CrawlableDataset for scanLocation does not exist." );
    }
    else if ( ! this.scanLocationCrDs.isCollection() )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=" ).append( scanLocation )
              .append( ">: CrawlableDataset for scanLocation not a collection." );
    }

    if ( filter != null ) {
      // Include atomic datasets that match the given filter string.
      this.filter = new RegExpMatchOnNameFilter( filter);
    } else
      this.filter = null;

    this.identifier = null;
    this.namer = null;
    this.addDatasetSize = addDatasetSize;

    this.sorter = new LexigraphicByNameSorter( sortOrderIncreasing );

    // Add latest resolver dataset to Map of ProxyDatasetHandlers
    this.proxyDatasetHandlers = new HashMap<>();
    if ( addLatest != null )
    {
      if ( addLatest.equalsIgnoreCase( "true" ) )
      {
        InvService service = catalog.findService( "latest" );
        if ( service != null )
        {
          ProxyDatasetHandler proxyDsHandler = new SimpleLatestProxyDsHandler( "latest.xml", true, service, true );
          this.proxyDatasetHandlers.put( "latest.xml", proxyDsHandler );
        }
      }
    }

    if ( datasetNameMatchPattern != null
         && startTimeSubstitutionPattern != null
         && duration != null )
    {
      childEnhancerList = new ArrayList<DatasetEnhancer>();
      childEnhancerList.add(
              RegExpAndDurationTimeCoverageEnhancer
                      .getInstanceToMatchOnDatasetName( datasetNameMatchPattern,
                                                        startTimeSubstitutionPattern,
                                                        duration ) );
    }

    // catalogRefExpander = new BooleanCatalogRefExpander( this.datasetScan.??? );
  }

  /**
   * Constructor. Used by InvDatasetFeatureCollection
   *
   * @param catalog parent catalog
   * @param parent parent dataset
   * @param name dataset name
   * @param path url path
   * @param scanLocation scan this directory
   * @param filter CrawlableDatasetFilter, may be null
   * @param addDatasetSize add a size element
   * @param addLatest add a latest element
   * @param sortOrderIncreasing sort
   * @param datasetNameMatchPattern dataset naming
   * @param startTimeSubstitutionPattern time range using the file name
   * @param duration  time range using the file name
   */
  public InvDatasetScan( InvCatalogImpl catalog, InvDatasetImpl parent, String name, String path, String scanLocation, CrawlableDatasetFilter filter,
                         boolean addDatasetSize, String addLatest, boolean sortOrderIncreasing,
                         String datasetNameMatchPattern, String startTimeSubstitutionPattern, String duration ) {

    super(parent, name, makeHref(path));
    log.debug(  "InvDatasetScan(): parent="+ parent + ", name="+ name +" , path="+ path +" , scanLocation="+ scanLocation +" , filter="+ filter +" , addLatest="+ addLatest +" , sortOrderIncreasing="+ sortOrderIncreasing +" , datasetNameMatchPattern="+ datasetNameMatchPattern +" , startTimeSubstitutionPattern= "+ startTimeSubstitutionPattern +", duration="+duration);

    this.rootPath = path;
    this.scanLocation = scanLocation;
    this.filter = filter;
    this.crDsClassName = null;
    this.crDsConfigObj = null;
    this.scanLocationCrDs = createScanLocationCrDs();
    this.isValid = true;
    if ( this.scanLocationCrDs == null )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=").append( path)
              .append( "; scanLocation=" ).append( scanLocation )
              .append(">: could not create CrawlableDataset for scanLocation." );
    }
    else if ( ! this.scanLocationCrDs.exists() )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=").append( scanLocation )
              .append(">: CrawlableDataset for scanLocation does not exist." );
    }
    else if ( ! this.scanLocationCrDs.isCollection() )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=" ).append( scanLocation )
              .append( ">: CrawlableDataset for scanLocation not a collection." );
    }

    this.identifier = null;
    this.namer = null;
    this.addDatasetSize = addDatasetSize;

    this.sorter = new LexigraphicByNameSorter( sortOrderIncreasing );

    // Add latest resolver dataset to Map of ProxyDatasetHandlers
    this.proxyDatasetHandlers = new HashMap<>();
    if ( addLatest != null )
    {
      if ( addLatest.equalsIgnoreCase( "true" ) )
      {
        InvService service = catalog.findService( "latest" );
        if ( service != null )
        {
          ProxyDatasetHandler proxyDsHandler = new SimpleLatestProxyDsHandler( "latest.xml", true, service, true );
          this.proxyDatasetHandlers.put( "latest.xml", proxyDsHandler );
        }
      }
    }

    if ( datasetNameMatchPattern != null
         && startTimeSubstitutionPattern != null
         && duration != null )
    {
      childEnhancerList = new ArrayList<DatasetEnhancer>();
      childEnhancerList.add(
              RegExpAndDurationTimeCoverageEnhancer
                      .getInstanceToMatchOnDatasetName( datasetNameMatchPattern,
                                                        startTimeSubstitutionPattern,
                                                        duration ) );
    }

    // catalogRefExpander = new BooleanCatalogRefExpander( this.datasetScan.??? );
  }

  public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanLocation,
                         String configClassName, Object configObj, CrawlableDatasetFilter filter,
                         CrawlableDatasetLabeler identifier, CrawlableDatasetLabeler namer,
                         boolean addDatasetSize,
                         CrawlableDatasetSorter sorter, Map<String, ProxyDatasetHandler> proxyDatasetHandlers,
                         List<DatasetEnhancer> childEnhancerList, CatalogRefExpander catalogRefExpander )
  {
    super( parent, name, makeHref(path) );
    this.rootPath = path;
    this.scanLocation = scanLocation;
    this.crDsClassName = configClassName;
    this.crDsConfigObj = configObj;

    this.scanLocationCrDs = createScanLocationCrDs();
    this.isValid = true;
    if ( this.scanLocationCrDs == null )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=" ).append( scanLocation )
              .append( ">: could not create CrawlableDataset for scanLocation." );
    }
    else if ( ! this.scanLocationCrDs.exists() )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=" ).append( scanLocation )
              .append( ">: CrawlableDataset for scanLocation does not exist." );
    }
    else if ( ! this.scanLocationCrDs.isCollection() )
    {
      isValid = false;
      invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( path )
              .append( "; scanLocation=" ).append( scanLocation )
              .append( ">: CrawlableDataset for scanLocation not a collection." );
    }

    this.filter = filter;
    this.identifier = identifier;
    this.namer = namer;
    this.addDatasetSize = addDatasetSize;
    this.sorter = sorter;
    this.childEnhancerList = childEnhancerList;
    this.catalogRefExpander = catalogRefExpander;

    if ( proxyDatasetHandlers == null )
      this.proxyDatasetHandlers = new HashMap<>();
    else
      this.proxyDatasetHandlers = proxyDatasetHandlers;
  }

  public String getPath() { return rootPath; }
  //public void setPath( String path) { this.rootPath = path; }

  public String getScanLocation() { return scanLocation; }

  /**
   * Resets the location being scanned (DO NOT USE THIS METHOD, "public by accident").
   *
   * <p>Used by DataRootHandler to allow scanning an aliased directory ("content").
   *
   * @param scanLocation the scan location.
   */
  public void setScanLocation(String scanLocation )
  {
    // ToDo LOOK Instead hand InvDatasetScan (or InvCatFactory?) an algorithm for converting an aliased location.
    if ( ! scanLocation.equals( this.scanLocation))
    {
      this.isValid = true;
      this.scanLocation = scanLocation;
      this.scanLocationCrDs = createScanLocationCrDs();
      if ( this.scanLocationCrDs == null )
      {
        isValid = false;
        invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( rootPath )
                .append( "; scanLocation=" ).append( scanLocation )
                .append( ">: could not create CrawlableDataset for scanLocation." );
      }
      else if ( !this.scanLocationCrDs.exists() )
      {
        isValid = false;
        invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( rootPath )
                .append( "; scanLocation=" ).append( scanLocation )
                .append( ">: CrawlableDataset for scanLocation does not exist." );
      }
      else if ( !this.scanLocationCrDs.isCollection() )
      {
        isValid = false;
        invalidMessage = new StringBuilder( "Invalid InvDatasetScan <path=" ).append( rootPath )
                .append( "; scanLocation=" ).append( scanLocation )
                .append( ">: CrawlableDataset for scanLocation not a collection." );
      }
    }
  }

  public String getCrDsClassName() { return crDsClassName; }
  public Object getCrDsConfigObj() { return crDsConfigObj; }

  public CrawlableDatasetFilter getFilter() { return filter; }
  public CrawlableDatasetLabeler getIdentifier() { return identifier; }

  public CrawlableDatasetLabeler getNamer() { return namer; }
  public CrawlableDatasetSorter getSorter() { return sorter; }

  public Map<String, ProxyDatasetHandler> getProxyDatasetHandlers() { return proxyDatasetHandlers; }

  public boolean getAddDatasetSize() { return addDatasetSize; }
  public List<DatasetEnhancer> getChildEnhancerList() { return childEnhancerList; }

  public CatalogRefExpander getCatalogRefExpander() { return catalogRefExpander; }

  public boolean isValid() { return isValid; }
  public String getInvalidMessage() { return invalidMessage.toString(); }

  private CrawlableDataset createScanLocationCrDs() {
    try {
      // Create the CrawlableDataset for the scan location (scanLocation).
      return CrawlableDatasetFactory.createCrawlableDataset(scanLocation, crDsClassName, crDsConfigObj);
    } catch (IllegalAccessException | NoSuchMethodException | IOException | InvocationTargetException |
            InstantiationException | ClassNotFoundException e) {
      String message = String.format(
              "createScanLocationCrDs(): failed to create CrawlableDataset for collectionLevel <%s> and class <%s>",
              scanLocation, crDsClassName);
      log.error(message, e);
      return null;
    }
  }

  private CatalogBuilder buildCatalogBuilder()
  {
    // Setup and create catalog builder.

    InvService service = getServiceDefault();
    DatasetScanCatalogBuilder dsScanCatBuilder;
    try
    {
      dsScanCatBuilder = new DatasetScanCatalogBuilder( this, scanLocationCrDs, service );
    }
    catch ( IllegalArgumentException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CatalogBuilder for this collection <" + scanLocationCrDs.getPath() + ">: " + e.getMessage() );
      return null;
    }
    return dsScanCatBuilder;
  }

  /**
   * Return the CrawlableDataset path/location that corresponds to the
   * given dataset path. The given dataset path must start with the
   * datasetScan path for this InvDatasetScan, if not, a null is returned.
   *
   * @param dsPath a datasetScan dsPath that
   * @return the CrawlableDataset path that corresponds to the given dataset path or null.
   */
  public String translatePathToLocation( String dsPath )
  {
    if ( dsPath == null ) return null;

    if ( dsPath.length() > 0 )
      if ( dsPath.startsWith( "/" ) )
        dsPath = dsPath.substring( 1 );

    if ( ! dsPath.startsWith( this.getPath()))
      return null;

    // remove the matching part, the rest is the "data directory"
    String dataDir = dsPath.substring( this.getPath().length() );
    if ( dataDir.startsWith( "/" ) )
      dataDir = dataDir.substring( 1 );

    CrawlableDataset curCrDs = scanLocationCrDs.getDescendant( dataDir);

    if ( log.isDebugEnabled() ) log.debug( "translatePathToLocation(): url dsPath= " + dsPath + " to dataset dsPath= " + curCrDs.getPath() );

    return curCrDs.getPath();
  }

  /**
   * Return the CrawlableDataset for the given path, null if this InvDatasetScan
   * does not allow (filters out) the requested CrawlableDataset.
   *
   * <p>This method can handle requests for regular datasets and proxy datasets.
   *
   * @param path the path of the requested CrawlableDataset
   * @return the CrawlableDataset for the given path or null if the path is not allowed by this InvDatasetScan.
   * @throws IOException if an I/O error occurs while locating the children datasets.
   * @throws IllegalArgumentException if the given path is not a descendant of (or the same as) this InvDatasetScan collection level.
   */
  public CrawlableDataset requestCrawlableDataset( String path )
          throws IOException
  {
    String crDsPath = translatePathToLocation( path );
    if ( crDsPath == null )
      return null;

    CatalogBuilder catBuilder = buildCatalogBuilder();
    if ( catBuilder == null ) return null;
    return catBuilder.requestCrawlableDataset( crDsPath );
  }

  /**
   * Try to build a catalog for the given path by scanning the location
   * associated with this InvDatasetScan. The given path must start with
   * the path of this InvDatasetScan.
   *
   * @param orgPath the part of the baseURI that is the path
   * @param catURI the base URL for the catalog, used to resolve relative URLs.
   *
   * @return the catalog for this path (uses version 1.1) or null if build unsuccessful.
   */
  public InvCatalogImpl makeCatalogForDirectory( String orgPath, URI catURI ) {

    if ( log.isDebugEnabled())
    {
      log.debug( "baseURI=" + catURI );
      log.debug( "orgPath=" + orgPath );
      log.debug( "rootPath=" + rootPath );
      log.debug( "scanLocation=" + scanLocation );
    }

    // Get the dataset path.
    String dsDirPath = translatePathToLocation( orgPath );
    if ( dsDirPath == null )
    {
      String tmpMsg = "makeCatalogForDirectory(): Requesting path <" + orgPath + "> must start with \"" + rootPath + "\".";
      log.error( tmpMsg );
      return null;
    }


    // Setup and create catalog builder.
    CatalogBuilder catBuilder = buildCatalogBuilder();
    if ( catBuilder == null )
      return null;

    // A very round about way to remove the filename (e.g., "catalog.xml").
    // Note: Gets around "path separator at end of path" issues that are CrDs implementation dependant.
    // Note: Does not check that CrDs is allowed by filters.
    String dsPath = dsDirPath.substring( scanLocationCrDs.getPath().length() );
    if ( dsPath.startsWith( "/" ))
      dsPath = dsPath.substring( 1 );
    CrawlableDataset reqCrDs = scanLocationCrDs.getDescendant( dsPath );
    CrawlableDataset parent = reqCrDs.getParentDataset();
    if (parent == null) {
      log.error( "makeCatalogForDirectory(): I/O error getting parent crDs level <" + dsDirPath + ">: ");
      return null;
    }
    dsDirPath = parent.getPath();

    // Get the CrawlableDataset for the desired catalog level (checks that allowed by filters).
    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = catBuilder.requestCrawlableDataset( dsDirPath );
    }
    catch ( IOException e )
    {
      log.error( "makeCatalogForDirectory(): I/O error getting catalog level <" + dsDirPath + ">: " + e.getMessage(), e );
      return null;
    }

    if ( catalogCrDs == null )
    {
      log.warn( "makeCatalogForDirectory(): requested catalog level <" + dsDirPath + "> not allowed (filtered out).");
      return null;
    }
    if ( ! catalogCrDs.isCollection() )
    {
      log.warn( "makeCatalogForDirectory(): requested catalog level <" + dsDirPath + "> is not a collection.");
      return null;
    }

    // Generate the desired catalog using the builder.
    InvCatalogImpl catalog;
    try
    {
      catalog = catBuilder.generateCatalog( catalogCrDs );
    }
    catch ( IOException e )
    {
      log.error( "makeCatalogForDirectory(): catalog generation failed <" + catalogCrDs.getPath() + ">: " + e.getMessage() );
      return null;
    }

    // Set the catalog base URI.
    if ( catalog != null )
      catalog.setBaseURI( catURI );

//    InvDatasetImpl top = (InvDatasetImpl) catalog.getDataset();
//    // if we name it carefully, can get catalogRef to useProxy == true (disappear top dataset)
//    if ( service.isRelativeBase()) {
//      pos = dataDir.lastIndexOf("/");
//      String lastDir = (pos > 0) ? dataDir.substring(pos+1) : dataDir;
//      String topName = lastDir.length() > 0 ? lastDir : getName();
//      top.setName( topName);
//    }

    return catalog;
  }

  /**
   * Try to build a catalog for the given resolver path by scanning the
   * location associated with this InvDatasetScan. The given path must start
   * with the path of this InvDatasetScan and refer to a resolver
   * ProxyDatasetHandler that is part of this InvDatasetScan.
   *
   * @param path the part of the baseURI that is the path
   * @param baseURI the base URL for the catalog, used to resolve relative URLs.
   *
   * @return the resolver catalog for this path (uses version 1.1) or null if build unsuccessful.
   */
  public InvCatalogImpl makeProxyDsResolverCatalog( String path, URI baseURI )
  {
    if ( path == null ) return null;
    if ( path.endsWith( "/")) return null;

    // Get the dataset path.
    String dsDirPath = translatePathToLocation( path );
    if ( dsDirPath == null )
    {
      log.error( "makeProxyDsResolverCatalog(): Requsting path <" + path + "> must start with \"" + rootPath + "\"." );
      return null;
    }

    // Split into parent path and dataset name.
    int pos = dsDirPath.lastIndexOf('/');
    if ( pos == -1 )
    {
      log.error( "makeProxyDsResolverCatalog(): Requsting path <" + path + "> must contain a slash (\"/\")." );
      return null;
    }
    String dsName = dsDirPath.substring( pos + 1 );
    dsDirPath = dsDirPath.substring( 0, pos );

    // Find matching ProxyDatasetHandler.
    ProxyDatasetHandler pdh = this.getProxyDatasetHandlers().get( dsName );
    if ( pdh == null )
    {
      log.error( "makeProxyDsResolverCatalog(): No matching proxy dataset handler found <" + dsName + ">." );
      return null;
    }

    // Setup and create catalog builder.
    CatalogBuilder catBuilder = buildCatalogBuilder();
    if ( catBuilder == null )
      return null;

    // Get the CrawlableDataset for the desired catalog level.
    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = catBuilder.requestCrawlableDataset( dsDirPath );
    }
    catch ( IOException e )
    {
      log.error( "makeProxyDsResolverCatalog(): failed to create CrawlableDataset for catalogLevel <" + dsDirPath + "> and class <" + crDsClassName + ">: " + e.getMessage(), e );
      return null;
    }
    if ( catalogCrDs == null )
    {
      log.warn( "makeProxyDsResolverCatalog(): requested catalog level <" + dsDirPath + "> not allowed (filtered out)." );
      return null;
    }
    if ( ! catalogCrDs.isCollection())
    {
      log.warn( "makeProxyDsResolverCatalog(): requested catalog level <" + dsDirPath + "> not a collection." );
      return null;
    }

    // Generate the desired catalog using the builder.
    InvCatalogImpl catalog;
    try
    {
      catalog = (InvCatalogImpl) catBuilder.generateProxyDsResolverCatalog( catalogCrDs, pdh );
    }
    catch ( IOException e )
    {
      log.error( "makeProxyDsResolverCatalog(): catalog generation failed <" + catalogCrDs.getPath() + ">: " + e.getMessage() );
      return null;
    }

    // Set the catalog base URI.
    if ( catalog != null )
      catalog.setBaseURI( baseURI );

    return catalog;
  }

  /**
   * Try to build a catalog for the given path by scanning the location
   * associated with this InvDatasetScan. The given path must start with
   * the path of this InvDatasetScan.
   *
   * @param orgPath the part of the baseURI that is the path
   * @param baseURI the base URL for the catalog, used to resolve relative URLs.
   *
   * @return the catalog for this path (uses version 1.1) or null if build unsuccessful.
   *
   * @deprecated  Instead use {@link #makeProxyDsResolverCatalog(String, URI) makeProxyDsResolver()} which provides more general proxy dataset handling.
   */
  public InvCatalog makeLatestCatalogForDirectory( String orgPath, URI baseURI )
 {
   InvCatalogImpl cat = this.makeCatalogForDirectory( orgPath, baseURI );
   if ( cat == null ) return null;
   InvDatasetImpl topDs = (InvDatasetImpl) cat.getDatasets().get( 0); // Assumes catalog has one top-level dataset.

   // Keep only the regular datasets (i.e., remove CatalogRef and DatasetScan).
   for ( Iterator it = topDs.getDatasets().iterator(); it.hasNext(); )
   {
     InvDatasetImpl curDs = (InvDatasetImpl) it.next();
     if ( ! ( curDs.hasAccess()) || curDs.getUrlPath().endsWith( "latest.xml"))
     {
       it.remove();
     }
   }

   if ( topDs.getDatasets().isEmpty())
   {
     return( null);
   }

   // Grab the newest dataset.
   InvDatasetImpl latestDs = (InvDatasetImpl) java.util.Collections.max( topDs.getDatasets(), new java.util.Comparator()
   {
     public int compare( Object obj1, Object obj2 )
     {
       InvDataset ds1 = (InvDataset) obj1;
       InvDataset ds2 = (InvDataset) obj2;
       return ( ds1.getName().compareTo( ds2.getName() ) );
     }
   } );
   String baseName = topDs.getName();
   String latestName = baseName.length() == 0 ? "Latest" : "Latest " + baseName;
   latestDs.setName( latestName );

   InvCatalog subsetCat = DeepCopyUtils.subsetCatalogOnDataset( cat, latestDs );

   return ( subsetCat );
 }

  // override some InvCatalogRef stuf

   public boolean isRead() { return false; }


  boolean check(StringBuilder out, boolean show) {
    boolean isValid = true;

    if (getPath() == null) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): must have path attribute\n" );
      isValid = false;
    }

    if ( getScanLocation() == null) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): must have dirLocation attribute\n" );
      isValid = false;
    }

    if (getServiceDefault() == null) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): must have a default service\n" );
      isValid = false;
    }

    if (datasets.size() > 0) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): may not have nested datasets\n" );
      isValid = false;
      this.datasets = null;
    }

    return isValid && super.check(out, show);
  }
}
