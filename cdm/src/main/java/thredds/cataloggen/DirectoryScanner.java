// $Id: DirectoryScanner.java,v 1.32 2006/05/19 19:23:04 edavis Exp $
package thredds.cataloggen;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvService;
import thredds.cataloggen.catalogrefexpander.BooleanCatalogRefExpander;
import thredds.cataloggen.datasetenhancer.RegExpAndDurationTimeCoverageEnhancer;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.filter.MultiSelectorFilter;
import thredds.crawlabledataset.filter.RegExpMatchOnNameFilter;
import thredds.crawlabledataset.filter.RegExpMatchOnNameSelector;
import thredds.crawlabledataset.sorter.LexigraphicByNameSorter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Allow generation of THREDDS InvCatalog documents from the contents of a local directory.
 *
 * @author edavis
 * @since 2004-12-10T15:33:32-0700
 */
public class DirectoryScanner
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectoryScanner.class);

  private String serviceTitle;
  private File serviceBaseUrlDir;
  private CrawlableDataset collectionCrDs;

  private String prefixPath;
  private boolean createCatalogRefs = true;

  private InvService service;

  /**
   * Construct a DirectoryScanner given information about the data server to be cataloged.
   *
   * @param service - the service for the datasets found in the directory scanned.
   * @param serviceTitle - a title for the service (used as the title of the top-level dataset).
   * @param serviceBaseUrlDir - the local directory to which the service's base URL references.
   * @param prefixPath - path name to append to the urlPath of resulting datasets, can be null.
   * @param createCatalogRefs - if true, generate a catalogRef for each directory, otherwise, recurse into directories.
   *
   * @throws IllegalArgumentException if invalid service type is given or root directory is not a directory.
   */
  public DirectoryScanner( InvService service, String serviceTitle, File serviceBaseUrlDir, String prefixPath, boolean createCatalogRefs )
  {
    this.service = service;
    this.serviceTitle = serviceTitle;
    this.serviceBaseUrlDir = serviceBaseUrlDir;
    try
    {
      collectionCrDs = CrawlableDatasetFactory.createCrawlableDataset( serviceBaseUrlDir.getAbsolutePath(), null, null );
    }
    catch ( IOException e )
    {
      // @todo Should throw an IOException!
      throw new IllegalArgumentException( "IOException while creating dataset: " + e.getMessage() );
    }
    catch ( ClassNotFoundException e )
    {
      throw new IllegalArgumentException( "Did not find class: " + e.getMessage() );
    }
    catch ( NoSuchMethodException e )
    {
      throw new IllegalArgumentException( "Required constructor not found in class: " + e.getMessage() );
    }
    catch ( IllegalAccessException e )
    {
      throw new IllegalArgumentException( "Did not have necessary access to class: " + e.getMessage() );
    }
    catch ( InvocationTargetException e )
    {
      throw new IllegalArgumentException( "Could not invoke required method in class: " + e.getMessage() );
    }
    catch ( InstantiationException e )
    {
      throw new IllegalArgumentException( "Could not instatiate class: " + e.getMessage() );
    }
    if ( ! collectionCrDs.isCollection())
      throw new IllegalArgumentException( "Base URL directory is not a directory <" + serviceBaseUrlDir.getAbsolutePath() + ">.");

    this.prefixPath = prefixPath;
    this.createCatalogRefs = createCatalogRefs;

//    log.debug( "DirectoryScanner(): service="+service.getName()+"; serviceTitle="+serviceTitle+"; serviceBaseUrlDir="+serviceBaseUrlDir+"; createCatalogRefs="+createCatalogRefs);
//    this.serviceTitle = serviceTitle;
//    this.serviceBaseUrlDir = serviceBaseUrlDir;
//    if ( this.serviceBaseUrlDir.getPath().indexOf( "*") == -1)
//    {
//      if ( ! this.serviceBaseUrlDir.isDirectory()) throw new IllegalArgumentException( "Base URL directory is not a directory <" + serviceBaseUrlDir.getAbsolutePath() + ">.");
//    }
//    this.service = new ResultService( service, serviceBaseUrlDir.getAbsolutePath());
//
//    this.prefixPath = prefixPath;
//    this.createCatalogRefs = createCatalogRefs;
  }


  /**
   * Return a catalog for the given directory.
   *
   * @param directory - the directory to represent as a catalog.
   * @param filterPattern
   * @param sortInIncreasingOrder
   * @return An InvCatalog that represents the given directory of datasets.
   *
   * @throws NullPointerException if the given directory is null.
   * @throws IllegalArgumentException if the given directory is not actually a directory or if it is not under the service base URL directory.
   */
  public InvCatalog getDirCatalog( File directory, String filterPattern, boolean sortInIncreasingOrder, boolean addDatasetSize )
  {
    return( this.getDirCatalog( directory, filterPattern, sortInIncreasingOrder, null, addDatasetSize, null, null, null) );
  }

  public InvCatalog getDirCatalog( File directory, String filterPattern, boolean sortInIncreasingOrder, String addIdBase, boolean addDatasetSize, String dsNameMatchPattern, String startTimeSubstitutionPattern, String duration )
  {
    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = CrawlableDatasetFactory.createCrawlableDataset( directory.getAbsolutePath(), null, null );
    }
    catch ( IOException e )
    {
      // @todo Should throw an IOException!
      throw new IllegalArgumentException( "IOException while creating dataset: " + e.getMessage() );
    }
    catch ( ClassNotFoundException e )
    {
      throw new IllegalArgumentException( "Did not find class: " + e.getMessage() );
    }
    catch ( NoSuchMethodException e )
    {
      throw new IllegalArgumentException( "Required constructor not found in class: " + e.getMessage() );
    }
    catch ( IllegalAccessException e )
    {
      throw new IllegalArgumentException( "Did not have necessary access to class: " + e.getMessage() );
    }
    catch ( InvocationTargetException e )
    {
      throw new IllegalArgumentException( "Could not invoke required method in class: " + e.getMessage() );
    }
    catch ( InstantiationException e )
    {
      throw new IllegalArgumentException( "Could not instatiate class: " + e.getMessage() );
    }
    if ( ! catalogCrDs.isCollection() )
      throw new IllegalArgumentException( "catalog directory is not a directory <" + serviceBaseUrlDir.getAbsolutePath() + ">." );

    return getDirCatalog( catalogCrDs, filterPattern, sortInIncreasingOrder, addIdBase, addDatasetSize, dsNameMatchPattern, startTimeSubstitutionPattern, duration );
  }
  public InvCatalog getDirCatalog( CrawlableDataset catalogCrDs, String filterPattern, boolean sortInIncreasingOrder, String addIdBase, boolean addDatasetSize, String dsNameMatchPattern, String startTimeSubstitutionPattern, String duration )
  {

    // Setup the filter
    CrawlableDatasetFilter filter = null;
    if ( filterPattern != null )
    {
      // Include atomic datasets that match the given filter string.
      RegExpMatchOnNameSelector filterSelector = new RegExpMatchOnNameSelector( filterPattern, true, true, false );
      filter = new MultiSelectorFilter( Collections.singletonList( filterSelector ) );
    }
    else
    {
      filter = new RegExpMatchOnNameFilter( ".*" );
    }
    List enhancerList = null;
    if ( dsNameMatchPattern != null
         && startTimeSubstitutionPattern != null
         && duration != null )
    {
      enhancerList = new ArrayList();
      enhancerList.add( new RegExpAndDurationTimeCoverageEnhancer( dsNameMatchPattern, startTimeSubstitutionPattern, duration ) );
    }
    CatalogBuilder catBuilder = new StandardCatalogBuilder( prefixPath, null, collectionCrDs, filter, service,
                                                            addIdBase, null, null, addDatasetSize,
                                                            new LexigraphicByNameSorter( sortInIncreasingOrder ), null, enhancerList , null,
                                                            new BooleanCatalogRefExpander( ! this.createCatalogRefs) );

    InvCatalog catalog;
    try
    {
      catalog = catBuilder.generateCatalog( catalogCrDs );
    }
    catch ( IOException e )
    {
      throw new IllegalArgumentException( "Could not generate catalog: " + e.getMessage() );
    }

//    log.debug( "getDirCatalog(): directory=" + directory + "; filterPattern=" + filterPattern + "; sortInIncreasingOrder=" + sortInIncreasingOrder + "; addIdBase="+addIdBase+"; dsNameMatchPattern=" + dsNameMatchPattern + "; startTimeSubstitutionPattern=" + startTimeSubstitutionPattern + "; duration=" + duration );
//    if ( !directory.isDirectory() )
//    {
//      String tmpMsg = "Given directory is not a directory <" + directory.getAbsolutePath() + ">.";
//      log.warn( tmpMsg);
//      throw new IllegalArgumentException( tmpMsg );
//    }
//    DatasetSource dsSource = DatasetSource.newDatasetSource( directory.getName(),
//                                                             DatasetSourceType.LOCAL, DatasetSourceStructure.DIRECTORY_TREE,
//                                                             directory.getAbsolutePath(), this.service );
//    dsSource.setPrefixUrlPath( this.prefixPath );
//    dsSource.setCreateCatalogRefs( this.createCatalogRefs );
//    dsSource.setAddDatasetSize( addDatasetSize );
//    if ( filterPattern != null )
//    {
//      DatasetFilter datasetFilter = new DatasetFilter( dsSource, "Filter files on \"" + filterPattern + "\"",
//                                                       DatasetFilter.Type.REGULAR_EXPRESSION, filterPattern );
//      datasetFilter.setMatchPatternTarget( "name");
//      dsSource.addDatasetFilter( datasetFilter );
//      datasetFilter = new DatasetFilter( dsSource, "Allow all dirs",
//                                         DatasetFilter.Type.REGULAR_EXPRESSION,
//                                         "", true, false, false );
//      datasetFilter.setMatchPatternTarget( "name" );
//      dsSource.addDatasetFilter( datasetFilter );
//    }
//    dsSource.setDatasetSorter( new DatasetSorter( sortInIncreasingOrder));
//    if ( dsNameMatchPattern != null
//         && startTimeSubstitutionPattern != null
//         && duration != null)
//    {
//      dsSource.addDatasetEnhancer( DatasetEnhancer1.createAddTimeCoverageEnhancer( dsNameMatchPattern, startTimeSubstitutionPattern, duration));
//    }
//
//    if ( addIdBase != null)
//    {
//      dsSource.addDatasetEnhancer( DatasetEnhancer1.createAddIdEnhancer( addIdBase));
//    }
//
//    InvCatalog cat = null;
//    try
//    {
//      cat = dsSource.fullExpand();
//    }
//    catch ( IOException e )
//    {
//      throw new IllegalArgumentException( "Given directory is not a collection dataset <" + directory.getAbsolutePath() + ">: " + e.getMessage() );
//    }
    InvDataset topDs = catalog.getDataset();
    if ( collectionCrDs.getPath().equals( catalogCrDs.getPath() ) && this.serviceTitle != null )
    //if ( topDs.getName().equals( "" ) && this.serviceTitle != null )
    {
      logger.warn( "getDirCatalog(): top dataset name is null, setting to serviceTitle <" + this.serviceTitle + ">");
      ( (InvDatasetImpl) topDs ).setName( this.serviceTitle );
    }


    return ( catalog );
  }

