// $Id$
package thredds.crawlabledataset;

/**
 * A filter for CrawlableDatasets.
 *
 * <p>Instances of this interface may be passed to the
 * <code>{@link CrawlableDataset#listDatasets(CrawlableDatasetFilter)}</code>
 * method of the <code>{@link CrawlableDataset}</code> class.</p>
 *
 * Implementation note:
 * The TDS framework (InvDatasetScan, etc) uses a public constructor
 * with a single configuration Object argument to instantiate instances
 * of a CrawlableDatasetFilter. If your implementation will not be used
 * in the TDS framework, other constructors can be used.
 *
 * @author edavis
 * @since Jun 22, 2005 9:30:43 AM
 * @see CrawlableDataset#listDatasets(CrawlableDatasetFilter)
 */
public interface CrawlableDatasetFilter
{
  /**
   * Test whether the specified CrawlableDataset should be included
   * in a list of CrawlableDatasets.
   *
   * @param dataset the CrawlableDataset to test for inclusion.
   * @return true if the given CrawlableDataset should be included, false otherwise.
   */
  public boolean accept( CrawlableDataset dataset);

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();
}

/*
 * $Log: CrawlableDatasetFilter.java,v $
 * Revision 1.2  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/15 18:40:49  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/06/24 22:08:32  edavis
 * Second stab at the CrawlableDataset interface.
 *
 */