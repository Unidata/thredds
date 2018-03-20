/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.vertical.AtmosSigma;
import ucar.unidata.util.Parameter;

/**
 * Create a atmosphere_sigma_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 *  *
 * @author caron
 */
public class CFSigma extends AbstractTransformBuilder implements VertTransformBuilderIF {
  private String sigma="", ps="", ptop="";

  public String getTransformName() {
    return VerticalCT.Type.Sigma.name();
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms) return null;

     // parse the formula string
    String[] values = parseFormula(formula_terms, "sigma ps ptop");
    if (values == null) return null;

    sigma = values[0];
    ps = values[1];
    ptop = values[2];

    VerticalCT rs = new VerticalCT("AtmSigma_Transform_"+ctv.getName(), getTransformName(), VerticalCT.Type.Sigma, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));

    rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

    if (!addParameter( rs, AtmosSigma.PS, ds, ps)) return null;
    if (!addParameter( rs, AtmosSigma.SIGMA, ds, sigma)) return null;
    if (!addParameter( rs, AtmosSigma.PTOP, ds, ptop)) return null;

    return rs;
  }

  public String toString() { 
    return "Sigma:" + "sigma:"+sigma + " ps:"+ps + " ptop:"+ptop;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new AtmosSigma(ds, timeDim, vCT.getParameters());
  }
}

