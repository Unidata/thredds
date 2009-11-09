/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.ArrayDouble;

import java.io.IOException;

/**
 * Cosmic data - version 1.
 * Add time coordinate from global atts start_time, stop_time, assuming its linear along the vertical dimension.
 *
 * @author caron
 * @since Jul 29, 2009
 */
public class Cosmic1Convention extends CoordSysBuilder {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a Zebra file.
   */
  public static boolean isMine(NetcdfFile ncfile) {

    //   :start_time = 9.17028312E8; // double
   // :stop_time = 9.170284104681826E8; // double

    if (null == ncfile.findDimension("MSL_alt")) return false;
    if (null == ncfile.findGlobalAttribute( "start_time")) return false;
    if (null == ncfile.findGlobalAttribute( "stop_time")) return false;

    String center = ncfile.findAttValueIgnoreCase(null, "center", null);
    return (center != null) && center.equals("UCAR/CDAAC");
  }

  public Cosmic1Convention() {
    this.conventionName = "Cosmic1";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {

    // create a time variable - assume its linear along the vertical dimension
    double start =  ds.readAttributeDouble(null, "start_time", Double.NaN);
    double stop =  ds.readAttributeDouble(null, "stop_time", Double.NaN);

    Dimension dim = ds.findDimension("MSL_alt");
    int n = dim.getLength();
    double incr = (stop - start) / n;

    String timeUnits = "seconds since 1980-01-01 00:00 UTC";
    Variable timeVar = new VariableDS(ds, null, null, "time", DataType.DOUBLE, dim.getName(), timeUnits, null);
    ds.addVariable(null, timeVar);
    timeVar.addAttribute(new Attribute("units", timeUnits));
    timeVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ArrayDouble.D1 data = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[] {n});
    for (int i=0; i<n; i++)
      data.set(i, stop - i * incr);
    timeVar.setCachedData(data, false);

    Variable v = ds.findVariable("Lat");
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    v = ds.findVariable("Lon");
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    ds.finish();
  }

  protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {
    String name =  v.getShortName();
    if (name.equals("time")) return AxisType.Time;
    if (name.equals("Lat")) return AxisType.Lat;
    if (name.equals("Lon")) return AxisType.Lon;
    if (name.equals("MSL_alt")) return AxisType.Height;    
    return null;
  }
}
