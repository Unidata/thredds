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

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.IOException;
import java.util.List;

/**
 * "Epic In Situ", dapper conventions.
 *
 * @see "http://www.epic.noaa.gov/epic/software/dapper/dapperdocs/metadata.html"
 *
 * @author caron
 */

public class EpicInsitu extends ucar.nc2.dataset.CoordSysBuilder {

  public EpicInsitu() {
    this.conventionName = "EpicInsitu";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    List<Variable> vars = ds.getVariables();
    findAxes(vars);
    ds.finish();
  }

  private void findAxes(List<Variable> vars) {
    for (Variable v : vars) {
      checkIfAxis(v);

      if (v instanceof Structure) {
        List<Variable> nested = ((Structure) v).getVariables();
        findAxes(nested);
      }
    }
  }

  private void checkIfAxis(Variable v) {
    Attribute att = v.findAttributeIgnoreCase("axis");
    if (att == null) return;
    String axisType = att.getStringValue();
    if (axisType.equalsIgnoreCase("X"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon .toString()));
    else if (axisType.equalsIgnoreCase("Y"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    else if (axisType.equalsIgnoreCase("Z"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
    else if (axisType.equalsIgnoreCase("T"))
      v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
  }

}
