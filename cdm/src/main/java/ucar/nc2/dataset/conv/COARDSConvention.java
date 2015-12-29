/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dataset.conv;

import ucar.nc2.Attribute;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;
import ucar.nc2.time.Calendar;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.List;

/**
 * COARDS Convention.
 * see http://ferret.wrc.noaa.gov/noaa_coop/coop_cdf_profile.html
 *
 * @author caron
 */

public class COARDSConvention extends CoordSysBuilder {

  /*
  The COARDS standard offers limited support for climatological time. For compatibility with COARDS, time coordinates should also be
  recognised as climatological if they have a units attribute of time-units relative to midnight on 1 January in year 0
  i.e. since 0-1-1 in udunits syntax , and provided they refer to the real-world calendar. We do not recommend this convention because
   (a) it does not provide any information about the intervals used to compute the climatology, and
   (b) there is no standard for how dates since year 1 will be encoded with units having a reference time in year 0,
  since this year does not exist; consequently there may be inconsistencies among software packages in the interpretation of the
  time coordinates. Year 0 may be a valid year in non-real-world calendars, and therefore cannot be used to signal climatological
  time in such cases.
   */

  public static boolean isMine(String hasName) {
    if (hasName.equalsIgnoreCase("COARDS")) return true;
    List<String> names = breakupConventionNames(hasName);
    for (String name : names) {
      if (name.equalsIgnoreCase("COARDS")) return true;
    }
    return false;
  }


  public COARDSConvention() {
    this.conventionName = "COARDS";
  }

  protected boolean checkTimeVarForCalendar (Variable var) {
    boolean hasChanged = false;
    String unit = var.getUnitsString();
    if (unit != null) {
      unit = unit.trim();
      if (SimpleUnit.isDateUnit(unit)) {
        Attribute calAttr = var.findAttributeIgnoreCase(CF.CALENDAR);
        if (calAttr == null) {
          calAttr = new Attribute(CF.CALENDAR, Calendar.gregorian.toString());
          var.addAttribute(calAttr);
          hasChanged = true;
        }
      }
    }
    return hasChanged;
  }

  @Override
  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    boolean hasChanged = false;
    boolean hasChangedThisTime;
    for (Variable var : ds.getVariables()) {
      hasChangedThisTime = checkTimeVarForCalendar(var);
      if (hasChangedThisTime) {
        hasChanged = true;
      }
    }
    if (hasChanged) {
      ds.finish();
    }
  }

  protected boolean checkForMeter = true;

  // we assume that coordinate axes get identified by being coordinate variables
  protected AxisType getAxisType( NetcdfDataset ncDataset, VariableEnhanced v) {

    String unit = v.getUnitsString();
    if (unit == null)
      return null;
    unit = unit.trim();
    
    if( unit.equalsIgnoreCase("degrees_east") ||
            unit.equalsIgnoreCase("degrees_E") ||
            unit.equalsIgnoreCase("degreesE") ||
            unit.equalsIgnoreCase("degree_east") ||
            unit.equalsIgnoreCase("degree_E") ||
            unit.equalsIgnoreCase("degreeE"))
      return AxisType.Lon;

    if ( unit.equalsIgnoreCase("degrees_north") ||
            unit.equalsIgnoreCase("degrees_N") ||
            unit.equalsIgnoreCase("degreesN") ||
            unit.equalsIgnoreCase("degree_north") ||
            unit.equalsIgnoreCase("degree_N") ||
            unit.equalsIgnoreCase("degreeN"))
      return AxisType.Lat;

    if (SimpleUnit.isDateUnit(unit)) {
      return AxisType.Time;
    }
    // look for other z coordinate
    if (SimpleUnit.isCompatible("mbar", unit))
      return AxisType.Pressure;
    if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level"))
      return AxisType.GeoZ;

    String positive = ncDataset.findAttValueIgnoreCase((Variable) v, CF.POSITIVE, null);
    if (positive != null) {
      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
      else
        return AxisType.GeoZ;
    }

    // a bad idea, but CDC SST relies on it :
    // :Source = "NOAA/National Climatic Data Center";
   // :Contact = "Dick Reynolds, email: Richard.W.Reynolds@noaa.gov & Chunying Liu, email: Chunying.liu@noaa.gov";
   //:netcdf_Convention = "COARDS";
   // if (checkForMeter && SimpleUnit.isCompatible("m", unit))
   //   return AxisType.Height;

    return null;
  }

}

 