package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.IOException;
import java.util.List;


public class EpicInsitu extends ucar.nc2.dataset.CoordSysBuilder {

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    this.conventionName = "EpicInsitu";

    List vars = ds.getVariables();
    findAxes(vars);
    ds.finish();
  }

  private void findAxes(List vars) {
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      checkIfAxis(v);

      if (v instanceof Structure) {
        java.util.List nested = ((Structure) v).getVariables();
        findAxes( nested);
      }
    }
  }

  private void checkIfAxis(Variable v) {
    Attribute att = v.findAttributeIgnoreCase("axis");
    if (att == null) return;
    String axisType = att.getStringValue();
    if (axisType.equalsIgnoreCase("X"))
      v.addAttribute( new Attribute("_CoordinateAxisType", AxisType.Lon .toString()));
    else if (axisType.equalsIgnoreCase("Y"))
      v.addAttribute( new Attribute("_CoordinateAxisType", AxisType.Lat.toString()));
    else if (axisType.equalsIgnoreCase("Z"))
      v.addAttribute( new Attribute("_CoordinateAxisType", AxisType.Height.toString()));
    else if (axisType.equalsIgnoreCase("T"))
      v.addAttribute( new Attribute("_CoordinateAxisType", AxisType.Time.toString()));
  }

}
