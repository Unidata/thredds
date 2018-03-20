/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.vertical.OceanSG1;
import ucar.unidata.util.Parameter;

/**
 * Create a ocean_s_coordinate_g1 Vertical Transform from the information in the Coordinate Transform Variable.
 *
 * @author Sachin (skbhate@ngi.msstate.edu)
 */
public class VOceanSG1 extends AbstractTransformBuilder implements VertTransformBuilderIF {
  private String s = "", eta = "", depth = "", c = "", depth_c = "";

  public String getTransformName() {
    return VerticalCT.Type.OceanSG1.name();
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms) return null;

    // :formula_terms = "s: s_rho eta: zeta depth: h c: Cs_r  depth_c: hc";
    String[] values = parseFormula(formula_terms, "s C eta depth  depth_c");
    if (values == null) return null;

    s = values[0];
    c = values[1];
    eta = values[2];
    depth = values[3];
    depth_c = values[4];

    VerticalCT rs = new VerticalCT("OceanSG1_Transform_" + ctv.getName(), getTransformName(), VerticalCT.Type.OceanSG1, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    rs.addParameter((new Parameter("height_formula", "height(x,y,z) =  depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z) + eta(x,y)*(1+(depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z))/depth([n],x,y))")));
    if (!addParameter(rs, OceanSG1.ETA, ds, eta)) return null;
    if (!addParameter(rs, OceanSG1.S, ds, s)) return null;
    if (!addParameter(rs, OceanSG1.DEPTH, ds, depth)) return null;
    if (!addParameter(rs, OceanSG1.DEPTH_C, ds, depth_c)) return null;
    if (!addParameter(rs, OceanSG1.C, ds, c)) return null;

    return rs;
  }

  public String toString() {
    return "OceanSG1:" + " s:" + s + " c:" + c + " eta:" + eta + " depth:" + depth + " depth_c:" + depth_c;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new OceanSG1(ds, timeDim, vCT.getParameters());
  }
}