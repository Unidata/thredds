/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.unidata.geoloc.projection.proj4;

import ucar.unidata.geoloc.*;
import ucar.unidata.util.Parameter;

import java.util.Formatter;


/**
 * Adapted from com.jhlabs.map.proj.AlbersProjection
 *
 * @see "http://www.jhlabs.com/java/maps/proj/index.html"
 * @see "http://trac.osgeo.org/proj/"
 *
 * @author caron
 * @since Oct 8, 2009
 */
public class AlbersEqualAreaEllipse extends ProjectionImpl {
  private final static double EPS10 = 1.e-10;
  private final static double TOL7 = 1.e-7;
  private final static int N_ITER = 15;
  private final static double EPSILON = 1.0e-7;
  private final static double TOL = 1.0e-10;

  // projection parameters
  private double lat0deg, lon0deg; // projection origin, degrees
  private double lat0rad, lon0rad; // projection origin, radians
  private double par1deg, par2deg; // standard parellels, degrees
  private double phi1, phi2; // standard parellels, radians
  private double falseEasting, falseNorthing; // km

  // earth shape
  private Earth earth;
  private double e;   // earth.getEccentricitySquared
  private double es;  // earth.getEccentricitySquared
  private double one_es;  // 1-es
  private double totalScale; // scale to convert cartesian coords in km

  private double ec;
  private double n;
  private double c;
  private double dd;
  private double n2;
  private double rho0;

  // spherical vs ellipsoidal
  private boolean isSpherical;

  /**
   * copy constructor - avoid clone !!
   */
  public ProjectionImpl constructCopy() {
    return new AlbersEqualAreaEllipse(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(),
            getFalseEasting(), getFalseNorthing(), getEarth());
  }

  /**
   * Constructor with default parameters
   */
  public AlbersEqualAreaEllipse() {
    this(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
  }

  /**
   * Construct a AlbersEqualAreaEllipse Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param falseEasting false easting in km
   * @param falseNorthing false easting in km
   * @param earth shape of the earth
   * @throws IllegalArgumentException if Math.abs(par1 + par2) < 1.e-10
   */
  public AlbersEqualAreaEllipse(double lat0, double lon0, double par1, double par2, double falseEasting, double falseNorthing, Earth earth) {
    name = "AlbersEqualAreaEllipse";

    this.lat0deg = lat0;
    this.lon0deg = lon0;

    this.lat0rad = Math.toRadians(lat0);
    this.lon0rad = Math.toRadians(lat0);

    this.par1deg = par1;
    this.par2deg = par2;

    this.phi1 = Math.toRadians(par1);
    this.phi2 = Math.toRadians(par2);

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
		this.e = earth.getEccentricity();
		this.es = earth.getEccentricitySquared();
    this.isSpherical = (e == 0.0);
		this.one_es = 1.0-es;
		this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    precalculate();

    addParameter(ATTR_NAME, "albers_conical_equal_area");
    addParameter("latitude_of_projection_origin", lat0);
    addParameter("longitude_of_central_meridian", lon0);

    if (par2 == par1) {
      addParameter("standard_parallel", par1);
    } else {
      double[] data = new double[2];
      data[0] = par1;
      data[1] = par2;
      addParameter(new Parameter("standard_parallel", data));
    }

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter("false_easting", falseEasting);
      addParameter("false_northing", falseNorthing);
      addParameter("units", "km");
    }

    addParameter("semi_major_axis", earth.getMajor());
    addParameter("inverse_flattening", 1.0/earth.getFlattening());

  }

  private void precalculate() {

		if (Math.abs(phi1 + phi2) < EPS10)
			throw new IllegalArgumentException("Math.abs(par1 + par2) < 1.e-10");

		double sinphi = Math.sin(phi1);
    n = sinphi;
		double cosphi = Math.cos(phi1);
		boolean secant = Math.abs(phi1 - phi2) >= EPS10;

		if (!isSpherical) { // not spherical  LOOK CHANGE

      // 		if (!(P->en = pj_enfn(P->es))) E_ERROR_0; ??
      if ((MapMath.enfn(es)) == null)
				throw new IllegalArgumentException("0");

			double m1 = MapMath.msfn(sinphi, cosphi, es);
			double ml1 = MapMath.qsfn(sinphi, e, one_es);
			if (secant) { /* secant cone */
				sinphi = Math.sin(phi2);
				cosphi = Math.cos(phi2);
				double m2 = MapMath.msfn(sinphi, cosphi, es);
				double ml2 = MapMath.qsfn(sinphi, e, one_es);
				n = (m1 * m1 - m2 * m2) / (ml2 - ml1);
			}

			ec = 1. - .5 * one_es * Math.log((1. - e) / (1. + e)) / e;
			c = m1 * m1 + n * ml1;
			dd = 1. / n;
			rho0 = dd * Math.sqrt(c - n * MapMath.qsfn(Math.sin(lat0rad), e, one_es));

		} else { // sphere
			if (secant) n = .5 * (n + Math.sin(phi2));
			n2 = n + n;
			c = cosphi * cosphi + n2 * sinphi;
			dd = 1. / n;
			rho0 = dd * Math.sqrt(c - n2 * Math.sin(lat0rad));
		}
	}

