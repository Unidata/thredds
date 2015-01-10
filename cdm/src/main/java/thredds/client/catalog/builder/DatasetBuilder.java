/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog.builder;

import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.Metadata;
import ucar.nc2.constants.FeatureType;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/8/2015
 */
public class DatasetBuilder {

  DatasetBuilder parent;
  String authority;
  String name;
  String collectionType;
  FeatureType featureType;
  Boolean harvest;
  String id;
  String serviceName;
  String urlPath;
  List<Metadata> metadata;
  List<AccessBuilder> accessBuilders;
  List<DatasetBuilder> datasetBuilders;

  public DatasetBuilder(DatasetBuilder parent) {
    this.parent = parent;
  }

  public void setAuthority(String authority) {
    this.authority = authority;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCollectionType(String collectionType) {
    this.collectionType = collectionType;
  }

  public void setFeatureType(FeatureType featureType) {
    this.featureType = featureType;
  }

  public void setHarvest(Boolean harvest) {
    this.harvest = harvest;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public void setUrlPath(String urlPath) {
    this.urlPath = urlPath;
  }

  public void addDataset(DatasetBuilder d) {
    if (d == null) return;
    if (datasetBuilders == null) datasetBuilders = new ArrayList<>();
    datasetBuilders.add(d);
  }


  public void addAccess(AccessBuilder d) {
    if (accessBuilders == null) accessBuilders = new ArrayList<>();
    accessBuilders.add(d);
  }


  public void addMetadata(Metadata d) {
    if (metadata == null) metadata = new ArrayList<>();
    metadata.add(d);
  }

  public Dataset makeDataset(DatasetNode parent) {
    return new Dataset(parent, name, collectionType, harvest, id, urlPath, metadata, accessBuilders, datasetBuilders);
  }

}
