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

import ucar.unidata.util.Parameter;
import ucar.unidata.geoloc.vertical.WRFEta;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;

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

    if (cs.getXaxis() != null)
    ct.addParameter(new Parameter(WRFEta.IsStaggeredX, ""+isStaggered(cs.getXaxis())));
    else
      ct.addParameter(new Parameter(WRFEta.IsStaggeredX, ""+isStaggered2(cs.getLonAxis(), 1)));

    if (cs.getYaxis() != null)
    ct.addParameter(new Parameter(WRFEta.IsStaggeredY, ""+isStaggered(cs.getYaxis())));
    else
      ct.addParameter(new Parameter(WRFEta.IsStaggeredY, ""+isStaggered2(cs.getLonAxis(), 0)));

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

  private boolean isStaggered2(CoordinateAxis axis, int dimIndex) {
  	if (axis == null) return false;
  	Dimension dim = axis.getDimension(dimIndex);
  	if (dim == null) return false;
  	if (dim.getName().endsWith("stag")) return true;
  	return false;
}

}

