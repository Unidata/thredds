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
// $Id: SimpleLatestProxyDsHandler.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen.inserter;

import thredds.catalog.InvService;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.cataloggen.InvCrawlablePair;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.io.IOException;
import java.util.*;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 29, 2005 12:12:53 PM
 */
public class SimpleLatestProxyDsHandler implements ProxyDatasetHandler
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( LatestCompleteProxyDsHandler.class );

  private String latestName;
  private boolean locateAtTopOrBottom;
  private InvService service;
  private final boolean isResolver;

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
   */
  public SimpleLatestProxyDsHandler( String latestName, boolean locateAtTopOrBottom,
                                     InvService service, boolean isResolver )
  {
    this.latestName = latestName;
    this.locateAtTopOrBottom = locateAtTopOrBottom;
    this.service = service;
    this.isResolver = isResolver;
  }

  public boolean isLocateAtTopOrBottom() { return locateAtTopOrBottom; }

  public Object getConfigObject() { return null; }

  public String getProxyDatasetName()
  {
    return this.latestName;
  }

  public CrawlableDataset createProxyDataset( CrawlableDataset parent )
  {
    return new MyCrawlableDataset( parent, this.latestName );
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

    // Get the maximum item according to lexigraphic comparison of InvDataset names.
    return (InvCrawlablePair) Collections.max( atomicDsInfo, new Comparator()
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
    return baseName.equals( "" ) ? "Latest" : "Latest " + baseName;
  }

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

    public CrawlableDataset getParentDataset()
    {
      return parent;
    }

    public boolean exists()
    {
      return true; // @todo ????
    }

    public boolean isCollection()
    {
      return false;
    }

    public CrawlableDataset getDescendant( String childPath)
    {
      return null;
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
