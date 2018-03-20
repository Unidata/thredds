/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
        parseInfo.format(" cant find time variable %s%n", vname);
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
        parseInfo.format(" cant find time variable %s%n",vname);
      }
      count++;
    }
  }

}