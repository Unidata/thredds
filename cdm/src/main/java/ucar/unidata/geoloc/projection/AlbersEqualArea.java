/*
 * $Id$
 *
 * Copyright  1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.geoloc.projection;


import ucar.unidata.geoloc.*;
import ucar.unidata.util.Format;
import ucar.unidata.util.Parameter;


/**
 * Albers Equal Area Conic Projection, one or two standard parallels,
 * spherical earth.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 * 2nd edition (1983), p 98
 *
 * @see Projection
 * @see ProjectionImpl
 * @author Unidata Development Team
 * @version $Id$
 */

public class AlbersEqualArea extends ProjectionImpl {

    /** constants from Snyder's equations */
    private double n, C, rho0;

    /** lat/lon in radians */
    private double lat0, lon0;       // radians

    /** parallel 1 and 2 */
    private double par1, par2;       // degrees

    /** lon naught */
    private double lon0Degrees;

    /** origin */
    private LatLonPointImpl origin;  // why are we keeping this?

    // spherical vs ellipsoidal

    /** flag for spherical calculation */
    private boolean spherical = true;

    /**
     *  Constructor with default parameters
     */
    public AlbersEqualArea() {
        this(23, -96, 29.5, 45.5);
    }

    /**
     * Construct a AlbersEqualArea Projection, two standard parellels.
     * For the one standard parellel case, set them both to the same value.
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     * @param par1   standard parallel 1
     * @param par2   standard parallel 2
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public AlbersEqualArea(double lat0, double lon0, double par1,
                           double par2) {

        this.lat0   = Math.toRadians(lat0);
        lon0Degrees = lon0;
        this.lon0   = Math.toRadians(lon0);

        this.par1   = par1;
        this.par2   = par2;

        origin      = new LatLonPointImpl(lat0, lon0);
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
    }

    /**
     * Precalculate some stuff
     */
    private void precalculate() {

        double par1r = Math.toRadians(this.par1);
        double par2r = Math.toRadians(this.par2);

        if (Math.abs(par2 - par1) < TOLERANCE) {  // single parallel
            n = Math.sin(par1r);
        } else {
            n = (Math.sin(par1r) + Math.sin(par2r))/2.0;
        }

        double c2 = Math.pow(Math.cos(par1r), 2);
        C    = c2 + 2 * n * Math.sin(par1r);
        rho0 = computeRho(lat0);

    }

    /**
     * Compute the RHO parameter
     *
     * @param lat latitude
     *
     * @return the rho parameter
     */
    private double computeRho(double lat) {
        return EARTH_RADIUS * Math.sqrt(C - 2 * n * Math.sin(lat)) / n;
    }

    /**
     * Compute theta
     *
     * @param lon  longitude
     *
     * @return theta
     */
    private double computeTheta(double lon) {
        double dlon = LatLonPointImpl.lonNormal(Math.toDegrees(lon)
                                                - lon0Degrees);
        return n * Math.toRadians(dlon);
    }

    /**
     * Clone this projection.
     *
     * @return Clone of this
     */
    public Object clone() {
        AlbersEqualArea cl = (AlbersEqualArea) super.clone();
        cl.origin = new LatLonPointImpl(getOriginLat(), getOriginLon());
        return (Object) cl;
    }

    /**
     * Check for equality with the Object in question
     *
     * @param proj  object to check
     * @return true if they are equal
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof AlbersEqualArea)) {
            return false;
        }

        AlbersEqualArea oo = (AlbersEqualArea) proj;
        return ((this.getParallelOne() == oo.getParallelOne())
                && (this.getParallelTwo() == oo.getParallelTwo())
                && (this.getOriginLat() == oo.getOriginLat())
                && (this.getOriginLon() == oo.getOriginLon())
                && this.defaultMapArea.equals(oo.defaultMapArea));
    }


    // bean properties

    /**
     * Get the second standard parallel
     *
     * @return the second standard parallel
     */
    public double getParallelTwo() {
        return par2;
    }

    /**
     * Set the second standard parallel
     *
     * @param par   the second standard parallel
     */
    public void setParallelTwo(double par) {
        par2 = par;
        precalculate();
    }

    /**
     * Get the first standard parallel
     *
     * @return the first standard parallel
     */
    public double getParallelOne() {
        return par1;
    }

    /**
     * Set the first standard parallel
     *
     * @param par   the first standard parallel
     */
    public void setParallelOne(double par) {
        par1 = par;
        precalculate();
    }

