/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.dataset.transform;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.VerticalCT;
import ucar.unidata.geoloc.vertical.OceanSG2;
import ucar.unidata.util.Parameter;


/**
 * Create a ocean_s_coordinate_g2 Vertical Transform from the information in the Coordinate Transform Variable.
 *
 * @author Sachin (skbhate@ngi.msstate.edu)
 */
public class VOceanSG2 extends AbstractCoordTransBuilder {
  private String s = "", eta = "", depth = "", c = "", depth_c = "";

  public String getTransformName() {
    return "ocean_s_coordinate_g2";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula_terms = getFormula(ds, ctv);
    if (null == formula_terms) return null;

    // :formula_terms = "s: s_rho c: Cs_r eta: zeta depth: h  depth_c: hc";
    String[] values = parseFormula(formula_terms, "s C eta depth depth_c");
    if (values == null) return null;

    s = values[0];
    c = values[1];
    eta = values[2];
    depth = values[3];
    depth_c = values[4];


    CoordinateTransform rs = new VerticalCT("OceanSG2_Transform_" + ctv.getFullName(), getTransformName(), VerticalCT.Type.OceanSG2, this);
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