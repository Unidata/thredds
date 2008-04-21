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

package ucar.nc2.dt2;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;

/**
 * Interface for factories that wrap a NetcdfDataset with a FeatureDataset.
 * Class must have a no-arg Constructor.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface FeatureDatasetFactory {

  /** Determine if the factory can open this dataset as an instance of the given feature type
   * @param ftype can open as this feature type? If null, can open as any feature type?
   * @param ncd examine this NetcdfDataset.
   * @return true if this class knows how to create a FeatureDataset out of this NetcdfDataset.
   */
  public boolean isMine( FeatureType ftype, NetcdfDataset ncd) throws IOException;

  // since isMine can be expensive, make copy instead of reanalyze
  public FeatureDatasetFactory copy();

  /**
   * Open a NetcdfDataset as a FeatureDataset.
   * Should only be called if isMine() return true.
   *
   * @param ftype open as this feature type. If null, open as any feature type.
   * @param ncd an already opened NetcdfDataset.
   * @param task use may cancel
   * @param errlog place errors here
   * @return a subclass of FeatureDataset
   * @throws java.io.IOException on error
   */
  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException;

}