//  public InvCatalog getDirCatalog( String dirPattern, String filterPattern, boolean sortInIncreasingOrder, boolean addDatasetSize )
//  {
//    return( this.getDirCatalog( dirPattern, filterPattern, sortInIncreasingOrder, null, addDatasetSize, null, null, null) );
//  }

//  public InvCatalog getDirCatalog( String dirPattern, String filterPattern, boolean sortInIncreasingOrder, String addIdBase, boolean addDatasetSize, String dsNameMatchPattern, String startTimeSubstitutionPattern, String duration )
//  {
//    // If no wildcards in directory pattern, return catalog for single directory.
//    if ( dirPattern.indexOf( "*") == -1) return( this.getDirCatalog( new File( dirPattern), filterPattern, sortInIncreasingOrder, addIdBase, addDatasetSize, dsNameMatchPattern, startTimeSubstitutionPattern, duration));
//
//    //
//    log.debug( "dirPattern=" + dirPattern + " - filterPattern=" + filterPattern);
//    int index1 = dirPattern.lastIndexOf( "/", dirPattern.indexOf( "*"));
//    int index2 = dirPattern.indexOf( "/", index1+1 );
//    log.debug( "[" + index1 + "] - [" + index2 + "]");
//    String pathName1 = dirPattern.substring( 0, index1);
//    StringBuffer pathNameWC = new StringBuffer( dirPattern.substring(  index1+1, index2) );
//    String pathName2 = dirPattern.substring(  index2+1);
//    log.debug( "dirPattern <" + dirPattern + ">=<" + pathName1 + "[" + index1 + "]" + pathNameWC + "[" + index2 + "]" + pathName2 + ">");
//
//    File startDir = new File( pathName1 );
//    if ( ! startDir.isDirectory())
//    {
//      String tmpMsg = "Not a directory <" + startDir.getPath() + ">";
//      log.warn( tmpMsg);
//      throw new IllegalArgumentException( tmpMsg);
//    }
//    File[] curMatchFiles = startDir.listFiles( new MyFileFilter( pathNameWC.toString()));
//    log.debug( "Match Files:" );
//    List fullMatchDirs = new ArrayList();
//    for( int i=0; i<curMatchFiles.length; i++)
//    {
//      log.debug( "  [" + i + "]=" + curMatchFiles[ i] );
//      String curMatchPathName = pathName2 == null ? curMatchFiles[i].getPath() : curMatchFiles[i].getPath() + "/" + pathName2;
//      if ( new File( curMatchPathName).isDirectory())
//      {
//        fullMatchDirs.add( curMatchPathName);
//      }
//    }
//
//    // Get catalogs for each matching path.
//    List matchPathCatalogs = new ArrayList();
//    for ( Iterator it = fullMatchDirs.iterator(); it.hasNext(); )
//    {
//      String curMatchPath = (String) it.next();
//      InvCatalog curCatalog = this.getDirCatalog( new File( curMatchPath), filterPattern, sortInIncreasingOrder, addIdBase, addDatasetSize, dsNameMatchPattern, startTimeSubstitutionPattern, duration );
//      matchPathCatalogs.add( curCatalog);
//    }
//
//    return( this.combineCatalogs( dirPattern, matchPathCatalogs));
//  }

