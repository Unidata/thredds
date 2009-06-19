/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.HybridSigmaPressure;
import ucar.unidata.util.Parameter;

/**
 * Create a atmosphere_hybrid_sigma_pressure_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 *  *
 * @author caron
 */
public class VAtmHybridSigmaPressure extends AbstractCoordTransBuilder {
  private boolean useAp;
  private String a, b, ps, p0, ap;

  public String getTransformName() {
    return "atmosphere_hybrid_sigma_pressure_coordinate";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula_terms = getFormula(ds, ctv);
    if (null == formula_terms) return null;

    useAp = formula_terms.indexOf("ap:") >= 0;

    CoordinateTransform rs = new VerticalCT("AtmHybridSigmaPressure_Transform_"+ctv.getShortName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));

     // parse the formula string
    if (useAp) {
      String[] values = parseFormula(formula_terms, "ap b ps");
      if (values == null) return null;

      ap = values[0];
      b = values[1];
      ps = values[2];

      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ap(z) + b(z)*surfacePressure(x,y)"));
      if (!addParameter(rs, HybridSigmaPressure.PS, ds, ps)) return null;
      if (!addParameter(rs, HybridSigmaPressure.AP, ds, ap)) return null;
      if (!addParameter(rs, HybridSigmaPressure.B, ds, b)) return null;

    } else {
      String[] values = parseFormula(formula_terms, "a b ps p0");
      if (values == null) return null;

      a = values[0];
      b = values[1];
      ps = values[2];
      p0 = values[3];

      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));
      if (!addParameter(rs, HybridSigmaPressure.PS, ds, ps)) return null;
      if (!addParameter(rs, HybridSigmaPressure.A, ds, a)) return null;
      if (!addParameter(rs, HybridSigmaPressure.B, ds, b)) return null;
      if (!addParameter(rs, HybridSigmaPressure.P0, ds, p0)) return null;
    }

    return rs;
  }

  public String toString() {
    return  "HybridSigmaPressure:" + (useAp ? "ps:"+ps + " p0:"+p0 + " a:"+a + " b:"+b : "ps:"+ps + " ap:"+ap + " b:"+b);
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new HybridSigmaPressure(ds, timeDim, vCT.getParameters());
  }
}

