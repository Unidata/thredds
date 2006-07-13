// $Id: CrawlableDatasetSorter.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

import java.util.List;

/**
 * The CrawlableDatasetSorter interface provides for sorting a list of
 * CrawlableDatasets. An instance of the CrawlableDatasetSorter interface
 * defines an ordering for a list of CrawlableDatasets.
 *
 * The CrawlableDatasetSorter interface is used by the CollectionLevelScanner
 * class to sort the datasets it is cataloging.
 *
 * @author edavis
 * @since Nov 18, 2005 4:12:50 PM
 */
public interface CrawlableDatasetSorter
{
  /**
   * Sort the given CrawlableDataset list into the order defined by this sorter.
   *
   * @param datasetList the CrawlableDataset list to be sorted.
   * 
   * @throws ClassCastException if the list contains elements that are not CrawlableDatasets.
   * @throws UnsupportedOperationException if the given list does not allow the necessary list manipulation.
   */
  public void sort( List datasetList );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();
}