//  private InvCatalog combineCatalogs( String combinedName, List catalogs)
//  {
//    if ( catalogs == null) throw new IllegalArgumentException( "Null catalog list.");
//    if ( catalogs.isEmpty()) throw new IllegalArgumentException( "Empty catalog list.");
//    InvCatalog cat = (InvCatalog) catalogs.get(0);
//    InvDataset top = (InvDataset) cat.getDataset();
//    for ( int i = 1; i < catalogs.size(); i++ )
//    {
//      InvCatalog curCat = (InvCatalog) catalogs.get( i );
//      top.getDatasets().addAll( curCat.getDataset().getDatasets() );
//    }
//
//    // @todo sort the datasets.
//
//    // Rename catalog with combined name.
//    if ( combinedName != null ) ((InvDatasetImpl) top).setName( combinedName);
//
//    // Finish up.
//    ((InvCatalogImpl) cat).finish();
//
//    return( cat);
//  }

//  private class MyFileFilter implements FileFilter
//  {
//    private String matchPattern;
//    private java.util.regex.Pattern pattern;
//    private java.util.regex.Matcher matcher;
//    MyFileFilter( String matchPattern )
//    {
//      StringBuffer tmpSb = new StringBuffer( matchPattern );
//      int index = tmpSb.indexOf( "*");
//      while ( index != -1)
//      {
//        tmpSb.replace( index, index+1, ".*");
//
//        index = tmpSb.indexOf( "*", index+2);
//      }
//      this.matchPattern = tmpSb.toString();
//      this.pattern = java.util.regex.Pattern.compile( this.matchPattern);
//    }
//    public boolean accept( File file )
//    {
//      if ( !file.isDirectory() ) return ( false );
//      this.matcher = this.pattern.matcher( file.getName());
//      if ( this.matcher.matches()) return( true);
//      return ( false );
//    }
//
//  }
}

