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


package ucar.unidata.geoloc;


import java.lang.Math;


/**
 * Computes the distance, azimuth, and back azimuth between
 * two lat-lon positions on the Earth's surface. Reference ellipsoid is the WGS-84.
 *
 * You may use a default Earth (equator radius = 6378137.0 meters,
 * flattening =  1.0 / 298.257223563) or you may define your own using
 * a ucar.unidata.geoloc.Earth object.
 *
 * @author Unidata Development Team
 */
public class Bearing {

    /**
     * Default Earth.  Major radius and flattening;
     *
     */
    private static final Earth defaultEarth = new Earth(6378137.0, 0., 298.257223563);

    /**
     * Earth radius
     */
    private static double A;
    //private static double A = 6378137.0;  // in meters (for reference)

    /**
     * The Earth flattening value
     */
    private static double F;
    //private static double F = 1.0 / 298.257223563;  (for reference)

    /**
     * epsilon
     */
    private static final double EPS = 0.5E-13;

    /**
     * constant R
     */
    //private static final double R = 1.0 - F;  (for reference)
    private static double R;

    /**
     * conversion for degrees to radians
     */
    private static final double rad = Math.toRadians(1.0);

    /**
     * conversion for radians to degrees
     */
    private static final double deg = Math.toDegrees(1.0);

    /**
     * Calculate the bearing between the 2 points.
     * See calculateBearing below.
     *
     * @param e      Earth object (defines radius & flattening)
     * @param pt1    Point 1
     * @param pt2    Point 2
     * @param result Object to use if non-null
     * @return The bearing
     */
    public static Bearing calculateBearing(Earth e, LatLonPoint pt1,
                                           LatLonPoint pt2, Bearing result) {

        return calculateBearing(e, pt1.getLatitude(), pt1.getLongitude(),
                                pt2.getLatitude(), pt2.getLongitude(),
                                result);
    }


    /**
     * Calculate the bearing between the 2 points.
     * See calculateBearing below.  Uses default Earth object.
     *
     * @param pt1    Point 1
     * @param pt2    Point 2
     * @param result Object to use if non-null
     * @return The bearing
     */
    public static Bearing calculateBearing(LatLonPoint pt1, LatLonPoint pt2,
                                           Bearing result) {

        return calculateBearing(defaultEarth, pt1.getLatitude(),
                                pt1.getLongitude(), pt2.getLatitude(),
                                pt2.getLongitude(), result);
    }

    /** _more_          */
    private static int maxLoopCnt = 0;

    /**
     * Computes distance (in km), azimuth (degrees clockwise positive
     * from North, 0 to 360), and back azimuth (degrees clockwise positive
     * from North, 0 to 360), from latitude-longituide point pt1 to
     * latitude-longituide pt2. Uses default Earth object.
     *
     * @param lat1   Lat of point 1
     * @param lon1   Lon of point 1
     * @param lat2   Lat of point 2
     * @param lon2   Lon of point 2
     * @param result put result here, or null to allocate
     * @return a Bearing object with distance (in km), azimuth from
     *         pt1 to pt2 (degrees, 0 = north, clockwise positive)
     */
    public static Bearing calculateBearing(double lat1, double lon1,
                                           double lat2, double lon2,
                                           Bearing result) {

        return calculateBearing(defaultEarth, lat1, lon1, lat2, lon2, result);
    }



