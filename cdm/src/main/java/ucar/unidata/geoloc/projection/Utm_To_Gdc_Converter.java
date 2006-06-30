//
// Filename: Utm_To_Gdc_Converter.java
//
// Author: Dan Toms, SRI International
//
// Package: GeoTransform http://www.ai.sri.com/geotransform/
//
// Acknowledgements:
//   The algorithms used in the package were created by Ralph Toms and
//   first appeared as part of the SEDRIS Coordinate Transformation API.
//   These were subsequently modified for this package. This package is
//   not part of the SEDRIS project, and the Java code written for this
//   package has not been certified or tested for correctness by NIMA.
//
// License:
//   The contents of this file are subject to GeoTransform License Agreement
//   (the "License"); you may not use this file except in compliance with
//   the License. You may obtain a copy of the License at
//   http://www.ai.sri.com/geotransformtest/license.html
//
//   Software distributed under the License is distributed on an "AS IS"
//   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
//   the License for the specific language governing rights and limitations
//   under the License.
//
//   Portions are Copyright (c) SRI International, 1998.
//

package ucar.unidata.geoloc.projection;


import java.lang.*;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.EarthEllipsoid;


/**
 * Class: Utm_To_Gdc_Converter
 *
 * Converts UTM coordinate(s) to GDC.
 * <p/>
 * This class provides the capability to convert from
 * Universal Transverse Mercator (UTM) coordinates to
 * geodetic (GDC), i.e. lat/long.
 * This is a direct conversion.
 *
 * @author Dan Toms, SRI International
 *
 * modified JCaron 01/2005
 * <ol>
 * <li> turn static methods into object methods, to make thread-safe
 * <li> rename methods to follow upper/lower case conventions
 * <li> add convenience methods for ucar.unidata.geoloc.Projection
 * <li> longitude must be in range +=180.
 * </ol>
 *
 * random testing shows:
 *   avg error x= 0.4 y=0.06 meters
 *   but sometimes x error can be as high as 15 meters
 *   where err = abs(x - inverse(f(x)))
 *
 * timing: inverse(f(x)) takes 2 - 3 microseconds.
 *
 * @version $Id: Utm_To_Gdc_Converter.java,v 1.4 2006/01/14 22:15:59 caron Exp $
 */

class Utm_To_Gdc_Converter {

    /** _more_ */
    static final double DEGREES_PER_RADIAN = 57.2957795130823208768;

    /** _more_ */
    private double A, F, C, Eps2, Eps21, Eps25, Con, Con2, EF, Epsp2, Con6,
                   Con24, Con120, Con720, polx2b, polx3b, polx4b, polx5b,
                   conap;

    /** _more_ */
    private int zone;

    /** _more_ */
    private boolean hemisphere_north;

    /**
     * Constructor.
     * @param a                the semi-major axis (meters) for the ellipsoid
     * @param f                the inverse flattening for the ellipsoid
     * @param zone             the UTM zone number (1..60)
     * @param hemisphere_north true if the UTM coordinate is in the northern hemisphere
     */
    public Utm_To_Gdc_Converter(double a, double f, int zone,
                                boolean hemisphere_north) {
        init(a, f, zone, hemisphere_north);
    }

    /**
     * Default contructor uses WGS 84 ellipsoid
     * @param zone             the UTM zone number (1..60)
     * @param hemisphere_north true if the UTM coordinate is in the northern hemisphere
     */
    public Utm_To_Gdc_Converter(int zone, boolean hemisphere_north) {
        this(EarthEllipsoid.WGS84, zone, hemisphere_north);  // default to wgs 84
    }

    /*
     * Constructor with ellipsoid.
     * @param E an Ellipsoid instance for the ellipsoid, e.g. WE_Ellipsoid
     * @param zone             the UTM zone number (1..60)
     * @param hemisphere_north true if the UTM coordinate is in the northern hemisphere
     */

    /**
     * _more_
     *
     * @param ellipse _more_
     * @param zone _more_
     * @param hemisphere_north _more_
     */
    public Utm_To_Gdc_Converter(EarthEllipsoid ellipse, int zone,
                                boolean hemisphere_north) {
        init(ellipse.getMajor(), ellipse.getFlattening(), zone,
             hemisphere_north);
    }

