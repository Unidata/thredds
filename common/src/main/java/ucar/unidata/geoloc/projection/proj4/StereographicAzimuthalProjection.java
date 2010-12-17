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

import java.awt.geom.Point2D;
import java.util.Formatter;
import ucar.unidata.geoloc.*;

/**
 * taken from the USGS PROJ package.
 * @author Heiko.Klein@met.no
 */
public class StereographicAzimuthalProjection extends ProjectionImpl {
    // projection paramters
    double projectionLatitude, projectionLongitude; // origin in radian
    double n; // Math.sin(projectionLatitude
    double scaleFactor, trueScaleLatitude;  // scale or trueScale in radian
    double falseEasting, falseNorthing; // km

    // earth shape
    private Earth earth;
    private double e;   // earth.getEccentricitySquared
    private double es;  // earth.getEccentricitySquared
    private double one_es;  // 1-es
    private double totalScale; // scale to convert cartesian coords in km
    // spherical vs ellipsoidal
    private boolean isSpherical;

	public final static int NORTH_POLE = 1;
	public final static int SOUTH_POLE = 2;
	public final static int EQUATOR = 3;
	public final static int OBLIQUE = 4;

	private final static double TOL = 1.e-8;
    private double EPS10 = 1.e-10;
	
	private double akm1, sinphi0, cosphi0;
    private int mode;
	
	public StereographicAzimuthalProjection() {
		this(Math.toRadians(90.0), Math.toRadians(0.0), 0.994, 90., 0, 0, new Earth());
	}

      /**
   * Construct a Stereographic Projection.
   *
   * @param latt  tangent point of projection, also origin of
   *              projection coord system, in degree
   * @param lont  tangent point of projection, also origin of
   *              projection coord system, in degree
   * @param trueScaleLat latitude in degree where scale is scale, use 90. if not used
   * @param scale scale factor at tangent point, "normally 1.0 but
   *              may be reduced"
   */
	public StereographicAzimuthalProjection(double latt, double lont, double scale, double trueScaleLat, double false_easting, double false_northing, Earth earth) {
		projectionLatitude = Math.toRadians(latt);
        n = Math.sin(projectionLatitude);
        projectionLongitude = Math.toRadians(lont);
        trueScaleLatitude = Math.toRadians(trueScaleLat);
        scaleFactor = scale;
        falseEasting = false_easting;
        falseNorthing = false_northing;
        // earth figure
        this.earth = earth;
		this.e = earth.getEccentricity();
		this.es = earth.getEccentricitySquared();
        this.isSpherical = (e == 0.0);
		this.one_es = 1.0-es;
		this.totalScale = earth.getMajor(); // scale factor for cartesion coords in km.
		initialize();

        // parameters
        addParameter(ATTR_NAME, "stereographic");
        addParameter("longitude_of_projection_origin", lont);
        addParameter("latitude_of_projection_origin", latt);
        addParameter("scale_factor_at_projection_origin", scale);
        if ((false_easting != 0.0) || (false_northing != 0.0)) {
            addParameter("false_easting", false_easting);
            addParameter("false_northing", false_northing);
            addParameter("units", "km");
        }
        addParameter("semi_major_axis", earth.getMajor());
        addParameter("inverse_flattening", 1.0/earth.getFlattening());

        //System.err.println(paramsToString());

	}
		
