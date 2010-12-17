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

import java.awt.geom.*;
import java.util.Formatter;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

/**
* Transverse Mercator Projection algorithm is taken from the USGS PROJ package.
 * @author Heiko.Klein@met.no
*/
public class TransverseMercatorProjection extends ProjectionImpl {
	
	private final static double FC1 = 1.0;
	private final static double FC2 = 0.5;
	private final static double FC3 = 0.16666666666666666666;
	private final static double FC4 = 0.08333333333333333333;
	private final static double FC5 = 0.05;
	private final static double FC6 = 0.03333333333333333333;
	private final static double FC7 = 0.02380952380952380952;
	private final static double FC8 = 0.01785714285714285714;

	private double esp;
	private double ml0;
	private double[] en;

    private double projectionLatitude, projectionLongitude;
    private double scaleFactor;
    private double falseEasting, falseNorthing;

    Earth ellipsoid;
    private double e;   // earth.getEccentricitySquared
    private double es;  // earth.getEccentricitySquared
    private double one_es;  // 1-es
    private double totalScale; // scale to convert cartesian coords in km
    private boolean spherical;

	public TransverseMercatorProjection() {
		ellipsoid = new Earth();
		projectionLatitude = Math.toRadians(0);
		projectionLongitude = Math.toRadians(0);
		initialize();
	}
	
	/**
	 * Set up a projection suitable for State Plane Coordinates.
     * Best used with earth ellipsoid and false-easting/northing in km
	 */
	public TransverseMercatorProjection(Earth ellipsoid, double lon_0_deg, double lat_0_deg, double k, double falseEast, double falseNorth) {
        this.ellipsoid = ellipsoid;
        projectionLongitude = Math.toRadians(lon_0_deg);
		projectionLatitude = Math.toRadians(lat_0_deg);
		scaleFactor = k;
		falseEasting = falseEast;
		falseNorthing = falseNorth;
		initialize();

                // parameters
        addParameter(ATTR_NAME, "transverse_mercator");
        addParameter("longitude_of_central_meridian", lon_0_deg);
        addParameter("latitude_of_projection_origin", lat_0_deg);
        addParameter("scale_factor_at_central_meridian", scaleFactor);
        if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
            addParameter("false_easting", falseEasting);
            addParameter("false_northing", falseNorthing);
            addParameter("units", "km");
        }
        addParameter("semi_major_axis", ellipsoid.getMajor());
        addParameter("inverse_flattening", 1.0/ellipsoid.getFlattening());