    /**
     * Get the origin longitude.
     * @return the origin longitude.
     */
    public double getOriginLon() {
        return origin.getLongitude();
    }

    /**
     * Set the origin longitude.
     * @param lon   the origin longitude.
     */
    public void setOriginLon(double lon) {
        origin.setLongitude(lon);
        lon0 = Math.toRadians(lon);
        precalculate();
    }

    /**
     * Get the origin latitude.
     * @return the origin latitude.
     */
    public double getOriginLat() {
        return origin.getLatitude();
    }

    /**
     * Set the origin latitude.
     *
     * @param lat   the origin latitude.
     */
    public void setOriginLat(double lat) {
        origin.setLatitude(lat);
        lat0 = Math.toRadians(lat);
        precalculate();
    }

    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "Albers Equal Area";
    }

    /**
     * Create a String of the parameters.
     * @return a String of the parameters
     */
    public String paramsToString() {
        return " origin " + origin.toString() + " parallels: "
               + Format.d(par1, 3) + " " + Format.d(par2, 3);
    }


    /**
     * Get the scale for the lat.
     *
     * @param lat   lat to use
     * @return scale
     */
    public double getScale(double lat) {
        lat = Math.toRadians(lat);
        double n = Math.cos(lat);
        double d = Math.sqrt(C - 2 * n * Math.sin(lat));
        return n / d;
    }


    /**
     *  This returns true when the line between pt1 and pt2 crosses the seam.
     *  When the cone is flattened, the "seam" is lon0 +- 180.
     *
     * @param pt1   point 1
     * @param pt2   point 2
     * @return true when the line between pt1 and pt2 crosses the seam.
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
        // either point is infinite
        if (ProjectionPointImpl.isInfinite(pt1)
                || ProjectionPointImpl.isInfinite(pt2)) {
            return true;
        }
        // opposite signed X values, larger then 5000 km
        return (pt1.getX() * pt2.getX() < 0)
               && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0);
    }


    /*MACROBODY
      latLonToProj {} {
        fromLat = Math.toRadians(fromLat);
        fromLon = Math.toRadians(fromLon);
        double rho = computeRho(fromLat);
        double theta = computeTheta(fromLon);

        toX = rho * Math.sin(theta);
        toY = rho0 - rho*Math.cos(theta);
      }
      projToLatLon {double rrho0 = rho0;} {
        if (n < 0) {
            rrho0 *= -1.0;
            fromX *= -1.0;
            fromY *= -1.0;
        }


        double yd = rrho0-fromY;
        double rho = Math.sqrt(fromX * fromX + yd*yd);
        double theta = Math.atan2( fromX, yd);
        if (n < 0) rho *= -1.0;

        toLat = Math.toDegrees(Math.asin((C-Math.pow((rho*n/EARTH_RADIUS),2))/(2*n)));

        toLon = Math.toDegrees(theta/n + lon0);

             }
    MACROBODY*/

    /*BEGINGENERATED*/
 
