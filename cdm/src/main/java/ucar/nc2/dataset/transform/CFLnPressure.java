/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.AtmosLnPressure;
import ucar.unidata.util.Parameter;

/**
 * implementation for CF vertical coordinate "atmosphere_ln_pressure_coordinate".
 * DO NOT USE: see CF1Convention.makeAtmLnCoordinate()
 * @author caron
 * @since May 6, 2008
 */
public class CFLnPressure extends AbstractTransformBuilder implements VertTransformBuilderIF {
  private String p0, lev;

  public String getTransformName() {
    return VerticalCT.Type.LnPressure.name();
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms) return null;

     // parse the formula string
    String[] values = parseFormula(formula_terms, "p0 lev");
    if (values == null) return null;

    p0 = values[0];
    lev = values[1];

    VerticalCT rs = new VerticalCT("AtmSigma_Transform_"+ctv.getName(), getTransformName(), VerticalCT.Type.LnPressure, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    rs.addParameter(new Parameter("formula", "pressure(z) = p0 * exp(-lev(k))"));

    if (!addParameter( rs, AtmosLnPressure.P0, ds, p0)) return null;
    if (!addParameter( rs, AtmosLnPressure.LEV, ds, lev)) return null;

    return rs;
  }

  public String toString() {
    return "AtmLnPressure:" + "p0:"+p0 + " lev:"+lev;
  }


  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new AtmosLnPressure(ds, timeDim, vCT.getParameters());
  }
}


