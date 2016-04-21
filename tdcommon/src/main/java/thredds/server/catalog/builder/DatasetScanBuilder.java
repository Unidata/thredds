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
package thredds.server.catalog.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.Property;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.DatasetScanConfig;

/**
 * Builder of DatasetScan
 *
 * @author caron
 * @since 1/15/2015
 */
public class DatasetScanBuilder extends DatasetBuilder {
  static private final Logger logger = LoggerFactory.getLogger(DatasetScanBuilder.class);

  DatasetScanConfig config;

  public DatasetScanBuilder(DatasetBuilder parent, DatasetScanConfig config) {
    super(parent);
    this.config = config;
  }

  public DatasetScan makeDataset(DatasetNode parent) {
    addToList(Dataset.Properties, new Property("DatasetScan", "true"));

    String xlink = "/thredds/catalog/"+config.path+"/catalog.xml";   // LOOK hardcoded thredds, need context, or make it reletive ??
    DatasetScan dscan = new DatasetScan(parent, name, xlink, flds, accessBuilders, datasetBuilders, config);

    if (null == dscan.getServiceNameDefault() && null == dscan.getFeatureType())
      logger.error("DatasetScan "+name+" does not have a default serviceName or dataType/featureType");

    return dscan;
  }
}
