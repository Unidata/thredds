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

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * Build CrawlableDatasetFilters from other CrawlableDatasetFilters
 * using logical composition (AND, OR, NOT).
 *
 * <p> For instance, the expression
 *
 * <pre>(A || B) && !(C && D)</pre>
 *
 * can be expressed with the following code
 * (assuming A, B, C, and D are CrawlableDatasetFilters)
 *
 * <pre>
 * LogicalFilterComposer.getAndFilter(
 *     LogicalFilterComposer.getOrFilter( A, B),
 *     LogicalFilterComposer.getNotFilter(
 *         LogicalFilterComposer.getAndFilter( C, D) ) );
 * </pre>
 *
 * @author edavis
 * @since Jan 19, 2007 9:53:00 AM
 */
public class LogicalFilterComposer
{
//  private org.slf4j.Logger logger =
//          org.slf4j.LoggerFactory.getLogger( LogicalFilterComposer.class );

  public static CrawlableDatasetFilter getAndFilter( CrawlableDatasetFilter filter1,
                                                     CrawlableDatasetFilter filter2 )
  {
    return new AndFilter( filter1, filter2 );
  }

  public static CrawlableDatasetFilter getOrFilter( CrawlableDatasetFilter filter1,
                                                     CrawlableDatasetFilter filter2 )
  {
    return new OrFilter( filter1, filter2 );
  }

  public static CrawlableDatasetFilter getNotFilter( CrawlableDatasetFilter filter )
  {
    return new NotFilter( filter );
  }

  private static class AndFilter implements CrawlableDatasetFilter
  {
    private CrawlableDatasetFilter filter1;
    private CrawlableDatasetFilter filter2;

    AndFilter( CrawlableDatasetFilter filter1, CrawlableDatasetFilter filter2 )
    {
      this.filter1 = filter1;
      this.filter2 = filter2;
    }

    public boolean accept( CrawlableDataset dataset )
    {
      return filter1.accept( dataset) && filter2.accept( dataset);
    }

    public Object getConfigObject()
    {
      return null;
    }
  }

  private static class OrFilter implements CrawlableDatasetFilter
  {
    private CrawlableDatasetFilter filter1;
    private CrawlableDatasetFilter filter2;

    OrFilter( CrawlableDatasetFilter filter1, CrawlableDatasetFilter filter2 )
    {
      this.filter1 = filter1;
      this.filter2 = filter2;
    }

    public boolean accept( CrawlableDataset dataset )
    {
      return filter1.accept( dataset) || filter2.accept( dataset);
    }

    public Object getConfigObject()
    {
      return null;
    }
  }

  private static class NotFilter implements CrawlableDatasetFilter
  {
    private CrawlableDatasetFilter filter;

    NotFilter( CrawlableDatasetFilter filter1 )
    {
      this.filter = filter1;
    }

    public boolean accept( CrawlableDataset dataset )
    {
      return ! filter.accept( dataset);
    }

    public Object getConfigObject()
    {
      return null;
    }
  }

}