    /**
     * Computes distance (in km), azimuth (degrees clockwise positive
     * from North, 0 to 360), and back azimuth (degrees clockwise positive
     * from North, 0 to 360), from latitude-longituide point pt1 to
     * latitude-longituide pt2.<p>
     * Algorithm from U.S. National Geodetic Survey, FORTRAN program "inverse,"
     * subroutine "INVER1," by L. PFEIFER and JOHN G. GERGEN.
     * See http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
     * <P>Original documentation:
     * <br>SOLUTION OF THE GEODETIC INVERSE PROBLEM AFTER T.VINCENTY
     * <br>MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
     * <br>EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
     * <br>STANDPOINT/FOREPOINT MUST NOT BE THE GEOGRAPHIC POLE
     * </P>
     * Reference ellipsoid is the WGS-84 ellipsoid.
     * <br>See http://www.colorado.edu/geography/gcraft/notes/datum/elist.html
     * <p/>
     * Requires close to 1.4 E-5 seconds wall clock time per call
     * on a 550 MHz Pentium with Linux 7.2.
     *
     * @param e      Earth object (defines radius and flattening)
     * @param lat1   Lat of point 1
     * @param lon1   Lon of point 1
     * @param lat2   Lat of point 2
     * @param lon2   Lon of point 2
     * @param result put result here, or null to allocate
     * @return a Bearing object with distance (in km), azimuth from
     *         pt1 to pt2 (degrees, 0 = north, clockwise positive)
     */
    public static Bearing calculateBearing(Earth e, double lat1, double lon1,
                                           double lat2, double lon2,
                                           Bearing result) {

        if (result == null) {
            result = new Bearing();
        }

        if ((lat1 == lat2) && (lon1 == lon2)) {
            result.distance    = 0;
            result.azimuth     = 0;
            result.backazimuth = 0;
            return result;
        }

        A = e.getMajor();
        F = e.getFlattening();
        R = 1.0 - F;

        // Algorithm from National Geodetic Survey, FORTRAN program "inverse,"
        // subroutine "INVER1," by L. PFEIFER and JOHN G. GERGEN.
        // http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
        // Conversion to JAVA from FORTRAN was made with as few changes as possible
        // to avoid errors made while recasting form, and to facilitate any future
        // comparisons between the original code and the altered version in Java.
        // Original documentation:
        // SOLUTION OF THE GEODETIC INVERSE PROBLEM AFTER T.VINCENTY
        // MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
        // EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
        // STANDPOINT/FOREPOINT MUST NOT BE THE GEOGRAPHIC POLE
        // A IS THE SEMI-MAJOR AXIS OF THE REFERENCE ELLIPSOID
        // F IS THE FLATTENING (NOT RECIPROCAL) OF THE REFERNECE ELLIPSOID
        // LATITUDES GLAT1 AND GLAT2
        // AND LONGITUDES GLON1 AND GLON2 ARE IN RADIANS POSITIVE NORTH AND EAST
        // FORWARD AZIMUTHS AT BOTH POINTS RETURNED IN RADIANS FROM NORTH
        //
        // Reference ellipsoid is the WGS-84 ellipsoid.
        // See http://www.colorado.edu/geography/gcraft/notes/datum/elist.html
        // FAZ is forward azimuth in radians from pt1 to pt2;
        // BAZ is backward azimuth from point 2 to 1;
        // S is distance in meters.
        //
        // Conversion to JAVA from FORTRAN was made with as few changes as possible
        // to avoid errors made while recasting form, and to facilitate any future
        // comparisons between the original code and the altered version in Java.
        //
        //IMPLICIT REAL*8 (A-H,O-Z)
        //  COMMON/CONST/PI,RAD
        //  COMMON/ELIPSOID/A,F
        double GLAT1 = rad * lat1;
        double GLAT2 = rad * lat2;
        double TU1   = R * Math.sin(GLAT1) / Math.cos(GLAT1);
        double TU2   = R * Math.sin(GLAT2) / Math.cos(GLAT2);
        double CU1   = 1. / Math.sqrt(TU1 * TU1 + 1.);
        double SU1   = CU1 * TU1;
        double CU2   = 1. / Math.sqrt(TU2 * TU2 + 1.);
        double S     = CU1 * CU2;
        double BAZ   = S * TU2;
        double FAZ   = BAZ * TU1;
        double GLON1 = rad * lon1;
        double GLON2 = rad * lon2;
        double X     = GLON2 - GLON1;
        double D, SX, CX, SY, CY, Y, SA, C2A, CZ, E, C;
        int    loopCnt = 0;
        do {
            loopCnt++;
            //Check for an infinite loop
            if (loopCnt > 1000) {
                throw new IllegalArgumentException(
                    "Too many iterations calculating bearing:" + lat1 + " "
                    + lon1 + " " + lat2 + " " + lon2);
            }
            SX  = Math.sin(X);
            CX  = Math.cos(X);
            TU1 = CU2 * SX;
            TU2 = BAZ - SU1 * CU2 * CX;
            SY  = Math.sqrt(TU1 * TU1 + TU2 * TU2);
            CY  = S * CX + FAZ;
            Y   = Math.atan2(SY, CY);
            SA  = S * SX / SY;
            C2A = -SA * SA + 1.;
            CZ  = FAZ + FAZ;
            if (C2A > 0.) {
                CZ = -CZ / C2A + CY;
            }
            E = CZ * CZ * 2. - 1.;
            C = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
            D = X;
            X = ((E * CY * C + CZ) * SY * C + Y) * SA;
            X = (1. - C) * X * F + GLON2 - GLON1;
            //IF(DABS(D-X).GT.EPS) GO TO 100
        } while (Math.abs(D - X) > EPS);

        if (loopCnt > maxLoopCnt) {
            maxLoopCnt = loopCnt;
            //        System.err.println("loopCnt:" + loopCnt);
        }

        FAZ = Math.atan2(TU1, TU2);
        BAZ = Math.atan2(CU1 * SX, BAZ * CX - SU1 * CU2) + Math.PI;
        X   = Math.sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
        X   = (X - 2.) / X;
        C   = 1. - X;
        C   = (X * X / 4. + 1.) / C;
        D   = (0.375 * X * X - 1.) * X;
        X   = E * CY;
        S   = 1. - E - E;
        S = ((((SY * SY * 4. - 3.) * S * CZ * D / 6. - X) * D / 4. + CZ) * SY
             * D + Y) * C * A * R;

        result.distance = S / 1000.0;  // meters to km
        result.azimuth  = FAZ * deg;   // radians to degrees

        if (result.azimuth < 0.0) {
            result.azimuth += 360.0;  // reset azs from -180 to 180 to 0 to 360
        }

        result.backazimuth = BAZ * deg;  // radians to degrees; already in 0 to 360 range

        return result;
    }

