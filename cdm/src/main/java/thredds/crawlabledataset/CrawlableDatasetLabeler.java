// $Id: CrawlableDatasetLabeler.java,v 1.2 2005/12/30 00:18:54 edavis Exp $
package thredds.crawlabledataset;

/**
 * An interface for determining a String label for a CrawlableDataset.
 *
 * @author edavis
 * @since Nov 14, 2005 10:49:12 AM
 */
public interface CrawlableDatasetLabeler
{
  /**
   * Return a label for the given dataset or null if this CrawlableDatasetLabeler
   * does not know how to label the given dataset.
   *
   * @param dataset the CrawlableDataset to be labeled.
   * @return the label for the given dataset or null.
   * @throws NullPointerException if the given dataset is null.
   */
  public String getLabel( CrawlableDataset dataset );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();

}
/*
 * $Log: CrawlableDatasetLabeler.java,v $
 * Revision 1.2  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/18 23:51:04  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 *
 */