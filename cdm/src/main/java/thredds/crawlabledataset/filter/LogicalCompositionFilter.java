package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * _more_
 *
 * @author edavis
 * @since Jan 19, 2007 9:53:00 AM
 */
public class LogicalCompositionFilter implements CrawlableDatasetFilter
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( LogicalCompositionFilter.class );

  private CrawlableDatasetFilter filter1;
  private CrawlableDatasetFilter filter2;

  // factory to determine and, or, not
  // 
  public LogicalCompositionFilter( CrawlableDatasetFilter filter1,
                                   CrawlableDatasetFilter filter2 )
  {
    if ( filter1 == null || filter2 == null )
      throw new IllegalArgumentException( "Null filter not allowed.");

    this.filter1 = filter1;
    this.filter2 = filter2;
  }

  public boolean accept( CrawlableDataset dataset )
  {
    return filter1.accept( dataset ) && filter2.accept( dataset );
  }

  public Object getConfigObject()
  {
    return null;
  }

  
}