  /**
   * Clone this projection.
   *
   * @return Clone of this
   */
  public Object clone() {
    AlbersEqualAreaEllipse cl = (AlbersEqualAreaEllipse) super.clone();
    cl.earth = this.earth;
    return cl;
  }

  /**
   * Check for equality with the Object in question
   *
   * @param proj object to check
   * @return true if they are equal
   */
  public boolean equals(Object proj) {
    if (!(proj instanceof AlbersEqualAreaEllipse)) {
      return false;
    }

    AlbersEqualAreaEllipse oo = (AlbersEqualAreaEllipse) proj;
    return ((this.getParallelOne() == oo.getParallelOne())
            && (this.getParallelTwo() == oo.getParallelTwo())
            && (this.getOriginLat() == oo.getOriginLat())
            && (this.getOriginLon() == oo.getOriginLon())
            && this.earth.equals(oo.earth));
  }

  // bean properties

  public Earth getEarth() { return earth; }

  /**
   * Get the second standard parallel
   *
   * @return the second standard parallel in degrees
   */
  public double getParallelTwo() {
    return par2deg;
  }

  /**
   * Get the first standard parallel
   *
   * @return the first standard parallel in degrees
   */
  public double getParallelOne() {
    return par1deg;
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLon() {
    return lon0deg;
  }


  /**
   * Get the origin latitude.
   *
   * @return the origin latitude in degrees
   */
  public double getOriginLat() {
    return lat0deg;
  }

     /**
   * Get the false easting, in km.
   *
   * @return the false easting in km
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in km.
   *
   * @return the false northing in km
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

   /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Albers Equal Area Ellipsoidal Earth";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  public String paramsToString() {
    Formatter f = new Formatter();
    f.format("origin lat,lon=%f,%f parellels=%f,%f earth=%s", lat0deg, lon0deg, par1deg, par2deg, earth );
    return f.toString();
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }

    return false;

    /* opposite signed X values, larger then 5000 km  LOOK ????
    return (pt1.getX() * pt2.getX() < 0)
            && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0); */
  }

  /*
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latLon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   *
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    fromLat = Math.toRadians(fromLat);
    fromLon = Math.toRadians(fromLon);
    double rho = computeRho(fromLat);
    double theta = computeTheta(fromLon);

    toX = rho * Math.sin(theta) + falseEasting;
    toY = rho0 - rho * Math.cos(theta) + falseNorthing;

    result.setLocation(toX, toY);
    return result;
  }

  /*
  	public Point2D.Double project(double lplam, double lpphi, Point2D.Double out) {
		double rho;
		if ((rho = c - (!spherical ? n * MapMath.qsfn(Math.sin(lpphi), e, one_es) : n2 * Math.sin(lpphi))) < 0.)
			throw new ProjectionException("F");
		rho = dd * Math.sqrt(rho);
		out.x = rho * Math.sin( lplam *= n );
		out.y = rho0 - rho * Math.cos(lplam);
		return out;
	}
   */

  private double computeTheta(double lon) {
    double dlon = LatLonPointImpl.lonNormal(lon - lon0deg);
    return n * Math.toRadians(dlon);
  }

  // also see Snyder p 101
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
    double fromLat = Math.toRadians(latLon.getLatitude());
    double theta = computeTheta(latLon.getLongitude());

    double term = isSpherical ? n2 * Math.sin(fromLat) : n * MapMath.qsfn(Math.sin(fromLat), e, one_es);
    double rho = c - term;

    if (rho < 0.0)
      throw new RuntimeException("F");

    rho = dd * Math.sqrt(rho);

    double toX = rho * Math.sin(theta);
    double toY = rho0 - rho * Math.cos(theta);

    result.setLocation(totalScale * toX + falseEasting, totalScale * toY + falseNorthing);
    return result;
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   *
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double toLat, toLon;
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double rrho0 = rho0;

    if (n < 0) {
      rrho0 *= -1.0;
      fromX *= -1.0;
      fromY *= -1.0;
    }

    double yd = rrho0 - fromY;
    double rho = Math.sqrt(fromX * fromX + yd * yd);
    double theta = Math.atan2(fromX, yd);
    if (n < 0) {
      rho *= -1.0;
    }
    toLat = Math.toDegrees(Math.asin((C - Math.pow((rho * n / EARTH_RADIUS), 2)) / (2 * n)));

    toLon = Math.toDegrees(theta / n + lon0);