/*
Note this section has been generated using the convert.tcl script.
This script, run as: 
tcl convert.tcl AlbersEqualArea.java
takes the actual projection conversion code defined in the MACROBODY 
section above and generates the following 6 methods
*/


    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj (LatLonPoint latLon, ProjectionPointImpl result) {
        double toX, toY;
        double fromLat = latLon.getLatitude ();
	double fromLon = latLon.getLongitude ();
        	
        
        fromLat = Math.toRadians(fromLat);
        fromLon = Math.toRadians(fromLon);
        double rho = computeRho(fromLat);
        double theta = computeTheta(fromLon);

        toX = rho * Math.sin(theta);
        toY = rho0 - rho*Math.cos(theta);
      	
	result.setLocation (toX, toY);
	return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
	double toLat, toLon;
	double fromX = world.getX ();
	double fromY = world.getY ();
        double rrho0 = rho0;	
        
        if (n < 0) {
            rrho0 *= -1.0;
            fromX *= -1.0;
            fromY *= -1.0;
        }


        double yd = rrho0-fromY;
        double rho = Math.sqrt(fromX * fromX + yd*yd);
        double theta = Math.atan2( fromX, yd);
        if (n < 0) rho *= -1.0;

        toLat = Math.toDegrees(Math.asin((C-Math.pow((rho*n/EARTH_RADIUS),2))/(2*n)));

        toLon = Math.toDegrees(theta/n + lon0);

             	
	result.setLatitude (toLat);
	result.setLongitude (toLon);
	return result;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates, 
     *                 where to[0][i], to[1][i] is the (x,y) coordinate 
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */
    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
	int cnt = from[0].length;
	float []fromLatA = from[latIndex];
	float []fromLonA = from[lonIndex];
	float []resultXA = to[INDEX_X];
	float []resultYA = to[INDEX_Y];
        double toX, toY;
        
	for (int i=0; i<cnt; i++) {
	   double fromLat = fromLatA[i];
	   double fromLon = fromLonA[i];
           
        fromLat = Math.toRadians(fromLat);
        fromLon = Math.toRadians(fromLon);
        double rho = computeRho(fromLat);
        double theta = computeTheta(fromLon);

        toX = rho * Math.sin(theta);
        toY = rho0 - rho*Math.cos(theta);
      
           resultXA[i] = (float)toX;
           resultYA[i] = (float)toY;
	}
	return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public float[][] projToLatLon (float[][]from, float[][]to) {
	int cnt = from[0].length;
	float []fromXA = from[INDEX_X];
	float []fromYA = from[INDEX_Y];
	float []toLatA = to[INDEX_LAT];
	float []toLonA = to[INDEX_LON];
        double rrho0 = rho0;				 
        double toLat, toLon;
	for (int i=0;i<cnt;i++) {
	      double fromX = fromXA[i];
              double fromY = fromYA[i];
              
        if (n < 0) {
            rrho0 *= -1.0;
            fromX *= -1.0;
            fromY *= -1.0;
        }


        double yd = rrho0-fromY;
        double rho = Math.sqrt(fromX * fromX + yd*yd);
        double theta = Math.atan2( fromX, yd);
        if (n < 0) rho *= -1.0;

        toLat = Math.toDegrees(Math.asin((C-Math.pow((rho*n/EARTH_RADIUS),2))/(2*n)));

        toLon = Math.toDegrees(theta/n + lon0);

             				 
              toLatA[i]= (float)toLat;
              toLonA[i]= (float)toLon;
	}
	return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates, 
     *                 where to[0][i], to[1][i] is the (x,y) coordinate 
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */
    public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
	int cnt = from[0].length;
	double []fromLatA = from[latIndex];
	double []fromLonA = from[lonIndex];
	double []resultXA = to[INDEX_X];
	double []resultYA = to[INDEX_Y];
        double toX, toY;
        
	for (int i=0; i<cnt; i++) {
	   double fromLat = fromLatA[i];
	   double fromLon = fromLonA[i];
           
        fromLat = Math.toRadians(fromLat);
        fromLon = Math.toRadians(fromLon);
        double rho = computeRho(fromLat);
        double theta = computeTheta(fromLon);

        toX = rho * Math.sin(theta);
        toY = rho0 - rho*Math.cos(theta);
      
           resultXA[i] = (double)toX;
           resultYA[i] = (double)toY;
	}
	return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public double[][] projToLatLon (double[][]from, double[][]to) {
	int cnt = from[0].length;
	double []fromXA = from[INDEX_X];
	double []fromYA = from[INDEX_Y];
	double []toLatA = to[INDEX_LAT];
	double []toLonA = to[INDEX_LON];
        double rrho0 = rho0;				 
        double toLat, toLon;
	for (int i=0;i<cnt;i++) {
	      double fromX = fromXA[i];
              double fromY = fromYA[i];
              
        if (n < 0) {
            rrho0 *= -1.0;
            fromX *= -1.0;
            fromY *= -1.0;
        }


        double yd = rrho0-fromY;
        double rho = Math.sqrt(fromX * fromX + yd*yd);
        double theta = Math.atan2( fromX, yd);
        if (n < 0) rho *= -1.0;

        toLat = Math.toDegrees(Math.asin((C-Math.pow((rho*n/EARTH_RADIUS),2))/(2*n)));

        toLon = Math.toDegrees(theta/n + lon0);

             				 
              toLatA[i]= (double)toLat;
              toLonA[i]= (double)toLon;
	}
	return to;
    }

/*ENDGENERATED*/

    /**
     * Test
     *
     * @param args not used
     */
    public static void main(String[] args) {
        AlbersEqualArea     a = new AlbersEqualArea(23, -96, 29.5, 45.5);
        System.out.println("ll = 35N 75W");
        ProjectionPointImpl p = a.latLonToProj(35, -75);
        System.out.println("proj point = " + p);
        LatLonPoint ll = a.projToLatLon(p);
        System.out.println("ll = " + ll);
    }

}















