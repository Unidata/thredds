// $Id$
package thredds.crawlabledataset;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.io.IOException;

import thredds.crawlabledataset.filter.WildcardMatchOnNameFilter;

// @todo Add "?" as another possible wildcard character.

/**
 * An alias for a collection of datasets (i.e., the dataset path contains
 * one or more wildcard characters ("*")).
 *
 * @author edavis
 * @since Jun 21, 2005T4:53:43 PM
 */
public class CrawlableDatasetAlias implements CrawlableDataset
{
  //private static Log log = LogFactory.getLog( CrawlableDatasetAlias.class );
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetAlias.class);

  private String path;
  private String name;

  private String wildcardPattern;
  private String postWildcardPath;

  private CrawlableDataset startDs;

  private String className;
  private Object configObj;

  public static boolean isAlias( String path )
  {
    return ( path.indexOf( "*" ) != -1 );
    //return ( path.indexOf( "?" ) != -1 || path.indexOf( "*" ) != -1 );
  }

  public CrawlableDatasetAlias( String path, String className, Object configObj )
  {
    if ( !isAlias( path ) ) throw new IllegalArgumentException( "No wildcard in path <" + path + ">." );

    this.path = path;

    this.className = className;
    this.configObj = configObj;

    // @todo Make sure works if "*" is in first part of dataset path
    // Determine the location of the first path section containing wildcard.
    int preWildcardIndex = this.path.lastIndexOf( "/", this.path.indexOf( "*" ) );
    int postWildcardIndex = this.path.indexOf( "/", preWildcardIndex + 1 );
    log.debug( "[" + preWildcardIndex + "] - [" + postWildcardIndex + "]" );
    String preWildcardPath = this.path.substring( 0, preWildcardIndex );
    this.wildcardPattern = postWildcardIndex == -1
                           ? this.path.substring( preWildcardIndex + 1 )
                           : this.path.substring( preWildcardIndex + 1, postWildcardIndex );
    this.postWildcardPath = postWildcardIndex == -1
                            ? null : this.path.substring( postWildcardIndex + 1 );
    log.debug( "dirPattern <" + this.path + ">=<" + preWildcardPath + "[" + preWildcardIndex + "]" + wildcardPattern + "[" + postWildcardIndex + "]" + postWildcardPath + ">" );

    // Set the name to be all segments from first wildcard on
    this.name = this.path.substring( preWildcardIndex + 1 );

    // Make sure pre-wildcard path is a directory.
    try
    {
      startDs = CrawlableDatasetFactory.createCrawlableDataset( preWildcardPath, this.className, this.configObj );
    }
    catch ( Exception e )
    {
      String tmpMsg = "Pre-wildcard path <" + preWildcardPath + "> not a CrawlableDataset of expected type <" + this.className + ">: " + e.getMessage();
      log.warn( "CrawlableDatasetAlias(): " + tmpMsg );
      throw new IllegalArgumentException( tmpMsg );
    }
    if ( !startDs.isCollection() )
    {
      String tmpMsg = "Pre-wildcard path not a directory <" + startDs.getPath() + ">";
      log.warn( "CrawlableDatasetAlias(): " + tmpMsg );
      throw new IllegalArgumentException( tmpMsg );
    }
  }

  public Object getConfigObject()
  {
    return configObj;
  }

  /** */
  public String getPath()
  {
    return ( this.path );
  }

  /**
   * Returns the name (unlike a CrawlableDataset, the name may not be related to the path).
   */
  public String getName()
  {
    return ( this.name );
  }

  public CrawlableDataset getParentDataset() throws IOException
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean isCollection()
  {
    return true;
  }

  public List listDatasets() throws IOException
  {
    // Get list of files in pre-wildcard directory that match the wildcard pattern.
    List curMatchDatasets = startDs.listDatasets( new CrawlableDatasetAlias.MyFilter( wildcardPattern, postWildcardPath != null ) );

    // The wildcard is in the last part of the alias path, so
    // the list from startDs is what we want.
    if ( postWildcardPath == null )
    {
      return curMatchDatasets;
    }
    //
    else
    {
      List list = new ArrayList();
      for ( Iterator it = curMatchDatasets.iterator(); it.hasNext(); )
      {
        CrawlableDataset curDs = (CrawlableDataset) it.next();

        // Append the remaining path to the end of the current dataset path.
        String curMatchPathName = curDs.getPath() + "/" + postWildcardPath;

        // Create a new CrawlableDataset with the new path.
        CrawlableDataset newCrawlableDs = null;
        try
        {
          newCrawlableDs = CrawlableDatasetFactory.createCrawlableDataset( curMatchPathName, className, configObj );
        }
        catch ( Exception e )
        {
          String tmpMsg = "Couldn't create CrawlableDataset for path <" + curMatchPathName + "> and given class name <" + className + ">: " + e.getMessage();
          log.warn( "listDatasets(): " + tmpMsg );
          continue;
        }

        // If the new dataset's path contains wildcard characters, add its
        // list of datasets to the return list.
        if ( isAlias( postWildcardPath ) )
        {
          list.addAll( newCrawlableDs.listDatasets() );
        }
        // If the new dataset's path does not contain any wildcard characters,
        // add the it to the return list.
        else
        {
          list.add( newCrawlableDs );
        }
      }
      return ( list );
    }
  }

  public List listDatasets( CrawlableDatasetFilter filter ) throws IOException
  {
    List list = this.listDatasets();
    if ( filter == null ) return list;
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curDs = (CrawlableDataset) it.next();
      if ( !filter.accept( curDs ) )
      {
        // @todo concurrency problems, remove from iterator or build new list
        list.remove( curDs );
      }
    }
    return ( list );
  }

  public long length()
  {
    return -1;
  }

  public Date lastModified()
  {
    return null;
  }

  private class MyFilter implements CrawlableDatasetFilter
  {
    private boolean mustBeCollection;
    private WildcardMatchOnNameFilter proxyFilter;

    /**
     * A CrawlableDatasetFilter that finds CrawlableDatasets where their name matches the given
     * wildcard string and are collections if mustBeCollection is true.
     *
     * @param wildcardString     a string containing wildcard characters ("*") to match against the CrawlableDataset name
     * @param mustBeCollection if true the filter only accepts collection datasets
     */
    MyFilter( String wildcardString, boolean mustBeCollection )
    {
      proxyFilter = new WildcardMatchOnNameFilter( wildcardString );
      this.mustBeCollection = mustBeCollection;
    }

  public Object getConfigObject() { return null; }

    public boolean accept( CrawlableDataset dataset )
    {
      if ( mustBeCollection && !dataset.isCollection() ) return ( false );
      return proxyFilter.accept( dataset);
    }

  }


}

/*
 * $Log: CrawlableDatasetAlias.java,v $
 * Revision 1.6  2006/02/08 20:46:11  edavis
 * Add comment.
 *
 * Revision 1.5  2006/01/20 02:08:24  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.4  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.3  2005/12/16 23:19:36  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/11/18 23:51:04  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:48  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.3  2005/08/22 17:40:23  edavis
 * Another round on CrawlableDataset: make CrawlableDatasetAlias a subclass
 * of CrawlableDataset; start generating catalogs (still not using in
 * InvDatasetScan or CatalogGen, yet).
 *
 * Revision 1.2  2005/07/13 22:54:22  edavis
 * Fix CrawlableDatasetAlias.
 *
 * Revision 1.1  2005/06/24 22:08:32  edavis
 * Second stab at the CrawlableDataset interface.
 *
 */