// $Id: InvDatasetScan.java 48 2006-07-12 16:15:40Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.catalog;

import thredds.cataloggen.*;
import thredds.cataloggen.datasetenhancer.RegExpAndDurationTimeCoverageEnhancer;
import thredds.cataloggen.inserter.SimpleLatestProxyDsHandler;
import thredds.crawlabledataset.*;
import thredds.crawlabledataset.sorter.LexigraphicByNameSorter;
import thredds.crawlabledataset.filter.RegExpMatchOnNameSelector;
import thredds.crawlabledataset.filter.MultiSelectorFilter;

import java.net.URI;
import java.io.IOException;
import java.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Used to automatically generate catalogs.
 */
public class InvDatasetScan extends InvCatalogRef {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvDatasetScan.class);
  static private String context = "/thredds";
  static public void setContext(String c) { context = c; }
  static private String catalogServletName = "/catalog";
  static public void setCatalogServletName( String catServletName ) { catalogServletName = catServletName; }
  static private String makeHref(String path)
  {
    return context+( catalogServletName == null ? "" : catalogServletName )+"/"+path+"/catalog.xml";
  }

  ////////////////////////////////////////////////
  private String rootPath, scanDir;

  private String crDsClassName;
  private Object crDsConfigObj;

  private CrawlableDatasetFilter filter;

  private CrawlableDatasetLabeler identifier;
  private CrawlableDatasetLabeler namer;

  private CrawlableDatasetSorter sorter;

  private Map proxyDatasetHandlers;

  private boolean addDatasetSize;

  private List childEnhancerList;
  private CatalogRefExpander catalogRefExpander;

  public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanDir, String id, InvDatasetScan from )  {
    this(parent, name, path , scanDir,
         from.crDsClassName, from.crDsConfigObj, from.filter,
         from.identifier, from.namer, from.addDatasetSize,
         from.sorter, from.proxyDatasetHandlers, from.childEnhancerList,
         from.catalogRefExpander );

    setID( id);
  }

  public InvDatasetScan( InvCatalogImpl catalog, InvDatasetImpl parent, String name, String path, String scanDir, String filter,
                         boolean addDatasetSize, String addLatest, boolean sortOrderIncreasing,
                         String datasetNameMatchPattern, String startTimeSubstitutionPattern, String duration ) {

    super(parent, name, makeHref(path));
    log.debug(  "InvDatasetScan(): parent="+ parent + ", name="+ name +" , path="+ path +" , scanDir="+ scanDir +" , filter="+ filter +" , addLatest="+ addLatest +" , sortOrderIncreasing="+ sortOrderIncreasing +" , datasetNameMatchPattern="+ datasetNameMatchPattern +" , startTimeSubstitutionPattern= "+ startTimeSubstitutionPattern +", duration="+duration);

    this.rootPath = path;
    this.scanDir = scanDir;
    this.crDsClassName = null;
    this.crDsConfigObj = null;

    if ( filter != null )
    {
      // Include atomic datasets that match the given filter string.
      RegExpMatchOnNameSelector filterSelector = new RegExpMatchOnNameSelector( filter, true, true, false );
      this.filter = new MultiSelectorFilter( Collections.singletonList( filterSelector ) );
    }
    else
    {
      this.filter = null;
    }

    this.identifier = null;
    this.namer = null;
    this.addDatasetSize = addDatasetSize;

    this.sorter = new LexigraphicByNameSorter( sortOrderIncreasing );

    // Add latest resolver dataset to Map of ProxyDatasetHandlers
    this.proxyDatasetHandlers = new HashMap();
    if ( addLatest != null )
    {
      if ( addLatest.equalsIgnoreCase( "true" ) )
      {
        InvService service = catalog.findService( "latest" );
        if ( service != null )
        {
          ProxyDatasetHandler proxyDsHandler = new SimpleLatestProxyDsHandler( "latest.xml", true, service );
          this.proxyDatasetHandlers.put( "latest.xml", proxyDsHandler );
        }
      }
    }

    if ( datasetNameMatchPattern != null
         && startTimeSubstitutionPattern != null
         && duration != null )
    {
      childEnhancerList = new ArrayList();
      childEnhancerList.add( new RegExpAndDurationTimeCoverageEnhancer( datasetNameMatchPattern, startTimeSubstitutionPattern, duration ) );
    }

    // catalogRefExpander = new BooleanCatalogRefExpander( this.datasetScan.??? );
  }

  public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanDir,
                         String configClassName, Object configObj, CrawlableDatasetFilter filter,
                         CrawlableDatasetLabeler identifier, CrawlableDatasetLabeler namer,
                         boolean addDatasetSize,
                         CrawlableDatasetSorter sorter, Map proxyDatasetHandlers,
                         List childEnhancerList, CatalogRefExpander catalogRefExpander )
  {
    super( parent, name, makeHref(path) );
    this.rootPath = path;
    this.scanDir = scanDir;
    this.crDsClassName = configClassName;
    this.crDsConfigObj = configObj;
    this.filter = filter;
    this.identifier = identifier;
    this.namer = namer;
    this.addDatasetSize = addDatasetSize;
    this.sorter = sorter;
    this.childEnhancerList = childEnhancerList;
    this.catalogRefExpander = catalogRefExpander;

    if ( proxyDatasetHandlers == null )
      this.proxyDatasetHandlers = new HashMap();
    else
      this.proxyDatasetHandlers = proxyDatasetHandlers;
  }

  public String getPath() { return rootPath; }
  public void setPath( String path) { this.rootPath = path; }

  public String getScanDir() { return scanDir; }
  public void setScanDir(String scanDir) {
    this.scanDir = scanDir;
  }

  public String getCrDsClassName() { return crDsClassName; }
  public Object getCrDsConfigObj() { return crDsConfigObj; }

  public CrawlableDatasetFilter getFilter() { return filter; }
  public CrawlableDatasetLabeler getIdentifier() { return identifier; }

  public CrawlableDatasetLabeler getNamer() { return namer; }
  public CrawlableDatasetSorter getSorter() { return sorter; }

  public Map getProxyDatasetHandlers() { return proxyDatasetHandlers; }

  public boolean getAddDatasetSize() { return addDatasetSize; }
  public List getChildEnhancerList() { return childEnhancerList; }

  public CatalogRefExpander getCatalogRefExpander() { return catalogRefExpander; }

  private CatalogBuilder buildCatalogBuilder()
  {
    // Setup and create catalog builder.
    CrawlableDataset collectionCrDs;
    try
    {
      collectionCrDs = CrawlableDatasetFactory.createCrawlableDataset( scanDir, crDsClassName, crDsConfigObj );
    }
    catch ( IllegalAccessException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CrawlableDataset for collectionLevel <" + scanDir + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }
    catch ( NoSuchMethodException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CrawlableDataset for collectionLevel <" + scanDir + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }
    catch ( IOException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CrawlableDataset for collectionLevel <" + scanDir + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }
    catch ( InvocationTargetException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CrawlableDataset for collectionLevel <" + scanDir + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }
    catch ( InstantiationException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CrawlableDataset for collectionLevel <" + scanDir + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }
    catch ( ClassNotFoundException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CrawlableDataset for collectionLevel <" + scanDir + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }

    InvService service = getServiceDefault();
    DatasetScanCatalogBuilder dsScanCatBuilder;
    try
    {
      dsScanCatBuilder = new DatasetScanCatalogBuilder( this, collectionCrDs, service );
    }
    catch ( IllegalArgumentException e )
    {
      log.error( "buildCatalogBuilder(): failed to create CatalogBuilder for this collection <" + collectionCrDs.getPath() + ">: " + e.getMessage() );
      return null;
    }
    return dsScanCatBuilder;
  }

  public String translatePathToLocation( String path )
  {
    if ( path == null ) return null;

    if ( path.length() > 0 )
      if ( path.startsWith( "/" ) )
        path = path.substring( 1 );

    if ( ! path.startsWith( this.getPath()))
      return null;

    // remove the matching part, the rest is the "data directory"
    String dataDir = path.substring( this.getPath().length() );
    if ( dataDir.startsWith( "/" ) )
      dataDir = dataDir.substring( 1 );

    // the translated path is the dirLocaton plus the data directory
    String fullPath = this.getScanDir()
                      + ( this.getScanDir().endsWith( "/" ) ? "" : "/" )
                      + dataDir;
    fullPath = CrawlableDatasetFactory.normalizePath( fullPath );

    if ( log.isDebugEnabled() ) log.debug( "translatePathToLocation(): url path= " + path + " to dataset path= " + fullPath );

    return fullPath;
  }

  public CrawlableDataset requestCrawlableDataset( String path )
  {
    String crDsPath = translatePathToLocation( path );
    if ( crDsPath == null )
      return null;

    CatalogBuilder catBuilder = buildCatalogBuilder();
    if ( catBuilder == null ) return null;
    try
    {
      return catBuilder.requestCrawlableDataset( crDsPath);
    }
    catch ( IOException e )
    {
      log.debug( "requestCrawlableDataset(): IOException trying to create requested dataset: " + e.getMessage());
      return null;
    }
  }

  /**
   *
   * @param baseURI the base URL for the catalog, used to resolve relative URLs.
   * @param orgPath the part of the baseURI that is the path
   * @return the catalog for this path, uses version 1.1
   */
  public InvCatalogImpl makeCatalogForDirectory( URI baseURI, String orgPath ) {

    if ( log.isDebugEnabled())
    {
      log.debug( "baseURI=" + baseURI );
      log.debug( "orgPath=" + orgPath );
      log.debug( "rootPath=" + rootPath );
      log.debug( "scanDir=" + scanDir );
    }

    // Get the dataset path.
    String dsDirPath = translatePathToLocation( orgPath );
    if ( dsDirPath == null )
    {
      String tmpMsg = "makeCatalogForDirectory(): Requsting path <" + orgPath + "> must start with \"" + rootPath + "\".";
      log.error( tmpMsg );
      return null;
    }

    // Remove the filename, if any.
    int pos = dsDirPath.lastIndexOf( "/" );
    if ( pos >= 0 )
      dsDirPath = dsDirPath.substring( 0, pos );

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
      log.error( "makeCatalogForDirectory(): failed to create CrawlableDataset for catalogLevel <" + dsDirPath + "> and class <" + crDsClassName + ">: " + e.getMessage() );
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
      catalog = (InvCatalogImpl) catBuilder.generateCatalog( catalogCrDs );
    }
    catch ( IOException e )
    {
      log.error( "makeCatalogForDirectory(): catalog generation failed <" + catalogCrDs.getPath() + ">: " + e.getMessage() );
      return null;
    }

    // Set the catalog base URI.
    if ( catalog != null )
      catalog.setBaseURI( baseURI );

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
    int pos = dsDirPath.lastIndexOf( "/" );
    if ( pos == -1 )
    {
      log.error( "makeProxyDsResolverCatalog(): Requsting path <" + path + "> must contain a slash (\"/\")." );
      return null;
    }
    String dsName = dsDirPath.substring( pos + 1 );
    dsDirPath = dsDirPath.substring( 0, pos );

    // Find matching ProxyDatasetHandler.
    ProxyDatasetHandler pdh = (ProxyDatasetHandler) this.getProxyDatasetHandlers().get( dsName );
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
      log.error( "makeCatalogForDirectory(): failed to create CrawlableDataset for catalogLevel <" + dsDirPath + "> and class <" + crDsClassName + ">: " + e.getMessage() );
      return null;
    }
    if ( catalogCrDs == null )
    {
      log.warn( "makeCatalogForDirectory(): requested catalog level <" + dsDirPath + "> not allowed (filtered out)." );
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
      log.error( "makeCatalogForDirectory(): catalog generation failed <" + catalogCrDs.getPath() + ">: " + e.getMessage() );
      return null;
    }

    // Set the catalog base URI.
    if ( catalog != null )
      catalog.setBaseURI( baseURI );

    return catalog;
  }

  public InvCatalog makeLatestCatalogForDirectory( URI baseURI, String orgPath )
 {
   InvCatalogImpl cat = this.makeCatalogForDirectory( baseURI, orgPath );
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
   String latestName = baseName.equals( "" ) ? "Latest" : "Latest " + baseName;
   latestDs.setName( latestName );

   cat.subset( latestDs );

   return ( cat );
 }

  // override some InvCatalogRef stuf

   public boolean isRead() { return false; }


  boolean check(StringBuffer out, boolean show) {
    boolean isValid = true;

    if (getPath() == null) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): must have path attribute\n" );
      isValid = false;
    }

    if (getScanDir() == null) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): must have dirLocation attribute\n" );
      isValid = false;
    }

    if (getServiceDefault() == null) {
      out.append( "**Error: DatasetScan (" ).append( getFullName() ).append( "): must have a default service\n" );
      isValid = false;
    }

    return isValid && super.check(out, show);
  }
}