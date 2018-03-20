/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.util.List;

/**
 * A FeatureDataset, composed of one or more DsgFeatureCollections.
 *
 * @author caron
 */
public interface FeatureDatasetPoint extends FeatureDataset {
  /**
   * Get a list of DsgFeatureCollection contained in this dataset.
   * These will all be of type PointFeatureCollection, PointFeatureCC, or PointFeatureCCC.
   * @return list of DsgFeatureCollection contained in this dataset
   */
  List<DsgFeatureCollection> getPointFeatureCollectionList();

  void calcBounds(java.util.Formatter sf);
}
