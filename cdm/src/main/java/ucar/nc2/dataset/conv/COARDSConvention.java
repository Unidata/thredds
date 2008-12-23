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

import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;

/**
 * COARDS Convention.
 * see http://ferret.wrc.noaa.gov/noaa_coop/coop_cdf_profile.html
 *
 * @author caron
 */

public class COARDSConvention extends CoordSysBuilder {

  public COARDSConvention() {
    this.conventionName = "COARDS";
  }

  // we assume that coordinate axes get identified by being coordinate variables
  protected AxisType getAxisType( NetcdfDataset ncDataset, VariableEnhanced v) {

    String unit = v.getUnitsString();
    if (unit == null)
      return null;

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

    if (SimpleUnit.isDateUnit(unit)) // || SimpleUnit.isTimeUnit(unit)) removed dec 18, 2008
      return AxisType.Time;

    // look for other z coordinate
    //if (SimpleUnit.isCompatible("m", unit))
    //  return AxisType.Height;
    if (SimpleUnit.isCompatible("mbar", unit))
      return AxisType.Pressure;
    if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level"))
      return AxisType.GeoZ;

    String positive = ncDataset.findAttValueIgnoreCase((Variable) v, "positive", null);
    if (positive != null) {
      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
      else
        return AxisType.GeoZ;
    }

    return null;
  }

}

 