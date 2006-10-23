// $Id:CSMConvention.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;
import ucar.unidata.util.Parameter;
import ucar.unidata.geoloc.vertical.HybridSigmaPressure;
import ucar.unidata.geoloc.vertical.AtmosSigma;

import java.io.*;
import java.util.*;

/**
 * CSM-1 Convention. Deprecated: use CF
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class CSMConvention extends COARDSConvention {

  protected HashMap ctHash = new HashMap();

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) {
    this.conventionName = "NCAR-CSM";

    List vars = ds.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      VariableEnhanced ve = (VariableEnhanced) vars.get(i);
      String unit = ve.getUnitsString();
      if (unit != null) {
        if (unit.equalsIgnoreCase("hybrid_sigma_pressure") || unit.equalsIgnoreCase("sigma_level")) {
          // both a coordinate axis and transform
          Variable v = (Variable) ve;
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));
          v.addAttribute(new Attribute(_Coordinate.TransformType, TransformType.Vertical.toString()));
          v.addAttribute(new Attribute(_Coordinate.Axes, v.getName()));
        }
      }
    }

  }

  /**
   * The attribute "coordinates" is an alias for _CoordinateAxes.
   */
  protected void findCoordinateAxes(NetcdfDataset ds) {

    // coordinates is an alias for _CoordinateAxes
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.coordAxes == null) { // dont override if already set
        String coordsString = ds.findAttValueIgnoreCase(vp.v, "coordinates", null);
        if (coordsString != null) {
          vp.coordinates = coordsString;
        }
      }
    }

    super.findCoordinateAxes(ds);
  }

  protected CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    CoordinateTransform ct = null;

    String unit = ctv.getUnitsString();
    if (unit != null) {
      if (unit.equalsIgnoreCase("hybrid_sigma_pressure")) {
        HybridSigmaPressureBuilder b = new HybridSigmaPressureBuilder();
        ct = b.makeCoordinateTransform(ds, ctv);

      } else if (unit.equalsIgnoreCase("sigma_level")) { // LOOK - no test case for CSM Sigma Vertical coord ??
        SigmaBuilder b = new SigmaBuilder();
        ct = b.makeCoordinateTransform(ds, ctv);
      }
    }
    if (ct != null)
      return ct;

    return super.makeCoordinateTransform(ds, ctv);
  }

  private class HybridSigmaPressureBuilder extends AbstractCoordTransBuilder {
    public String getTransformName() {
      return "csm_hybrid_sigma_pressure";
    }

    public TransformType getTransformType() {
      return TransformType.Vertical;
    }

    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
      CoordinateTransform rs = new VerticalCT(ctv.getName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));

      if (!addParameter2(rs, HybridSigmaPressure.PS, ds, ctv, "PS_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.A, ds, ctv, "A_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.B, ds, ctv, "B_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.P0, ds, ctv, "P0_var", false)) return null;
      parseInfo.append("CSMConvention made SigmaPressureCT " + ctv.getName() + "\n");
      return rs;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new HybridSigmaPressure(ds, timeDim, vCT);
    }
  }

  private class SigmaBuilder extends AbstractCoordTransBuilder {
    public String getTransformName() {
      return "csm_sigma_level";
    }

    public TransformType getTransformType() {
      return TransformType.Vertical;
    }

    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
      CoordinateTransform rs = new VerticalCT("sigma-" + ctv.getName(), conventionName, VerticalCT.Type.Sigma, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

      if (!addParameter2(rs, AtmosSigma.PS, ds, ctv, "PS_var", false)) return null;
      if (!addParameter2(rs, AtmosSigma.SIGMA, ds, ctv, "B_var", false)) return null;
      if (!addParameter2(rs, AtmosSigma.PTOP, ds, ctv, "P0_var", false)) return null;
      parseInfo.append("CSMConvention made SigmaCT " + ctv.getName() + "\n");
      return rs;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new AtmosSigma(ds, timeDim, vCT);
    }
  }

  /**
   * Add a Parameter to a CoordinateTransform. The variable attribute points to a another variable that has the data in it.
   * Make sure that atrribute and variable exist. Id readData is true, read the data and use it as the value of the
   * parameter, otherwise use the name as the value of the parameter.
   *
   * @param rs        the CoordinateTransform
   * @param paramName the parameter name
   * @param ds        dataset
   * @param v         variable
   * @param attName   variable attribute name
   * @param readData  if true, read data and use a  s parameter value
   * @return true if success, false is failed
   */
  protected boolean addParameter2(CoordinateTransform rs, String paramName, NetcdfFile ds, Variable v, String attName, boolean readData) {
    String varName;
    if (null == (varName = ds.findAttValueIgnoreCase(v, attName, null))) {
      parseInfo.append("CSMConvention No Attribute named " + attName);
      return false;
    }
    varName = varName.trim();

    Variable dataVar;
    if (null == (dataVar = ds.findVariable(varName))) {
      parseInfo.append("CSMConvention No Variable named " + varName);
      return false;
    }

    if (readData) {
      Array data;
      try {
        data = dataVar.read();
      } catch (IOException e) {
        parseInfo.append("CSMConvention failed on read of " + varName + " err=" + e + "\n");
        return false;
      }
      double[] vals = (double []) data.get1DJavaArray(double.class);
      rs.addParameter(new Parameter(paramName, vals));

    } else
      rs.addParameter(new Parameter(paramName, varName));

    return true;
  }

}