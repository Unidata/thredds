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
// $Id: DatasetScanCatalogBuilder.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import org.jdom.Document;
import thredds.catalog.*;
import thredds.crawlabledataset.CrawlableDataset;

import java.io.IOException;

/*
 * Here's an example of the CrawlableDatasets that are involved in
 * satisfying a request:

 - A snippet from the catalog.xml config file (corresponds to http://my.server:8080/thredds/catalog.xml):

 <service name="myservice" type="OPENDAP" base="/thredds/dodsC/"/>
 <datasetScan name="NCEP data" ID="NCEP" path="ncep" location="/my/data/collection/model/ncep"
 serviceName="myservice"/>

 - And a request for a catalog:

 http://my.server:8080/thredds/ncep/nam/80km/catalog.xml

 - Collection ID: "ncep" (from datasetScan@path)
 - collectionLevel.getPath(): "/my/data/collection/model/ncep"
 (from datasetScan@location)
 - catalogLevel.getPath(): "/my/data/collection/model/ncep/nam/80km"
 (from request URL: "ncep" matches the collectionID so the
 part of the URL following the collection ID is appended onto the
 collectionLevel path)
 - currentLevel = null (let's ignore this for now)
 - childAtomicCrDs.getPath(): "/my/data/collection/model/ncep/nam/80km/20060208_1200_nam80km.grib"
 - childCollectionCrDs.getPath(): "/my/data/collection/model/ncep/nam/80km/2000archive"

 Here are the parts of the resulting datasets and catalogRef elements:

 1) The name of a dataset element (and the xlink:title of a catalogRef
 element) is the name of the corresponding CrawlableDataset. Example:

 <dataset name="20060208_1200_nam80km.grib"/>
 <catalogRef xlink:title="2000archive"/>

 the values were determined as follows:

 - name = childAtomicCrDs.getName()
 - xlink:title = childCollectionCrDs.getName()

 2) The ID of a catalog dataset element is the ID of the parent dataset
 and the name of the corresponding CrawlableDataset seperated by a "/".
 So, it ends up being the path of the CrawlableDataset from the point
 where the collection CrawlableDataset path ends then prefixed by the
 ID of the datasetScan element for that collection. Example:

 <dataset name="20060208_1200_nam80km.grib" ID="NCEP/nam/80km/20060208_1200_nam80km.grib"/>
 -- ID equals datasetScan@ID + childAtomicCrDs.getPath().substring( collectionLevel.getPath().length + 1)
 <catalogRef xlink:title="2000archive" ID="NCEP/nam/80km/2000archive" />
 -- ID equals datasetScan@ID + childCollectionCrDs.getPath().substring( collectionLevel.getPath().length + 1)

 3) The urlPath of a dataset element is the ID of the collection (i.e.,
 the path attribute of the datasetScan element) plus the part of the
 CrawlableDatasets path after the collection CrawlableDataset path.
 Example:

 <dataset name="20060208_1200_nam80km.grib" ID="NCEP/nam/80km/20060208_1200_nam80km.grib"
 urlPath="ncep/nam/80km/20060208_1200_nam80km.grib" />
 -- urlPath equals datasetScan@path + "/" + childAtomicCrDs.getPath().substring( collectionLevel.getPath().length + 1)

 4) The xlink:href of a catalogRef element is the path of the
 CrawlableDataset after removing the leading section that corresponds to
 the catalog level path plus "/catalog.xml". Example:

*/

/**
 * Build a catalog from one or more single level catalogs produced by
 * CollectionScanners.
 *
 * @author edavis
 * @since Aug 2, 2005T3:16:37 PM
 */
public class DatasetScanCatalogBuilder implements CatalogBuilder
{


  private CatalogBuilder stdCatBuilder;

  public DatasetScanCatalogBuilder( InvDatasetScan datasetScan, CrawlableDataset collectionCrDs, InvService service )
  {
    // Setup for ID
    String baseID = null;
    if ( datasetScan.getID() != null )
      baseID = datasetScan.getID();
    else if ( datasetScan.getPath() != null )
      baseID = datasetScan.getPath();

    stdCatBuilder = new StandardCatalogBuilder( datasetScan.getPath(),
                                                datasetScan.getName(),
                                                collectionCrDs,
                                                datasetScan.getFilter(), service,
                                                baseID, datasetScan.getIdentifier(),
                                                datasetScan.getNamer(), datasetScan.getAddDatasetSize(),
                                                datasetScan.getSorter(), datasetScan.getProxyDatasetHandlers(),
                                                datasetScan.getChildEnhancerList(), datasetScan,
                                                datasetScan.getCatalogRefExpander()
                                               );

  }

  public CrawlableDataset requestCrawlableDataset( String path ) throws IOException
  {
    return stdCatBuilder.requestCrawlableDataset( path);
  }

  public InvCatalogImpl generateCatalog( CrawlableDataset catalogCrDs )
          throws IOException
  {
    // Generate and return the catalog.
    return stdCatBuilder.generateCatalog( catalogCrDs );
  }

  public InvCatalogImpl generateProxyDsResolverCatalog( CrawlableDataset catalogCrDs, ProxyDatasetHandler pdh )
          throws IOException
  {
    // Generate the catalog
    //noinspection UnnecessaryLocalVariable
    InvCatalogImpl catalog = stdCatBuilder.generateProxyDsResolverCatalog( catalogCrDs, pdh );

    // Return the catalog.
    return catalog;
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
