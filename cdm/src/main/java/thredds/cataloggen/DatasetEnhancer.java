// $Id: DatasetEnhancer.java,v 1.8 2005/12/30 00:18:53 edavis Exp $
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
/*
 * $Log: DatasetEnhancer.java,v $
 * Revision 1.8  2005/12/30 00:18:53  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.7  2005/12/06 19:39:20  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 */