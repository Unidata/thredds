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
