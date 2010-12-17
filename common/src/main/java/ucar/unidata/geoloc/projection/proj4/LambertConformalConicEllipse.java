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
 * Adapted from com.jhlabs.map.proj.LambertConformalConicProjection
 *
 * @see "http://www.jhlabs.com/java/maps/proj/index.html"
 * @see "http://trac.osgeo.org/proj/"
 *
 * @author caron
 * @since Oct 20, 2009
 */
public class LambertConformalConicEllipse extends ProjectionImpl {
  private final static double TOL = 1.0e-10;

  // projection parameters
  private double lat0deg, lon0deg; // projection origin, degrees
  private double lat0rad, lon0rad; // projection origin, radians
  private double par1deg, par2deg; // standard parellels, degrees
  private double par1rad, par2rad; // standard parellels, radians
  private double falseEasting, falseNorthing; // km

  // earth shape
  private Earth earth;
  private double e;   // earth.getEccentricitySquared
  private double es;  // earth.getEccentricitySquared
  private double totalScale; // scale to convert cartesian coords in km

  private double n;
  private double c;
  private double rho0;

  // spherical vs ellipsoidal
  private boolean isSpherical;

  /**
   * copy constructor - avoid clone !!
   */
  public ProjectionImpl constructCopy() {
    return new LambertConformalConicEllipse(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(),
            getFalseEasting(), getFalseNorthing(), getEarth());
  }

