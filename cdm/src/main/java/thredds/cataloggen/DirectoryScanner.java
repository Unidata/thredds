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
// $Id: DirectoryScanner.java 63 2006-07-12 21:50:51Z edavis $
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
import thredds.crawlabledataset.sorter.LexigraphicByNameSorter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
      MultiSelectorFilter.Selector selector = new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( filterPattern), true, true, false );
      filter = new MultiSelectorFilter( selector );
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
      enhancerList.add( RegExpAndDurationTimeCoverageEnhancer.getInstanceToMatchOnDatasetName( dsNameMatchPattern, startTimeSubstitutionPattern, duration ) );
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
