/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

/**
 * A Variable decorator that handles Coordinates Systems and "standard attributes" and adds them to the object model.
 * Specifically, this:
 * <ul>
 * <li> adds a list of <b>CoordinateSystem</b>.
 * <li> adds <b>unitString</b> from the standard attribute <i>units</i>
 * <li> adds <b>description</b> from the standard attributes <i> long_name, description or title</i>
 * </ul>
 * if those "standard attributes" are present.
 *
 * @author caron
 */

public  interface Enhancements {

 /** Get the description of the Variable, or null if none.
  * @return description of the Variable, or null
  */
  String getDescription();

  /** Get the Unit String for the Variable, or null if none.
   * @return Unit String for the Variable, or null
   */
  String getUnitsString();

  /**
   * Get the list of Coordinate Systems for this Variable.
   * @return list of type CoordinateSystem; may be empty but not null.
   */
  java.util.List<CoordinateSystem> getCoordinateSystems();

  /** Add a CoordinateSystem to the dataset.
   * @param cs add this Coordinate System
   */
  void addCoordinateSystem( CoordinateSystem cs);

  /** Remove a CoordinateSystem from the dataset.
   * @param cs remove this coordinate system
   */
  void removeCoordinateSystem( CoordinateSystem cs);

}
