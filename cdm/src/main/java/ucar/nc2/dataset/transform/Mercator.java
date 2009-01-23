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

import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;

/**
 * Create a Mercator Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class Mercator extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return "mercator";
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    double par = readAttributeDouble( ctv, "standard_parallel", Double.NaN);
    double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin", Double.NaN);
    double false_easting = readAttributeDouble(ctv, "false_easting", 0.0);
    double false_northing = readAttributeDouble(ctv, "false_northing", 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = getFalseEastingScaleFactor(ds, ctv);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    ucar.unidata.geoloc.projection.Mercator proj =
            new ucar.unidata.geoloc.projection.Mercator( lon0, par, false_easting, false_northing);
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
