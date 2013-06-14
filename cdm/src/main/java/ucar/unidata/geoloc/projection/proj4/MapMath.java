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

package ucar.unidata.geoloc.projection.proj4;

import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;

/**
 * Taken from com.jhlabs.map.proj
 *
 * Also see "http://search.cpan.org/src/DSTAHLKE/Cartography-Projection-GCTP-0.03/gctpc/" for C code
 *
 * @see "http://www.jhlabs.com/java/maps/proj/index.html"
 * @see "http://trac.osgeo.org/proj/"
 *
 * @since Oct 8, 2009
 */

public class MapMath {

	public final static double HALFPI = Math.PI/2.0;
	public final static double QUARTERPI = Math.PI/4.0;
	public final static double TWOPI = Math.PI*2.0;

	public final static ProjectionRect WORLD_BOUNDS_RAD = new ProjectionRect(-Math.PI, -Math.PI/2, Math.PI*2, Math.PI);
	public final static ProjectionRect WORLD_BOUNDS = new ProjectionRect(-180, -90, 360, 180);

  public final static double EPS10 = 1e-10;
  public final static double RTD = 180.0 / Math.PI;
  public final static double DTR = Math.PI / 180.0;

	/**
	 * Degree versions of trigonometric functions
	 */
	public static double sind(double v) {
		return Math.sin(v * DTR);
	}

	public static double cosd(double v) {
		return Math.cos(v * DTR);
	}

	public static double tand(double v) {
		return Math.tan(v * DTR);
	}

	public static double asind(double v) {
		return Math.asin(v) * RTD;
	}

	public static double acosd(double v) {
		return Math.acos(v) * RTD;
	}

	public static double atand(double v) {
		return Math.atan(v) * RTD;
	}

	public static double atan2d(double y, double x) {
		return Math.atan2(y, x) * RTD;
	}

	public static double asin(double v) {
		if (Math.abs(v) > 1.)
			return v < 0.0 ? -Math.PI/2 : Math.PI/2;
		return Math.asin(v);
	}

	public static double acos(double v) {
		if (Math.abs(v) > 1.)
			return v < 0.0 ? Math.PI : 0.0;
		return Math.acos(v);
	}

	public static double sqrt(double v) {
		return v < 0.0 ? 0.0 : Math.sqrt(v);
	}

	public static double distance(double dx, double dy) {
		return Math.sqrt(dx*dx+dy*dy);
	}

	public static double distance(ProjectionPoint a, ProjectionPoint b) {
		return distance(a.getX()-b.getX(), a.getY()-b.getY());
	}

	public static double hypot(double x, double y) {
		if (x < 0.0)
			x = -x;
		else if (x == 0.0)
			return y < 0.0 ? -y : y;
		if (y < 0.0)
			y = -y;
		else if (y == 0.0)
			return x;
		if (x < y) {
			x /= y;
			return y * Math.sqrt(1.0 + x * x);
		} else {
			y /= x;
			return x * Math.sqrt(1.0 + y * y);
		}
	}

	public static double atan2(double y, double x) {
		return Math.atan2(y, x);
	}

	public static double trunc(double v) {
		return v < 0.0 ? Math.ceil(v) : Math.floor(v);
	}

	public static double frac(double v) {
		return v - trunc(v);
	}

	public static double degToRad(double v) {
		return v * Math.PI / 180.0;
	}

	public static double radToDeg(double v) {
		return v * 180.0 / Math.PI;
	}

	// For negative angles, d should be negative, m & s positive.
	public static double dmsToRad(double d, double m, double s) {
		if (d >= 0)
			return (d + m/60 + s/3600) * Math.PI / 180.0;
		return (d - m/60 - s/3600) * Math.PI / 180.0;
	}

	// For negative angles, d should be negative, m & s positive.
	public static double dmsToDeg(double d, double m, double s) {
		if (d >= 0)
			return (d + m/60 + s/3600);
		return (d - m/60 - s/3600);
	}

