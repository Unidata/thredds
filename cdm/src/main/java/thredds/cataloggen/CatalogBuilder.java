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
// $Id: CatalogBuilder.java 58 2006-07-12 20:21:43Z edavis $
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
   * Return the CrawlableDataset for the given path, null if this CatalogBuilder
   * does not allow the requested CrawlableDataset.
   *
   * <p>This method can handle requests for regular datasets and proxy datasets.
   *
   * @param path the path of the requested CrawlableDataset
   * @return the CrawlableDataset for the given path or null if the path is not allowed by this CatalogBuilder either due to filtering or due to the path not being a descendant (or self) of the collection level path.
   * @throws IOException if an I/O error occurs while locating the children datasets.
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
   * Find the actual dataset represented by the given proxy dataset path. Use
   * the appropriate ProxyDatasetHandler to determine which dataset is being
   * represented.
   *
   * @param path the proxy dataset path for which a CrawlableDataset is desired.
   * @return todo CrDs or InvCrawlablePair????
   * @throws IOException              if an I/O error occurs while determining the actual dataset.
   * @throws IllegalArgumentException if the given path has no corresponding proxy dataset handler or its parent dataset is not an allowed.
   * @throws NullPointerException     if the given path is null.
   */
  //public CrawlableDataset findActualProxyDataset( String path ) throws IOException;

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
