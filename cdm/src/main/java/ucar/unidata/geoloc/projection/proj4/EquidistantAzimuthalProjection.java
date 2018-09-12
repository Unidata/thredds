/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 */
package ucar.unidata.geoloc.projection.proj4;

import java.util.Objects;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * AzimuthalEquidistant Projection.
 * Port from proj4.
 */
public class EquidistantAzimuthalProjection extends ProjectionImpl {
  public final static int NORTH_POLE = 1;
  public final static int SOUTH_POLE = 2;
  public final static int EQUATOR = 3;
  public final static int OBLIQUE = 4;

  private final static double TOL = 1.e-8;

  private double lat0, lon0; // degrees
  private double projectionLatitude, projectionLongitude; // radians
  private double falseEasting;
  private double falseNorthing;
  private Earth earth;
  private double e, es, one_es;
  private double totalScale;

  private int mode;
  private double[] en;
  private double N1;
  private double Mp;
  private double He;
  private double G;
  private double sinphi0, cosphi0;

  public EquidistantAzimuthalProjection() {
    this(90, 0, 0, 0, new Earth());
  }

  public EquidistantAzimuthalProjection(double lat0, double lon0, double falseEasting, double falseNorthing, Earth earth) {
    super("EquidistantAzimuthalProjection", false);

    Objects.requireNonNull(earth, "Azimuthal equidistant constructor requires non-null Earth");

    this.lat0 = lat0;
    this.lon0 = lon0;

    this.projectionLatitude = Math.toRadians(lat0);
    this.projectionLongitude = Math.toRadians(lon0);

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
    this.e = earth.getEccentricity();
    this.es = earth.getEccentricitySquared();
    this.one_es = 1 - es;
    this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    addParameter(CF.GRID_MAPPING_NAME, CF.AZIMUTHAL_EQUIDISTANT);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());

