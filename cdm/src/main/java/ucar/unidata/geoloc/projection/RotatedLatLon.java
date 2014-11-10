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
package ucar.unidata.geoloc.projection;

import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * Grib 1 projection 10 and Grib 2 projection 1.
 *
 * The Rotated Latitude Longitude projection algorithms that are coded
 * here were given by Tor Christian Bekkvik <torc@cmr.no>. The rotated
 * lat/lon projection coordinates are defined in the grid file that
 * need to be converted back to unrotated lat/lon projection coordinates
 * before they can be displayed. The X/Y axis only makes sense in the rotated
 * projection.
 *
 * @author Tor Christian Bekkvik <torc@cmr.no>
 * @since Nov 11, 2008
 */

/*

Grib-1 doc: (http://rda.ucar.edu/docs/formats/grib/gribdoc/llgrid.html)

Three parameters define a general latitude/longitude coordinate system, formed by a general rotation of the sphere.
One choice for these parameters is: <ol>
  <l1> The geographic latitude in degrees of the southern pole of the coordinate system, thetap for example;
  <li> The geographic longitude in degrees of the southern pole of the coordinate system, lamdap for example;
  <li> The angle of rotation in degrees about the new polar axis (measured clockwise when looking from the southern to the
  northern pole) of the coordinate system, assuming the new axis to have been obtained by first rotating the sphere through lamdap degrees about
the geographic polar axis, and then rotating through (90 + thetap ) degrees so that the southern pole moved along the (previously
rotated) Greenwhich meridian. </ol>

Grib2:

Grid Definition Template 3.1: 	Rotated Latitude/longitude (or equidistant cylindrical, or Plate Carrée)

Octet No.		Contents
      15-72		Same as Grid Definition Template 3.0 (see Note 1)
      73-76		Latitude of the southern pole of projection
      77-80		Longitude of the southern pole of projection
      81-84		Angle of rotation of projection
      85-nn		List of number of points along each meridian or parallel (These octets are only present for quasi-regular grids as described in Note 3)

Notes:
(1) 	Basic angle of the initial production domain and subdivisions of this basic angle are provided to manage cases where the
recommended unit of 10-6 degrees is not applicable to describe the extreme longitudes and latitudes, and direction increments.
For these last six descriptors, unit is equal to the ratio of the basic angle and the subdivisions number.  For ordinary cases,
zero and missing values should be coded, equivalent to respective values of 1 and 106 (10-6 degrees unit).
(2) 	Three parameters define a general latitude/longitude coordinate system, formed by a general rotation of the sphere.
One choice for these parameters is:
(a)  	The geographic latitude in degrees of the southern pole of the coordinate system, θp for example.
(b) 	The geographic longitude in degrees of the southern pole of the coordinate system, λp for example.
(c)  	The angle of rotation in degrees about the new polar axis (measured clockwise when looking from the southern to the northern pole) of the coordinate system, assuming the new axis to have been obtained by first rotating the sphere through λp degrees about the geographic polar axis, and then rotating through (90 + θp) degrees so that the southern pole moved along the (previously rotated) Greenwich meridian.

 */

public class RotatedLatLon extends ProjectionImpl {
  public static final String GRID_MAPPING_NAME = "rotated_latlon_grib";
  public static final String GRID_SOUTH_POLE_LONGITUDE = "grid_south_pole_longitude";
  public static final String GRID_SOUTH_POLE_LATITUDE = "grid_south_pole_latitude";
  public static final String GRID_SOUTH_POLE_ANGLE = "grid_south_pole_angle";

  private static boolean show = false;

  private final double lonpole; // Longitude of south pole
  private final double latpole; // Latitude of south pole
  private final double polerotate; // Angle of south pole  rotation

  private double cosDlat;
  private double sinDlat;

  /**
   * Default Constructor, needed for beans.
   */
  public RotatedLatLon() {
    this(0.0, 0.0, 0.0);
  }

