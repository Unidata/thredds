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

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.Earth;

/**
 * VerticalPerspectiveView projection.
 *
 * @author caron
 */
public class VerticalPerspective extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return "vertical_perspective";
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {

    double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin");
    double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
    double distance = readAttributeDouble( ctv, "height_above_earth");
    if (Double.isNaN(lon0) || Double.isNaN(lat0) || Double.isNaN(distance))
      throw new IllegalArgumentException("Vertical Perspective must have longitude_of_projection_origin, latitude_of_projection_origin, height_above_earth attributes");

    double east = readAttributeDouble( ctv, "false_easting");
    if (Double.isNaN(east)) east = 0.0;
    double north = readAttributeDouble( ctv, "false_northing");
    if (Double.isNaN(north)) north = 0.0;
    double earthRadius = readAttributeDouble( ctv, "earth_radius");
    if (Double.isNaN(earthRadius)) earthRadius = Earth.getRadius() / 1000.0; // km

    ucar.unidata.geoloc.projection.VerticalPerspectiveView proj =
            new ucar.unidata.geoloc.projection.VerticalPerspectiveView(lat0, lon0, earthRadius, distance, east, north);
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