    /*
    * This method is for same use as calculateBearing, but has much simpler calculations
    * by assuming a spherical earth. It is actually slower than
    * "calculateBearing" code, probably due to having more trig function calls.
    * It is less accurate, too.
    * Errors are on the order of 1/300 or less. This code
    * saved here only as a warning to future programmers thinking of this approach.
    *
    * Requires close to 2.0 E-5 seconds wall clock time per call
    * on a 550 MHz Pentium with Linux 7.2.
    *
    * public static Bearing calculateBearingAlternate
    *   (LatLonPoint pt1, LatLonPoint pt2, Bearing result) {
    *
    * // to convert degrees to radians, multiply by:
    * final double rad = Math.toRadians(1.0);
    * // to convert radians to degrees:
    * final double deg = Math.toDegrees(1.0);
    *
    * if (result == null)
    *   result = new Bearing();
    *
    * double R = 6371008.7;  // mean earth radius in meters; WGS 84 definition
    * double GLAT1 = rad*(pt1.getLatitude());
    * double GLAT2 = rad*(pt2.getLatitude());
    * double GLON1 = rad*(pt1.getLongitude());
    * double GLON2 = rad*(pt2.getLongitude());
    *
    * // great circle angular separation in radians
    * double alpha = Math.acos( Math.sin(GLAT1)*Math.sin(GLAT2)
    *                          +Math.cos(GLAT1)*Math.cos(GLAT2)*Math.cos(GLON1-GLON2) );
    * // great circle distance in meters
    * double gcd = R * alpha;
    *
    * result.distance = gcd / 1000.0;      // meters to km
    *
    * // forward azimuth from point 1 to 2 in radians
    * double s2 = rad*(90.0-pt2.getLatitude());
    * double FAZ = Math.asin(Math.sin(s2)*Math.sin(GLON2-GLON1) / Math.sin(alpha));
    *
    * result.azimuth = FAZ * deg;        // radians to degrees
    * if (result.azimuth < 0.0)
    * result.azimuth += 360.0;       // reset az from -180 to 180 to 0 to 360
    *
    * // back azimuth from point 2 to 1 in radians
    * double s1 = rad*(90.0-pt1.getLatitude());
    * double BAZ = Math.asin(Math.sin(s1)*Math.sin(GLON1-GLON2) / Math.sin(alpha));
    *
    * result.backazimuth = BAZ * deg;
    * if (result.backazimuth < 0.0)
    *     result.backazimuth += 360.0;   // reset backaz from -180 to 180 to 0 to 360
    *
    * return result;
    * }
    */

    /**
     * the azimuth, degrees, 0 = north, clockwise positive
     */
    private double azimuth;

    /**
     * the back azimuth, degrees, 0 = north, clockwise positive
     */
    private double backazimuth;

    /**
     * separation in kilometers
     */
    private double distance;

    /**
     * Get the azimuth in degrees, 0 = north, clockwise positive
     *
     * @return azimuth in degrees
     */
    public double getAngle() {
        return azimuth;
    }

