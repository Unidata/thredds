// $Id:Enhancements.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.ma2.DataType;

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
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public interface Enhancements {

 /** Get the description of the Variable, or null if none. */
  public String getDescription();

  /** Get the Unit String for the Variable, or null if none. */
  public String getUnitsString();

  /**
   * Get the list of Coordinate Systems for this Variable.
   * @return list of type CoordinateSystem; may be empty but not null.
   */
  public java.util.List getCoordinateSystems();

  /** Add a CoordinateSystem to the dataset. */
  public void addCoordinateSystem( CoordinateSystem cs);

  /** Remove a CoordinateSystem from the dataset. */
  public void removeCoordinateSystem( CoordinateSystem cs);

  /** Get the original variable */
  public Variable getOriginalVariable();

}