    initialize();
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result = new EquidistantAzimuthalProjection(lat0, lon0, falseEasting, falseNorthing, earth);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  private void initialize() {
    if (Math.abs(Math.abs(projectionLatitude) - MapMath.HALFPI) < MapMath.EPS10) {
      mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
      sinphi0 = projectionLatitude < 0. ? -1. : 1.;
      cosphi0 = 0.;

    } else if (Math.abs(projectionLatitude) < MapMath.EPS10) {
      mode = EQUATOR;
      sinphi0 = 0.;
      cosphi0 = 1.;

    } else {
      mode = OBLIQUE;
      sinphi0 = Math.sin(projectionLatitude);
      cosphi0 = Math.cos(projectionLatitude);
    }

    if (!earth.isSpherical()) {
      en = MapMath.enfn(es);
      switch (mode) {
        case NORTH_POLE:
          Mp = MapMath.mlfn(MapMath.HALFPI, 1., 0., en);
          break;
        case SOUTH_POLE:
          Mp = MapMath.mlfn(-MapMath.HALFPI, -1., 0., en);
          break;
        case EQUATOR:
        case OBLIQUE:
          N1 = 1. / Math.sqrt(1. - es * sinphi0 * sinphi0);
          G = sinphi0 * (He = e / Math.sqrt(one_es));
          He *= cosphi0;
          break;
      }
    }
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl xy) {
    double lam = Math.toRadians(latlon.getLongitude() - lon0);
    double phi = Math.toRadians(latlon.getLatitude());

    if (earth.isSpherical()) {
      double coslam, cosphi, sinphi;

      sinphi = Math.sin(phi);
      cosphi = Math.cos(phi);
      coslam = Math.cos(lam);
      switch (mode) {
        case EQUATOR:
        case OBLIQUE:
          if (mode == EQUATOR)
            xy.setY(cosphi * coslam);
          else
            xy.setY(sinphi0 * sinphi + cosphi0 * cosphi * coslam);

          if (Math.abs(Math.abs(xy.getY()) - 1.) < TOL) {
            if (xy.getY() < 0.)
              throw new IllegalStateException();
            else
              xy.setLocation(0, 0);

          } else {
            double y = Math.acos(xy.getY());
            y /= Math.sin(y);
            double x = y * cosphi * Math.sin(lam);
            y *= (mode == EQUATOR) ? sinphi : cosphi0 * sinphi - sinphi0 * cosphi * coslam;
            xy.setLocation(x, y);
          }
          break;

        case NORTH_POLE:
          phi = -phi;
          coslam = -coslam;

        case SOUTH_POLE:
          if (Math.abs(phi - MapMath.HALFPI) < MapMath.EPS10)
            throw new IllegalStateException();
          double y = (MapMath.HALFPI + phi);
          double x = y * Math.sin(lam);
          y *= coslam;
          xy.setLocation(x, y);
          break;
      }

    } else {
      double coslam, cosphi, sinphi, rho, s, H, H2, c, Az, t, ct, st, cA, sA;

      coslam = Math.cos(lam);
      cosphi = Math.cos(phi);
      sinphi = Math.sin(phi);
      switch (mode) {
        case NORTH_POLE:
          coslam = -coslam;
          //coverity[missing_break]
        case SOUTH_POLE:
          double x = (rho = Math.abs(Mp - MapMath.mlfn(phi, sinphi, cosphi, en))) * Math.sin(lam);
          double y = rho * coslam;
          xy.setLocation(x, y);
          break;
        case EQUATOR:
        case OBLIQUE:
          if (Math.abs(lam) < MapMath.EPS10 && Math.abs(phi - projectionLatitude) < MapMath.EPS10) {
            xy.setLocation(0, 0);
            break;
          }
          t = Math.atan2(one_es * sinphi + es * N1 * sinphi0 * Math.sqrt(1. - es * sinphi * sinphi), cosphi);
          ct = Math.cos(t);
          st = Math.sin(t);
          Az = Math.atan2(Math.sin(lam) * ct, cosphi0 * st - sinphi0 * coslam * ct);
          cA = Math.cos(Az);
          sA = Math.sin(Az);
          s = MapMath.asin(Math.abs(sA) < TOL ?
                  (cosphi0 * st - sinphi0 * coslam * ct) / cA :
                  Math.sin(lam) * ct / sA);
          H = He * cA;
          H2 = H * H;
          c = N1 * s * (1. + s * s * (-H2 * (1. - H2) / 6. +
                  s * (G * H * (1. - 2. * H2 * H2) / 8. +
                          s * ((H2 * (4. - 7. * H2) - 3. * G * G * (1. - 7. * H2)) /
                                  120. - s * G * H / 48.))));
          xy.setLocation(c * sA, c * cA);
          break;
      }
    }

    xy.setLocation(totalScale * xy.getX() + falseEasting, totalScale * xy.getY() + falseNorthing);
    return xy;
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl lp) {
    double x = (ppt.getX() - falseEasting) / totalScale;  // assumes cartesion coords in km
    double y = (ppt.getY() - falseNorthing) / totalScale;

    if (earth.isSpherical()) {
      double cosc, c_rh, sinc;

      if ((c_rh = MapMath.distance(x, y)) > Math.PI) {
        if (c_rh - MapMath.EPS10 > Math.PI)
          throw new IllegalStateException();
        c_rh = Math.PI;

      } else if (c_rh < MapMath.EPS10) {
        lp.setLatitude(lat0);
        lp.setLongitude(0.0);
        return lp;
      }
      if (mode == OBLIQUE || mode == EQUATOR) {
        sinc = Math.sin(c_rh);
        cosc = Math.cos(c_rh);
        if (mode == EQUATOR) {
          lp.setLatitude(Math.toDegrees(MapMath.asin(y * sinc / c_rh)));
          x *= sinc;
          y = cosc * c_rh;

        } else {
          lp.setLatitude(Math.toDegrees(MapMath.asin(cosc * sinphi0 + y * sinc * cosphi0 / c_rh)));
          y = (cosc - sinphi0 * MapMath.sind(lp.getLatitude())) * c_rh;
          x *= sinc * cosphi0;
        }
        lp.setLongitude(Math.toDegrees(y == 0. ? 0. : Math.atan2(x, y)));

      } else if (mode == NORTH_POLE) {
        lp.setLatitude(Math.toDegrees(MapMath.HALFPI - c_rh));
        lp.setLongitude(Math.toDegrees(Math.atan2(x, -y)));

      } else {
        lp.setLatitude(Math.toDegrees(c_rh - MapMath.HALFPI));
        lp.setLongitude(Math.toDegrees(Math.atan2(x, y)));
      }

    } else {
      double c, Az, cosAz, A, B, D, E, F, psi, t;

      if ((c = MapMath.distance(x, y)) < MapMath.EPS10) {
        lp.setLatitude(lat0);
        lp.setLongitude(0.0);
        return (lp);
      }

      if (mode == OBLIQUE || mode == EQUATOR) {
        cosAz = Math.cos(Az = Math.atan2(x, y));
        t = cosphi0 * cosAz;
        B = es * t / one_es;
        A = -B * t;
        B *= 3. * (1. - A) * sinphi0;
        D = c / N1;
        E = D * (1. - D * D * (A * (1. + A) / 6. + B * (1. + 3. * A) * D / 24.));
        F = 1. - E * E * (A / 2. + B * E / 6.);
        psi = MapMath.asin(sinphi0 * Math.cos(E) + t * Math.sin(E));
        lp.setLongitude(Math.toDegrees(MapMath.asin(Math.sin(Az) * Math.sin(E) / Math.cos(psi))));
        if ((t = Math.abs(psi)) < MapMath.EPS10)
          lp.setLatitude(0.0);
        else if (Math.abs(t - MapMath.HALFPI) < 0.)
          lp.setLatitude(Math.toDegrees(MapMath.HALFPI));
        else
          lp.setLatitude(Math.toDegrees(Math.atan((1. - es * F * sinphi0 / Math.sin(psi)) * Math.tan(psi) / one_es)));
      } else {
        lp.setLatitude(Math.toDegrees(MapMath.inv_mlfn(mode == NORTH_POLE ? Mp - c : Mp + c, es, en)));
        lp.setLongitude(Math.toDegrees(Math.atan2(x, mode == NORTH_POLE ? -y : y)));
      }
    }

    lp.setLongitude(lp.getLongitude() + lon0);
    return lp;
  }

  @Override
  public String paramsToString() {
    return null;
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EquidistantAzimuthalProjection that = (EquidistantAzimuthalProjection) o;

    if (Double.compare(that.falseEasting, falseEasting) != 0) return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0) return false;
    if (Double.compare(that.projectionLatitude, projectionLatitude) != 0) return false;
    if (Double.compare(that.projectionLongitude, projectionLongitude) != 0) return false;
    if (earth != null ? !earth.equals(that.earth) : that.earth != null) return false;
    if ((defaultMapArea == null) != (that.defaultMapArea == null)) return false; // common case is that these are null
    if (defaultMapArea != null && !that.defaultMapArea.equals(defaultMapArea)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = projectionLatitude != +0.0d ? Double.doubleToLongBits(projectionLatitude) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = projectionLongitude != +0.0d ? Double.doubleToLongBits(projectionLongitude) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (earth != null ? earth.hashCode() : 0);
    return result;
  }
}

