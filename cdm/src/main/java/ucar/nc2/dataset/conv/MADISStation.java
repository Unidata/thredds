// $Id: MADISStation.java,v 1.2 2006/01/14 22:15:02 caron Exp $
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
package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MADIS Station Convention.
 *
 * @author caron
 * @version $Revision: 1.2 $ $Date: 2006/01/14 22:15:02 $
 */

public class MADISStation extends CoordSysBuilder {
  private Attribute translation, affine;

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    this.conventionName = "MADIS_Station_1.0";

    String timeVars = ds.findAttValueIgnoreCase(null, "timeVariables", "");
    StringTokenizer stoker = new StringTokenizer( timeVars, ", ");
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      Variable v = ds.findVariable(vname);
      if (v != null) {
        v.addAttribute(new Attribute("_CoordinateAxisType", AxisType.Time.toString()));
      } else {
        parseInfo.append(" cant find time variable "+vname);
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
        v.addAttribute(new Attribute("_CoordinateAxisType", atype.toString()));
      } else {
        parseInfo.append(" cant find time variable "+vname);
      }
      count++;
    }
  }

}