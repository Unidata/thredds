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
// $Id: SimpleCatalogBuilder.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.catalog.*;

import java.io.IOException;

import org.jdom.Document;

/**
 * Provide a simple interface for building single-level catalogs at any level
 * of the specified collection.
 *
 * @author edavis
 * @since Nov 30, 2005 3:17:23 PM
 */
public class SimpleCatalogBuilder implements CatalogBuilder
{


  private String collectionPath;

  private CrawlableDataset collectionCrDs;
  private InvService service;

  /**
   * Construct a SimpleCatalogBuilder for a given collection.
   *
   * @param collectionPath an path for the collection to be cataloged (used as the base of the generated URL, can be an empty string or null). If null, the collectionLevel final path segment is used.
   * @param collectionCrDs the root level of the collection to be cataloged.
   * @param serviceName specifies the name of the service to be used in the resulting catalogs.
   * @param serviceTypeName specifies the THREDDS service type, e.g., "OPENDAP"
   * @param serviceURL the base URL to the service
   */
  public SimpleCatalogBuilder( String collectionPath, CrawlableDataset collectionCrDs, String serviceName, String serviceTypeName, String serviceURL )
  {

    this.collectionPath = ( collectionPath == null
                            ? collectionCrDs.getName()
                            : collectionPath );
    this.collectionCrDs = collectionCrDs;

    service = new InvService( serviceName, serviceTypeName, serviceURL, null, null );
  }

  public CrawlableDataset requestCrawlableDataset( String path )
  {
    return CatalogBuilderHelper.verifyDescendantDataset( collectionCrDs, path, null );
  }

  public InvCatalogImpl generateCatalog( CrawlableDataset catalogCrDs ) throws IOException
  {
    CollectionLevelScanner scanner = new CollectionLevelScanner( collectionPath, collectionCrDs, catalogCrDs, null, null, service );

    scanner.scan();
    return scanner.generateCatalog();
  }

  /** Not supported by SimpleCatalogBuilder. */
  public InvCatalogImpl generateProxyDsResolverCatalog( CrawlableDataset catalogCrDs, ProxyDatasetHandler pdh ) throws IOException
  {
    throw new java.lang.UnsupportedOperationException( "This method not supported by SimpleCatalogBuilder.");
  }

  public Document generateCatalogAsDocument( CrawlableDataset catalogCrDs ) throws IOException
  {
    return CatalogBuilderHelper.convertCatalogToDocument( generateCatalog( catalogCrDs ) );
  }

  public String generateCatalogAsString( CrawlableDataset catalogCrDs ) throws IOException
  {
    return CatalogBuilderHelper.convertCatalogToString( generateCatalog( catalogCrDs ) );
  }
}
