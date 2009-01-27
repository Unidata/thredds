/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
        parseInfo.format(" cant find time variable %s\n", vname);
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
        parseInfo.format(" cant find time variable %s\n",vname);
      }
      count++;
    }
  }

}