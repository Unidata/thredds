/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;

/**
 * Interface for factories that wrap a NetcdfDataset with a subclass of TypedDataset
 *
 * @deprecated use ucar.nc2.ft.*
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
