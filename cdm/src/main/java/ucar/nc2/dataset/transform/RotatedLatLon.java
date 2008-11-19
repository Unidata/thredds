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
 * Create a Rotated LatLon Projection from the information in the Coordinate Transform Variable.
 *
 * @author kambic
 */
public class RotatedLatLon extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return "rotated_lat_lon";
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
//      addParameter(ATTR_NAME, "rotated_lat_lon");
//      addParameter("grid_south_pole_latitude", southPoleLat);
//      addParameter("grid_south_pole_longitude", southPoleLon);
//      addParameter("grid_south_pole_angle", southPoleAngle);
    double lon = readAttributeDouble( ctv, "grid_south_pole_longitude");
    double lat = readAttributeDouble( ctv, "grid_south_pole_latitude");
    double angle = readAttributeDouble( ctv, "grid_south_pole_latitude");

    ucar.unidata.geoloc.projection.RotatedLatLon proj = new ucar.unidata.geoloc.projection.RotatedLatLon( lat, lon, angle);
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }

}


