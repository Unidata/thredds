/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.dt;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;

/**
 * Interface for factories that wrap a NetcdfDataset with a subclass of TypedDataset
 *
 * @author caron
 */
public interface TypedDatasetFactoryIF {

  /** Determine if this dataset belongs to you
   * @param ncd examine this NetcdfDataset to see if it belongs to this class.
   * @return true if this class knows how to create a TypedDataset out of this NetcdfDataset.
   */
  public boolean isMine( NetcdfDataset ncd);

  /**
   * Open a NetcdfDataset as a TypedDataset.
   *
   * @param ncd already opened NetcdfDataset.
   * @param task use may cancel
   * @param errlog place errors here
   * @return a subclass of TypedDataset
   * @throws java.io.IOException on error
   */
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException;

  /**
   * What kind of cientific data type will this return?
   * @return scientific data type
   */
  public FeatureType getScientificDataType();

}