    /**
     * Get the back azimuth in degrees, 0 = north, clockwise positive
     *
     * @return back azimuth in degrees
     */
    public double getBackAzimuth() {
        return backazimuth;
    }

    /**
     * Get the distance in kilometers
     *
     * @return distance in km
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Nice format.
     *
     * @return return a nice format of this Bearing
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Azimuth: ");
        buf.append(azimuth);
        buf.append(" Back azimuth: ");
        buf.append(backazimuth);
        buf.append(" Distance: ");
        buf.append(distance);
        return buf.toString();
    }


    /**
     * Test the calculations - forward and back
     *
     * @param args non used
     */
    public static void main(String[] args) {
        //Bearing         workBearing = new Bearing();
        LatLonPointImpl pt1 = new LatLonPointImpl(40, -105);
        LatLonPointImpl pt2 = new LatLonPointImpl(37.4, -118.4);
        Bearing         b   = calculateBearing(pt1, pt2, null);
        System.out.println("Bearing from " + pt1 + " to " + pt2 + " = \n\t"
                           + b);
        LatLonPointImpl pt3 = new LatLonPointImpl();
        pt3 = findPoint(pt1, b.getAngle(), b.getDistance(), pt3);
        System.out.println(
            "using first point, angle and distance, found second point at "
            + pt3);
        pt3 = findPoint(pt2, b.getBackAzimuth(), b.getDistance(), pt3);
        System.out.println(
            "using second point, backazimuth and distance, found first point at "
            + pt3);
        /*  uncomment for timing tests
        for(int j=0;j<10;j++) {
            long t1 = System.currentTimeMillis();
            for(int i=0;i<30000;i++) {
                workBearing = Bearing.calculateBearing(42.5,-93.0,
                                                       48.9,-117.09,workBearing);
            }
            long t2 = System.currentTimeMillis();
            System.err.println ("time:" + (t2-t1));
        }
        */
    }


    /**
     * Calculate a position given an azimuth and distance from
     * another point.
     *
     * @param e      Earth object (defines radius and flattening)
     * @param pt1    Point 1
     * @param az     azimuth (degrees)
     * @param dist   distance from the point (km)
     * @param result Object to use if non-null
     * @return The LatLonPoint
     * @see #findPoint(double,double,double,double,LatLonPointImpl)
     */
    public static LatLonPointImpl findPoint(Earth e, LatLonPoint pt1,
                                            double az, double dist,
                                            LatLonPointImpl result) {
        return findPoint(e, pt1.getLatitude(), pt1.getLongitude(), az, dist,
                         result);
    }

    /**
     * Calculate a position given an azimuth and distance from
     * another point.  Uses default Earth.
     *
     * @param pt1    Point 1
     * @param az     azimuth (degrees)
     * @param dist   distance from the point (km)
     * @param result Object to use if non-null
     * @return The LatLonPoint
     * @see #findPoint(double,double,double,double,LatLonPointImpl)
     */
    public static LatLonPointImpl findPoint(LatLonPoint pt1, double az,
                                            double dist,
                                            LatLonPointImpl result) {
        return findPoint(defaultEarth, pt1.getLatitude(), pt1.getLongitude(),
                         az, dist, result);
    }

    /**
     * Calculate a position given an azimuth and distance from
     * another point.  See details, below.  Uses default Earth.
     *
     * @param lat1   latitude of starting point
     * @param lon1   longitude of starting point
     * @param az     forward azimuth (degrees)
     * @param dist   distance from the point (km)
     * @param result Object to use if non-null
     * @return the position as a LatLonPointImpl
     */
    public static LatLonPointImpl findPoint(double lat1, double lon1,
                                            double az, double dist,
                                            LatLonPointImpl result) {
        return findPoint(defaultEarth, lat1, lon1, az, dist, result);
    }


