/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
