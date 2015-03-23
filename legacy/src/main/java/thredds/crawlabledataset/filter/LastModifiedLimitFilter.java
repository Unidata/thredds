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
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.util.Date;

/**
 * Accept datasets whose last modified date is at least the given number
 * of milliseconds in the past.
 *
 * @author edavis
 */
public class LastModifiedLimitFilter implements CrawlableDatasetFilter
{
  private long lastModifiedLimitInMillis;

  /**
   * Constructor.
   *
   * @param lastModifiedLimitInMillis accept datasets whose lastModified() time is at least this many msecs in the past
   */
  public LastModifiedLimitFilter(long lastModifiedLimitInMillis )
  {
    this.lastModifiedLimitInMillis = lastModifiedLimitInMillis;
  }

  /**
   * Accept datasets whose last modified date is at least the
   * last modified limit of milliseconds in the past.
   *
   * @param dataset the dataset to filter
   * @return true if the datasets last modified date is at least lastModifiedLimitInMillis in the past.
   */
  public boolean accept( CrawlableDataset dataset)
  {
    Date lastModDate = dataset.lastModified();
    if ( lastModDate != null )
    {
      long now = System.currentTimeMillis();
      if ( now - lastModDate.getTime() > lastModifiedLimitInMillis )
        return true;
    }
    return false;
  }

  public Object getConfigObject()   { return null; }
  public long getLastModifiedLimitInMillis()   { return lastModifiedLimitInMillis; }
}
