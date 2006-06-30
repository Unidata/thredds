// $Id: VOceanS.java,v 1.2 2006/05/25 20:15:27 caron Exp $
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

import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.OceanS;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;

/**
 * Create a ocean_s_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 */
public class VOceanS extends AbstractCoordTransBuilder {
  private String s = "", eta = "", depth = "", a = "", b = "", depth_c = "";

  public String getTransformName() {
    return "ocean_s_coordinate";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula = getFormula(ds, ctv);
    if (null == formula) return null;

    // :formula_terms = "s: s_rho eta: zeta depth: h a: theta_s b: theta_b depth_c: hc";
    StringTokenizer stoke = new StringTokenizer(formula);
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equalsIgnoreCase("s:"))
        s = stoke.nextToken();
      else if (toke.equalsIgnoreCase("eta:"))
        eta = stoke.nextToken();
      else if (toke.equalsIgnoreCase("depth:"))
        depth = stoke.nextToken();
      else if (toke.equalsIgnoreCase("a:"))
        a = stoke.nextToken();
      else if (toke.equalsIgnoreCase("b:"))
        b = stoke.nextToken();
      else if (toke.equalsIgnoreCase("depth_c:"))
        depth_c = stoke.nextToken();
    }

    CoordinateTransform rs = new VerticalCT("oceanS-" + ctv.getName(), getTransformName(), VerticalCT.Type.OceanS, this);
    rs.addParameter((new Parameter("height formula", "height(x,y,z) = eta(x,y)*(1+s(z)) + depth_c*s(z) + (depth(x,y)-depth_c)*C(z)")));
    rs.addParameter((new Parameter("C formula", "C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)")));

    if (!addParameter(rs, OceanS.ETA, ds, eta, false)) return null;
    if (!addParameter(rs, OceanS.S, ds, s, false)) return null;
    if (!addParameter(rs, OceanS.DEPTH, ds, depth, false)) return null;

    if (!addParameter(rs, OceanS.DEPTH_C, ds, depth_c, true)) return null;
    if (!addParameter(rs, OceanS.A, ds, a, true)) return null;
    if (!addParameter(rs, OceanS.B, ds, b, true)) return null;

    return rs;
  }

  public String toString() {
    return "OceanS:" + " s:"+s + " eta:"+eta + " depth_c:"+depth + " a:"+a + " b:"+b;    

  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new OceanS(ds, timeDim, vCT);
  }
}

