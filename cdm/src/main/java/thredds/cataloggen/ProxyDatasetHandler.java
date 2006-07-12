// $Id$
package thredds.cataloggen;

import thredds.catalog.InvService;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.util.List;

/**
 * The ProxyDatasetHandler interface allows implementors to define a proxy
 * CrawlableDataset, how its corresponding InvDataset should be added to
 * collection InvDatasets, and how the proxy CrawlableDataset maps to the
 * underlying concrete CrawlableDataset.
 *
 * This interface is used by both thredds.cataloggen.CollectionLevelScanner
 * and thredds.catalog.InvDatasetScan. In CollectionLevelScanner, it is used
 * to add proxy datasets to the InvCatalog being generated. In InvDatasetScan,
 * it is used to map (proxy) dataset requests to the underlying
 * CrawlableDataset.
 *
 * @author edavis
 * @since Nov 29, 2005 8:42:37 AM
 */
public interface ProxyDatasetHandler
{
  /**
   * Return the name of the proxy dataset.
   *
   * @return the name of the proxy dataset.
   */
  public String getProxyDatasetName();

  /**
   * Create a new dataset to add to the parent collection dataset.
   *
   * @param parent the collection dataset in which to add the dataset being created.
   * @return A new CrawlableDataset to be added to the parent dataset (in the InvDataset arena).
   */
  public CrawlableDataset createProxyDataset( CrawlableDataset parent );

  /**
   * Return the InvService to be used by the InvDataset that corresponds to the created dataset.
   *
   * @param parent the collection dataset in which to add the dataset being created.
   * @return the InvService used by the InvDataset that corresponds to the created dataset.
   */
  public InvService getProxyDatasetService( CrawlableDataset parent );

  /**
   * Return an integer which indicates the location/index at which
   * the new dataset should be added to the parent collection dataset.
   *
   * @param parent the collection dataset in which to add the dataset being created.
   * @param collectionDatasetSize the number of datasets currentlyin the parent collection dataset.
   * @return The location at which the new dataset is to be added to the parent collection dataset.
   */
  public int getProxyDatasetLocation( CrawlableDataset parent, int collectionDatasetSize );

  public boolean isProxyDatasetResolver();

  public InvCrawlablePair getActualDataset( List possibleDatasets );
  
  public String getActualDatasetName( InvCrawlablePair actualDataset, String baseName );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();
}
/*
 * $Log: ProxyDatasetHandler.java,v $
 * Revision 1.1  2006/05/19 19:23:04  edavis
 * Convert DatasetInserter to ProxyDatasetHandler and allow for a list of them (rather than one) in
 * CatalogBuilders and CollectionLevelScanner. Clean up division between use of url paths (req.getPathInfo())
 * and translated (CrawlableDataset) paths.
 *
 * Revision 1.2  2005/12/30 00:18:53  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/12/16 23:19:35  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Move from thredds.crawlabledataset.CrawlableDatasetInserter
 * to thredds.cataloggen.ProxyDatasetHandler.
 *
 * Revision 1.1  2005/12/01 00:15:02  edavis
 * More work on move to using CrawlableDataset.
 *
 */