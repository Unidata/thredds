/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
//
// Filename: Gdc_To_Utm_Converter.java
//
// Author: Dan Toms, SRI International
//
// Package: GeoTransform <http://www.ai.sri.com/geotransformtest/>
//
// Acknowledgements:
// The algorithms used in the package were created by Ralph Toms and
// first appeared as part of the SEDRIS Coordinate Transformation API.
// These were subsequently modified for this package. This package is
// not part of the SEDRIS project, and the Java code written for this
// package has not been certified or tested for correctness by NIMA.
//
// License:
// The contents of this file are subject to GeoTransform License Agreement
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.ai.sri.com/geotransformtest/license.html
//
// Software distributed under the License is distributed on an "AS IS"
// basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
// the License for the specific language governing rights and limitations
// under the License.
//
// Portions are Copyright (c) SRI International, 1998.
//

package ucar.unidata.geoloc.projection;


import ucar.unidata.geoloc.EarthEllipsoid;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;


import java.lang.*;

/**
 * Converts GDC coordinate(s) to UTM.
 * <p/>
 * This class provides the capability to convert from
 * geodetic (GDC), i.e. lat/long coordinates to
 * Universal Transverse Mercator (UTM).
 * This is a direct conversion.
 *
 * @author Dan Toms, SRI International
 *         <p/>
 *         modified JCaron 01/2005
 *         <ol>
 *         <li> turn static methods into object methods, to make thread-safe
 *         <li> rename methods to follow upper/lower case conventions
 *         <li> add convenience methods for ucar.unidata.geoloc.Projection
 *         <li> longitude must be in range +=180.
 *         </ol>
 *         <p/>
 *         random testing shows:
 *         avg error x= 0.4 y=0.06 meters
 *         but sometimes x error can be as high as 15 meters
 *         where err = abs(x - inverse(f(x)))
 *         <p/>
 *         timing: inverse(f(x)) takes 2 - 3 microseconds.
 */

class Gdc_To_Utm_Converter {
  static final double RADIANS_PER_DEGREE = 0.0174532925199432957692;

  private double A;
  private double F; // flattening
  private double Eps2, Eps25, Epps2, CScale = .9996, poly1b, poly2b, poly3b, poly4b, poly5b;
  private double axlon0, axlon0_deg;

  /**
   * Constructor.
   *
   * @param a                the semi-major axis (meters) for the ellipsoid
   * @param f                the inverse flattening for the ellipsoid
   * @param zone             the UTM zone number (1..60)
   * @param hemisphere_north true if the UTM coordinate is in the northern hemisphere
   */
  public Gdc_To_Utm_Converter(double a, double f, int zone, boolean hemisphere_north) {
    init(a, f, zone, hemisphere_north);
  }

  /**
   * Default contructor uses WGS 84 ellipsoid
   *
   * @param zone             the UTM zone number (1..60)
   * @param hemisphere_north true if the UTM coordinate is in the northern hemisphere
   */
  public Gdc_To_Utm_Converter(int zone, boolean hemisphere_north) {
    this(EarthEllipsoid.WGS84, zone, hemisphere_north);  // default to wgs 84
  }

  /**
  * Constructor with ellipsoid.
  * @param ellipse an EarthEllipsoid, e.g. WE_Ellipsoid
  * @param zone             the UTM zone number (1..60)
  * @param isNorth   true if the UTM coordinate is in the northern hemisphere
  */
  public Gdc_To_Utm_Converter(EarthEllipsoid ellipse, int zone, boolean isNorth) {
    init(ellipse.getMajor(), 1.0 / ellipse.getFlattening(), zone, isNorth);
  }