/*
 * $Log: DirectoryScanner.java,v $
 * Revision 1.32  2006/05/19 19:23:04  edavis
 * Convert DatasetInserter to ProxyDatasetHandler and allow for a list of them (rather than one) in
 * CatalogBuilders and CollectionLevelScanner. Clean up division between use of url paths (req.getPathInfo())
 * and translated (CrawlableDataset) paths.
 *
 * Revision 1.31  2006/01/20 02:08:23  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.30  2006/01/17 20:58:51  edavis
 * Several small fixes to read/write of datasetScan element. A few documentation updates.
 *
 * Revision 1.29  2005/12/30 00:18:53  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.28  2005/12/16 23:19:35  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.27  2005/12/06 19:39:20  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 * Revision 1.26  2005/08/22 19:39:12  edavis
 * Changes to switch /thredds/dqcServlet URLs to /thredds/dqc.
 * Expand testing for server installations: TestServerSiteFirstInstall
 * and TestServerSite. Fix problem with compound services breaking
 * the filtering of datasets.
 *
 * Revision 1.25  2005/07/22 16:19:51  edavis
 * Allow DatasetSource and InvDatasetScan to add dataset size metadata.
 *
 * Revision 1.24  2005/07/20 22:44:55  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.23  2005/07/13 22:48:06  edavis
 * Improve server logging, includes adding a final log message
 * containing the response time for each request.
 *
 * Revision 1.22  2005/07/08 18:35:00  edavis
 * Fix problem dealing with service URLs that are relative
 * to the catalog (base="") and those that are relative to
 * the collection (base URL is not empty).
 *
 * Revision 1.21  2005/06/28 18:36:30  edavis
 * Fixes to adding TimeCoverage and ID to datasets.
 *
 * Revision 1.20  2005/06/24 22:00:57  edavis
 * Write DatasetEnhancer1 to allow adding metadata to datasets.
 * Implement DatasetEnhancers for adding timeCoverage and for
 * adding ID to datasets. Also fix DatasetFilter so that 1) if
 * no filter is applicable for collection datasets, allow all
 * collection datasets and 2) if no filter is applicable for
 * atomic datasets, allow all atomic datasets.
 *
 * Revision 1.19  2005/06/07 22:50:23  edavis
 * Fixed catalogRef links so relative to catalog instead of to service.
 * Fixed all tests in TestAllCatalogGen (including changing directory
 * filters because catalogRef names no longer contain slashes ("/").
 *
 * Revision 1.18  2005/06/06 18:25:53  edavis
 * Update DirectoryScanner to allow all directories even if name
 * doesn't send with "/".
 *
 * Revision 1.17  2005/06/03 19:12:42  edavis
 * Start adding wildcard handling in DirectoryScanner. Change
 * how DatasetSource names datasets and how catalogRefs are
 * constructed in DatasetSource.expand().
 *
 * Revision 1.16  2005/05/04 03:37:06  edavis
 * Remove several unnecessary methods in DirectoryScanner.
 *
 * Revision 1.15  2005/05/03 17:04:03  edavis
 * Add sort to datasetScan element and handle wildcard character in directory name.
 *
 * Revision 1.14  2005/04/29 14:55:57  edavis
 * Fixes for change in InvCatalogFactory.writeXML( cat, filename) method
 * signature. And start on allowing wildcard characters in pathname given
 * to DirectoryScanner.
 *
 * Revision 1.13  2005/04/27 23:05:41  edavis
 * Move sorting capabilities into new DatasetSorter class.
 * Fix a bunch of tests and such.
 *
 * Revision 1.12  2005/04/27 21:34:09  caron
 * cleanup DirectoryScanner, InvDatasetScan
 *
 * Revision 1.11  2005/04/05 22:37:02  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.10  2005/02/03 20:47:59  edavis
 * Fix filters to allow all directories.
 *
 * Revision 1.9  2005/02/01 22:55:16  edavis
 * Add dataset filtering to DirectoryScanner.
 *
 * Revision 1.8  2005/01/20 23:13:31  edavis
 * Extend DirectoryScanner to handle catalog generation for a list of top-level
 * data directories:
 * 1) add getMainCatalog(List):void to DirectoryScanner;
 * 2) add expand(List):void to DatasetSource, and
 * 3) two changes to the abstract methods in DatasetSource:
 *   a) add createDataset(String):InvDataset and
 *   b) rename getTopLevelDataset():InvDataset to
 *      createSkeletonCatalog():InvDataset.
 *
 * Revision 1.7  2005/01/14 21:24:34  edavis
 * Add handling of datasetSource@createCatalogRefs to DTD/XSD and
 * CatGenConfigMetadataFactory and testing.
 *
 * Revision 1.6  2005/01/14 18:02:24  edavis
 * Add createCatalogRef to DirectoryScanner constructor. Add testing.
 *
 * Revision 1.5  2004/12/29 21:53:21  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.4  2004/12/23 22:56:51  edavis
 * Change variable from serviceRootDir to serviceBaseUrlDir for clarity.
 *
 * Revision 1.3  2004/12/22 22:28:59  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.2  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.1  2004/12/14 22:47:22  edavis
 * Add simple interface to thredds.cataloggen and continue adding catalogRef capabilities.
 *
 */