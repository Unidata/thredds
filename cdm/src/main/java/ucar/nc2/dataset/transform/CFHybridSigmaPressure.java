/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.HybridSigmaPressure;
import ucar.unidata.util.Parameter;

/**
 * Create a atmosphere_hybrid_sigma_pressure_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 *  *
 * @author caron
 */
public class CFHybridSigmaPressure extends AbstractTransformBuilder implements VertTransformBuilderIF {
  private boolean useAp;
  private String a, b, ps, p0, ap;

  public String getTransformName() {
    return VerticalCT.Type.HybridSigmaPressure.name();
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms) return null;

    useAp = formula_terms.contains("ap:");

    VerticalCT rs = new VerticalCT("AtmHybridSigmaPressure_Transform_"+ctv.getName(), getTransformName(), VerticalCT.Type.HybridSigmaPressure, this);
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