  /**
   * _more_
   *
   * @param a       major axis
   * @param f       inverse flattening
   * @param zone    _more_
   * @param isNorth _more_
   */
  protected void init(double a, double f, int zone, boolean isNorth) {
    A = a;
    F = 1.0 / f;  // F is flattening
    this.axlon0_deg = (zone * 6 - 183);
    this.axlon0 = axlon0_deg * RADIANS_PER_DEGREE;

    double polx2b, polx3b, polx4b, polx5b;

    //  Create the ERM constants.
    Eps2 = (F) * (2.0 - F);
    Eps25 = .25 * (Eps2);
    Epps2 = (Eps2) / (1.0 - Eps2);
    polx2b = Eps2 + 1.0 / 4.0 * Math.pow(Eps2, 2)
        + 15.0 / 128.0 * Math.pow(Eps2, 3)
        - 455.0 / 4096.0 * Math.pow(Eps2, 4);

    polx2b = 3.0 / 8.0 * polx2b;
    polx3b = Math.pow(Eps2, 2) + 3.0 / 4.0 * Math.pow(Eps2, 3)
        - 77.0 / 128.0 * Math.pow(Eps2, 4);
    polx3b = 15.0 / 256.0 * polx3b;
    polx4b = Math.pow(Eps2, 3) - 41.0 / 32.0 * Math.pow(Eps2, 4);
    polx4b = polx4b * 35.0 / 3072.0;
    polx5b = -315.0 / 131072.0 * Math.pow(Eps2, 4);

    poly1b = 1.0 - (1.0 / 4.0 * Eps2) - (3.0 / 64.0 * Math.pow(Eps2, 2))
        - (5.0 / 256.0 * Math.pow(Eps2, 3))
        - (175.0 / 16384.0 * Math.pow(Eps2, 4));

    poly2b = polx2b * -2.0 + polx3b * 4.0 - polx4b * 6.0 + polx5b * 8.0;

    poly3b = polx3b * -8.0 + polx4b * 32.0 - polx5b * 80.0;

    poly4b = polx4b * -32.0 + polx5b * 192.0;

    poly5b = polx5b * -128.0;
  }

  /**
   * get central meridian in degrees (depends on zone)
   * @return central meridian in degrees
   */
  public double getCentralMeridian() {
    return this.axlon0_deg;
  }

  public ProjectionPoint latLonToProj(double latitude, double longitude, ProjectionPointImpl result) {
    double source_lat, source_lon, s1, c1, tx, s12, rn, al, al2, sm, tn2,
        cee, poly1, poly2;

    longitude = LatLonPointImpl.lonNormal(longitude, axlon0_deg);  // normalize to the central meridian

    source_lat = latitude * RADIANS_PER_DEGREE;
    source_lon = longitude * RADIANS_PER_DEGREE;

    s1 = Math.sin(source_lat);
    c1 = Math.cos(source_lat);
    tx = s1 / c1;
    s12 = s1 * s1;

    rn = A / ((.25 - Eps25 * s12 + .9999944354799 / 4)
        + (.25 - Eps25 * s12)
        / (.25 - Eps25 * s12 + .9999944354799 / 4));

    al = (source_lon - axlon0) * c1;
    sm = s1 * c1
        * (poly2b + s12 * (poly3b + s12 * (poly4b + s12 * poly5b)));
    sm = A * (poly1b * source_lat + sm);

    tn2 = tx * tx;
    cee = Epps2 * c1 * c1;
    al2 = al * al;
    poly1 = 1.0 - tn2 + cee;
    poly2 = 5.0 + tn2 * (tn2 - 18.0) + cee * (14.0 - tn2 * 58.0);

    double x = CScale * rn * al
        * (1.0
        + al2
        * (.166666666666667 * poly1
        + .00833333333333333 * al2 * poly2));
    x += 5.0E5;

    poly1 = 5.0 - tn2 + cee * (cee * 4.0 + 9.0);
    poly2 = 61.0 + tn2 * (tn2 - 58.0) + cee * (270.0 - tn2 * 330.0);
    double y = CScale
        * (sm + rn * tx * al2
        * (0.5
        + al2
        * (.0416666666666667 * poly1
        + .00138888888888888 * al2 * poly2)));

    if (source_lat < 0.0) {
      y += 1.0E7;
    }

    result.setLocation(x * .001, y * .001);  // wants km
    return result;
  }


  public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
    double source_lat, source_lon, s1, c1, tx, s12, rn, al, al2, sm, tn2, cee, poly1, poly2;

