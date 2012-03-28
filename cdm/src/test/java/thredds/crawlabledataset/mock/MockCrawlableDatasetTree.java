/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.crawlabledataset.mock;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Collections;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class MockCrawlableDatasetTree implements CrawlableDataset
{
  private final MockCrawlableDataset delegate;
  private final Map<String,MockCrDsInfo> crDsTree;

//  private final String[] crDsTree =
//          {
//                  "/data/grid/ncep/fred.nc",
//                  "/data/grid/ncep/gfs/ak/20090531_00.grib2",
//                  "/data/grid/ncep/gfs/ak/20090531_06.grib2",
//                  "/data/grid/ncep/gfs/ak/20090531_12.grib2",
//                  "/data/grid/ncep/gfs/ak/20090531_18.grib2",
//                  "/data/grid/ncep/gfs/conus/20090531_00.grib2",
//                  "/data/grid/ncep/gfs/conus/20090531_06.grib2",
//                  "/data/grid/ncep/gfs/conus/20090531_12.grib2",
//                  "/data/grid/ncep/gfs/conus/20090531_18.grib2",
//          };

//  private final String path;
//  private final String name;
//  private final int lastPathSegmentSeparatorIndx;
//
//  private Date lastModified = null;
//  private long length = -1;
//  private boolean exists = true;
//
//  private final boolean isCollection;


  public MockCrawlableDatasetTree( String path, Map<String,MockCrDsInfo> crDsTree )
  {
    if ( crDsTree == null || crDsTree.isEmpty() )
      throw new IllegalArgumentException( "CrawlableDataset tree must not be null or empty.");

    if ( path == null )
    {
      path = crDsTree.keySet().iterator().next();
      this.delegate = new MockCrawlableDataset( crDsTree.get( path ) );
    }
    else
    {
      if ( crDsTree.containsKey( path ))
        this.delegate = new MockCrawlableDataset( crDsTree.get( path));
      else
        this.delegate = new MockCrawlableDataset( new MockCrDsInfo( path, false, false, null, -1 ));
    }

    this.crDsTree = crDsTree;
  }

  public Object getConfigObject()
  {
    return null;
  }

  public String getPath()
  {
    return this.delegate.getPath();
  }

  public String getName()
  {
    return this.delegate.getName();
  }

  public CrawlableDataset getParentDataset()
  {
    if ( ! this.delegate.exists() )
      return null;

    CrawlableDataset possibleParent = this.delegate.getParentDataset();
    if ( possibleParent == null )
      return null;

    if ( crDsTree.containsKey( possibleParent.getPath() ))
      return new MockCrawlableDatasetTree( possibleParent.getPath(), this.crDsTree );
    // ToDo
    return this.delegate.getParentDataset();
  }

  public boolean exists()
  {
    return this.delegate.exists();
  }

  public boolean isCollection()
  {
    return this.delegate.isCollection();
  }

  public CrawlableDataset getDescendant( String relativePath )
  {
    if ( ! this.delegate.exists())
      return null;

    // ToDo
    return this.delegate.getDescendant( relativePath );
  }

  public List<CrawlableDataset> listDatasets() throws IOException
  {
    if ( ! this.delegate.exists() )
      return Collections.emptyList();

    // ToDo
    return null;
  }

  public List<CrawlableDataset> listDatasets( CrawlableDatasetFilter filter ) throws IOException
  {
    if ( ! this.delegate.exists() )
      return Collections.emptyList();

    // ToDo
    return null;
  }

  public long length()
  {
    return this.delegate.length();
  }

  public Date lastModified()
  {
    return this.delegate.lastModified();
  }
}