    /**
     * _more_
     *
     * @param a _more_
     * @param f _more_
     * @param zone _more_
     * @param hemisphere_north _more_
     */
    protected void init(double a, double f, int zone,
                        boolean hemisphere_north) {
        A                     = a;
        F                     = f;
        this.zone             = zone;
        this.hemisphere_north = hemisphere_north;

        //  Create the ERM constants.
        F      = 1 / (F);
        C      = (A) * (1 - F);
        Eps2   = (F) * (2.0 - F);
        Eps25  = .25 * (Eps2);
        EF     = F / (2.0 - F);
        Con    = (1.0 - Eps2);
        Con2   = 2 / (1.0 - Eps2);
        Con6   = .166666666666667;
        Con24  = 4 * .0416666666666667 / (1 - Eps2);
        Con120 = .00833333333333333;
        Con720 = 4 * .00138888888888888 / (1 - Eps2);
        double polx1a = 1.0 - Eps2 / 4.0 - 3.0 / 64.0 * Math.pow(Eps2, 2)
                        - 5.0 / 256.0 * Math.pow(Eps2, 3)
                        - 175.0 / 16384.0 * Math.pow(Eps2, 4);

        conap = A * polx1a;

        double polx2a = 3.0 / 2.0 * EF - 27.0 / 32.0 * Math.pow(EF, 3);

        double polx4a = 21.0 / 16.0 * Math.pow(EF, 2)
                        - 55.0 / 32.0 * Math.pow(EF, 4);

        double polx6a = 151.0 / 96.0 * Math.pow(EF, 3);

        double polx8a = 1097.0 / 512.0 * Math.pow(EF, 4);

        polx2b = polx2a * 2.0 + polx4a * 4.0 + polx6a * 6.0 + polx8a * 8.0;

        polx3b = polx4a * -8.0 - polx6a * 32.0 - 80.0 * polx8a;
        polx4b = polx6a * 32.0 + 192.0 * polx8a;
        polx5b = -128.0 * polx8a;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getA() {
        return A;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getF() {
        return F;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getZone() {
        return zone;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isNorth() {
        return hemisphere_north;
    }

    /**
     *  @param x      the UTM easting coordinate (meters)
     *  @param y      the UTM northing coordinate (meters)
     *  @param latlon put result here
     *  @return LatLonPoint
     */
    public LatLonPoint projToLatLon(double x, double y,
                                    LatLonPointImpl latlon) {

        double source_x, source_y, u, su, cu, su2, xlon0, temp, phi1;
        double sp, sp2, cp, cp2, tp, tp2, eta2, top, rn, b3, b4, b5, b6, d1,
               d2;

        source_x = x * 1000.0;  // wants meters
        source_x = (source_x - 500000.0) / .9996;

        if (hemisphere_north) {
            source_y = y * 1000.0 / .9996;
        } else {
            source_y = (y * 1000.0 - 1.0E7) / .9996;
        }

        u = source_y / conap;

        /* TEST U TO SEE IF AT POLES */
        su    = Math.sin(u);
        cu    = Math.cos(u);
        su2   = su * su;

        xlon0 = (6.0 * ((double) zone) - 183.0) / DEGREES_PER_RADIAN;
        temp  = polx2b + su2 * (polx3b + su2 * (polx4b + su2 * polx5b));
        phi1  = u + su * cu * temp;

        sp    = Math.sin(phi1);
        cp    = Math.cos(phi1);
        tp    = sp / cp;
        tp2   = tp * tp;
        sp2   = sp * sp;
        cp2   = cp * cp;
        eta2  = Epsp2 * cp2;

        top   = .25 - (sp2 * (Eps2 / 4));

        /* inline sq root*/
        rn = A / ((.25 - Eps25 * sp2 + .9999944354799 / 4)
                  + (.25 - Eps25 * sp2)
                    / (.25 - Eps25 * sp2 + .9999944354799 / 4));

        b3 = 1.0 + tp2 + tp2 + eta2;
        b4 = 5 + tp2 * (3 - 9 * eta2) + eta2 * (1 - 4 * eta2);
        b5 = 5 + tp2 * (tp2 * 24.0 + 28.0);
        b5 += eta2 * (tp2 * 8.0 + 6.0);
        b6 = 46.0 - 3.0 * eta2 + tp2 * (-252.0 - tp2 * 90.0);
        b6 = eta2 * (b6 + eta2 * tp2 * (tp2 * 225.0 - 66.0));
        b6 += 61.0 + tp2 * (tp2 * 45.0 + 90.0);

        d1 = source_x / rn;
        d2 = d1 * d1;

        double latitude = phi1
                          - tp * top
                            * (d2 * (Con2
                                     + d2 * ((-Con24) * b4
                                             + d2 * Con720 * b6)));
        double longitude = xlon0
                           + d1 * (1.0 + d2 * (-Con6 * b3 + d2 * Con120 * b5))
                             / cp;

        latlon.setLatitude(latitude * DEGREES_PER_RADIAN);
        latlon.setLongitude(longitude * DEGREES_PER_RADIAN);
        return latlon;
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     *
     * @return _more_
     */
    public float[][] projToLatLon(float[][] from, float[][] to) {

        double source_x, source_y, u, su, cu, su2, temp, phi1;
        double sp, sp2, cp, cp2, tp, tp2, eta2, top, rn, b3, b4, b5, b6, d1,
               d2;

        double xlon0 = (6.0 * ((double) zone) - 183.0) / DEGREES_PER_RADIAN;

        for (int i = 0; i < from[0].length; i++) {
            source_x = from[0][i] * 1000.0;  // wants meters
            source_x = (source_x - 500000.0) / .9996;

            if (hemisphere_north) {
                source_y = from[1][i] * 1000.0 / .9996;
            } else {
                source_y = (from[1][i] * 1000.0 - 1.0E7) / .9996;
            }

            u    = source_y / conap;
            su   = Math.sin(u);
            cu   = Math.cos(u);
            su2  = su * su;

            temp = polx2b + su2 * (polx3b + su2 * (polx4b + su2 * polx5b));
            phi1 = u + su * cu * temp;

            sp   = Math.sin(phi1);
            cp   = Math.cos(phi1);
            tp   = sp / cp;
            tp2  = tp * tp;
            sp2  = sp * sp;
            cp2  = cp * cp;
            eta2 = Epsp2 * cp2;

            top  = .25 - (sp2 * (Eps2 / 4));

            /* inline sq root*/
            rn = A / ((.25 - Eps25 * sp2 + .9999944354799 / 4)
                      + (.25 - Eps25 * sp2)
                        / (.25 - Eps25 * sp2 + .9999944354799 / 4));

            b3 = 1.0 + tp2 + tp2 + eta2;
            b4 = 5 + tp2 * (3 - 9 * eta2) + eta2 * (1 - 4 * eta2);
            b5 = 5 + tp2 * (tp2 * 24.0 + 28.0);
            b5 += eta2 * (tp2 * 8.0 + 6.0);
            b6 = 46.0 - 3.0 * eta2 + tp2 * (-252.0 - tp2 * 90.0);
            b6 = eta2 * (b6 + eta2 * tp2 * (tp2 * 225.0 - 66.0));
            b6 += 61.0 + tp2 * (tp2 * 45.0 + 90.0);

            d1 = source_x / rn;
            d2 = d1 * d1;

            double latitude = phi1
                              - tp * top
                                * (d2 * (Con2
                                         + d2 * ((-Con24) * b4
                                                 + d2 * Con720 * b6)));
            double longitude =
                xlon0
                + d1 * (1.0 + d2 * (-Con6 * b3 + d2 * Con120 * b5)) / cp;

            to[0][i] = (float) (latitude * DEGREES_PER_RADIAN);
            to[1][i] = (float) (longitude * DEGREES_PER_RADIAN);
        }

        return to;
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     *
     * @return _more_
     */
    public double[][] projToLatLon(double[][] from, double[][] to) {

        double source_x, source_y, u, su, cu, su2, temp, phi1;
        double sp, sp2, cp, cp2, tp, tp2, eta2, top, rn, b3, b4, b5, b6, d1,
               d2;

        double xlon0 = (6.0 * ((double) zone) - 183.0) / DEGREES_PER_RADIAN;

        for (int i = 0; i < from[0].length; i++) {
            source_x = from[0][i] * 1000.0;  // wants meters
            source_x = (source_x - 500000.0) / .9996;

            if (hemisphere_north) {
                source_y = from[1][i] * 1000.0 / .9996;
            } else {
                source_y = (from[1][i] * 1000.0 - 1.0E7) / .9996;
            }

            u    = source_y / conap;
            su   = Math.sin(u);
            cu   = Math.cos(u);
            su2  = su * su;

            temp = polx2b + su2 * (polx3b + su2 * (polx4b + su2 * polx5b));
            phi1 = u + su * cu * temp;

            sp   = Math.sin(phi1);
            cp   = Math.cos(phi1);
            tp   = sp / cp;
            tp2  = tp * tp;
            sp2  = sp * sp;
            cp2  = cp * cp;
            eta2 = Epsp2 * cp2;

            top  = .25 - (sp2 * (Eps2 / 4));

            /* inline sq root*/
            rn = A / ((.25 - Eps25 * sp2 + .9999944354799 / 4)
                      + (.25 - Eps25 * sp2)
                        / (.25 - Eps25 * sp2 + .9999944354799 / 4));

            b3 = 1.0 + tp2 + tp2 + eta2;
            b4 = 5 + tp2 * (3 - 9 * eta2) + eta2 * (1 - 4 * eta2);
            b5 = 5 + tp2 * (tp2 * 24.0 + 28.0);
            b5 += eta2 * (tp2 * 8.0 + 6.0);
            b6 = 46.0 - 3.0 * eta2 + tp2 * (-252.0 - tp2 * 90.0);
            b6 = eta2 * (b6 + eta2 * tp2 * (tp2 * 225.0 - 66.0));
            b6 += 61.0 + tp2 * (tp2 * 45.0 + 90.0);

            d1 = source_x / rn;
            d2 = d1 * d1;

            double latitude = phi1
                              - tp * top
                                * (d2 * (Con2
                                         + d2 * ((-Con24) * b4
                                                 + d2 * Con720 * b6)));
            double longitude =
                xlon0
                + d1 * (1.0 + d2 * (-Con6 * b3 + d2 * Con120 * b5)) / cp;

            to[0][i] = (latitude * DEGREES_PER_RADIAN);
            to[1][i] = (longitude * DEGREES_PER_RADIAN);
        }

        return to;
    }

}