        //System.err.println(paramsToString());
	}
	
    @Override
	public Object clone() {
		return new TransverseMercatorProjection(ellipsoid, Math.toDegrees(projectionLongitude), Math.toDegrees(projectionLatitude), scaleFactor, falseEasting, falseNorthing);
	}
	
	public boolean isRectilinear() {
		return false;
	}

	public void initialize() {
		this.e = ellipsoid.getEccentricity();
		this.es = ellipsoid.getEccentricitySquared();
        this.spherical = (e == 0.0);
		this.one_es = 1.0-es;
		this.totalScale = ellipsoid.getMajor(); // scale factor for cartesion coords in km.

		if (spherical) {
			esp = scaleFactor;
			ml0 = .5 * esp;
		} else {
			en = MapMath.enfn(es);
			ml0 = MapMath.mlfn(projectionLatitude, Math.sin(projectionLatitude), Math.cos(projectionLatitude), en);
			esp = es / (1. - es);
		}
	}

	public int getRowFromNearestParallel(double latitude) {
		int degrees = (int)MapMath.radToDeg(MapMath.normalizeLatitude(latitude));
		if (degrees < -80 || degrees > 84)
			return 0;
		if (degrees > 80)
			return 24;
		return (degrees + 80) / 8 + 3;
	}
	
	public int getZoneFromNearestMeridian(double longitude) {
		int zone = (int)Math.floor((MapMath.normalizeLongitude(longitude) + Math.PI) * 30.0 / Math.PI) + 1;
		if (zone < 1)
			zone = 1;
		else if (zone > 60)
			zone = 60;
		return zone;
	}
	
	public void setUTMZone(int zone) {
		zone--;
		projectionLongitude = (zone + .5) * Math.PI / 30. -Math.PI;
		projectionLatitude = 0.0;
		scaleFactor = 0.9996;
        falseEasting = 500000;
		initialize();
	}

	public Point2D.Double project(double lplam, double lpphi, Point2D.Double xy) {
		if (spherical) {
			double cosphi = Math.cos(lpphi);
			double b = cosphi * Math.sin(lplam);

			xy.x = ml0 * scaleFactor * Math.log((1. + b) / (1. - b));
			double ty = cosphi * Math.cos(lplam) / Math.sqrt(1. - b * b);
			ty = MapMath.acos(ty);
			if (lpphi < 0.0)
				ty = -ty;
			xy.y = esp * (ty - projectionLatitude);
		} else {
			double al, als, n, t;
			double sinphi = Math.sin(lpphi);
			double cosphi = Math.cos(lpphi);
			t = Math.abs(cosphi) > 1e-10 ? sinphi/cosphi : 0.0;
			t *= t;
			al = cosphi * lplam;
			als = al * al;
			al /= Math.sqrt(1. - es * sinphi * sinphi);
			n = esp * cosphi * cosphi;
			xy.x = scaleFactor * al * (FC1 +
				FC3 * als * (1. - t + n +
				FC5 * als * (5. + t * (t - 18.) + n * (14. - 58. * t)
				+ FC7 * als * (61. + t * ( t * (179. - t) - 479. ) )
			)));
			xy.y = scaleFactor * (MapMath.mlfn(lpphi, sinphi, cosphi, en) - ml0 +
				sinphi * al * lplam * FC2 * ( 1. +
				FC4 * als * (5. - t + n * (9. + 4. * n) +
				FC6 * als * (61. + t * (t - 58.) + n * (270. - 330 * t)
				+ FC8 * als * (1385. + t * ( t * (543. - t) - 3111.) )
			))));
		}
		return xy;
	}

	public Point2D.Double projectInverse(double x, double y, Point2D.Double out) {
		if (spherical) {
			double h = Math.exp(x / scaleFactor);
			double g = .5 * (h - 1. / h);
			h = Math.cos(projectionLatitude + y / scaleFactor);
			out.y = MapMath.asin(Math.sqrt((1. - h*h) / (1. + g*g)));
			if (y < 0)
				out.y = -out.y;
			out.x = Math.atan2(g, h);
		} else {
			double n, con, cosphi, d, ds, sinphi, t;

			out.y = MapMath.inv_mlfn(ml0 + y/scaleFactor, es, en);
			if (Math.abs(y) >= MapMath.HALFPI) {
				out.y = y < 0. ? -MapMath.HALFPI : MapMath.HALFPI;
				out.x = 0.;
			} else {
				sinphi = Math.sin(out.y);
				cosphi = Math.cos(out.y);
				t = Math.abs(cosphi) > 1e-10 ? sinphi/cosphi : 0.;
				n = esp * cosphi * cosphi;
				d = x * Math.sqrt(con = 1. - es * sinphi * sinphi) / scaleFactor;
				con *= t;
				t *= t;
				ds = d * d;
				out.y -= (con * ds / (1.-es)) * FC2 * (1. -
					ds * FC4 * (5. + t * (3. - 9. *  n) + n * (1. - 4 * n) -
					ds * FC6 * (61. + t * (90. - 252. * n +
						45. * t) + 46. * n
					- ds * FC8 * (1385. + t * (3633. + t * (4095. + 1574. * t)) )
				)));
				out.x = d*(FC1 -
					ds*FC3*( 1. + 2.*t + n -
					ds*FC5*(5. + t*(28. + 24.*t + 8.*n) + 6.*n
					- ds * FC7 * (61. + t * (662. + t * (1320. + 720. * t)) )
				))) / cosphi;
			}
		}
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

    @Override
	public String getProjectionTypeLabel() {
		return "Transverse Mercator Ellipsoidal Earth";
	}

    @Override
    public ProjectionImpl constructCopy() {
        return (ProjectionImpl) clone();
    }

    @Override
    public String paramsToString() {
        Formatter f = new Formatter();
        f.format("origin lat,lon=%f,%f scale=%f earth=%s falseEast/North=%f,%f", Math.toDegrees(projectionLatitude), Math.toDegrees(projectionLongitude), scaleFactor, ellipsoid, falseEasting, falseNorthing );
        return f.toString();
    }

    @Override
    public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl destPoint) {
        double fromLat = Math.toRadians(latLon.getLatitude());
        double theta = Math.toRadians(latLon.getLongitude());
        if (projectionLongitude != 0) {
            theta = MapMath.normalizeLongitude(theta-projectionLongitude);
        }

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
        // TODO: check, taken from ucar.unidata.geoloc.projection.TransverseMercator
        // either point is infinite
        if (ProjectionPointImpl.isInfinite(pt1)
                || ProjectionPointImpl.isInfinite(pt2)) {
            return true;
        }

        double y1 = pt1.getY() - falseNorthing;
        double y2 = pt2.getY() - falseNorthing;

        // opposite signed long lines: LOOK ????
        return (y1 * y2 < 0) && (Math.abs(y1 - y2) > 2 * ellipsoid.getMajor());
    }

    @Override
    public boolean equals(Object proj) {
        if (!(proj instanceof TransverseMercatorProjection)) {
            return false;
        }

        TransverseMercatorProjection oo = (TransverseMercatorProjection) proj;
        return ((this.projectionLatitude == oo.projectionLatitude)
                && (this.projectionLongitude == oo.projectionLongitude)
                && (this.scaleFactor == oo.scaleFactor)
                && (this.falseEasting == oo.falseEasting)
                && (this.falseNorthing == oo.falseNorthing)
                && this.ellipsoid.equals(oo.ellipsoid));

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.projectionLatitude) ^ (Double.doubleToLongBits(this.projectionLatitude) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.projectionLongitude) ^ (Double.doubleToLongBits(this.projectionLongitude) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.scaleFactor) ^ (Double.doubleToLongBits(this.scaleFactor) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.falseEasting) ^ (Double.doubleToLongBits(this.falseEasting) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.falseNorthing) ^ (Double.doubleToLongBits(this.falseNorthing) >>> 32));
        hash = 97 * hash + (this.ellipsoid != null ? this.ellipsoid.hashCode() : 0);
        return hash;
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
        Earth e = new Earth(6378.137, 6356.7523142, 0);
        ProjectionImpl proj = new TransverseMercatorProjection(e, 9., 0., 0.9996, 500.000, 0.);

        double[] lat = {60., 90., 60.};
        double[] lon = {0., 0., 10.};
        test(proj, lat, lon);

        proj = new TransverseMercatorProjection(e, 9., 0., 0.9996, 500., 0.);
        test(proj, lat, lon);
    }

}
