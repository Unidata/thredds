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

import java.util.*;
import java.io.IOException;

public class MockCrawlableDataset implements CrawlableDataset
{
  private final String path;
  private final String name;
  private final int lastPathSegmentSeparatorIndx;

  private Date lastModified = null;
  private long length = -1;
  private boolean exists = true;

  private final boolean isCollection;

  public MockCrawlableDataset( String path, boolean isCollection )
  {
    this( new MockCrDsInfo( path, true, isCollection,  null, -1));
  }

  public MockCrawlableDataset( MockCrDsInfo crDsInfo )
  {
    if ( crDsInfo == null )
      throw new IllegalArgumentException( "CrawlableDataset info must not be null.");

    if ( crDsInfo.getPath() == null || crDsInfo.getPath().equals( "" ) )
      throw new IllegalArgumentException( "Path must not be null or empty." );
    this.path = crDsInfo.getPath();

    lastPathSegmentSeparatorIndx = this.path.lastIndexOf( "/" );
    if ( lastPathSegmentSeparatorIndx == -1 )
      this.name = this.path;
    else
    {
      this.name = this.path.substring( lastPathSegmentSeparatorIndx + 1 );
      if ( this.name.equals( "" ) )
        throw new IllegalArgumentException( "Path [" + path + "] must not end with a slash (\"/\")" );
      if ( !this.path.endsWith( "/" + this.name ) )
        throw new IllegalArgumentException( "Path [" + this.path + "] must end with name [" + this.name + "]." );
    }

    this.exists = crDsInfo.isExists();
    this.isCollection = crDsInfo.isCollection();
    this.lastModified = crDsInfo.getLastModified();
    this.length = crDsInfo.getLength();
  }

  public Object getConfigObject()
  {
    return null;
  }

  public String getPath()
  {
    return path;
  }

  public String getName()
  {
    return name;
  }

  public CrawlableDataset getParentDataset()
  {
    if ( this.lastPathSegmentSeparatorIndx == -1 )
      return null;
    return new MockCrawlableDataset( this.path.substring( 0, lastPathSegmentSeparatorIndx ), true );
  }

  public boolean exists()
  {
    return this.exists;
  }

  public void setExists( boolean exists)
  {
    this.exists = exists;
  }

  public boolean isCollection()
  {
    return this.isCollection;
  }

  public CrawlableDataset getDescendant( String relativePath )
  {
    return null;
  }

  public List<CrawlableDataset> listDatasets() throws IOException
  {
    return null;
  }

  public List<CrawlableDataset> listDatasets( CrawlableDatasetFilter filter ) throws IOException
  {
    return null;
  }

  public long length()
  {
    return this.length;
  }

  public void setLength( long length )
  {
    this.length = length;
  }

  public Date lastModified() // or long milliseconds?
  {
    return this.lastModified;
  }

  public void setLastModified( Date lastModified)
  {
    this.lastModified = lastModified;
  }
}