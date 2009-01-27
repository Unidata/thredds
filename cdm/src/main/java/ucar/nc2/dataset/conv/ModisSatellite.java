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

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.ma2.DataType;
import ucar.ma2.ArrayLong;

import java.util.List;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 26, 2009
 */
public class ModisSatellite extends ucar.nc2.dataset.CoordSysBuilder {

  public static boolean isMine(NetcdfFile ncfile) {
    String satName = ncfile.findAttValueIgnoreCase(null, "SATNAME", null);
    if ((satName == null) || !(satName.equalsIgnoreCase("Aqua")))
      return false;

    String instName = ncfile.findAttValueIgnoreCase(null, "INTRUMENT_NAME", null);
    if ((instName == null) || !(instName.equalsIgnoreCase("modis")))
      return false;

    return true;
  }

  public ModisSatellite() {
    this.conventionName = "ModisSatellite";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
   for (Variable v : ds.getVariables())
     checkIfAxis(v);

    int year = ds.readAttributeInteger(null, "YEAR", -1);
    int doy = ds.readAttributeInteger(null, "DAY", -1);
    double time = ds.readAttributeDouble(null, "TIME", Double.NaN);

    if ((year >0) && (doy > 0) && !Double.isNaN(time)) {
      Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      cal.clear();
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.DAY_OF_YEAR, doy);

      int hour = (int) time;
      cal.set(Calendar.HOUR_OF_DAY, hour);

      time -= hour;
      time *= 60;
      int minute = (int) time;
      cal.set(Calendar.MINUTE, minute);

      time -= minute;
      time *= 60;
      cal.set(Calendar.SECOND, (int) time);

      VariableDS var = new VariableDS(ds, null, null, "timeFromAtts", DataType.LONG, "",
              "seconds since 1970-01-01 00:00", "time generated from global attributes");
      // LOOK : cant handle scalar coordinates yet
      // var.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
      ds.addVariable(null, var);
      ArrayLong.D0 data = new ArrayLong.D0();
      data.set(cal.getTime().getTime()/1000);
      var.setCachedData(data, true);
    }

    ds.finish();
  }

  private void checkIfAxis(Variable v) {
    String name = v.getName();
    if (name.equalsIgnoreCase("Longitude"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    else if (name.equalsIgnoreCase("Latitude"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
  }

}
