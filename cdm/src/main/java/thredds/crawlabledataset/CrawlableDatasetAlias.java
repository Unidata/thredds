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
// $Id: CrawlableDatasetAlias.java 63 2006-07-12 21:50:51Z edavis $
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

  public boolean exists()
  {
    return true; // @todo ????
  }

  public boolean isCollection()
  {
    return true;
  }

  public CrawlableDataset getDescendant( String childPath )
  {
    return null; // @todo ????
  }

  public CrawlableDataset getParentDataset()
  {
    return null;
  }

  public List<CrawlableDataset> listDatasets() throws IOException
  {
    // Get list of files in pre-wildcard directory that match the wildcard pattern.
    List<CrawlableDataset> curMatchDatasets = startDs.listDatasets( new CrawlableDatasetAlias.MyFilter( wildcardPattern, postWildcardPath != null ) );

    // The wildcard is in the last part of the alias path, so
    // the list from startDs is what we want.
    if ( postWildcardPath == null )
    {
      return curMatchDatasets;
    }
    //
    else
    {
      List<CrawlableDataset> list = new ArrayList<CrawlableDataset>();
      for( CrawlableDataset curDs : curMatchDatasets)
      {
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

  public List<CrawlableDataset> listDatasets( CrawlableDatasetFilter filter ) throws IOException
  {
    List<CrawlableDataset> list = this.listDatasets();
    if ( filter == null ) return list;
    for ( CrawlableDataset curDs: list )
    {
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
