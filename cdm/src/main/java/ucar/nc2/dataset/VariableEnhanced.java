/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.Set;

/**
 * interface to an "enhanced Variable", implemented by the ucar.nc2.dataset package.
 * @author john caron
 */

public interface VariableEnhanced extends ucar.nc2.VariableIF, Enhancements {

  ucar.nc2.Variable getOriginalVariable();
  void setOriginalVariable(ucar.nc2.Variable orgVar);
  String getOriginalName();

  /** Set the Unit String for this Variable. Default is to use the CDM.UNITS attribute.
   * @param units  unit string
   */
  void setUnitsString( String units);
  
  /**
   * Enhance using the given set of NetcdfDataset.Enhance
   */
  void enhance(Set<NetcdfDataset.Enhance> mode);

  /**
   *  clear previous coordinate systems. if any
   */
  void clearCoordinateSystems();  
}