	public static double normalizeLatitude(double angle) {
		if (Double.isInfinite(angle) || Double.isNaN(angle))
			throw new RuntimeException("Infinite latitude");

		while (angle > MapMath.HALFPI)
			angle -= Math.PI;
		while (angle < -MapMath.HALFPI)
			angle += Math.PI;
		return angle;
//		return Math.IEEEremainder(angle, Math.PI);
	}

	public static double normalizeLongitude(double angle) {
		if (Double.isInfinite(angle) || Double.isNaN(angle))
			throw new RuntimeException("Infinite longitude");

		while (angle > Math.PI)
			angle -= TWOPI;
		while (angle < -Math.PI)
			angle += TWOPI;
		return angle;
//		return Math.IEEEremainder(angle, Math.PI);
	}

	public static double normalizeAngle(double angle) {
		if (Double.isInfinite(angle) || Double.isNaN(angle))
			throw new RuntimeException("Infinite angle");
    
		while (angle > TWOPI)
			angle -= TWOPI;
		while (angle < 0)
			angle += TWOPI;
		return angle;
	}

/*
	public static void latLongToXYZ(ProjectionPoint ll, Point3D xyz) {
		double c = Math.cos(ll.y);
		xyz.x = c * Math.cos(ll.x);
		xyz.y = c * Math.sin(ll.x);
		xyz.z = Math.sin(ll.y);
	}

	public static void xyzToLatLong(Point3D xyz, ProjectionPoint ll) {
		ll.y = MapMath.asin(xyz.z);
		ll.x = MapMath.atan2(xyz.y, xyz.x);
	}
*/

	public static double greatCircleDistance(double lon1, double lat1, double lon2, double lat2 ) {
		double dlat = Math.sin((lat2-lat1)/2);
		double dlon = Math.sin((lon2-lon1)/2);
		double r = Math.sqrt(dlat*dlat + Math.cos(lat1)*Math.cos(lat2)*dlon*dlon);
		return 2.0 * Math.asin(r);
	}

	public static double sphericalAzimuth(double lat0, double lon0, double lat, double lon) {
		double diff = lon - lon0;
		double coslat = Math.cos(lat);

		return Math.atan2(
			coslat * Math.sin(diff),
			(Math.cos(lat0) * Math.sin(lat) -
			Math.sin(lat0) * coslat * Math.cos(diff))
		);
	}

	public static boolean sameSigns(double a, double b) {
		return a < 0 == b < 0;
	}

	public static boolean sameSigns(int a, int b) {
		return a < 0 == b < 0;
	}

	public static double takeSign(double a, double b) {
		a = Math.abs(a);
		if (b < 0)
			return -a;
		return a;
	}

	public static int takeSign(int a, int b) {
		a = Math.abs(a);
		if (b < 0)
			return -a;
		return a;
	}

	public final static int DONT_INTERSECT = 0;
	public final static int DO_INTERSECT = 1;
	public final static int COLLINEAR = 2;

	public static int intersectSegments(ProjectionPoint aStart, ProjectionPoint aEnd, ProjectionPoint bStart, ProjectionPoint bEnd, ProjectionPointImpl p) {
		double a1, a2, b1, b2, c1, c2;
		double r1, r2, r3, r4;
		double denom, offset, num;

		a1 = aEnd.getY()-aStart.getY();
		b1 = aStart.getX()-aEnd.getX();
		c1 = aEnd.getX()*aStart.getY() - aStart.getX()*aEnd.getY();
		r3 = a1*bStart.getX() + b1*bStart.getY() + c1;
		r4 = a1*bEnd.getX() + b1*bEnd.getY() + c1;

		if (r3 != 0 && r4 != 0 && sameSigns(r3, r4))
			return DONT_INTERSECT;

		a2 = bEnd.getY()-bStart.getY();
		b2 = bStart.getX()-bEnd.getX();
		c2 = bEnd.getX()*bStart.getY()-bStart.getX()*bEnd.getY();
		r1 = a2*aStart.getX() + b2*aStart.getY() + c2;
		r2 = a2*aEnd.getX() + b2*aEnd.getY() + c2;

		if (r1 != 0 && r2 != 0 && sameSigns(r1, r2))
			return DONT_INTERSECT;

		denom = a1*b2 - a2*b1;
		if (denom == 0)
			return COLLINEAR;

		offset = denom < 0 ? -denom/2 : denom/2;

		num = b1*c2 - b2*c1;
		p.setX((num < 0 ? num-offset : num+offset) / denom);

		num = a2*c1 - a1*c2;
		p.setY((num < 0 ? num-offset : num+offset) / denom);

		return DO_INTERSECT;
	}

