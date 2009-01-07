/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.util.Formatter;

/**
 * To analyze specific datasets, implement a TableConfigurer, whose job is to
 *   create TableConfig, used by TableAnalyzer.
 * @author caron
 * @since Apr 23, 2008
 */
public interface TableConfigurer {
  /**
   * Determine if this is a dataset that can be opened as a point obs dataset.
   * @param wantFeatureType want this FeatureType
   * @param ds for this dataset
   * @return true if it can be opened as a wantFeatureType dataset
   * @throws IOException on read error
   */
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) throws IOException;

  /**
   * Create a TableConfig for this dataset.
   * @param wantFeatureType want this FeatureType
   * @param ds for this dataset, which has already passed isMine() test
   * @param errlog put error messages here, may be null.
   * @return TableConfig for this dataset
   * @throws IOException on read error
   */
  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException;
}
