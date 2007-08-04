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
import ucar.nc2.Attribute;
import ucar.unidata.geoloc.projection.UtmProjection;

/**
 * Create a UTM Projection from the information in the Coordinate Transform Variable.
 *  *
 * @author caron
 */
public class UTM extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return "UTM";
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    int zone = (int) readAttributeDouble( ctv, "utm_zone_number");
    boolean isNorth = zone > 0;
    zone = Math.abs(zone);

    Attribute a;
    double axis = 0.0, f = 0.0;
    if (null != (a = ctv.findAttribute( "semimajor_axis")))
      axis = a.getNumericValue().doubleValue();
    if (null != (a = ctv.findAttribute( "inverse_flattening ")))
      f = a.getNumericValue().doubleValue();

    // double a, double f, int zone, boolean isNorth
    UtmProjection proj = (axis != 0.0) ? new UtmProjection(axis, f, zone, isNorth) : new UtmProjection(zone, isNorth);
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