	public static double dot(ProjectionPoint a, ProjectionPoint b) {
		return a.getX()*b.getX() + a.getY()*b.getY();
	}

	public static ProjectionPoint perpendicular(ProjectionPoint a) {
		return new ProjectionPointImpl(-a.getY(), a.getX());
	}

	public static ProjectionPoint add(ProjectionPoint a, ProjectionPoint b) {
		return new ProjectionPointImpl(a.getX()+b.getX(), a.getY()+b.getY());
	}

	public static ProjectionPoint subtract(ProjectionPoint a, ProjectionPoint b) {
		return new ProjectionPointImpl(a.getX()-b.getX(), a.getY()-b.getY());
	}

	public static ProjectionPoint multiply(ProjectionPoint a, ProjectionPoint b) {
		return new ProjectionPointImpl(a.getX()*b.getX(), a.getY()*b.getY());
	}

	public static double cross(ProjectionPoint a, ProjectionPoint b) {
		return a.getX()*b.getY() - b.getX()*a.getY();
	}

	public static double cross(double x1, double y1, double x2, double y2) {
		return x1*y2 - x2*y1;
	}

	public static void normalize(ProjectionPointImpl a) {
		double d = distance(a.getX(), a.getY());
		a.setLocation( a.getX() / d, a.getY() / d);
	}

	public static void negate(ProjectionPointImpl a) {
    a.setLocation( -a.getX(), -a.getY());
	}

	public static double longitudeDistance(double l1, double l2) {
		return Math.min(
			Math.abs(l1-l2),
			((l1 < 0) ? l1+Math.PI : Math.PI-l1) + ((l2 < 0) ? l2+Math.PI : Math.PI-l2)
		);
	}

	public static double geocentricLatitude(double lat, double flatness) {
		double f = 1.0 - flatness;
		return Math.atan((f*f) * Math.tan(lat));
	}

	public static double geographicLatitude(double lat, double flatness) {
		double f = 1.0 - flatness;
		return Math.atan(Math.tan(lat) / (f*f));
	}

	public static double tsfn(double phi, double sinphi, double e) {
		sinphi *= e;
		return (Math.tan (.5 * (MapMath.HALFPI - phi)) /
		   Math.pow((1. - sinphi) / (1. + sinphi), .5 * e));
	}

	public static double msfn(double sinphi, double cosphi, double es) {
		return cosphi / Math.sqrt(1.0 - es * sinphi * sinphi);
	}

	private final static int N_ITER = 15;

	public static double phi2(double ts, double e) {
		double eccnth, phi, con, dphi;
		int i;

		eccnth = .5 * e;
		phi = MapMath.HALFPI - 2. * Math.atan(ts);
		i = N_ITER;
		do {
			con = e * Math.sin(phi);
			dphi = MapMath.HALFPI - 2. * Math.atan(ts * Math.pow((1. - con) / (1. + con), eccnth)) - phi;
			phi += dphi;
		} while (Math.abs(dphi) > 1e-10 && --i != 0);
		if (i <= 0)
			throw new RuntimeException();
		return phi;
	}

