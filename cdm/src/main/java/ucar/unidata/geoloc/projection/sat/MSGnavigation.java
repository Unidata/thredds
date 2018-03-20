/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection.sat;

import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Port Eumetsat MSG_navigation.c to java.
 * from http://www.eumetsat.int/idcplg?IdcService=GET_FILE&dDocName=zip_tools_msg_nav_c&RevisionSelectionMethod=LatestReleased
 *
 * @author caron
 * @see "http://www.eumetsat.int/idcplg?IdcService=GET_FILE&dDocName=PDF_CGMS_03&RevisionSelectionMethod=LatestReleased"
 * @see "www.itc.nl/library/papers_2005/conf/gieske_pro.pdf"
 * @since Jan 9, 2010
 */


public class MSGnavigation extends ProjectionImpl {

  /**
   * **********************************************************************
   * Introduction:
   * =============
   * The Program "MSG_navigation.c" is an example code provided to give
   * the users guidance for a possible implementation of the equations
   * given in the LRIT/HRIT Global Specification [1] to navigate MSG
   * (METEOSAT 8 onwards) data, i.e. to link the pixel coordinates column
   * and line to the corresponding geographical coordinates latitude and
   * longitude.
   * <p/>
   * Users should take note, however, that it does NOT provide software
   * for reading MSG data either in LRIT/HRIT, in native or any other
   * format and that EUMETSAT cannot guarantee the accuracy of this
   * software. The software is for use with MSG data only and will not
   * work in the given implementation for Meteosat first generation data.
   * <p/>
   * Two functions/subroutines are provided:
   * pixcoord2geocoord: for conversion of column/line into lat./long.
   * geocoord2pixcoord: for conversion of lat./long. into column/line
   * <p/>
   * The main routine gives an example how to utilize these two functions by
   * reading a value for column and line at the start of the program on the
   * command line and convert these values into the corresponding
   * geographical coordinates and back again. The results are then printed
   * out on the screen.
   * <p/>
   * To Compile the program use for example:
   * <p/>
   * COMMAND PROMPT: gcc MSG_navigation.c -o MSG_navigation -lm
   * <p/>
   * Run the program by typing
   * <p/>
   * COMMAND PROMPT: ./MSG_navigation <COLUMS> <ROWS>
   * <p/>
   * ----------------------------------------------------------------------
   * <p/>
   * NOTE: Please be aware, that the program assumes the MSG image is
   * ordered in the operational scanning direction which means from south
   * to north and from east to west. With that the VIS/IR channels contains
   * of 3712 x 3712 pixels, start to count on the most southern line and the
   * most eastern column with pixel number 1,1.
   * <p/>
   * <p/>
   * NOTE on CFAC/LFAC and COFF/LOFF:
   * The parameters CFAC/LFAC and COFF/LOFF are the scaling coefficients
   * provided by the navigation record of the LRIT/HRIT header and used
   * by the scaling function given in Ref [1], page 28.
   * <p/>
   * COFF/LOFF are the offsets for column and line which are basically 1856
   * and 1856 for the VIS/IR channels and refer to the middle of the image
   * (centre pixel). The values regarding the High Resolution Visible Channel
   * (HRVis) will be made available in a later issue of this software.
   * <p/>
   * CFAC/LFAC are responsible for the image "spread" in the NS and EW
   * directions. They are calculated as follows:
   * CFAC = LFAC = 2^16 / delta
   * with delta = 83.84333 micro Radian (size of one VIS/IR MSG pixel)
   * <p/>
   * CFAC     = LFAC     =  781648343.404  rad^-1 for VIS/IR
   * <p/>
   * which should be rounded to the nearest integer as stated in Ref [1].
   * <p/>
   * CFAC     = LFAC     =  781648343  rad^-1 for VIS/IR
   * <p/>
   * the sign of CFAC/LFAC gives the orientation of the image.
   * Negative sign give data scanned from south to north as in the
   * operational scanning. Positive sign vice versa.
   * <p/>
   * The terms "line" and "row" are used interchangeable.
   * <p/>
   * PLEASE NOTE that the values of CFAC/LFAC which are given in the
   * Header of the LRIT/HRIT Level 1.5 Data (see [2]) are actually in
   * Degrees and should be converted in Radians for use with these
   * routines (see example and values above).
   * <p/>
   * The other parameters are given in Ref [1].
   * <p/>
   * Further information may be found in either Ref [1], Ref [2] or
   * Ref [3] or on the Eumetsat website http://www.eumetsat.int/ .
   * <p/>
   * REFERENCE:
   * [1] LRIT/HRIT Global Specification
   * (CGMS 03, Issue 2.6, 12.08.1999)
   * for the parameters used in the program.
   * [2] MSG Ground Segment LRIT/HRIT Mission Specific
   * Implementation, EUMETSAT Document,
   * (EUM/MSG/SPE/057, Issue 6, 21. June 2006).
   * [3] MSG Level 1.5 Image Data Format Description
   * (EUM/MSG/ICD/105, Issue v5A, 22. August 2007).
   * <p/>
   * Please email the User Service (via
   * http://www.eumetsat.int/Home/Basic/Contact_Us/index.htm) if you have
   * any questions regarding this software.
   * <p/>
   * ================================
   * From simon.elliott@eumetsat.int
   * As explained in notes 4 and 6 to the GRIB definition (and evidenced by drawing a sketch of the Earth and satellite
   * in the equatorial plane and looking down from above), we have the following from GRIB:
   * <p/>
   * # The distance from the Earth's centre to the satellite is Nr (in units of Earth equatorial radius and multiplied by 10^6.
   * # The distance from the Earth's centre to the tangent point on the equator is the Earths equatorial radius
   * # The sine of the angle subtended by the Earths centre and the tangent point on the equator as seen from the spacecraft = Re / (( Nr * Re) / 10^6) = 10^6 / Nr
   * # The angle subtended by the Earth equator as seen by the spacecraft is, by symmetry twice the inverse sine above, 2 * arcsine (10^6 / Nr)
   * # As the number of pixels occupied by the Earth's equatorial radius is dx, the number of radians scanned by the spacecraft per pixel, Rx, is simply [2 * arcsine (10^6 / Nr)] / dx
   * <p/>
   * To calculate CFAC, one needs to know delta, the number radians per pixel which is Rx above.
   * <p/>
   * So CFAC = 2^16 / delta
   * = 2^16 / Rx
   * = 2^16 / {[2 * arcsine (10^6 / Nr)] / dx }
   * <p/>
   * <p/>
   * <p/>
   * **********************************************************************
   */

