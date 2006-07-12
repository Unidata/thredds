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
package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.units.SimpleUnit;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class UnidataObsConvention extends CoordSysBuilder {

    /** create a NetcdfDataset out of this NetcdfFile, adding coordinates etc. */
  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {

    // latitude
    if (!hasAxisType( ds, AxisType.Lat)) { // already has _CoordinateAxisType

      if ( !addAxisType( ds, "latitude", AxisType.Lat)) { // directly named

        String vname = ds.findAttValueIgnoreCase(null, "latitude_coordinate", null);
        if (!addAxisType( ds, vname, AxisType.Lat)) { // attribute named

          Variable v = hasUnits(ds, "degrees_north,degrees_N,degreesN,degree_north,degree_N,degreeN");
          if (v != null)
            addAxisType( v, AxisType.Lat); // CF-1
        }
      }
    }

    // longitude
    if (!hasAxisType( ds, AxisType.Lon)) { // already has _CoordinateAxisType

      if ( !addAxisType( ds, "longitude", AxisType.Lon)) { // directly named

        String vname = ds.findAttValueIgnoreCase(null, "longitude_coordinate", null);
        if (!addAxisType( ds, vname, AxisType.Lon)) { // attribute named

          Variable v = hasUnits(ds, "degrees_east,degrees_E,degreesE,degree_east,degree_E,degreeE");
          if (v != null)
            addAxisType( v, AxisType.Lon); // CF-1
        }
      }
    }

    // altitude
    if (!hasAxisType( ds, AxisType.Height)) { // already has _CoordinateAxisType

      if ( !addAxisType( ds, "altitude", AxisType.Height)) { // directly named
        if ( !addAxisType( ds, "depth", AxisType.Height)) { // directly named

        String vname = ds.findAttValueIgnoreCase(null, "altitude_coordinate", null);
        if (!addAxisType( ds, vname, AxisType.Height)) { // attribute named

          for (int i = 0; i < ds.getVariables().size(); i++) {
            VariableEnhanced ve = (VariableEnhanced) ds.getVariables().get(i);
            String positive = ds.findAttValueIgnoreCase(null, "positive", null);
            if (positive != null)
              addAxisType( (Variable) ve, AxisType.Height); // CF-1
              break;
            }
          }
        }
      }
    }

     // time
    if (!hasAxisType( ds, AxisType.Time)) { // already has _CoordinateAxisType

      if ( !addAxisType( ds, "time", AxisType.Time)) { // directly named

        String vname = ds.findAttValueIgnoreCase(null, "time_coordinate", null);
        if (!addAxisType( ds, vname, AxisType.Time)) { // attribute named

          for (int i = 0; i < ds.getVariables().size(); i++) {
            VariableEnhanced ve = (VariableEnhanced) ds.getVariables().get(i);
            String unit = ve.getUnitsString();
            if (unit == null) continue;
            if (SimpleUnit.isDateUnit(unit)) {
              addAxisType( (Variable) ve, AxisType.Time); // CF-1
              break;
            }
          }
        }
      }
    }

  }

  private boolean hasAxisType(NetcdfDataset ds, AxisType a) {
    List varList = ds.getVariables();
    for (int i = 0; i < varList.size(); i++) {
      Variable v = (Variable) varList.get(i);
      String axisType = ds.findAttValueIgnoreCase(v, "CoordinateAxisType", null);
      if ((axisType != null) && axisType.equals(a.toString()))
        return true;
    }
    return false;
  }

  private Variable hasUnits(NetcdfDataset ds, String unitList) {
    List varList = ds.getVariables();
    StringTokenizer stoker = new StringTokenizer(unitList, ",");
    while (stoker.hasMoreTokens()) {
      String unit = stoker.nextToken();

      for (int i = 0; i < varList.size(); i++) {
        VariableEnhanced ve = (VariableEnhanced) varList.get(i);
        String hasUnit = ve.getUnitsString();
        if (hasUnit == null) continue;
        if (hasUnit.equalsIgnoreCase(unit))
          return (Variable) ve;
      }
    }
    return null;
  }


  private boolean addAxisType(NetcdfDataset ds, String vname, AxisType a) {
    if (vname == null) return false;
    Variable v = ds.findVariable(vname);
    if (v == null) return false;
    addAxisType( v, a);
    return true;
  }

  private void addAxisType(Variable v, AxisType a) {
    v.addAttribute( new Attribute("_CoordinateAxisType", a.toString()));
  }

}
