/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.partition.PartitionManager;

import java.io.File;

/**
 * Builds Grib2 PartitionCollections (version 2)
 *
 * @author John
 * @since 12/7/13
 */
class Grib2PartitionBuilder extends GribPartitionBuilder {
  public static final String MAGIC_START = "Grib2Partition2Index";  // was Grib2Partition0Index

  Grib2PartitionBuilder(String name, File directory, PartitionManager tpc, org.slf4j.Logger logger) {
    super(name, tpc, logger);

    FeatureCollectionConfig config = null;
    if (tpc != null)
      config = (FeatureCollectionConfig) tpc.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    this.result = new PartitionCollectionMutable(name, directory, config, false, logger);
  }

  //////////////////////////////////////////////////////////

  @Override
  public String getMagicStart() {
    return MAGIC_START;
  }

  protected int getVersion() {
    return Grib2CollectionWriter.version;
  }

}
