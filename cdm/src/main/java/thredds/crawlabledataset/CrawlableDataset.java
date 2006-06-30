// $Id: CrawlableDataset.java,v 1.5 2006/02/15 21:51:16 edavis Exp $
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
 *
 * The CrawlableDataset interface is a generalization (and simplification) of
 * the java.io.File class. A CrawlableDataset path is made up of ONE or more
 * path segments each seperated by a slash ("/"). The path may start with a
 * slash ("/") but may not end with a slash ("/").
 *
 * The CrawlableDatasetFile class stretches the definition of the
 * seperator character ("/") by allowing files to be given in their native
 * formats including Unix (/my/file), Windows (c:\my\file), and UNC file paths
 * (\\myhost\my\file).
 *
 * Implementation Notes:<br>
 * 1) The thredds.crawlabledataset.CrawlableDatasetFactory requires each
 * CrawlableDataset implementation to define a public constructor with one
 * String argument and one Object argument. The String argument is the path
 * for the CrawlableDataset being constructed, the Object argument is a
 * configuration object.
 * <br>
 * 2) The thredds.cataloggen.CollectionLevelScanner framework does not support
 * building a catalog for the collection based at the CrawlableDataset path "/"
 * (this is related to the assumption, specified above, that paths do not end
 * with a "/"). So, do not implement your CrawlableDataset so that the path "/"
 * is allowed, or at least so that it is not likely to be used as the root of
 * a dataset collection (e.g., CrawlableDatasetFile is probably safe because
 * the root directory ("/") should never be used as the base of a data
 * collection. If the backend data source on top of which you are implementing
 * CrawlabeDataset uses "/" as its root, you can simply prepend a string (e.g.,
 * "myDataCollection" or "root") to the backend path for the CrawlableDataset
 * view of the path.  
 *
 * @author edavis
 * @since May 3, 2005 20:18:59 -0600
 *
 * @see thredds.cataloggen.CollectionLevelScanner uses CrawlableDatasets to scan a dataset collection and create a THREDDS catalog.
 */
public interface CrawlableDataset
{
  /** Return the configuration Object (can be null). */
  public Object getConfigObject();

  /** Returns the dataset path. */
  public String getPath();

  /** Returns the dataset name, i.e., the last part of the dataset path. */
  public String getName();

  /** Returns the parent CrawlableDataset or null if this dataset has no parent. */
  public CrawlableDataset getParentDataset() throws IOException;

  /** Return true if the dataset is a collection dataset. */
  public boolean isCollection();

  /**
   * Returns the list of CrawlableDatasets contained in this collection dataset.
   * The returned list will be empty if this collection dataset does not contain
   * any children datasets. If this dataset is not a collection dataset, this
   * method returns null.
   *
   * @return Returns a list of the CrawlableDatasets contained in this collection dataset.
   *         The llist will be empty if no datasets are contained in this collection dataset.
   * @throws IOException if an I/O error occurs while accessing the children datasets.
   * @throws IllegalStateException if this dataset is not a collection, the isCollection() method should be used to check.
   */
  public List listDatasets() throws IOException;

  /**
   * Returns the list of CrawlableDatasets contained in this collection dataset
   * that satisfy the given filter. The returned list will be empty if this
   * collection dataset does not contain any children datasets that satisfy the
   * given filter. If this dataset is not a collection dataset, this
   * method returns null.
   *
   * @param filter a CrawlableDataset filter (if null, accept all datasets).
   * @return Returns a list of the CrawlableDatasets contained in this collection dataset
   *         that satisfy the given filter. The list will be empty if no datasets are
   *         contained in this collection dataset.
   * @throws IOException if an I/O error occurs while accessing the children datasets.
   * @throws IllegalStateException if this dataset is not a collection, the isCollection() method should be used to check.
   */
  public List listDatasets( CrawlableDatasetFilter filter ) throws IOException;

  /** Returns the size in bytes of the dataset, -1 if unknown. */
  public long length();
  /** Returns the date the dataset was last modified, null if unknown. */
  public Date lastModified(); // or long milliseconds?
}

/*
 * $Log: CrawlableDataset.java,v $
 * Revision 1.5  2006/02/15 21:51:16  edavis
 * For clarity, change CollectionLevelScanner.name to CollectionLevelScanner.collectionID.
 * (Not sure if that is better, collides with dataset@ID.) Also, improve javadocs.
 *
 * Revision 1.4  2006/01/23 18:51:06  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.3  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.2  2005/11/18 23:51:04  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:48  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.4  2005/08/22 17:40:23  edavis
 * Another round on CrawlableDataset: make CrawlableDatasetAlias a subclass
 * of CrawlableDataset; start generating catalogs (still not using in
 * InvDatasetScan or CatalogGen, yet).
 *
 * Revision 1.3  2005/07/13 22:54:22  edavis
 * Fix CrawlableDatasetAlias.
 *
 * Revision 1.2  2005/06/29 22:04:05  edavis
 * Remove the correspondingInvDataset() method. It should be in a
 * factory method perhaps in CollectionLevelScanner class.
 *
 * Revision 1.1  2005/06/24 22:08:32  edavis
 * Second stab at the CrawlableDataset interface.
 *
 */