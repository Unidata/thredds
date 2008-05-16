/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.unidata.util.Parameter;
import ucar.unidata.geoloc.vertical.AtmosSigma;
import ucar.unidata.geoloc.vertical.AtmosLnPressure;

import java.util.StringTokenizer;

/**
 * implementation for CF vertical coordinate "atmosphere_ln_pressure_coordinate".
 * @author caron
 * @since May 6, 2008
 */
public class VAtmLnPressure extends AbstractCoordTransBuilder {
  private String p0, lev;

  public String getTransformName() {
    return "atmosphere_ln_pressure_coordinate";
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula_terms = getFormula(ds, ctv);
    if (null == formula_terms) return null;

     // parse the formula string
    String[] values = parseFormula(formula_terms, "p0 lev");
    if (values == null) return null;

    p0 = values[0];
    lev = values[1];

    CoordinateTransform rs = new VerticalCT("AtmSigma_Transform_"+ctv.getShortName(), getTransformName(), VerticalCT.Type.Sigma, this);
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


