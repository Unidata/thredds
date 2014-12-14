/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.constants.CF;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.Variable;

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

    readStandardParams(ds, ctv);

    double distance = readAttributeDouble(ctv, CF.PERSPECTIVE_POINT_HEIGHT, Double.NaN);
    if (Double.isNaN(distance)) {
        distance = readAttributeDouble(ctv, "height_above_earth", Double.NaN);
    }
    if (Double.isNaN(lon0) || Double.isNaN(lat0) || Double.isNaN(distance))
      throw new IllegalArgumentException("Vertical Perspective must have: " +
              CF.LONGITUDE_OF_PROJECTION_ORIGIN + ", " +
              CF.LATITUDE_OF_PROJECTION_ORIGIN + ", and " +
              CF.PERSPECTIVE_POINT_HEIGHT + "(or height_above_earth) " +
              "attributes");

    // We assume distance comes in 'm' (CF-compliant) and we pass in as 'km'
    ucar.unidata.geoloc.projection.VerticalPerspectiveView proj =
            new ucar.unidata.geoloc.projection.VerticalPerspectiveView(lat0,
                    lon0, earth_radius, distance / 1000., false_easting,
                    false_northing);

    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
