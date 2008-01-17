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
package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * MADIS Station Convention.
 *
 * @author caron
 */

public class MADISStation extends CoordSysBuilder {
  public MADISStation() {
    this.conventionName = "MADIS_Station_1.0";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {

    String timeVars = ds.findAttValueIgnoreCase(null, "timeVariables", "");
    StringTokenizer stoker = new StringTokenizer( timeVars, ", ");
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      Variable v = ds.findVariable(vname);
      if (v != null) {
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
      } else {
        parseInfo.append(" cant find time variable ").append(vname);
      }
    }

    String locVars = ds.findAttValueIgnoreCase(null, "stationLocationVariables", "");
    stoker = new StringTokenizer( locVars, ", ");
    int count = 0;
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      Variable v = ds.findVariable(vname);
      if (v != null) {
        AxisType atype = count == 0 ? AxisType.Lat : count == 1 ? AxisType.Lon : AxisType.Height;
        v.addAttribute(new Attribute(_Coordinate.AxisType, atype.toString()));
      } else {
        parseInfo.append(" cant find time variable ").append(vname);
      }
      count++;
    }
  }

}