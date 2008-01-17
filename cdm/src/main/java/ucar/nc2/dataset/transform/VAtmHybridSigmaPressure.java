/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
import ucar.unidata.geoloc.vertical.HybridSigmaPressure;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;

/**
 * Create a atmosphere_hybrid_sigma_pressure_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 *  *
 * @author caron
 */
public class VAtmHybridSigmaPressure extends AbstractCoordTransBuilder {
  private String a = "", b = "", ps = "", p0 = "";

  public String getTransformName() {
    return "atmosphere_hybrid_sigma_pressure_coordinate";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula_terms = getFormula(ds, ctv);
    if (null == formula_terms) return null;

    // parse the formula string
    StringTokenizer stoke = new StringTokenizer(formula_terms);
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equalsIgnoreCase("a:"))
        a = stoke.nextToken();
      else if (toke.equalsIgnoreCase("b:"))
        b = stoke.nextToken();
      else if (toke.equalsIgnoreCase("ps:"))
        ps = stoke.nextToken();
      else if (toke.equalsIgnoreCase("p0:"))
        p0 = stoke.nextToken();
    }

    CoordinateTransform rs = new VerticalCT("AtmHybridSigmaPressure_Transform_"+ctv.getName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));

    rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));
    if (!addParameter(rs, HybridSigmaPressure.PS, ds, ps)) return null;
    if (!addParameter(rs, HybridSigmaPressure.A, ds, a)) return null;
    if (!addParameter(rs, HybridSigmaPressure.B, ds, b)) return null;
    if (!addParameter(rs, HybridSigmaPressure.P0, ds, p0)) return null;
    return rs;
  }

  public String toString() {
    return "HybridSigma:" + "ps:"+ps + " p0:"+p0 + " a:"+a + " b:"+b;    
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new HybridSigmaPressure(ds, timeDim, vCT.getParameters());
  }
}

