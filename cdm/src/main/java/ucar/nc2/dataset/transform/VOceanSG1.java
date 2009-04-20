// $Id:VOceanS.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.dataset.transform;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.VerticalCT;
import ucar.unidata.geoloc.vertical.OceanSG1;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;

/**
 * Create a ocean_s_coordinate_g1 Vertical Transform from the information in the Coordinate Transform Variable.
 *
 * @author Sachin (skbhate@ngi.msstate.edu)
 */
public class VOceanSG1 extends AbstractCoordTransBuilder {
  private String s = "", eta = "", depth = "", c = "", depth_c = "";

  public String getTransformName() {
    return "ocean_s_coordinate_g1";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula_terms = getFormula(ds, ctv);
    if (null == formula_terms) return null;

    // :formula_terms = "s: s_rho eta: zeta depth: h c: Cs_r  depth_c: hc";
    String[] values = parseFormula(formula_terms, "s C eta depth  depth_c");
    if (values == null) return null;

      s = values[0];
      c = values[1];
      eta = values[2];
      depth = values[3];
      depth_c = values[4];     


    CoordinateTransform rs = new VerticalCT("OceanSG1_Transform_"+ctv.getName(), getTransformName(), VerticalCT.Type.OceanSG1, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    rs.addParameter((new Parameter("height_formula", "height(x,y,z) =  depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z) + eta(x,y)*(1+(depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z))/depth([n],x,y))" ))) ;
    if (!addParameter(rs, OceanSG1.ETA, ds, eta)) return null;
    if (!addParameter(rs, OceanSG1.S, ds, s)) return null;
    if (!addParameter(rs, OceanSG1.DEPTH, ds, depth)) return null;
    if (!addParameter(rs, OceanSG1.DEPTH_C, ds, depth_c)) return null;
    if (!addParameter(rs, OceanSG1.C, ds, c)) return null;


    return rs;
  }

  public String toString() {
     return "OceanSG1:" + " s:"+s + " c:"+c + " eta:"+eta + " depth:"+depth + " depth_c:"+depth_c;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new OceanSG1(ds, timeDim, vCT.getParameters());
  }
}