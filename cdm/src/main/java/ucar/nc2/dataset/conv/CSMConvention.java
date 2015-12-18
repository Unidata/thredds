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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;
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

  private class HybridSigmaPressureBuilder extends AbstractCoordTransBuilder {
    public String getTransformName() {
      return "csm_hybrid_sigma_pressure";
    }

    public TransformType getTransformType() {
      return TransformType.Vertical;
    }

    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
      CoordinateTransform rs = new VerticalCT(ctv.getFullName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));

      if (!addParameter2(rs, HybridSigmaPressure.PS, ds, ctv, "PS_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.A, ds, ctv, "A_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.B, ds, ctv, "B_var", false)) return null;
      if (!addParameter2(rs, HybridSigmaPressure.P0, ds, ctv, "P0_var", false)) return null;
      parseInfo.format("CSMConvention made SigmaPressureCT %s%n",ctv.getFullName());
      return rs;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new HybridSigmaPressure(ds, timeDim, vCT.getParameters());
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
      CoordinateTransform rs = new VerticalCT("sigma-" + ctv.getFullName(), conventionName, VerticalCT.Type.Sigma, this);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

      if (!addParameter2(rs, AtmosSigma.PS, ds, ctv, "PS_var", false)) return null;
      if (!addParameter2(rs, AtmosSigma.SIGMA, ds, ctv, "B_var", false)) return null;
      if (!addParameter2(rs, AtmosSigma.PTOP, ds, ctv, "P0_var", false)) return null;
      parseInfo.format("CSMConvention made SigmaCT %s%n", ctv.getFullName());
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
  protected boolean addParameter2(CoordinateTransform rs, String paramName, NetcdfFile ds, Variable v, String attName, boolean readData) {
    String varName;
    if (null == (varName = ds.findAttValueIgnoreCase(v, attName, null))) {
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
      double[] vals = (double []) data.get1DJavaArray(double.class);
      rs.addParameter(new Parameter(paramName, vals));

    } else
      rs.addParameter(new Parameter(paramName, varName));

    return true;
  }

}