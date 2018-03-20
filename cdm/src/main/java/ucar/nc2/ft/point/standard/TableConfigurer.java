/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.util.Formatter;

/**
 * To analyze specific datasets, implement a TableConfigurer, whose job is to
 *   create a TableConfig, used by TableAnalyzer.
 * @author caron
 * @since Apr 23, 2008
 * @see TableAnalyzer for plugins
 */
public interface TableConfigurer {
  /**
   * Determine if this is a dataset that can be opened as a point obs dataset.
   * @param wantFeatureType want this FeatureType
   * @param ds for this dataset
   * @return true if it can be opened as a wantFeatureType dataset
   * @throws IOException on read error
   */
  boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) throws IOException;

  /**
   * Create a TableConfig for this dataset.
   * @param wantFeatureType want this FeatureType
   * @param ds for this dataset, which has already passed isMine() test
   * @param errlog put error messages here, may be null.
   * @return TableConfig for this dataset
   * @throws IOException on read error
   */
  TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException;

  String getConvName();
  void setConvName(String convName);
  String getConvUsed();
  void setConvUsed(String convUsed);
}
