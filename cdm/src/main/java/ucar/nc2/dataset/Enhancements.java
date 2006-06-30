// $Id: Enhancements.java,v 1.4 2005/11/07 20:46:08 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * @version $Revision: 1.4 $ $Date: 2005/11/07 20:46:08 $
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

  /** Get the original variable */
  public Variable getOriginalVariable();

}

/* Change History:
   $Log: Enhancements.java,v $
   Revision 1.4  2005/11/07 20:46:08  caron
   *** empty log message ***

   Revision 1.3  2005/11/07 16:41:18  caron
   NcML Aggregation
   new projections

   Revision 1.2  2005/02/22 22:12:14  caron
   *** empty log message ***

   Revision 1.1  2004/08/16 20:53:47  caron
   2.2 alpha (2)

  */
