// $Id$
package thredds.cataloggen;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;
import thredds.crawlabledataset.CrawlableDataset;

import java.io.IOException;
import java.util.List;

import org.jdom.Document;

/**
 * An interface for building catalogs where each instance only builds
 * catalogs for the dataset collection root it was setup to handle.
 *
 *
 * @author edavis
 * @since Dec 6, 2005 12:09:36 PM
 */
public interface CatalogBuilder
{
  /**
   * Return the CrawlableDataset for the given path, null if this CatalogBuilder does not allow the requested CrawlableDataset.
   *
   * @param path the path of the requested CrawlableDataset
   * @return the CrawlableDataset for the given path or null if the path is not allowed by this CatalogBuilder.
   * @throws IOException if an I/O error occurs trying to access the CrawlableDataset.
   * @throws IllegalStateException if the given path is not a descendent of (or the same as) this CatalogBuilders collection level.
   */
  public CrawlableDataset requestCrawlableDataset( String path )
          throws IOException;

  /**
   * Return an InvCatalog for the level in the collection hierarchy specified by catalogPath.
   *
   * @param catalogCrDs the location in the collection at which to generate a catalog
   * @return an InvCatalog for the specified location
   * @throws IOException if problems accessing the dataset collection.
   */
  public InvCatalogImpl generateCatalog( CrawlableDataset catalogCrDs ) throws IOException;

  /**
   * Generate the catalog for a resolver request of the given ProxyDatasetHandler.
   *
   * This method is optional, it does not need to be supported by all
   * CatalogBuilder implementations.
   *
   * @param catalogCrDs the location in the collection at which to generate a catalog
   * @param pdh the ProxyDatasetHandler corresponding to the resolver request.
   * @return the catalog for a resolver request of the given proxy dataset.
   * @throws IllegalArgumentException if the given ProxyDatasetHandler is not known by this CollectionLevelScanner.
   */
  public InvCatalogImpl generateProxyDsResolverCatalog( CrawlableDataset catalogCrDs, ProxyDatasetHandler pdh )
          throws IOException;

  /**
   * Return a JDOM Document representation of the catalog for the level in
   * the collection hierarchy specified by catalogPath.
   *
   * @param catalogCrDs the location in the collection at which to generate a catalog
   * @return an org.jdom.Document representing the catalog for the specified location
   * @throws IOException if problems accessing the dataset collection.
   */
  public Document generateCatalogAsDocument( CrawlableDataset catalogCrDs ) throws IOException;

  /**
   * Return a String containing the XML representation of the catalog for the
   * level in the collection hierarchy specified by catalogPath.
   *
   * @param catalogCrDs the location in the collection at which to generate a catalog
   * @return a String containing the XML representation of the catalog for the specified location
   * @throws IOException if problems accessing the dataset collection.
   */
  public String generateCatalogAsString( CrawlableDataset catalogCrDs ) throws IOException;

}
