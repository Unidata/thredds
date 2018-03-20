/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
