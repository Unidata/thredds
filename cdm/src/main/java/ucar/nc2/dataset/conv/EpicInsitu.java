/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
 * @see <a href="http://www.epic.noaa.gov/epic/software/dapper/dapperdocs/metadata.html">http://www.epic.noaa.gov/epic/software/dapper/dapperdocs/metadata.html</a>
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