  /**
   * Constructor with default parameters
   */
  public LambertConformalConicEllipse() {
    this(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0           lat origin of the coord. system on the projection plane
   * @param lon0           lon origin of the coord. system on the projection plane
   * @param par1           standard parallel 1
   * @param par2           standard parallel 2
   * @param falseEasting  natural_x_coordinate + false_easting = x coordinate in km
   * @param falseNorthing natural_y_coordinate + false_northing = y coordinate  in km
   * @param earth shape of the earth
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public LambertConformalConicEllipse(double lat0, double lon0, double par1, double par2,
                                      double falseEasting, double falseNorthing, Earth earth) {

    name = "LambertConformalConicEllipse";

    this.lat0deg = lat0;
    this.lon0deg = lon0;

    this.lat0rad = Math.toRadians(lat0);
    this.lon0rad = Math.toRadians(lat0);

    this.par1deg = par1;
    this.par2deg = par2;

    this.par1rad = Math.toRadians(par1);
    this.par2rad = Math.toRadians(par2);

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
		this.e = earth.getEccentricity();
		this.es = earth.getEccentricitySquared();
    this.isSpherical = (e == 0.0);
		this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    initialize();

    addParameter(ATTR_NAME, "lambert_conformal_conic");
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

  private void initialize() {

    if ( par1rad == 0 )
      par1rad = par2rad = lat0rad;

    if (Math.abs(par1rad + par2rad) < TOL)
      throw new IllegalArgumentException("par1rad + par2rad < TOL");

    double sinphi = Math.sin(par1rad);
    double cosphi = Math.cos(par1rad);
    boolean isSecant = Math.abs(par1rad - par2rad) >= TOL;
    n = sinphi;

    if (!isSpherical) {
      double ml1, m1;

      m1 = MapMath.msfn(sinphi, cosphi, es);
      ml1 = MapMath.tsfn(par1rad, sinphi, e);
      if (isSecant) {
        n = Math.log(m1 /
           MapMath.msfn(sinphi = Math.sin(par2rad), Math.cos(par2rad), es));
        n /= Math.log(ml1 / MapMath.tsfn(par2rad, sinphi, e));
      }
      c = (rho0 = m1 * Math.pow(ml1, -n) / n);
      rho0 *= (Math.abs(Math.abs(lat0rad) - MapMath.HALFPI) < TOL) ? 0. :
        Math.pow(MapMath.tsfn(lat0rad, Math.sin(lat0rad), e), n);

    } else {
      if (isSecant)
        n = Math.log(cosphi / Math.cos(par2rad)) /
           Math.log(Math.tan(MapMath.QUARTERPI + .5 * par2rad) /
           Math.tan(MapMath.QUARTERPI + .5 * par1rad));

      c = cosphi * Math.pow(Math.tan(MapMath.QUARTERPI + .5 * par1rad), n) / n;
      rho0 = (Math.abs(Math.abs(lat0rad) - MapMath.HALFPI) < TOL) ? 0. :
        c * Math.pow(Math.tan(MapMath.QUARTERPI + .5 * lat0rad), -n);
    }
  }


  /* private void precalculate() {
    if (Math.abs(lat0 - PI_OVER_2) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal lat0 = 90");
    }
    if (Math.abs(lat0 + PI_OVER_2) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal lat0 = -90");
    }
    if (Math.abs(par1 - 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par1 = 90");
    }
    if (Math.abs(par1 + 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par1 = -90");
    }
    if (Math.abs(par2 - 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par2 = 90");
    }
    if (Math.abs(par2 + 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par2 = -90");
    }

    double par1r = Math.toRadians(this.par1);
    double par2r = Math.toRadians(this.par2);

    double t1 = Math.tan(Math.PI / 4 + par1r / 2);
    double t2 = Math.tan(Math.PI / 4 + par2r / 2);

    if (Math.abs(par2 - par1) < TOLERANCE) {  // single parallel
      n = Math.sin(par1r);
    } else {
      n = Math.log(Math.cos(par1r) / Math.cos(par2r))
          / Math.log(t2 / t1);
    }

    double t1n = Math.pow(t1, n);
    F = Math.cos(par1r) * t1n / n;
    earthRadiusTimesF = EARTH_RADIUS * F;

    double t0n = Math.pow(Math.tan(Math.PI / 4 + lat0 / 2), n);
    rho = EARTH_RADIUS * F / t0n;

    lon0Degrees = Math.toDegrees(lon0);
    // need to know the pole value for crossSeam
    //Point2D pt = latLonToProj( 90.0, 0.0);
    //maxY = pt.getY();
    //System.out.println("LC = " +pt);
  }  */

  /**
   * Clone this projection.
   *
   * @return Clone of this
   */
  public Object clone() {
    LambertConformalConicEllipse cl = (LambertConformalConicEllipse) super.clone();
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
    if (!(proj instanceof LambertConformalConicEllipse))
      return false;

    LambertConformalConicEllipse oo = (LambertConformalConicEllipse) proj;
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
    return "Lambert Conformal Conic Ellipsoidal Earth";
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

  /*
   * Create a WKS string  LOOK NOT DONE
   *
   * @return WKS string
   *
  public String toWKS() {
    Formatter sbuff = new Formatter();
    sbuff.format("PROJCS[\"%s\",", getName());
    if (isSpherical) {
      sbuff.format("GEOGCS[\"Normal Sphere (r=6371007)\",");
      sbuff.format("DATUM[\"unknown\",");
      sbuff.format("SPHEROID[\"sphere\",6371007,0]],");
    } else {
      sbuff.format("GEOGCS[\"WGS 84\",");
      sbuff.format("DATUM[\"WGS_1984\",");
      sbuff.format("SPHEROID[\"WGS 84\",6378137,298.257223563],");
      sbuff.format("TOWGS84[0,0,0,0,0,0,0]],");
    }
    sbuff.format("PRIMEM[\"Greenwich\",0],");
    sbuff.format("UNIT[\"degree\",0.0174532925199433]],");
    sbuff.format("PROJECTION[\"Lambert_Conformal_Conic_1SP\"],");
    sbuff.format("PARAMETER[\"latitude_of_origin\",").append(getOriginLat()).append("],");  // LOOK assumes getOriginLat = getParellel
    sbuff.format("PARAMETER[\"central_meridian\",").append(getOriginLon()).append("],");
    sbuff.format("PARAMETER[\"scale_factor\",1],");
    sbuff.format("PARAMETER[\"false_easting\",").append(falseEasting).append("],");
    sbuff.format("PARAMETER[\"false_northing\",").append(falseNorthing).append("],");

    return sbuff.toString();
  } */

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
  public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                      ProjectionPointImpl result) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    fromLat = Math.toRadians(fromLat);
    double dlon = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
    double theta = n * Math.toRadians(dlon);
    double tn = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
    double r = earthRadiusTimesF / tn;
    toX = r * Math.sin(theta);
    toY = rho - r * Math.cos(theta);

    result.setLocation(toX + falseEasting, toY + falseNorthing);
    return result;
  }

  /*
	public Point2D.Double project(double x, double y, Point2D.Double out) {
		double rho;
		if (Math.abs(Math.abs(y) - MapMath.HALFPI) < 1e-10)
			rho = 0.0;
		else
			rho = c * (spherical ?
			Math.pow(Math.tan(MapMath.QUARTERPI + .5 * y), -n) :
			Math.pow(MapMath.tsfn(y, Math.sin(y), e), n));
		out.x = scaleFactor * (rho * Math.sin(x *= n));
		out.y = scaleFactor * (rho0 - rho * Math.cos(x));
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

		double rho = 0.0;
		if (Math.abs(Math.abs(fromLat) - MapMath.HALFPI) >= TOL) {
			double term;
      if (isSpherical)
        term = Math.pow(Math.tan(MapMath.QUARTERPI + .5 * fromLat), -n);
      else
        term = Math.pow(MapMath.tsfn(fromLat, Math.sin(fromLat), e), n);
			rho = c * term;
    }

		double toX = (rho * Math.sin(theta));
		double toY = (rho0 - rho * Math.cos(theta));

    result.setLocation(totalScale * toX + falseEasting, totalScale * toY + falseNorthing);
    return result;
  }

  /*
   public LatLonPoint projToLatLon(ProjectionPoint world,
                                   LatLonPointImpl result) {
     double toLat, toLon;
     double fromX = world.getX() - falseEasting;
     double fromY = world.getY() - falseNorthing;
     double rhop = rho;

     if (n < 0) {
       rhop *= -1.0;
       fromX *= -1.0;
       fromY *= -1.0;
     }

     double yd = (rhop - fromY);
     double theta = Math.atan2(fromX, yd);
     double r = Math.sqrt(fromX * fromX + yd * yd);
     if (n < 0.0) {
       r *= -1.0;
     }

     toLon = (Math.toDegrees(theta / n + lon0));

     if (Math.abs(r) < TOLERANCE) {
       toLat = ((n < 0.0)
           ? -90.0
           : 90.0);
     } else {
       double rn = Math.pow(EARTH_RADIUS * F / r, 1 / n);
       toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
     }

     result.setLatitude(toLat);
     result.setLongitude(toLon);
     return result;
   }



	public Point2D.Double projectInverse(double x, double y, Point2D.Double out) {
		x /= scaleFactor;
		y /= scaleFactor;
		double rho = MapMath.distance(x, y = rho0 - y);
		if (rho != 0) {
			if (n < 0.0) {
				rho = -rho;
				x = -x;
				y = -y;
			}
			if (spherical)
				out.y = 2.0 * Math.atan(Math.pow(c / rho, 1.0/n)) - MapMath.HALFPI;
			else
				out.y = MapMath.phi2(Math.pow(rho / c, 1.0/n), e);
			out.x = Math.atan2(x, y) / n;
		} else {
			out.x = 0.0;
			out.y = n > 0.0 ? MapMath.HALFPI : -MapMath.HALFPI;
		}
		return out;
	}
}  */

  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double toLat, toLon;
    double fromX = (world.getX() - falseEasting) / totalScale;  // assumes cartesion coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    fromY = rho0 - fromY;
    double rho = MapMath.distance(fromX, fromY);
		if (rho != 0) {
			if (n < 0.0) {
				rho = -rho;
				fromX = -fromX;
				fromY = -fromY;
			}
			if (isSpherical)
				toLat = 2.0 * Math.atan(Math.pow(c / rho, 1.0/n)) - MapMath.HALFPI;
			else
				toLat = MapMath.phi2(Math.pow(rho / c, 1.0/n), e);

			toLon = Math.atan2(fromX, fromY) / n;

		} else {
			toLon = 0.0;
			toLat = n > 0.0 ? MapMath.HALFPI : -MapMath.HALFPI;
		}

    result.setLatitude(Math.toDegrees(toLat));
    result.setLongitude(Math.toDegrees(toLon) + lon0deg);
    return result;
  }

 ////////////////////////////////////////////////////////
  // test

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
    LambertConformalConicEllipse a = new LambertConformalConicEllipse(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
    System.out.printf("proj = %s %s%n%n", a.getName(), a.paramsToString());
    //fromProj(a, 1730.692593817677, 1970.917991173046);
    //toProj(a, 39.089117, -75.649278);

    fromProj(a, 5747, 13470);

  }

}