    result.setLatitude(toLat);
    result.setLongitude(toLon);
    return result;
  }


  public Point2D.Double projectInverse(double xyx, double xyy, Point2D.Double out) {
  double rho;
  if ((rho = MapMath.distance(xyx, xyy = rho0 - xyy)) != 0) {
    double lpphi, lplam;
    if (n < 0.) {
      rho = -rho;
      xyx = -xyx;
      xyy = -xyy;
    }
    lpphi =  rho / dd;
    if (!spherical) {
      lpphi = (c - lpphi * lpphi) / n;
      if (Math.abs(ec - Math.abs(lpphi)) > TOL7) {
        if ((lpphi = phi1_(lpphi, e, one_es)) == Double.MAX_VALUE)
          throw new ProjectionException("I");
      } else
        lpphi = lpphi < 0. ? -MapMath.HALFPI : MapMath.HALFPI;
    } else if (Math.abs(out.y = (c - lpphi * lpphi) / n2) <= 1.)
      lpphi = Math.asin(lpphi);
    else
      lpphi = lpphi < 0. ? -MapMath.HALFPI : MapMath.HALFPI;
    lplam = Math.atan2(xyx, xyy) / n;
    out.x = lplam;
    out.y = lpphi;
  } else {
    out.x = 0.;
    out.y = n > 0. ? MapMath.HALFPI : - MapMath.HALFPI;
  }
  return out;
}  */

  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double toLat, toLon;
    double fromX = (world.getX() - falseEasting) / totalScale;  // assumes cartesion coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    fromY = rho0 - fromY;
    double rho = MapMath.distance(fromX, fromY);

    if (rho == 0.0) {
      toLon = 0.0;
      toLat = n > 0.0 ? MapMath.HALFPI : -MapMath.HALFPI;

    } else {
      if (n < 0.0) {
        rho = -rho;
        fromX = -fromX;
        fromY = -fromY;
      }
      double lpphi = rho / dd;

      if (!isSpherical) {
        lpphi = (c - lpphi * lpphi) / n;
        if (Math.abs(ec - Math.abs(lpphi)) > TOL7) {
          if (Math.abs(lpphi) > 2.0)
            throw new IllegalArgumentException("AlbersEqualAreaEllipse x,y="+world);

          lpphi = phi1_(lpphi, e, one_es);
          if (lpphi == Double.MAX_VALUE)
            throw new RuntimeException("I");
        } else {
          lpphi = (lpphi < 0.) ? -MapMath.HALFPI : MapMath.HALFPI;
        }

      } else { // spherical case
        lpphi = (c - lpphi * lpphi) / n2;
        if (Math.abs(lpphi) <= 1.0) {
          lpphi = Math.asin(lpphi);
        } else {
          lpphi = (lpphi < 0.) ? -MapMath.HALFPI : MapMath.HALFPI;
        }
      }

      toLon = Math.atan2(fromX, fromY) / n;
      toLat = lpphi;
    }

    result.setLatitude(Math.toDegrees(toLat));
    result.setLongitude(Math.toDegrees(toLon) + lon0deg);
    return result;
  }

  private static double phi1_(double qs, double Te, double Tone_es) {
    double phi, sinpi, cospi, con, com, dphi;

    phi = Math.asin(.5 * qs);
    if (Te < EPSILON)
      return (phi);

    int countIter = N_ITER;
    do {
      sinpi = Math.sin(phi);
      cospi = Math.cos(phi);
      con = Te * sinpi;
      com = 1. - con * con;
      dphi = .5 * com * com / cospi * (qs / Tone_es -
              sinpi / com + .5 / Te * Math.log((1. - con) /
              (1. + con)));
      phi += dphi;
    } while (Math.abs(dphi) > TOL && --countIter != 0);

    return (countIter != 0 ? phi : Double.MAX_VALUE);
  }


  /*
   proj +inv +proj=aea +lat_0=23.0 +lat_1=29.5 +lat_2=45.5 +a=6378137.0 +rf=298.257222101 +b=6356752.31414 +lon_0=-96.0

Input X,Y:     1730692.593817677      1970917.991173046

results in:

Output Lon,Lat:     -75.649278      39.089117
   *
   */

  private static void toProj(ProjectionImpl p, double lat, double lon) {
    System.out.printf("lon,lat = %f %f%n", lon, lat);
    ProjectionPoint pt = p.latLonToProj(lat, lon);
    System.out.printf("x,y     = %f %f%n", pt.getX(), pt.getY());
    LatLonPoint ll = p.projToLatLon(pt);
    System.out.printf("lon,lat = %f %f%n%n", ll.getLongitude(), ll.getLatitude());
  }

  private static void fromProj(ProjectionImpl p, double x, double y) {
    System.out.printf("x,y     = %f %f%n", x,y);
    LatLonPoint ll = p.projToLatLon(x, y);
    System.out.printf("lon,lat = %f %f%n", ll.getLongitude(), ll.getLatitude());
    ProjectionPoint pt = p.latLonToProj(ll);
    System.out.printf("x,y     = %f %f%n%n", pt.getX(), pt.getY());
  }

  public static void main(String[] args) {
    AlbersEqualAreaEllipse a = new AlbersEqualAreaEllipse(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
    System.out.printf("proj = %s %s%n%n", a.getName(), a.paramsToString());
    //fromProj(a, 1730.692593817677, 1970.917991173046);
    //toProj(a, 39.089117, -75.649278);
    
    fromProj(a, 5747, 13470);

  }

}