  public static final String HEIGHT_FROM_EARTH_CENTER = "height_from_earth_center";
  public static final String SCALE_X = "scale_x";
  public static final String SCALE_Y = "scale_y";

  // parameters used in the routines as given in Ref. [1]
  private static final double SAT_HEIGHT = 42164.0;     // distance from Earth centre to satellite
  private static final double R_EQ = 6378.169;   // radius from Earth centre to equator
  private static final double R_POL = 6356.5838;  // radius from Earth centre to pol
  private static final double SUB_LON = 0.0;     // longitude of sub-satellite point in radiant


  private double lat0 = 0.0; // always 0
  private double lon0; // longitude of sub-satellite point in radians
  private double major_axis, minor_axis; // ellipsoidal shape
  private double sat_height; // distance from Earth centre to satellite
  private double scale_x, scale_y;

  private double const1, const2, const3;
  private double maxR;

  public MSGnavigation() {
    this(0.0, SUB_LON, R_EQ, R_POL, SAT_HEIGHT, SAT_HEIGHT - R_EQ, SAT_HEIGHT - R_EQ);
  }

  /**
   * Constructor
   * @param lat0 in degrees; geosynch satelite is over this point
   * @param lon0 in degrees; geosynch satelite is over this point
   * @param major_axis in meters
   * @param minor_axis in meters
   * @param sat_height in meters
   * @param scale_x  convert between aperature size in radians and distance in km (xrad = xkm / scale_x)
   * @param scale_y  scale_factor = (nr - 1) * major_axis, nr = altitude of the camera from the Earths centre, measured in units of the Earth (equatorial) radius
   */
  public MSGnavigation(double lat0, double lon0, double major_axis, double minor_axis, double sat_height, double scale_x, double scale_y) {
    super("MSGnavigation", false);

    this.lon0 = Math.toRadians(lon0);
    this.major_axis = .001 * major_axis; // convert to km
    this.minor_axis = .001 * minor_axis;
    this.sat_height = .001 * sat_height;
    this.scale_x = scale_x;
    this.scale_y = scale_y;
    const1 = major_axis / minor_axis;
    const1 *= const1; //  (req * rpol)**2
    const2 = 1.0 - (minor_axis * minor_axis) / (major_axis * major_axis); //  (req**2 - rpol**2) / req**2 = 1 - rpol**2 / req**2
    const3 = this.sat_height * this.sat_height - this.major_axis * this.major_axis;

    // "map limit" circle of this radius from the origin, p 173 (Vertical Perspective Projection)
    double P = sat_height / major_axis;
    maxR = .99 * this.major_axis * Math.sqrt((P - 1) / (P + 1));

    addParameter(CF.GRID_MAPPING_NAME, "MSGnavigation");
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.SEMI_MAJOR_AXIS, major_axis);
    addParameter(CF.SEMI_MINOR_AXIS, minor_axis);
    addParameter(HEIGHT_FROM_EARTH_CENTER, sat_height);
    addParameter(SCALE_X, scale_x);
    addParameter(SCALE_Y, scale_y);

