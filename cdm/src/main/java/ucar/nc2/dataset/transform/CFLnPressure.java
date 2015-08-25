/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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