	private final static double C00 = 1.0;
	private final static double C02 = .25;
	private final static double C04 = .046875;
	private final static double C06 = .01953125;
	private final static double C08 = .01068115234375;
	private final static double C22 = .75;
	private final static double C44 = .46875;
	private final static double C46 = .01302083333333333333;
	private final static double C48 = .00712076822916666666;
	private final static double C66 = .36458333333333333333;
	private final static double C68 = .00569661458333333333;
	private final static double C88 = .3076171875;
	private final static int MAX_ITER = 10;

	public static double[] enfn(double es) {
		double t;
		double[] en = new double[5];
		en[0] = C00 - es * (C02 + es * (C04 + es * (C06 + es * C08)));
		en[1] = es * (C22 - es * (C04 + es * (C06 + es * C08)));
		en[2] = (t = es * es) * (C44 - es * (C46 + es * C48));
		en[3] = (t *= es) * (C66 - es * C68);
		en[4] = t * es * C88;
		return en;
	}

	public static double mlfn(double phi, double sphi, double cphi, double[] en) {
		cphi *= sphi;
		sphi *= sphi;
		return en[0] * phi - cphi * (en[1] + sphi*(en[2] + sphi*(en[3] + sphi*en[4])));
	}

	public static double inv_mlfn(double arg, double es, double[] en) {
		double s, t, phi, k = 1./(1.-es);

		phi = arg;
		for (int i = MAX_ITER; i != 0; i--) {
			s = Math.sin(phi);
			t = 1. - es * s * s;
			phi -= t = (mlfn(phi, s, Math.cos(phi), en) - arg) * (t * Math.sqrt(t)) * k;
			if (Math.abs(t) < 1e-11)
				return phi;
		}
		return phi;
	}

	private final static double P00 = .33333333333333333333;
	private final static double P01 = .17222222222222222222;
	private final static double P02 = .10257936507936507936;
	private final static double P10 = .06388888888888888888;
	private final static double P11 = .06640211640211640211;
	private final static double P20 = .01641501294219154443;

	public static double[] authset(double es) {
		double t;
		double[] APA = new double[3];
		APA[0] = es * P00;
		t = es * es;
		APA[0] += t * P01;
		APA[1] = t * P10;
		t *= es;
		APA[0] += t * P02;
		APA[1] += t * P11;
		APA[2] = t * P20;
		return APA;
	}

	public static double authlat(double beta, double []APA) {
		double t = beta+beta;
		return(beta + APA[0] * Math.sin(t) + APA[1] * Math.sin(t+t) + APA[2] * Math.sin(t+t+t));
	}

	public static double qsfn(double sinphi, double e, double one_es) {
		double con;

		if (e >= 1.0e-7) {
			con = e * sinphi;
			return (one_es * (sinphi / (1. - con * con) -
			   (.5 / e) * Math.log ((1. - con) / (1. + con))));
		} else
			return (sinphi + sinphi);
	}

	/*
	 * Java translation of "Nice Numbers for Graph Labels"
	 * by Paul Heckbert
	 * from "Graphics Gems", Academic Press, 1990
	 */
	public static double niceNumber(double x, boolean round) {
		int expv;				/* exponent of x */
		double f;				/* fractional part of x */
		double nf;				/* nice, rounded fraction */

		expv = (int)Math.floor(Math.log(x) / Math.log(10));
		f = x/Math.pow(10., expv);		/* between 1 and 10 */
		if (round) {
			if (f < 1.5)
				nf = 1.;
			else if (f < 3.)
				nf = 2.;
			else if (f < 7.)
				nf = 5.;
			else
				nf = 10.;
		} else if (f <= 1.)
			nf = 1.;
		else if (f <= 2.)
			nf = 2.;
		else if (f <= 5.)
			nf = 5.;
		else
			nf = 10.;
		return nf*Math.pow(10., expv);
	}
}
