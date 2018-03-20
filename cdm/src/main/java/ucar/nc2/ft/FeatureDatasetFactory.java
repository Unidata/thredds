/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
  Object isMine( FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException;

  /**
   * Open a NetcdfDataset as a FeatureDataset.
   * Should only be called if isMine() returns non-null.
   *
   * @param ftype open as this feature type. If null, open as any feature type.
   * @param ncd an already opened NetcdfDataset.
   * @param analysis the object returned from isMine(). Likely given to a different instance of FeatureDatasetFactory
   * @param task user may cancel, may be null
   * @param errlog write error messages here, may be null
   * @return a subclass of FeatureDataset
   * @throws java.io.IOException on error
   */
  FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException;

  /**
   * This Factory can open these types of Feature datasets.
   * @return array of FeatureType
   */
  FeatureType[] getFeatureTypes();
}