	public void initialize() {
		double t;

		if (Math.abs((t = Math.abs(projectionLatitude)) - MapMath.HALFPI) < EPS10)
			mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
		else
			mode = t > EPS10 ? OBLIQUE : EQUATOR;
		trueScaleLatitude = Math.abs(trueScaleLatitude);
		if (isSpherical) { // sphere
			switch (mode) {
			case OBLIQUE:
				sinphi0 = Math.sin(projectionLatitude);
				cosphi0 = Math.cos(projectionLatitude);
			case EQUATOR:
				akm1 = 2. * scaleFactor;
				break;
			case SOUTH_POLE:
			case NORTH_POLE:
				akm1 = Math.abs(trueScaleLatitude - MapMath.HALFPI) >= EPS10 ?
				   Math.cos(trueScaleLatitude) / Math.tan(MapMath.QUARTERPI - .5 * trueScaleLatitude) :
				   2. * scaleFactor ;
				break;
			}
		} else { // ellipsoid
			double X;

			switch (mode) {
			case NORTH_POLE:
			case SOUTH_POLE:
				if (Math.abs(trueScaleLatitude - MapMath.HALFPI) < EPS10)
					akm1 = 2. * scaleFactor /
					   Math.sqrt(Math.pow(1+e,1+e)*Math.pow(1-e,1-e));
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

	public Point2D.Double project(double lam, double phi, Point2D.Double xy) {
		double coslam = Math.cos(lam);
		double sinlam = Math.sin(lam);
		double sinphi = Math.sin(phi);

		if (isSpherical) { // sphere
			double cosphi = Math.cos(phi);

			switch (mode) {
			case EQUATOR:
				xy.y = 1. + cosphi * coslam;
				if (xy.y <= EPS10)
					throw new RuntimeException("I");
				xy.x = (xy.y = akm1 / xy.y) * cosphi * sinlam;
				xy.y *= sinphi;
				break;
			case OBLIQUE:
				xy.y = 1. + sinphi0 * sinphi + cosphi0 * cosphi * coslam;
				if (xy.y <= EPS10)
					throw new RuntimeException("I");
				xy.x = (xy.y = akm1 / xy.y) * cosphi * sinlam;
				xy.y *= cosphi0 * sinphi - sinphi0 * cosphi * coslam;
				break;
			case NORTH_POLE:
				coslam = - coslam;
				phi = - phi;
			case SOUTH_POLE:
				if (Math.abs(phi - MapMath.HALFPI) < TOL)
					throw new RuntimeException("I");
				xy.x = sinlam * ( xy.y = akm1 * Math.tan(MapMath.QUARTERPI + .5 * phi) );
				xy.y *= coslam;
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
				xy.y = A * (cosphi0 * sinX - sinphi0 * cosX * coslam);
				xy.x = A * cosX;
				break;
			case EQUATOR:
				A = 2. * akm1 / (1. + cosX * coslam);
				xy.y = A * sinX;
				xy.x = A * cosX;
				break;
			case SOUTH_POLE:
				phi = -phi;
				coslam = -coslam;
				sinphi = -sinphi;
			case NORTH_POLE:
				xy.x = akm1 * MapMath.tsfn(phi, sinphi, e);
				xy.y = - xy.x * coslam;
				break;
			}
			xy.x = xy.x * sinlam;
		}
		return xy;
	}

	public Point2D.Double projectInverse(double x, double y, Point2D.Double lp) {
		if (isSpherical) {
			double  c, rh, sinc, cosc;

			sinc = Math.sin(c = 2. * Math.atan((rh = MapMath.distance(x, y)) / akm1));
			cosc = Math.cos(c);
			lp.x = 0.;
			switch (mode) {
			case EQUATOR:
				if (Math.abs(rh) <= EPS10)
					lp.y = 0.;
				else
					lp.y = Math.asin(y * sinc / rh);
				if (cosc != 0. || x != 0.)
					lp.x = Math.atan2(x * sinc, cosc * rh);
				break;
			case OBLIQUE:
				if (Math.abs(rh) <= EPS10)
					lp.y = projectionLatitude;
				else
					lp.y = Math.asin(cosc * sinphi0 + y * sinc * cosphi0 / rh);
				if ((c = cosc - sinphi0 * Math.sin(lp.y)) != 0. || x != 0.)
					lp.x = Math.atan2(x * sinc * cosphi0, c * rh);
				break;
			case NORTH_POLE:
				y = -y;
			case SOUTH_POLE:
				if (Math.abs(rh) <= EPS10)
					lp.y = projectionLatitude;
				else
					lp.y = Math.asin(mode == SOUTH_POLE ? - cosc : cosc);
				lp.x = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
				break;
			}
		} else {
			double cosphi, sinphi, tp, phi_l, rho, halfe, halfpi;

			rho = MapMath.distance(x, y);
			switch (mode) {
			case OBLIQUE:
			case EQUATOR:
			default:	// To prevent the compiler complaining about uninitialized vars.
				cosphi = Math.cos( tp = 2. * Math.atan2(rho * cosphi0 , akm1) );
				sinphi = Math.sin(tp);
				phi_l = Math.asin(cosphi * sinphi0 + (y * sinphi * cosphi0 / rho));
				tp = Math.tan(.5 * (MapMath.HALFPI + phi_l));
				x *= sinphi;
				y = rho * cosphi0 * cosphi - y * sinphi0* sinphi;
				halfpi = MapMath.HALFPI;
				halfe = .5 * e;
				break;
			case NORTH_POLE:
				y = -y;
			case SOUTH_POLE:
				phi_l = MapMath.HALFPI - 2. * Math.atan(tp = - rho / akm1);
				halfpi = -MapMath.HALFPI;
				halfe = -.5 * e;
				break;
			}
			for (int i = 8; i-- != 0; phi_l = lp.y) {
				sinphi = e * Math.sin(phi_l);
				lp.y = 2. * Math.atan(tp * Math.pow((1.+sinphi)/(1.-sinphi), halfe)) - halfpi;
				if (Math.abs(phi_l - lp.y) < EPS10) {
					if (mode == SOUTH_POLE)
						lp.y = -lp.y;
					lp.x = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
					return lp;
				}
			}
			throw new RuntimeException("Iteration didn't converge");
		}
		return lp;
	}
	
	/**
	 * Returns true if this projection is conformal
	 */
	public boolean isConformal() {
		return true;
	}
	
	public boolean hasInverse() {
		return true;
	}

	private double ssfn(double phit, double sinphi, double eccen) {
		sinphi *= eccen;
		return Math.tan (.5 * (MapMath.HALFPI + phit)) *
		   Math.pow((1. - sinphi) / (1. + sinphi), .5 * eccen);
	}

    @Override
	public String getProjectionTypeLabel() {
		return "Stereographic Azimuthal Ellipsoidal Earth";
	}

    @Override
    public ProjectionImpl constructCopy() {
        return new StereographicAzimuthalProjection(Math.toDegrees(projectionLatitude), Math.toDegrees(projectionLongitude), scaleFactor, Math.toDegrees(trueScaleLatitude), falseEasting, falseNorthing, earth);
    }

    @Override
    public String paramsToString() {
        Formatter f = new Formatter();
        f.format("origin lat,lon=%f,%f scale,trueScaleLat=%f,%f earth=%s", Math.toDegrees(projectionLatitude), Math.toDegrees(projectionLongitude), scaleFactor, Math.toDegrees(trueScaleLatitude), earth );
        return f.toString();
    }

    @Override
    public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl destPoint) {
        double fromLat = Math.toRadians(latLon.getLatitude());
        double theta = computeTheta(latLon.getLongitude());

        Point2D.Double res = project(theta, fromLat, new Point2D.Double());

        destPoint.setLocation(totalScale * res.x + falseEasting, totalScale * res.y + falseNorthing);
        return destPoint;
    }

    @Override
    public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
        double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesian coords in km
        double fromY = (world.getY() - falseNorthing) / totalScale;
        
        Point2D.Double dst = projectInverse(fromX, fromY, new Point2D.Double());
        if (dst.x < -Math.PI)
			dst.x = -Math.PI;
		else if (dst.x > Math.PI)
			dst.x = Math.PI;
		if (projectionLongitude != 0)
			dst.x = MapMath.normalizeLongitude(dst.x+projectionLongitude);
        
        result.setLongitude(Math.toDegrees(dst.x));
        result.setLatitude(Math.toDegrees(dst.y));
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
            System.out.println(lp.getLatitude() + ", " + lp.getLongitude() + ": " +p.x + ", " +p.y);
            x[i] = p.x;
            y[i] = p.y;
        }
        for (int i = 0; i < lat.length; ++i) {
            ProjectionPointImpl p = new ProjectionPointImpl(x[i], y[i]);
            LatLonPointImpl lp = (LatLonPointImpl) proj.projToLatLon(p);
            if ((Math.abs(lp.getLatitude()-lat[i]) > 1e-5)
                || (Math.abs(lp.getLongitude()-lon[i]) > 1e-5)) {
                if (Math.abs(lp.getLatitude()) > 89.99 &&
                        (Math.abs(lp.getLatitude()-lat[i]) < 1e-5)) {
                    // ignore longitude singularities at poles
                } else {
                    System.err.print("ERROR:");
                }
            }
            System.out.println("reverse:" +p.x + ", " +p.y + ": " +lp.getLatitude() + ", " + lp.getLongitude());
            
        }
        
    }

    static public void main(String[] args) {
        // test-code
        Earth e = new Earth(6378.137, 0, 298.257224);
        StereographicAzimuthalProjection proj = new StereographicAzimuthalProjection(90., 0., 0.93306907, 90., 0., 0.,e);
        
        double[] lat = {60., 90., 60.};
        double[] lon = {0., 0., 10.};
        test(proj, lat, lon);

        proj = new StereographicAzimuthalProjection(90., -45., 0.96985819, 90., 0., 0.,e);
        test(proj, lat, lon);
    }
}