    /**
     * Calculate a position given an azimuth and distance from
     * another point.
     * <p/>
     * <p/>
     * Algorithm from National Geodetic Survey, FORTRAN program "forward,"
     * subroutine "DIRCT1," by stephen j. frakes.
     * http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
     * <p>Original documentation:
     * <pre>
     *    SOLUTION OF THE GEODETIC DIRECT PROBLEM AFTER T.VINCENTY
     *    MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
     *    EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
     *  </pre>
     *
     * @param e      Earth object (defines radius and flattening)
     * @param lat1   latitude of starting point
     * @param lon1   longitude of starting point
     * @param az     forward azimuth (degrees)
     * @param dist   distance from the point (km)
     * @param result Object to use if non-null
     * @return the position as a LatLonPointImpl
     */
    public static LatLonPointImpl findPoint(Earth e, double lat1,
                                            double lon1, double az,
                                            double dist,
                                            LatLonPointImpl result) {
        if (result == null) {
            result = new LatLonPointImpl();
        }

        if ((dist == 0)) {
            result.setLatitude(lat1);
            result.setLongitude(lon1);
            return result;
        }

        A = e.getMajor();
        F = e.getFlattening();
        R = 1.0 - F;

        // Algorithm from National Geodetic Survey, FORTRAN program "forward,"
        // subroutine "DIRCT1," by stephen j. frakes.
        // http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
        // Conversion to JAVA from FORTRAN was made with as few changes as
        // possible to avoid errors made while recasting form, and
        // to facilitate any future comparisons between the original
        // code and the altered version in Java.
        // Original documentation:
        //   SUBROUTINE DIRCT1(GLAT1,GLON1,GLAT2,GLON2,FAZ,BAZ,S)
        //
        //   SOLUTION OF THE GEODETIC DIRECT PROBLEM AFTER T.VINCENTY
        //   MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
        //   EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
        //
        //   A IS THE SEMI-MAJOR AXIS OF THE REFERENCE ELLIPSOID
        //   F IS THE FLATTENING OF THE REFERENCE ELLIPSOID
        //   LATITUDES AND LONGITUDES IN RADIANS POSITIVE NORTH AND EAST
        //   AZIMUTHS IN RADIANS CLOCKWISE FROM NORTH
        //   GEODESIC DISTANCE S ASSUMED IN UNITS OF SEMI-MAJOR AXIS A
        //
        //   PROGRAMMED FOR CDC-6600 BY LCDR L.PFEIFER NGS ROCKVILLE MD 20FEB75
        //   MODIFIED FOR SYSTEM 360 BY JOHN G GERGEN NGS ROCKVILLE MD 750608
        //

        if (az < 0.0) {
            az += 360.0;  // reset azs from -180 to 180 to 0 to 360
        }
        double FAZ   = az * rad;
        double GLAT1 = lat1 * rad;
        double GLON1 = lon1 * rad;
        double S     = dist * 1000.;  // convert to meters
        double TU    = R * Math.sin(GLAT1) / Math.cos(GLAT1);
        double SF    = Math.sin(FAZ);
        double CF    = Math.cos(FAZ);
        double BAZ   = 0.;
        if (CF != 0) {
            BAZ = Math.atan2(TU, CF) * 2;
        }
        double CU  = 1. / Math.sqrt(TU * TU + 1.);
        double SU  = TU * CU;
        double SA  = CU * SF;
        double C2A = -SA * SA + 1.;
        double X   = Math.sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
        X = (X - 2.) / X;
        double C = 1. - X;
        C = (X * X / 4. + 1) / C;
        double D = (0.375 * X * X - 1.) * X;
        TU = S / R / A / C;
        double Y = TU;
        double SY, CY, CZ, E, GLAT2, GLON2;
        do {
            SY = Math.sin(Y);
            CY = Math.cos(Y);
            CZ = Math.cos(BAZ + Y);
            E  = CZ * CZ * 2. - 1.;
            C  = Y;
            X  = E * CY;
            Y  = E + E - 1.;
            Y = (((SY * SY * 4. - 3.) * Y * CZ * D / 6. + X) * D / 4. - CZ)
                * SY * D + TU;
        } while (Math.abs(Y - C) > EPS);
        BAZ   = CU * CY * CF - SU * SY;
        C     = R * Math.sqrt(SA * SA + BAZ * BAZ);
        D     = SU * CY + CU * SY * CF;
        GLAT2 = Math.atan2(D, C);
        C     = CU * CY - SU * SY * CF;
        X     = Math.atan2(SY * SF, C);
        C     = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
        D     = ((E * CY * C + CZ) * SY * C + Y) * SA;
        GLON2 = GLON1 + X - (1. - C) * D * F;
        BAZ   = (Math.atan2(SA, BAZ) + Math.PI) * deg;
        result.setLatitude(GLAT2 * deg);
        result.setLongitude(GLON2 * deg);
        return result;
    }

}