    //System.out.printf("%s %n", this);
  }

  @Override
  public String toString() {
    return "MSGnavigation{" +
            "lat0=" + lat0 +
            ", lon0=" + lon0 +
            ", major_axis=" + major_axis +
            ", minor_axis=" + minor_axis +
            ", sat_height=" + sat_height +
            ", scale_x=" + scale_x +
            ", scale_y=" + scale_y +
            '}';
  }

  private int pixcoord2geocoord(double xkm, double ykm, LatLonPointImpl result) {

    /*  calculate viewing angle of the satellite by use of the equation  */
    /*  on page 28, Ref [1]. */
    //double x = (column - x_off) / cfac;
    //double y = (row - y_off) / lfac;

    // convert to radians
    double xrad = xkm / scale_x;
    double yrad = ykm / scale_y;

    double cosx = Math.cos(xrad);
    double cosy = Math.cos(yrad);
    double siny = Math.sin(yrad);

    /*  now calculate the inverse projection */
    /* first check for visibility, whether the pixel is located on the Earth   */
    /* surface or in space. 						     */
    /* To do this calculate the argument to sqrt of "sd", which is named "sa". */
    /* If it is negative then the sqrt will return NaN and the pixel will be   */
    /* located in space, otherwise all is fine and the pixel is located on the */
    /* Earth surface.                                                          */
    double sa = Math.pow(sat_height * cosx * cosy, 2) - (cosy * cosy + const1 * siny * siny) * const3;

    /* produce error values */
    if (sa <= 0.0) {
      result.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      return (-1);
    }

    /* now calculate the rest of the formulas using equations on */
    /* page 25, Ref. [1]  */

    double sd = Math.sqrt(sa);
    double sn = (sat_height * cosx * cosy - sd) / (cosy * cosy + const1 * siny * siny);

    double s1 = sat_height - sn * cosx * cosy;
    double s2 = sn * Math.sin(xrad) * cosy;
    double s3 = -sn * siny;

    double sxy = Math.sqrt(s1 * s1 + s2 * s2);

    /* using the previous calculations the inverse projection can be  */
    /* calculated now, which means calculating the lat./long. from    */
    /* the pixel row and column by equations on page 25, Ref [1].     */

    double longi = Math.atan(s2 / s1) + lon0;
    double lati = Math.atan(const1 * s3 / sxy);

    /* convert from radians into degrees */
    result.setLatitude(Math.toDegrees(lati));
    result.setLongitude(Math.toDegrees(longi));

    return (0);

  }

  private int geocoord2pixcoord(double latitude, double longitude, ProjectionPointImpl result) {

    /* check if the values are sane, otherwise return error values */
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
      result.setLocation(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      return (-1);
    }

    /* convert to radians */
    double lat = Math.toRadians(latitude);
    double lon = Math.toRadians(longitude) - lon0;
    double cosLon = Math.cos(lon);

    /* calculate the geocentric latitude from the          */
    /* geographic one using equations on page 24, Ref. [1] */

    double c_lat = Math.atan(Math.tan(lat) / const1);

    /* using c_lat calculate the length from the Earth */
    /* centre to the surface of the Earth ellipsoid    */
    /* equations on page 23, Ref. [1]                  */

    double coscLat = Math.cos(c_lat);
    double re = minor_axis / Math.sqrt(1.0 - const2 * coscLat * coscLat);

    /* calculate the forward projection using equations on */
    /* page 24, Ref. [1]                                        */

    double r1 = sat_height - re * coscLat * cosLon;
    double r2 = -re * coscLat * Math.sin(lon);
    double r3 = re * Math.sin(c_lat);
    double rn = Math.sqrt(r1 * r1 + r2 * r2 + r3 * r3);

    /* check for visibility, whether the point on the Earth given by the */
    /* latitude/longitude pair is visible from the satellte or not. This */
    /* is given by the dot product between the vectors of:               */
    /* 1) the point to the spacecraft,			               */
    /* 2) the point to the centre of the Earth.			       */
    /* If the dot product is positive the point is visible otherwise it  */
    /* is invisible.						       */

    double dotprod = r1 * (re * coscLat * cosLon) - r2 * r2 - r3 * r3 * const1;
    if (dotprod <= 0) {
      //System.out.printf("lat,lon=%f,%f NaN%n",latitude,longitude);
      result.setLocation(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      return (-1);
    }

    double xx = Math.atan(-r2 / r1);
    double yy = Math.asin(-r3 / rn);

    // convert to pixel column and row using the scaling functions on page 28, Ref. [1].
    //double cc = x_off + xx * cfac;
    //double ll = y_off + yy * lfac;

    //System.out.printf("lat,lon=%f,%f x,y=%f,%f i,j=%f,%f%n",latitude,longitude,xx, yy, cc, ll);
    result.setLocation(scale_x * xx, scale_y * yy);
    return (0);
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new MSGnavigation(
            lat0, Math.toDegrees(lon0), 1000 * major_axis, 1000 * minor_axis, 1000 * sat_height, scale_x, scale_y);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  @Override
  public String paramsToString() {
    return "";
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint) {
    geocoord2pixcoord(latlon.getLatitude(), latlon.getLongitude(), destPoint);
    return destPoint;
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    pixcoord2geocoord(ppt.getX(), ppt.getY(), destPoint);
    return destPoint;
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2))
      return true;

    // opposite signed X values, larger then 100 km
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 100);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MSGnavigation that = (MSGnavigation) o;

    if (Double.compare(that.lat0, lat0) != 0) return false;
    if (Double.compare(that.lon0, lon0) != 0) return false;
    if (Double.compare(that.major_axis, major_axis) != 0) return false;
    if (Double.compare(that.minor_axis, minor_axis) != 0) return false;
    if (Double.compare(that.sat_height, sat_height) != 0) return false;
    if (Double.compare(that.scale_x, scale_x) != 0) return false;
    return Double.compare(that.scale_y, scale_y) == 0;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(lat0);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon0);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(major_axis);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(minor_axis);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(sat_height);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(scale_x);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(scale_y);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /* public boolean equals2(Object proj) {
      if (this == proj) return true;
      if (proj == null || getClass() != proj.getClass()) return false;

      MSGnavigation that = (MSGnavigation) proj;
      if (Double.compare(that.lat0, lat0) != 0) return false;
      if (Double.compare(that.lon0, lon0) != 0) return false;
      if ((defaultMapArea == null) != (that.defaultMapArea == null)) return false; // common case is that these are null
      if (defaultMapArea != null && !that.defaultMapArea.equals(defaultMapArea)) return false;

      return proj instanceof MSGnavigation;
  }  */

  /**
   * Create a ProjectionRect from the given LatLonRect.
   * Handles lat/lon points that do not intersect the projection panel.
   *
   * @param rect the LatLonRect
   * @return ProjectionRect, or null if no part of the LatLonRect intersects the projection plane
   */
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect rect) {
    BoundingBoxHelper bbhelper = new BoundingBoxHelper(this, maxR);
    return bbhelper.latLonToProjBB(rect);
  }

  public double getLon0() {
    return lon0;  // Exposed for testing in MSGnavigationTest.
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  static void tryit(double want, double x) {
    System.out.printf("x = %f %f %f %n", x, x / want, want / x);
  }

  static private void doOne(ProjectionImpl proj, double lat, double lon) {
    LatLonPointImpl startL = new LatLonPointImpl(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPointImpl endL = (LatLonPointImpl) proj.projToLatLon(p);

    System.out.println("start  = " + startL.toString(8));
    System.out.println("xy   = " + p.toString());
    System.out.println("end  = " + endL.toString(8));

  }

  static private void doTwo(ProjectionImpl proj, double x, double y) {
    ProjectionPointImpl startL = new ProjectionPointImpl(x, y);
    LatLonPoint p = proj.projToLatLon(startL);
    ProjectionPointImpl endL = (ProjectionPointImpl) proj.latLonToProj(p);

    System.out.println("start  = " + startL.toString());
    System.out.println("lat,lon   = " + p.toString());
    System.out.println("end  = " + endL.toString());

  }

  public static void main(String arg[]) {
    double dx = 1207;
    double dy = 1189;
    double nr = 6610700.0;

    double scanx = 2 * Math.asin(1.e6 / nr) / dx;
    double scany = 2 * Math.asin(1.e6 / nr) / dy;
    System.out.printf("scanx = %g urad %n", scanx * 1e6);
    System.out.printf("scany = %g urad %n", scany * 1e6);

    double scan2 = 2 * Math.asin(1.e6 / nr) / 3566;
    System.out.printf("scan2 = %g urad %n", scan2 * 1e6);


    /* MSGnavigation msg = new MSGnavigation();
   System.out.printf("const1 = %f %n",msg.const1);
   System.out.printf("1/const1 = %f %n",1.0/msg.const1);
   System.out.printf("const2 = %10.8f %n",msg.const2);
   System.out.printf("1/const) = %f %n",1.0/msg.const2);
   System.out.printf("1/(1+const2) = %f %n",1.0/(1+msg.const2));
   System.out.printf("1/(1-const2) = %f %n",1.0/(1-msg.const2));

   double s = Math.sqrt(1737121856.);
   System.out.printf("sqrt = %f %n",s);
   System.out.printf("try = %f %n",SAT_HEIGHT*SAT_HEIGHT - R_EQ * R_EQ);

   /* tryit(s, SAT_HEIGHT/msg.const1);
   tryit(s, SAT_HEIGHT/(1+msg.const2));
   tryit(s, SAT_HEIGHT/(1+msg.const2*msg.const2));
   tryit(s, SAT_HEIGHT/msg.const1*msg.const1);
   tryit(s, SAT_HEIGHT/msg.const1*(1+msg.const2));

   tryit(s, SAT_HEIGHT*(1+msg.const2));

   /*
       :grid_mapping_name = "MSGnavigation";
  :longitude_of_projection_origin = 0.0; // double
  :latitude_of_projection_origin = 0.0; // double
  :semi_major_axis = 6378.14013671875; // double
  :semi_minor_axis = 6356.75537109375; // double
  :height_from_earth_center_km = 42163.97100180664; // double
  :dx = 1207.0; // double
  :dy = 1189.0; // double
    */

    // MSGnavigation m = new MSGnavigation(0, 0, 6378.14013671875, 6356.75537109375, 42163.97100180664);

    //doOne(m, 11, 51);
    //doOne(m, -34, 18);
    /* doTwo(m, 100, 100);
 doTwo(m, 5000, 5000);

 m = new MSGnavigation(0, 0, 6378.14013671875, 6356.75537109375, 42163.97100180664, 0, 0);
 System.out.printf("%ncfac = %f r = %f %n", m.cfac, CFAC/m.cfac);
 System.out.printf("lfac = %f r = %f %n", m.lfac, CFAC/m.lfac);

 doTwo(m, 100, 100);
 doTwo(m, 5000, 5000); */

  }
}
