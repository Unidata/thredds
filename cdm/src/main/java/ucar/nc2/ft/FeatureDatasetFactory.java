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

package ucar.nc2.ft;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.util.Formatter;

/**
 * Interface for factories that wrap a NetcdfDataset with a FeatureDataset.
 * Class must have a no-arg Constructor.
 * Implementations must be thread-safe
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface FeatureDatasetFactory {

  /**
   * Determine if the factory can open this dataset as an instance of the given feature type
   * @param wantFeatureType can factory open as this feature type? If null, can factory open as any feature type?
   * @param ncd examine this NetcdfDataset.
   * @param errlog place errors here
   * @return "analysis object" - null if cannot open, else an Object that is passed back into FeatureDatasetFactory.open().
   *   This allows expensive analysis results to be reused
   * @throws java.io.IOException on read error
   */
  public Object isMine( FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException;

  /**
   * Open a NetcdfDataset as a FeatureDataset.
   * Should only be called if isMine() return non-null.
   *
   * @param ftype open as this feature type. If null, open as any feature type.
   * @param ncd an already opened NetcdfDataset.
   * @param analysis the object returned from isMine(). This will posobbly be a different instance of FeatureDatasetFactory
   * @param task user may cancel
   * @param errlog place errors here
   * @return a subclass of FeatureDataset
   * @throws java.io.IOException on error
   */
  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException;

}
