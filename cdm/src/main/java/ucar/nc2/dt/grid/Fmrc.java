// $Id$
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

package ucar.nc2.dt.grid;

import ucar.ma2.Array;
import ucar.nc2.dt.GridDatatype;

import java.util.List;
import java.util.Date;

/**
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public interface Fmrc {

  // list of Date
  List getRunTimes();

  // list of GridCollection
  List getGrids();

  // list of Date. Note that not every Grid may have all valid Times.
  List getValidTimes();

  // or return CoordinateVariable1D ??
  double[] getValidOffset();
  String getOffsetUnitString();

  public interface GridCollection {

    /**
     * True if list of valid times depend on the runTime.
     * Otherwise, you can use Fmrc.getValidTimes()
     */
    boolean hasUniformValidTimes();

    /**
     * The list of valid times may depend on the runTime - check hasUniformValidTimes()
     * @param runTime
     * @return list of Date
     */
    List getValidTimes( Date runTime);
    double[] getValidOffset(Date runTime);

    // or return CoordinateVariable1D ??
    boolean hasVertCoord();
    double[] getVertCoord(Date runTime, Date validTime);
    double[] getVertCoord(Date runTime, double offset);
    String getVertUnitString();

    GridDatatype getGeoGrid(Date runTime, double offset);

    Array readData(Date runTime, double offset);
    Array readData(Date runTime, double offset, double vertCoord);
  }

}
