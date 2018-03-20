/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: ProxyDatasetHandler.java 63 2006-07-12 21:50:51Z edavis $
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
 * NOTE: todo Would this be better named AliasDatasetHandler???
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

  /**
   * Determine the InvCrawlablePair from the given dataset collection
   * (java.util.List) being proxied by this ProxyDatasetHandler.
   *
   * @param possibleDatasets a list of InvCrawlablePair objects representing a dataset collection.
   * @return the InvCrawlablePair being proxied by this proxy dataset
   */
  public InvCrawlablePair getActualDataset( List possibleDatasets );
  
  public String getActualDatasetName( InvCrawlablePair actualDataset, String baseName );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();
}
