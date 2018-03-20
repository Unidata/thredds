/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.crawlabledataset;

import java.util.Date;
import java.util.List;
import java.io.IOException;

/*
 * [Once support alias paths, these sentences or something like them should
 *  start the third paragraph below.]
 *
 * Two standard implementations affect what characters are allowed in a
 * CrawlableDataset path.The CrawlableDatasetAlias class uses wildcard
 * characters ("?", "*", and "**") to define a collection of CrawlableDatasets.
 * Therefore, the wildcard characters can not be used in a CrawlableDataset
 * path.
*/

/**
 * CrawlableDataset represents an abstract dataset that is part of a
 * hierarchical dataset collection. Parent and child datasets can be accessed
 * allowing the collection to be crawled.
 * <p/>
 * <p> The CrawlableDataset interface is a generalization (and simplification) of
 * the java.io.File class. A CrawlableDataset path is made up of ONE or more
 * path segments each seperated by a slash ("/"). The path may start or end with
 * a slash ("/").</p>
 * <p/>
 * <p>Implementation Notes:</p>
 * <ol>
 * <li> The thredds.crawlabledataset.CrawlableDatasetFactory requires each
 * CrawlableDataset implementation to define a public constructor with one
 * String argument and one Object argument. The String argument is the path
 * for the CrawlableDataset being constructed, the Object argument is a
 * configuration object.</li>
 * <li> The thredds.cataloggen.CollectionLevelScanner framework does not support
 * building a catalog for the collection based at the CrawlableDataset path "/".
 * So, do not implement your CrawlableDataset so that the path "/"
 * is allowed, or at least so that it is not likely to be used as the root of
 * a dataset collection (e.g., CrawlableDatasetFile is probably safe because
 * the root directory ("/") should never be used as the base of a data
 * collection. If the backend data source on top of which you are implementing
 * CrawlabeDataset uses "/" as its root, you can simply prepend a string (e.g.,
 * "myDataCollection" or "root") to the backend path for the CrawlableDataset
 * view of the path.</li>
 * </ol>
 *
 * @author edavis
 * @see thredds.cataloggen.CollectionLevelScanner CollectionLevelScanner uses CrawlableDatasets to scan a dataset collection and create a THREDDS catalog.
 * @since May 3, 2005 20:18:59 -0600
 */
public interface CrawlableDataset {
  /**
   * Return the configuration Object (can be null).
   * @return the configuration Object (can be null).
   */
  public Object getConfigObject();

  /**
   * Set the configuration Object.
   * @param config the config object.
   */
  // public void setConfigObject(Object config);

  /**
   * Returns the dataset path.
   * @return the dataset path.
   */
  public String getPath();

  /**
   * Returns the dataset name, i.e., the last part of the dataset path.
   * @return the dataset name, i.e., the last part of the dataset path.
   */
  public String getName();

  /**
   * Returns the parent CrawlableDataset or null if this dataset has no parent.
   * @return the parent CrawlableDataset or null if this dataset has no parent.
   */
  public CrawlableDataset getParentDataset();

  /**
   * Return true if the dataset represented by this CrawlableDataset actually
   * exists, null if it does not or an I/O error occurs.
   *
   * @return true if the dataset represented by this CrawlableDataset actually exists.
   */
  public boolean exists();

  /**
   * Return true if the dataset is a collection dataset.
   * @return true if the dataset is a collection dataset.
   */
  public boolean isCollection();

  /**
   * Return the requested descendant of this dataset.
   *
   * @param relativePath the path, relative to this dataset, of the requested dataset.
   * @return the requested descendant of this dataset.
   * @throws IllegalArgumentException if the relative path is not relative (e.g., starts with a slash ("/")).
   * @throws IllegalStateException if this dataset is not a collection, the isCollection() method should be used to check.
   */
  public CrawlableDataset getDescendant(String relativePath);

  /**
   * Returns the list of CrawlableDatasets contained in this collection dataset.
   * The returned list will be empty if this collection dataset does not contain
   * any children datasets. If this dataset is not a collection dataset, this
   * method returns null.
   *
   * @return Returns a list of the CrawlableDatasets contained in this collection dataset.
   *         The llist will be empty if no datasets are contained in this collection dataset.
   * @throws IOException           if an I/O error occurs while accessing the children datasets.
   * @throws IllegalStateException if this dataset is not a collection, the isCollection() method should be used to check.
   */
  public List<CrawlableDataset> listDatasets() throws IOException;

  /**
   * Returns the list of CrawlableDatasets contained in this collection dataset
   * that satisfy the given filter. The returned list will be empty if this
   * collection dataset does not contain any children datasets that satisfy the
   * given filter.
   *
   * @param filter a CrawlableDataset filter (if null, accept all datasets).
   * @return Returns a list of the CrawlableDatasets contained in this collection dataset
   *         that satisfy the given filter. The list will be empty if no datasets are
   *         contained in this collection dataset.
   * @throws IOException           if an I/O error occurs while accessing the children datasets.
   * @throws IllegalStateException if this dataset is not a collection, the isCollection() method should be used to check.
   */
  public List<CrawlableDataset> listDatasets(CrawlableDatasetFilter filter) throws IOException;

  /**
   * Returns the size in bytes of the dataset, -1 if unknown.
   * @return the size in bytes of the dataset, -1 if unknown.
   */
  public long length();

  /**
   * Returns the date the dataset was last modified, null if unknown.
   * @return the date the dataset was last modified, null if unknown.
   */
  public Date lastModified(); // or long milliseconds?
}
