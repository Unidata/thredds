/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.transform.AbstractTransformBuilder;
import ucar.nc2.dataset.transform.VertTransformBuilderIF;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.vertical.HybridSigmaPressure;
import ucar.unidata.geoloc.vertical.AtmosSigma;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.util.*;

/**
 * CSM-1 Convention. Deprecated: use CF
 *
 * @author caron
 */

public class CSMConvention extends COARDSConvention {

  public CSMConvention() {
    this.conventionName = "NCAR-CSM";
  }

  @Override
  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {

    List<Variable> vars = ds.getVariables();
    for (Variable var : vars) {
      String unit = var.getUnitsString();
      if (unit != null) {
        if (unit.equalsIgnoreCase("hybrid_sigma_pressure") || unit.equalsIgnoreCase("sigma_level")) {
          // both a coordinate axis and transform
          var.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));
          var.addAttribute(new Attribute(_Coordinate.TransformType, TransformType.Vertical.toString()));
          var.addAttribute(new Attribute(_Coordinate.Axes, var.getFullName()));
        }
      }
    }

  }

  /**
   * The attribute "coordinates" is an alias for _CoordinateAxes.
   */
  protected void findCoordinateAxes(NetcdfDataset ds) {

    // coordinates is an alias for _CoordinateAxes
    for (VarProcess vp : varList) {
      if (vp.coordAxes == null) { // dont override if already set
        String coordsString = ds.findAttValueIgnoreCase(vp.v, CF.COORDINATES, null);
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

  private class HybridSigmaPressureBuilder extends AbstractTransformBuilder implements VertTransformBuilderIF {
    public String getTransformName() {
      return "csm_hybrid_sigma_pressure";
    }

    public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
      VerticalCT rs = new VerticalCT(ctv.getName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));

      if (!addParameter2(rs, HybridSigmaPressure.PS, ds, ctv, "PS_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.A, ds, ctv, "A_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.B, ds, ctv, "B_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.P0, ds, ctv, "P0_var", false)) return null;
      parseInfo.format("CSMConvention made SigmaPressureCT %s%n",ctv.getName());
      return rs;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new HybridSigmaPressure(ds, timeDim, vCT.getParameters());
    }
  }

  private class SigmaBuilder extends AbstractTransformBuilder implements VertTransformBuilderIF {
    public String getTransformName() {
      return "csm_sigma_level";
    }

    public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
      VerticalCT rs = new VerticalCT("sigma-" + ctv.getName(), conventionName, VerticalCT.Type.Sigma, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

      if (!addParameter2(rs, AtmosSigma.PS, ds, ctv, "PS_var", false)) return null;
      if (!addParameter2(rs, AtmosSigma.SIGMA, ds, ctv, "B_var", false)) return null;
      if (!addParameter2(rs, AtmosSigma.PTOP, ds, ctv, "P0_var", false)) return null;
      parseInfo.format("CSMConvention made SigmaCT %s%n", ctv.getName());
      return rs;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new AtmosSigma(ds, timeDim, vCT.getParameters());
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
  protected boolean addParameter2(CoordinateTransform rs, String paramName, NetcdfFile ds, AttributeContainer v, String attName, boolean readData) {
    String varName;
    if (null == (varName = v.findAttValueIgnoreCase(attName, null))) {
      parseInfo.format("CSMConvention No Attribute named %s%n", attName);
      return false;
    }
    varName = varName.trim();

    Variable dataVar;
    if (null == (dataVar = ds.findVariable(varName))) {
      parseInfo.format("CSMConvention No Variable named %s%n", varName);
      return false;
    }

    if (readData) {
      Array data;
      try {
        data = dataVar.read();
      } catch (IOException e) {
        parseInfo.format("CSMConvention failed on read of %s err= %s%n", varName, e.getMessage());
        return false;
      }
      double[] vals = (double []) data.get1DJavaArray(DataType.DOUBLE);
      rs.addParameter(new Parameter(paramName, vals));

    } else
      rs.addParameter(new Parameter(paramName, varName));

    return true;
  }

}