    for (int i = 0; i < from[0].length; i++) {
      double longitude = LatLonPointImpl.lonNormal(from[lonIndex][i], axlon0_deg);  // normalize to the central meridian
      source_lat = from[latIndex][i] * RADIANS_PER_DEGREE;
      source_lon = longitude * RADIANS_PER_DEGREE;

      s1 = Math.sin(source_lat);
      c1 = Math.cos(source_lat);
      tx = s1 / c1;
      s12 = s1 * s1;

      rn = A / ((.25 - Eps25 * s12 + .9999944354799 / 4)
          + (.25 - Eps25 * s12)
          / (.25 - Eps25 * s12 + .9999944354799 / 4));

      al = (source_lon - axlon0) * c1;
      sm = s1 * c1
          * (poly2b + s12 * (poly3b + s12 * (poly4b + s12 * poly5b)));
      sm = A * (poly1b * source_lat + sm);

      tn2 = tx * tx;
      cee = Epps2 * c1 * c1;
      al2 = al * al;
      poly1 = 1.0 - tn2 + cee;
      poly2 = 5.0 + tn2 * (tn2 - 18.0) + cee * (14.0 - tn2 * 58.0);

      double x = CScale * rn * al
          * (1.0
          + al2
          * (.166666666666667 * poly1
          + .00833333333333333 * al2 * poly2));
      x += 5.0E5;

      poly1 = 5.0 - tn2 + cee * (cee * 4.0 + 9.0);
      poly2 = 61.0 + tn2 * (tn2 - 58.0) + cee * (270.0 - tn2 * 330.0);
      double y = CScale
          * (sm + rn * tx * al2
          * (0.5
          + al2
          * (.0416666666666667 * poly1
          + .00138888888888888 * al2 * poly2)));

      if (source_lat < 0.0) {
        y += 1.0E7;
      }

      to[0][i] = x * .001;
      to[1][i] = y * .001;
    }

    return to;
  }

  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
    double source_lat, source_lon, s1, c1, tx, s12, rn, al, al2, sm, tn2, cee, poly1, poly2;

    for (int i = 0; i < from[0].length; i++) {
      double longitude = LatLonPointImpl.lonNormal(from[lonIndex][i], axlon0_deg);  // normalize to the central meridian
      source_lat = from[latIndex][i] * RADIANS_PER_DEGREE;
      source_lon = longitude * RADIANS_PER_DEGREE;

      s1 = Math.sin(source_lat);
      c1 = Math.cos(source_lat);
      tx = s1 / c1;
      s12 = s1 * s1;

      rn = A / ((.25 - Eps25 * s12 + .9999944354799 / 4)
          + (.25 - Eps25 * s12)
          / (.25 - Eps25 * s12 + .9999944354799 / 4));

      al = (source_lon - axlon0) * c1;
      sm = s1 * c1
          * (poly2b + s12 * (poly3b + s12 * (poly4b + s12 * poly5b)));
      sm = A * (poly1b * source_lat + sm);

      tn2 = tx * tx;
      cee = Epps2 * c1 * c1;
      al2 = al * al;
      poly1 = 1.0 - tn2 + cee;
      poly2 = 5.0 + tn2 * (tn2 - 18.0) + cee * (14.0 - tn2 * 58.0);

      double x = CScale * rn * al
          * (1.0
          + al2
          * (.166666666666667 * poly1
          + .00833333333333333 * al2 * poly2));
      x += 5.0E5;

      poly1 = 5.0 - tn2 + cee * (cee * 4.0 + 9.0);
      poly2 = 61.0 + tn2 * (tn2 - 58.0) + cee * (270.0 - tn2 * 330.0);
      double y = CScale
          * (sm + rn * tx * al2
          * (0.5
          + al2
          * (.0416666666666667 * poly1
          + .00138888888888888 * al2 * poly2)));

      if (source_lat < 0.0) {
        y += 1.0E7;
      }

      to[0][i] = (float) (x * .001);
      to[1][i] = (float) (y * .001);
    }

    return to;
  }


}

