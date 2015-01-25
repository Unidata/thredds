/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.catalog;

import thredds.catalog.InvCatalogImpl;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dt.GridDataset;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Description
 *
 * @author John
 * @since 1/17/2015
 */
public class FeatureCollection extends CatalogRef {
  FeatureCollectionConfig config;
  String path;
  String topDirectoryLocation;
  String collectionName;

  public FeatureCollection(DatasetNode parent, String name, String xlink, Map<String, Object> flds, List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders,
                           FeatureCollectionConfig config) {
    super(parent, name, xlink, flds, accessBuilders, datasetBuilders);
    this.config = config;
  }

  public String getPath() {
    return path;
  }

  public String getTopDirectoryLocation() {
    return topDirectoryLocation;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public NetcdfFile getNetcdfDataset(String remaining) {
    return null;
  }

  public GridDataset getGridDataset(String remaining) {
    return null;
  }

  public Catalog makeCatalog(String match, String orgPath, URI catURI) throws IOException {
    return null;
  }

  public Catalog makeLatest(String matchPath, String reqPath, URI catURI) throws IOException {
    return null;
  }

  public MFile getFileFromRequestPath(String path) {
    return null;
  }


}
