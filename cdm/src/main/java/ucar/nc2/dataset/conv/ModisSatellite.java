/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.ma2.DataType;
import ucar.ma2.ArrayLong;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.io.IOException;

/**
 * Modis satellite conventions
 *
 * @author caron
 * @since Jan 26, 2009
 */
public class ModisSatellite extends ucar.nc2.dataset.CoordSysBuilder {

  public static boolean isMine(NetcdfFile ncfile) {
    String satName = ncfile.findAttValueIgnoreCase(null, "SATNAME", null);
    if ((satName == null) || !(satName.equalsIgnoreCase("Aqua")))
      return false;

    String instName = ncfile.findAttValueIgnoreCase(null, "INTRUMENT_NAME", null);    // LOOK "INTRUMENT_NAME" ??
    return !((instName == null) || !(instName.equalsIgnoreCase("modis")));

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
      ArrayLong.D0 data = new ArrayLong.D0(false);
      data.set(cal.getTime().getTime()/1000);
      var.setCachedData(data, true);
    }

    ds.finish();
  }

  private void checkIfAxis(Variable v) {
    String name = v.getShortName();
    if (name.equalsIgnoreCase("Longitude"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    else if (name.equalsIgnoreCase("Latitude"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
  }

}
