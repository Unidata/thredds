/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.vertical.OceanSG2;
import ucar.unidata.util.Parameter;


/**
 * Create a ocean_s_coordinate_g2 Vertical Transform from the information in the Coordinate Transform Variable.
 *
 * @author Sachin (skbhate@ngi.msstate.edu)
 */
public class VOceanSG2 extends AbstractTransformBuilder implements VertTransformBuilderIF {
  private String s = "", eta = "", depth = "", c = "", depth_c = "";

  public String getTransformName() {
    return VerticalCT.Type.OceanSG2.name();
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms) return null;

    // :formula_terms = "s: s_rho c: Cs_r eta: zeta depth: h  depth_c: hc";
    String[] values = parseFormula(formula_terms, "s C eta depth depth_c");
    if (values == null) return null;

    s = values[0];
    c = values[1];
    eta = values[2];
    depth = values[3];
    depth_c = values[4];

    VerticalCT rs = new VerticalCT("OceanSG2_Transform_" + ctv.getName(), getTransformName(), VerticalCT.Type.OceanSG2, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    rs.addParameter((new Parameter("height_formula", "height(x,y,z) = eta(x,y) + (eta(x,y) + depth([n],x,y)) * ((depth_c*s(z) + depth([n],x,y)*C(z))/(depth_c+depth([n],x,y)))")));
    if (!addParameter(rs, OceanSG2.ETA, ds, eta)) return null;
    if (!addParameter(rs, OceanSG2.S, ds, s)) return null;
    if (!addParameter(rs, OceanSG2.DEPTH, ds, depth)) return null;
    if (!addParameter(rs, OceanSG2.DEPTH_C, ds, depth_c)) return null;
    if (!addParameter(rs, OceanSG2.C, ds, c)) return null;

    return rs;
  }

  public String toString() {
    return "OceanSG2:" + " s:" + s + " c:" + c + " eta:" + eta + " depth:" + depth + " depth_c:" + depth_c;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new OceanSG2(ds, timeDim, vCT.getParameters());
  }
}