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

import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.Earth;

/**
 * Create a Polar Stereographic Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class PolarStereographic extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return CF.POLAR_STEREOGRAPHIC;
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    double lon0 = readAttributeDouble( ctv, CF.STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE, Double.NaN);
    if (Double.isNaN(lon0))
      lon0 = readAttributeDouble( ctv, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    if (Double.isNaN(lon0))
      throw new IllegalArgumentException("No longitude parameter");

    double lat0 = readAttributeDouble( ctv, CF.LATITUDE_OF_PROJECTION_ORIGIN, 90.0);
    double latD = 60.0;

    double scale = readAttributeDouble( ctv, CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, Double.NaN);
    if (Double.isNaN(scale)) {
      double stdpar = readAttributeDouble( ctv, CF.STANDARD_PARALLEL, Double.NaN);
      if (!Double.isNaN(stdpar)) {
        // caclulate scale snyder (21-7)
        // k = 2 * k0/(1 +/- sin stdpar)
        // then to make scale = 1 at stdpar, k0 = (1 +/- sin(stdpar))/2
        // double sin = Math.sin( Math.toRadians( stdpar));
        // scale = (lat0 > 0) ? (1.0 + sin)/2 : (1.0 - sin)/2;

        double sin = Math.abs(Math.sin( Math.toRadians( Math.abs(stdpar))));
        scale = (1.0 + sin)/2;
        latD = stdpar;

      } else {
        scale = 0.9330127018922193;
      }
    } else {
      // given the scale, calculate stdpar
      // k0 = (1 +/- sin(stdpar))/2
      // asin(2 * k0  - 1) =  stdpar)
      double temp = 2 * scale -1;
      latD = Math.toDegrees( Math.asin(temp));
    }
    double false_easting = readAttributeDouble(ctv, CF.FALSE_EASTING, 0.0);
    double false_northing = readAttributeDouble(ctv, CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = getFalseEastingScaleFactor(ds, ctv);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double earth_radius = getEarthRadiusInKm(ctv);
    double semi_major_axis = readAttributeDouble(ctv, CF.SEMI_MAJOR_AXIS, Double.NaN); // meters
    double semi_minor_axis = readAttributeDouble(ctv, CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = readAttributeDouble(ctv, CF.INVERSE_FLATTENING, 0.0);

    ucar.unidata.geoloc.ProjectionImpl proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection(lat0, lon0, scale, latD, false_easting, false_northing, earth);
    } else {
      proj = new ucar.unidata.geoloc.projection.Stereographic( lat0, lon0, scale, false_easting, false_northing, earth_radius);
    }
    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }

    public static void main(String arg[]) {
      double stdpar = 70;
      double sin = Math.abs(Math.sin( Math.toRadians( stdpar)));
      double scale = (1.0 + sin)/2;
      System.out.printf("stdpar = %f has scale = %f %n",stdpar, scale );
    }
}
