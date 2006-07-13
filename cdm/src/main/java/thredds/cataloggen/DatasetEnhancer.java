// $Id: DatasetEnhancer.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.catalog.InvDataset;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * An interface to allow for enhancing of InvDatasets.
 *
 * @author edavis
 * @since Dec 1, 2005 9:29:18 PM
 */
public interface DatasetEnhancer
{
  /**
   * Add metadata to the given InvDataset possibly using information from the
   * corresponding CrawlableDataset.
   *
   * @param dataset the InvDataset to enhance with additional metadata.
   * @param crDataset the CrawlableDataset that corresponds with the given InvDataset, possibly used to gather information not available from the InvDataset.
   * @return true if the InvDataset is successfully enhanced, otherwise false.
   */
  public boolean addMetadata( InvDataset dataset, CrawlableDataset crDataset );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();
}
