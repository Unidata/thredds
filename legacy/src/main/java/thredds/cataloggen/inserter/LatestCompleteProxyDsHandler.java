/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.cataloggen.inserter;

import thredds.catalog.InvService;
import thredds.cataloggen.InvCrawlablePair;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.io.IOException;
import java.util.*;

/**
 * Adds a latest complete dataset to a collection where the latest is determined
 * by the creation date stamp and the last modified date. The dataset name is
 * assumed to provide a creation date stamp that sorted lexigraphically gives
 * the most recent dataset at the end of the list. Any datasets whose last
 * modified date (CrawlableDataset.lastModified()) is more recent than the
 * present time minus the given lastModifiedLimit.
 *
 * @author edavis
 * @since Nov 29, 2005 12:12:53 PM
 */
public class LatestCompleteProxyDsHandler implements ProxyDatasetHandler
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( LatestCompleteProxyDsHandler.class );

  private final String latestName;
  private final boolean locateAtTopOrBottom;
  private final InvService service;
  private final boolean isResolver;

  private final long lastModifiedLimit;

  /**
   * Constructor.
   *
   * The latestName is used as the name of latest dataset created. The
   * location for the placement of the latest dataset is given by
   * locateAtTopOrBottom (true - locate on top; false - locate on bottom).
   *
   * @param latestName the name to be used for all latest dataset, if null, the default is "latest.xml".
   * @param locateAtTopOrBottom indicates where to locate the latest dataset (true - locate on top; false - locate on bottom).
   * @param service the InvService used by the created dataset.
   * @param lastModifiedLimit only use datasets whose lastModified() time is at least this many minutes in the past
   */
  public LatestCompleteProxyDsHandler( String latestName,
                                       boolean locateAtTopOrBottom,
                                       InvService service,
                                       boolean isResolver,
                                       long lastModifiedLimit )
  {
    this.latestName = latestName;
    this.locateAtTopOrBottom = locateAtTopOrBottom;
    this.service = service;
    this.isResolver = isResolver;

    this.lastModifiedLimit = lastModifiedLimit;
  }

  public String getName()
  { return latestName; }

  public boolean isLocateAtTopOrBottom()
  { return locateAtTopOrBottom; }

  public String getServiceName()
  { return service.getName(); }

  public long getLastModifiedLimit()
  { return lastModifiedLimit; }

  public String getProxyDatasetName()
  { return latestName; }

  public Object getConfigObject()
  { return null; }

  public CrawlableDataset createProxyDataset( CrawlableDataset parent )
  {
    return new LatestCompleteProxyDsHandler.MyCrawlableDataset( parent, this.latestName );
  }

  public InvService getProxyDatasetService( CrawlableDataset parent )
  {
    return service;
  }

  public int getProxyDatasetLocation( CrawlableDataset parent, int collectionDatasetSize )
  {
    if ( locateAtTopOrBottom )
      return 0;
    else
      return collectionDatasetSize;
  }

  public boolean isProxyDatasetResolver()
  {
    return this.isResolver;
  }

  public InvCrawlablePair getActualDataset( List atomicDsInfo )
  {
    if ( atomicDsInfo == null || atomicDsInfo.isEmpty() )
      return null;

    // Place into temporary list all dataset not modified more recently
    // than lastModifiedLimit before present.
    long targetTime = System.currentTimeMillis() - ( this.lastModifiedLimit * 60 * 1000 );
    List tmpList = new ArrayList( atomicDsInfo );
    for ( Iterator it = tmpList.iterator(); it.hasNext(); )
    {
      InvCrawlablePair curDsInfo = (InvCrawlablePair) it.next();
      CrawlableDataset curCrDs = curDsInfo.getCrawlableDataset();
      if ( curCrDs != null && curCrDs.lastModified() != null && curCrDs.lastModified().getTime() > targetTime )
      {
        it.remove();
      }
    }

    // Get the maximum item according to lexigraphic comparison of InvDataset names.

    return (InvCrawlablePair) Collections.max( tmpList, new Comparator()
    {
      public int compare( Object obj1, Object obj2 )
      {
        InvCrawlablePair dsInfo1 = (InvCrawlablePair) obj1;
        InvCrawlablePair dsInfo2 = (InvCrawlablePair) obj2;
        return ( dsInfo1.getInvDataset().getName().compareTo( dsInfo2.getInvDataset().getName() ) );
      }
    } );
  }

  public String getActualDatasetName( InvCrawlablePair actualDataset, String baseName )
  {
    if ( baseName == null ) baseName = "";
    return baseName.equals( "" ) ? "Latest" : "Latest " + baseName ;
  }

//  private static class LatestCompleteCrDS extends CrawlableDatasetFile
//  {
//    private CrawlableDataset parent;
//    private String name;
//    private CrawlableDatasetFile proxyCrDs;
//
//
//  }

  /**
   *
   */
  private static class MyCrawlableDataset implements CrawlableDataset
  {
    private CrawlableDataset parent;
    private String name;

    MyCrawlableDataset( CrawlableDataset parent, String name )
    {
      this.parent = parent;
      this.name = name;
    }

    public Object getConfigObject()
    {
      return null;
    }

    public String getPath()
    {
      return parent.getPath() + "/" + name;
    }

    public String getName()
    {
      return name;
    }

    public boolean exists()
    {
      return true; // @todo ????
    }

    public boolean isCollection()
    {
      return false;
    }

    public CrawlableDataset getDescendant( String childPath )
    {
      return null;
    }

    public CrawlableDataset getParentDataset()
    {
      return parent;
    }

    public List listDatasets()
    {
      return null;
    }

    public List listDatasets( CrawlableDatasetFilter filter )
    {
      return null;
    }

    public long length()
    {
      return -1;
    }

    public Date lastModified() // or long milliseconds?
    {
      return null;
    }
  }
}