  /**
   * Constructor.
   *
   * @param southPoleLat   in degrees
   * @param southPoleLon   in degrees
   * @param southPoleAngle in degrees
   */
  public RotatedLatLon(double southPoleLat, double southPoleLon, double southPoleAngle) {
    super("RotatedLatLon", false);

    /*
      lonsp = aLonsp;
      latsp = aLatsp;
      rotsp = aRotsp;
      double dlat_rad = (latsp - (-90)) * DEG2RAD; //delta latitude
      sinDlat = Math.sin(dlat_rad);
      cosDlat = Math.cos(dlat_rad);
     */
    this.latpole = southPoleLat;
    this.lonpole = southPoleLon;
    this.polerotate = southPoleAngle;
    double dlat_rad = Math.toRadians(latpole - (-90));
    sinDlat = Math.sin(dlat_rad);
    cosDlat = Math.cos(dlat_rad);

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(GRID_SOUTH_POLE_LATITUDE, southPoleLat);
    addParameter(GRID_SOUTH_POLE_LONGITUDE, southPoleLon);
    addParameter(GRID_SOUTH_POLE_ANGLE, southPoleAngle);
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new RotatedLatLon(latpole, lonpole, polerotate);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * returns constructor params as a String
   *
   * @return String
   */
  public String paramsToString() {
    return " southPoleLat =" + latpole + " southPoleLon =" + lonpole + " southPoleAngle =" + polerotate;
  }

  /**
   * Transform a "real" longitude and latitude into the rotated longitude (X) and
   * rotated latitude (Y).
   */
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint) {
    /*
    Tor's algorithm
    public double[] fwd(double[] lonlat)
      return transform(lonlat, lonpole, polerotate, sinDlat);
    */
    double[] lonlat = new double[2];
    lonlat[0] = latlon.getLongitude();
    lonlat[1] = latlon.getLatitude();

    double[] rlonlat = rotate(lonlat, lonpole, polerotate, sinDlat);
    if (destPoint == null)
      destPoint = new ProjectionPointImpl(rlonlat[0], rlonlat[1]);
    else
      destPoint.setLocation(rlonlat[0], rlonlat[1]);

    if (show)
      System.out.println("LatLon= " + latlon + " proj= " + destPoint);

    return destPoint;
  }

  /**
   * Transform a rotated longitude (X) and rotated latitude (Y) into a "real"
   * longitude-latitude pair.
   */
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    /*
    Tor's algorithm
    public double[] inv(double[] lonlat)
      return rotate(lonlat, -polerotate, -lonpole, -sinDlat);
    */
    double[] lonlat = new double[2];
    lonlat[0] = ppt.getX();
    lonlat[1] = ppt.getY();

    double[] rlonlat = rotate(lonlat, -polerotate, -lonpole, -sinDlat);
    if (destPoint == null)
      destPoint = new LatLonPointImpl(rlonlat[1], rlonlat[0]);
    else
      destPoint.set(rlonlat[1], rlonlat[0]);

    if (show)
      System.out.println("Proj= " + ppt + " latlon= " + destPoint);
    return destPoint;
  }

  // Tor's transform algorithm renamed to rotate for clarity
  private double[] rotate(double[] lonlat, double rot1, double rot2, double s) {

    /* original code
      double e = DEG2RAD * (lonlat[0] - rot1); //east
      double n = DEG2RAD * lonlat[1]; //north
      double cn = Math.cos(n);
      double x = cn * Math.cos(e);
      double y = cn * Math.sin(e);
      double z = Math.sin(n);
      double x2 = cosDlat * x + s * z;
      double z2 = -s * x + cosDlat * z;
      double R = Math.sqrt(x2 * x2 + y * y);
      double e2 = Math.atan2(y, x2);
      double n2 = Math.atan2(z2, R);
      double rlon = RAD2DEG * e2 - rot2;
      double rlat = RAD2DEG * n2;
      return new double[]{rlon, rlat};
     */

    double e = Math.toRadians(lonlat[0] - rot1); //east
    double n = Math.toRadians(lonlat[1]); //north
    double cn = Math.cos(n);
    double x = cn * Math.cos(e);
    double y = cn * Math.sin(e);
    double z = Math.sin(n);
    double x2 = cosDlat * x + s * z;
    double z2 = -s * x + cosDlat * z;
    double R = Math.sqrt(x2 * x2 + y * y);
    double e2 = Math.atan2(y, x2);
    double n2 = Math.atan2(z2, R);
    double rlon = Math.toDegrees(e2) - rot2;
    double rlat =  Math.toDegrees(n2);
    return new double[]{rlon, rlat};

  }

  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return Math.abs(pt1.getX() - pt2.getX()) > 270.0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RotatedLatLon that = (RotatedLatLon) o;

    if (Double.compare(that.latpole, latpole) != 0) return false;
    if (Double.compare(that.lonpole, lonpole) != 0) return false;
    if (Double.compare(that.polerotate, polerotate) != 0) return false;
    if ((defaultMapArea == null) != (that.defaultMapArea == null)) return false; // common case is that these are null
    if (defaultMapArea != null && !that.defaultMapArea.equals(defaultMapArea)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = lonpole != +0.0d ? Double.doubleToLongBits(lonpole) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = latpole != +0.0d ? Double.doubleToLongBits(latpole) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = polerotate != +0.0d ? Double.doubleToLongBits(polerotate) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  private static class Test {
    RotatedLatLon rll;
    static PrintStream ps = System.out;

    public Test(double lo, double la, double rot) {
      rll = new RotatedLatLon(la, lo, rot);
      ps.println("lonsp:" + rll.lonpole +
              ", latsp:" + rll.latpole +
              ", rotsp:" + rll.polerotate);
    }

    void pr(double[] pos, double[] pos2, double[] pos3) {
      ps.println(" " + pos[0] + "   " + pos[1]);
      ps.println("    fwd: " + pos2[0] + "   " + pos2[1]);
      ps.println("    inv: " + pos3[0] + "   " + pos3[1]);
    }

    final static double err = 0.0001;

    private double[] test(float lon, float lat) {
      double[] p = {lon, lat};
      double[] p2 = rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      double[] p3 = rll.rotate(p2, -rll.polerotate, -rll.lonpole, -rll.sinDlat);
      assert Math.abs(p[0] - p3[0]) < err;
      assert Math.abs(p[1] - p3[1]) < err;
      pr(p, p2, p3);
      return p2;
    }

    double[] proj(double lon, double lat, boolean fwd) {
      double[] pos = {lon, lat};
      double[] pos2 = fwd ?
              rll.rotate(pos, rll.lonpole, rll.polerotate, rll.sinDlat) :
              rll.rotate(pos, -rll.polerotate, -rll.lonpole, -rll.sinDlat);
      ps.println((fwd ? " fwd" : " inv")
              + " [" + lon + ", " + lat + "] -> " + Arrays.toString(pos2));
      return pos2;
    }
  }


  private static void test() {
    Test tst0 = new Test(0, -25, 0);
    tst0.proj(0, -25, true);

    Test t = new Test(0, 90, 0);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = new Test(0, 0, 0);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = new Test(10, 50, 25);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    RotatedLatLon rll = new RotatedLatLon(-50, 10, 20);
    long t0 = System.currentTimeMillis();
    long dt = 0;
    double[] p = {12., 60.};
    int i = 0;
    while (dt < 1000) {
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      i++;
      dt = System.currentTimeMillis() - t0;
    }
    System.out.println("fwd/sec: " + i * 10);

  }

  public static void main(String args[]) {
    test();
  }
}

