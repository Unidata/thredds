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
 * LogicalCompositionFilterFactory.getAndFilter(
 *     LocigalCompositionFilterFactory.getOrFilter( A, B),
 *     LocigalCompositionFilterFactory.getNotFilter(
 *         LocigalCompositionFilterFactory.getAndFilter( C, D) ) );
 * </pre>
 *
 * @author edavis
 * @since Jan 19, 2007 9:53:00 AM
 */
public class LogicalCompositionFilterFactory
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( LogicalCompositionFilterFactory.class );

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
