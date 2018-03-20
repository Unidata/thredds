/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.unidata.geoloc.vertical.WRFEta;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.unidata.util.Parameter;

/**
 * @author caron
 */
public class WRFEtaTransformBuilder extends AbstractTransformBuilder implements VertTransformBuilderIF {
  private CoordinateSystem cs;

  public WRFEtaTransformBuilder(CoordinateSystem cs) {
    this.cs = cs;
  }

  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer v) {
    VerticalCT.Type type = VerticalCT.Type.WRFEta;
    VerticalCT ct = new VerticalCT(type.toString(), getTransformName(), type, this);

    ct.addParameter(new Parameter("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
    ct.addParameter(new Parameter(WRFEta.PerturbationGeopotentialVariable, "PH"));
    ct.addParameter(new Parameter(WRFEta.BaseGeopotentialVariable, "PHB"));
    ct.addParameter(new Parameter("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
    ct.addParameter(new Parameter(WRFEta.PerturbationPressureVariable, "P"));
    ct.addParameter(new Parameter(WRFEta.BasePressureVariable, "PB"));

    if (cs.getXaxis() != null)
      ct.addParameter(new Parameter(WRFEta.IsStaggeredX, "" + isStaggered(cs.getXaxis())));
    else
      ct.addParameter(new Parameter(WRFEta.IsStaggeredX, "" + isStaggered2(cs.getLonAxis(), 1)));

    if (cs.getYaxis() != null)
      ct.addParameter(new Parameter(WRFEta.IsStaggeredY, "" + isStaggered(cs.getYaxis())));
    else
      ct.addParameter(new Parameter(WRFEta.IsStaggeredY, "" + isStaggered2(cs.getLonAxis(), 0)));

    ct.addParameter(new Parameter(WRFEta.IsStaggeredZ, "" + isStaggered(cs.getZaxis())));

    ct.addParameter(new Parameter("eta", "" + cs.getZaxis().getFullName()));

    return ct;
  }

  public String getTransformName() {
    return VerticalCT.Type.WRFEta.name();
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new WRFEta(ds, timeDim, vCT.getParameters());
  }

  private boolean isStaggered(CoordinateAxis axis) {
    if (axis == null) return false;
    String name = axis.getShortName();
    return name != null && name.endsWith("stag");
  }

  private boolean isStaggered2(CoordinateAxis axis, int dimIndex) {
    if (axis == null) return false;
    Dimension dim = axis.getDimension(dimIndex);
    return dim != null && dim.getShortName().endsWith("stag");
  }

}