/*

original code:


  private static class RotLatLon {
    double DEG2RAD = (Math.PI * 2) / 360;
    double RAD2DEG = 1 / DEG2RAD;
    double lonsp, latsp, rotsp, sinDlat, cosDlat;
    public RotLatLon(double aLonsp,
                     double aLatsp,
                     double aRotsp)
    {
       lonsp = aLonsp;
       latsp = aLatsp;
       rotsp = aRotsp;
       double dlat_rad = (latsp - (-90)) * DEG2RAD; //delta latitude
       sinDlat = Math.sin(dlat_rad);
       cosDlat = Math.cos(dlat_rad);
    }

    private double[] transform(double [] lonlat, double rot1, double rot2, double s)
    {
       double e = DEG2RAD * (lonlat[0] - rot1); //east
       double n = DEG2RAD * lonlat[1]; //north
       double cn = Math.cos(n);
       double x = cn * Math.cos(e);
       double y = cn * Math.sin(e);
       double z = Math.sin(n);
       double x2 = cosDlat * x + s * z;
       double z2 = -s * x + cosDlat * z;
       double R = Math.sqrt(x2 * x2 + y * y);
       double e2 = Math.atan2(y, x2);
       double n2 = Math.atan2(z2, R);
       double rlon = RAD2DEG * e2 - rot2;
       double rlat = RAD2DEG * n2;
       return new double[]{rlon, rlat};
    }

    public double[] fwd(double[] lonlat)
    {
       return transform(lonlat, lonsp, rotsp, sinDlat);
    }

    public double[] inv(double[] lonlat)
    {
       return transform(lonlat, -rotsp, -lonsp, -sinDlat);
    }
  }

  public static void main(String[] args) {
    RotLatLon rot = new RotLatLon(155, -30, 0);
    double[] lonlat = new double[] {130, 2};
    double[] rlonlat = rot.fwd(lonlat);
    double[] rrlonlat = rot.inv(rlonlat);
    System.out.printf("lonlat = %f %f%n", lonlat[0],  lonlat[1]);
    System.out.printf("rlonlat = %f %f%n", rlonlat[0],  rlonlat[1]);
    System.out.printf("rrlonlat = %f %f%n", rrlonlat[0],  rrlonlat[1]);

    RotatedLatLon rot2 = new RotatedLatLon(-30, 155, 0);
    LatLonPointImpl ll = new LatLonPointImpl(2,130);
    ProjectionPoint pp = rot2.latLonToProj(ll);
    LatLonPoint ll2 = rot2.projToLatLon(pp);
    System.out.printf("latlon = %s%n", ll);
    System.out.printf("pp = %s%n", pp);
    System.out.printf("latlon = %s%n", ll2);
  }


   */