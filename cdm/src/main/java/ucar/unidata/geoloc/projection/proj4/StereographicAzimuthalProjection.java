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

import java.util.Formatter;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * taken from the USGS PROJ package.
 *
 * @author Heiko.Klein@met.no
 */
public class StereographicAzimuthalProjection extends ProjectionImpl {
  // projection parameters
  double projectionLatitude, projectionLongitude; // origin in radian
  double n; // Math.sin(projectionLatitude)
  double scaleFactor, trueScaleLatitude;  // scale or trueScale in radian
  double falseEasting, falseNorthing; // km

  // earth shape
  private Earth earth;
  private double e;   // earth.getEccentricity
  private double totalScale; // scale to convert cartesian coords in km

  private final static int NORTH_POLE = 1;
  private final static int SOUTH_POLE = 2;
  private final static int EQUATOR = 3;
  private final static int OBLIQUE = 4;

  private final static double TOL = 1.e-8;

  private double akm1, sinphi0, cosphi0;
  private int mode;

  public StereographicAzimuthalProjection() {  // polar stereographic with true longitude at 60 deg
    this(90.0, 0.0, 0.9330127018922193, 60., 0, 0, new Earth());
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt         tangent point of projection, also origin of
   *                     projection coord system, in degree
   * @param lont         tangent point of projection, also origin of
   *                     projection coord system, in degree
   * @param trueScaleLat latitude in degree where scale is scale
   * @param scale        scale factor at tangent point, "normally 1.0 but may be reduced"
   */
  public StereographicAzimuthalProjection(double latt, double lont, double scale, double trueScaleLat, double false_easting, double false_northing, Earth earth) {
    super("StereographicAzimuthalProjection", false);

    projectionLatitude = Math.toRadians(latt);
    n = Math.abs(Math.sin(projectionLatitude));
    projectionLongitude = Math.toRadians(lont);
    trueScaleLatitude = Math.toRadians(trueScaleLat);
    scaleFactor = Math.abs(scale);
    falseEasting = false_easting;
    falseNorthing = false_northing;

    // earth figure
    this.earth = earth;
    this.e = earth.getEccentricity();
    this.totalScale = earth.getMajor() * 0.001; // scale factor for cartesion coords in km.     // issue if semimajor and semiminor axis defined in dataset?
    initialize();

    // parameters
    addParameter(CF.GRID_MAPPING_NAME, CF.STEREOGRAPHIC);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lont);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, latt);
    addParameter(CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, scale);
    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }
    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());         // seems correct for case where dataset has semimajor axis information, but where is semiminor?
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());     // this gets us the semiminor axis from the semimajor (semimajor - flattening*semimajor)

    //System.err.println(paramsToString());

  }

  private void initialize() {
    double t;

    if (Math.abs((t = Math.abs(projectionLatitude)) - MapMath.HALFPI) < MapMath.EPS10)
      mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
    else
      mode = t > MapMath.EPS10 ? OBLIQUE : EQUATOR;
    trueScaleLatitude = Math.abs(trueScaleLatitude);
    if (earth.isSpherical()) { // sphere
      switch (mode) {
        case OBLIQUE:
          sinphi0 = Math.sin(projectionLatitude);
          cosphi0 = Math.cos(projectionLatitude);
        case EQUATOR:
          akm1 = 2. * scaleFactor;
          break;
        case SOUTH_POLE:
        case NORTH_POLE:
          akm1 = Math.abs(trueScaleLatitude - MapMath.HALFPI) >= MapMath.EPS10 ?
                  Math.cos(trueScaleLatitude) / Math.tan(MapMath.QUARTERPI - .5 * trueScaleLatitude) :
                  2. * scaleFactor;
          break;
      }
    } else { // ellipsoid
      double X;

      switch (mode) {
        case NORTH_POLE:
        case SOUTH_POLE:
          if (Math.abs(trueScaleLatitude - MapMath.HALFPI) < MapMath.EPS10)
            akm1 = 2. * scaleFactor /
                    Math.sqrt(Math.pow(1 + e, 1 + e) * Math.pow(1 - e, 1 - e));
          else {
            akm1 = Math.cos(trueScaleLatitude) /
                    MapMath.tsfn(trueScaleLatitude, t = Math.sin(trueScaleLatitude), e);
            t *= e;
            akm1 /= Math.sqrt(1. - t * t);
          }
          break;
        case EQUATOR:
          akm1 = 2. * scaleFactor;
          break;
        case OBLIQUE:
          t = Math.sin(projectionLatitude);
          X = 2. * Math.atan(ssfn(projectionLatitude, t, e)) - MapMath.HALFPI;
          t *= e;
          akm1 = 2. * scaleFactor * Math.cos(projectionLatitude) / Math.sqrt(1. - t * t);
          sinphi0 = Math.sin(X);
          cosphi0 = Math.cos(X);
          break;
      }
    }
  }

  public ProjectionPoint project(double lam, double phi, ProjectionPointImpl xy) {
    double coslam = Math.cos(lam);
    double sinlam = Math.sin(lam);
    double sinphi = Math.sin(phi);

    if (earth.isSpherical()) { // sphere
      double cosphi = Math.cos(phi);

      switch (mode) {
        case EQUATOR:
          double y = 1. + cosphi * coslam;
          if (y <= MapMath.EPS10)
            throw new RuntimeException("I");
          double x = (y = akm1 / y) * cosphi * sinlam;
          y *= sinphi;
          xy.setLocation(x, y);
          break;
        case OBLIQUE:
          y = 1. + sinphi0 * sinphi + cosphi0 * cosphi * coslam;
          if (y <= MapMath.EPS10)
            throw new RuntimeException("I");
          x = (y = akm1 / y) * cosphi * sinlam;
          y *= cosphi0 * sinphi - sinphi0 * cosphi * coslam;
          xy.setLocation(x, y);
          break;
        case NORTH_POLE:
          coslam = -coslam;
          phi = -phi;
        //coverity[missing_break]
        case SOUTH_POLE:
          if (Math.abs(phi - MapMath.HALFPI) < TOL)
            throw new RuntimeException("I");
          y = akm1 * Math.tan(MapMath.QUARTERPI + .5 * phi);
          x = sinlam * y;
          y *= coslam;
          xy.setLocation(x, y);
          break;
      }
    } else { // ellipsoid
      double sinX = 0, cosX = 0, X, A;

      if (mode == OBLIQUE || mode == EQUATOR) {
        sinX = Math.sin(X = 2. * Math.atan(ssfn(phi, sinphi, e)) - MapMath.HALFPI);
        cosX = Math.cos(X);
      }
      switch (mode) {
        case OBLIQUE:
          A = akm1 / (cosphi0 * (1. + sinphi0 * sinX + cosphi0 * cosX * coslam));
          double y = A * (cosphi0 * sinX - sinphi0 * cosX * coslam);
          double x = A * cosX;
          xy.setLocation(x, y);
          break;
        case EQUATOR:
          A = 2. * akm1 / (1. + cosX * coslam);
          y = A * sinX;
          x = A * cosX;
          xy.setLocation(x, y);
          break;
        case SOUTH_POLE:
          phi = -phi;
          coslam = -coslam;
          sinphi = -sinphi;
        //coverity[missing_break]
        case NORTH_POLE:
          x = akm1 * MapMath.tsfn(phi, sinphi, e);
          y = -x * coslam;
          xy.setLocation(x, y);
          break;
      }
      xy.setX(xy.getX() * sinlam);
    }
    return xy;
  }

  public ProjectionPoint projectInverse(double x, double y, ProjectionPointImpl lp) {
    double lpx = 0.;
    double lpy = 0.;

    if (earth.isSpherical()) {
      double c, rh, sinc, cosc;

      sinc = Math.sin(c = 2. * Math.atan((rh = MapMath.distance(x, y)) / akm1));
      cosc = Math.cos(c);
      switch (mode) {
        case EQUATOR:
          if (Math.abs(rh) <= MapMath.EPS10)
            lpy = 0.;
          else
            lpy = Math.asin(y * sinc / rh);
          if (cosc != 0. || x != 0.)
            lpx = Math.atan2(x * sinc, cosc * rh);
          lp.setLocation(lpx, lpy);
          break;
        case OBLIQUE:
          if (Math.abs(rh) <= MapMath.EPS10)
            lpy = projectionLatitude;
          else
            lpy = Math.asin(cosc * sinphi0 + y * sinc * cosphi0 / rh);
          if ((c = cosc - sinphi0 * Math.sin(lpy)) != 0. || x != 0.)
            lpx = Math.atan2(x * sinc * cosphi0, c * rh);
          lp.setLocation(lpx, lpy);
          break;
        case NORTH_POLE:
          y = -y;
        case SOUTH_POLE:
          if (Math.abs(rh) <= MapMath.EPS10)
            lpy = projectionLatitude;
          else
            lpy = Math.asin(mode == SOUTH_POLE ? -cosc : cosc);
          lpx = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
          lp.setLocation(lpx, lpy);
          break;
      }
    } else {
      double cosphi, sinphi, tp, phi_l, rho, halfe, halfpi;

      rho = MapMath.distance(x, y);
      switch (mode) {
        case NORTH_POLE:
          y = -y;
        case SOUTH_POLE:
          phi_l = MapMath.HALFPI - 2. * Math.atan(tp = -rho / akm1);
          halfpi = -MapMath.HALFPI;
          halfe = -.5 * e;
          break;
        case OBLIQUE:
        case EQUATOR:
        default:
          cosphi = Math.cos(tp = 2. * Math.atan2(rho * cosphi0, akm1));
          sinphi = Math.sin(tp);
          phi_l = Math.asin(cosphi * sinphi0 + (y * sinphi * cosphi0 / rho));
          tp = Math.tan(.5 * (MapMath.HALFPI + phi_l));
          x *= sinphi;
          y = rho * cosphi0 * cosphi - y * sinphi0 * sinphi;
          halfpi = MapMath.HALFPI;
          halfe = .5 * e;
          break;
      }
      for (int i = 8; i-- != 0; phi_l = lpy) {
        sinphi = e * Math.sin(phi_l);
        lpy = 2. * Math.atan(tp * Math.pow((1. + sinphi) / (1. - sinphi), halfe)) - halfpi;
        if (Math.abs(phi_l - lpy) < MapMath.EPS10) {
          if (mode == SOUTH_POLE)
            lpy = -lpy;
          lpx = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
          lp.setLocation(lpx, lpy);
          return lp;
        }
      }
      throw new RuntimeException("Iteration didn't converge");
    }
    return lp;
  }

  private double ssfn(double phit, double sinphi, double eccen) {
    sinphi *= eccen;
    return Math.tan(.5 * (MapMath.HALFPI + phit)) *
            Math.pow((1. - sinphi) / (1. + sinphi), .5 * eccen);
  }

  @Override
  public String getProjectionTypeLabel() {
    return "Stereographic Azimuthal Ellipsoidal Earth";
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result = new StereographicAzimuthalProjection(Math.toDegrees(projectionLatitude), Math.toDegrees(projectionLongitude),
            scaleFactor, Math.toDegrees(trueScaleLatitude), falseEasting, falseNorthing, earth);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  @Override
  public String paramsToString() {
    Formatter f = new Formatter();
    f.format("origin lat,lon=%f,%f scale,trueScaleLat=%f,%f earth=%s", Math.toDegrees(projectionLatitude),
            Math.toDegrees(projectionLongitude), scaleFactor, Math.toDegrees(trueScaleLatitude), earth);
    return f.toString();
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl destPoint) {
    double fromLat = Math.toRadians(latLon.getLatitude());
    double theta = computeTheta(latLon.getLongitude());

    //System.err.println(Math.toDegrees(theta) + " " + Math.toDegrees(fromLat));
    ProjectionPoint res = project(theta, fromLat, new ProjectionPointImpl());

    destPoint.setLocation(totalScale * res.getX() + falseEasting, totalScale * res.getY() + falseNorthing);
    return destPoint;
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesian coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    ProjectionPointImpl dst = new ProjectionPointImpl();
    projectInverse(fromX, fromY, dst);
    if (dst.getX() < -Math.PI)
      dst.setX(-Math.PI);
    else if (dst.getX() > Math.PI)
      dst.setX(Math.PI);
    if (projectionLongitude != 0)
      dst.setX(MapMath.normalizeLongitude(dst.getX() + projectionLongitude));

    result.setLongitude(Math.toDegrees(dst.getX()));
    result.setLatitude(Math.toDegrees(dst.getY()));
    return result;
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // TODO: not sure what this is, HK
    //       just taken from ucar.unidata.geoloc.projection.Stereographic
    return false;
  }

  @Override
  public boolean equals(Object proj) {
    if (!(proj instanceof StereographicAzimuthalProjection)) {
      return false;
    }
    StereographicAzimuthalProjection oo = (StereographicAzimuthalProjection) proj;
    if ((this.getDefaultMapArea() == null) != (oo.defaultMapArea == null))
      return false; // common case is that these are null
    if (this.getDefaultMapArea() != null && !this.defaultMapArea.equals(oo.defaultMapArea)) return false;

    return ((this.projectionLatitude == oo.projectionLatitude)
            && (this.projectionLongitude == oo.projectionLongitude)
            && (this.scaleFactor == oo.scaleFactor)
            && (this.trueScaleLatitude == oo.trueScaleLatitude)
            && (this.falseEasting == oo.falseEasting)
            && (this.falseNorthing == oo.falseNorthing)
            && this.earth.equals(oo.earth));
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.projectionLatitude) ^ (Double.doubleToLongBits(this.projectionLatitude) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.projectionLongitude) ^ (Double.doubleToLongBits(this.projectionLongitude) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.scaleFactor) ^ (Double.doubleToLongBits(this.scaleFactor) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.trueScaleLatitude) ^ (Double.doubleToLongBits(this.trueScaleLatitude) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.falseEasting) ^ (Double.doubleToLongBits(this.falseEasting) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.falseNorthing) ^ (Double.doubleToLongBits(this.falseNorthing) >>> 32));
    hash = 67 * hash + (this.earth != null ? this.earth.hashCode() : 0);
    return hash;
  }

  private double computeTheta(double lon) {
    double dlon = LatLonPointImpl.lonNormal(lon - Math.toDegrees(projectionLongitude));
    return n * Math.toRadians(dlon);
  }

  static private void test(ProjectionImpl proj, double[] lat, double[] lon) {
    double[] x = new double[lat.length];
    double[] y = new double[lat.length];
    for (int i = 0; i < lat.length; ++i) {
      LatLonPoint lp = new LatLonPointImpl(lat[i], lon[i]);
      ProjectionPointImpl p = (ProjectionPointImpl) proj.latLonToProj(lp, new ProjectionPointImpl());
      x[i] = p.getX();
      y[i] = p.getY();
    }
    for (int i = 0; i < lat.length; ++i) {
      ProjectionPointImpl p = new ProjectionPointImpl(x[i], y[i]);
      LatLonPointImpl lp = (LatLonPointImpl) proj.projToLatLon(p);
      if ((Math.abs(lp.getLatitude() - lat[i]) > 1e-5) || (Math.abs(lp.getLongitude() - lon[i]) > 1e-5)) {
        if (Math.abs(lp.getLatitude()) > 89.99 && (Math.abs(lp.getLatitude() - lat[i]) < 1e-5)) {
          // ignore longitude singularities at poles
        } else {
          System.err.print("ERROR:");
        }
      }
      System.out.println("reverse:" + p.getX() + ", " + p.getY() + ": " + lp.getLatitude() + ", " + lp.getLongitude());

    }

  }

  static public void main(String[] args) {
    // test-code
    Earth e = new Earth(6378137., 0, 298.257224);
    StereographicAzimuthalProjection proj = new StereographicAzimuthalProjection(90., 0., 0.93306907, 90., 0., 0., e);

    double[] lat = {60., 90., 60.};
    double[] lon = {0., 0., 10.};
    test(proj, lat, lon);

    proj = new StereographicAzimuthalProjection(90., -45., 0.96985819, 90., 0., 0., e);
    test(proj, lat, lon);

    // southpole
    proj = new StereographicAzimuthalProjection(-90., 0., -1, -70., 0., 0., e);

    double[] latS = {-60., -90., -60.};
    double[] lonS = {0., 0., 10.};
    test(proj, latS, lonS);


  }
}