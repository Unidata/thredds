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

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Unidata Observation Dataset v1.0
 * Use CF for new files.
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html"
 * @author caron
 */
public class UnidataObsConvention extends CoordSysBuilder {

  public UnidataObsConvention() {
    this.conventionName = "Unidata Observation Dataset v1.0";
  }

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
      if (!hasAxisType(ds, AxisType.Height)) { // already has _CoordinateAxisType

        if (!addAxisType(ds, "altitude", AxisType.Height)) { // directly named
          if (!addAxisType(ds, "depth", AxisType.Height)) { // directly named

            String vname = ds.findAttValueIgnoreCase(null, "altitude_coordinate", null);
            if (!addAxisType(ds, vname, AxisType.Height)) { // attribute named

              for (int i = 0; i < ds.getVariables().size(); i++) {
                VariableEnhanced ve = (VariableEnhanced) ds.getVariables().get( i );
                String positive = ds.findAttValueIgnoreCase((Variable) ve, "positive", null);
                if (positive != null) {
                  addAxisType((Variable) ve, AxisType.Height); // CF-1
                  break;
                }
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
    List<Variable> varList = ds.getVariables();
    for (Variable v : varList) {
      String axisType = ds.findAttValueIgnoreCase(v, "CoordinateAxisType", null);
      if ((axisType != null) && axisType.equals(a.toString()))
        return true;
    }
    return false;
  }

  private Variable hasUnits(NetcdfDataset ds, String unitList) {
    List<Variable> varList = ds.getVariables();
    StringTokenizer stoker = new StringTokenizer(unitList, ",");
    while (stoker.hasMoreTokens()) {
      String unit = stoker.nextToken();

      for (Variable ve : varList) {
        String hasUnit = ve.getUnitsString();
        if (hasUnit == null) continue;
        if (hasUnit.equalsIgnoreCase(unit))
          return ve;
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
    v.addAttribute( new Attribute(_Coordinate.AxisType, a.toString()));
  }

}
