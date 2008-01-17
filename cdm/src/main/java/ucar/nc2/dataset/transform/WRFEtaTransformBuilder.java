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

import ucar.unidata.util.Parameter;
import ucar.unidata.geoloc.vertical.WRFEta;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;

/**
 *
 * @author caron
 */
public class WRFEtaTransformBuilder extends AbstractCoordTransBuilder {
  private CoordinateSystem cs;

  public WRFEtaTransformBuilder() {}

  public WRFEtaTransformBuilder(CoordinateSystem cs) {
    this.cs = cs;
  }

  public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable v) {
    VerticalCT.Type type = VerticalCT.Type.WRFEta;
    VerticalCT ct = new VerticalCT(type.toString(), getTransformName(), type, this);

    ct.addParameter(new Parameter("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
    ct.addParameter(new Parameter(WRFEta.PerturbationGeopotentialVariable, "PH"));
    ct.addParameter(new Parameter(WRFEta.BaseGeopotentialVariable, "PHB"));
    ct.addParameter(new Parameter("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
    ct.addParameter(new Parameter(WRFEta.PerturbationPressureVariable, "P"));
    ct.addParameter(new Parameter(WRFEta.BasePressureVariable, "PB"));
    ct.addParameter(new Parameter(WRFEta.IsStaggeredX, ""+isStaggered(cs.getXaxis())));
    ct.addParameter(new Parameter(WRFEta.IsStaggeredY, ""+isStaggered(cs.getYaxis())));
    ct.addParameter(new Parameter(WRFEta.IsStaggeredZ, ""+isStaggered(cs.getZaxis())));
    ct.addParameter(new Parameter("eta", ""+cs.getZaxis().getName()));

    return ct;
  }

  public String getTransformName() {
    return "WRF_Eta";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new WRFEta(ds, timeDim, vCT.getParameters());
  }

  private boolean isStaggered(CoordinateAxis axis) {
  	if (axis == null) return false;
  	String name = axis.getName();
  	if (name == null) return false;
  	if (name.endsWith("stag")) return true;
  	return false;
  }

}

