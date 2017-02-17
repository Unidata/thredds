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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.*;

/**
 * Models-3/EDSS Input/Output netcf format.
 * <p/>
 * The Models-3/EDSS Input/Output Applications Programming Interface (I/O API)
 * is the standard data access library for both NCSC's EDSS project and EPA's Models-3.
 * <p/>
 * 6/24/09: Modified to support multiple projection types of data by Qun He <qunhe@unc.edu> and Alexis Zubrow <azubrow@unc.edu>
 * added makePolarStereographicProjection, UTM, and modified latlon
 * <p/>
 * 09/2010 plessel.todd@epa.gov add projections 7,8,9,10
 *
 * @author plessel.todd@epa.gov
 * @author caron
 * @see <a href="http://www.baronams.com/products/ioapi/index.html">http://www.baronams.com/products/ioapi/index.html</a>
 */

public class M3IOConvention extends CoordSysBuilder {
   private static final double earthRadius = 6370.000; // km
  /**
   * Do we think this is a M3IO file.
   *
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a M3IO file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    return (null != ncfile.findGlobalAttribute("XORIG"))
            && (null != ncfile.findGlobalAttribute("YORIG"))
            && (null != ncfile.findGlobalAttribute("XCELL"))
            && (null != ncfile.findGlobalAttribute("YCELL"))
            && (null != ncfile.findGlobalAttribute("NCOLS"))
            && (null != ncfile.findGlobalAttribute("NROWS"));

    // M3IOVGGridConvention - is this true for this class ??
    // return ncFile.findGlobalAttribute( "VGLVLS" ) != null && isValidM3IOFile_( ncFile );

  }

  public M3IOConvention() {
    this.conventionName = "M3IO";
  }

  public void augmentDataset(NetcdfDataset ncd, CancelTask cancelTask) {
    if (null != ncd.findVariable("x")) return; // check if its already been done - aggregating enhanced datasets.
    if (null != ncd.findVariable("lon")) return; // check if its already been done - aggregating enhanced datasets.

    constructCoordAxes(ncd);
    ncd.finish();
  }

  private CoordinateTransform ct = null;

  protected void constructCoordAxes(NetcdfDataset ds) {

    Dimension dimx = ds.findDimension("COL");
    int nx = dimx.getLength();

    Dimension dimy = ds.findDimension("ROW");
    int ny = dimy.getLength();

    int projType = findAttributeInt(ds, "GDTYP");
    boolean isLatLon = (projType == 1);
    if (isLatLon) {
      //ds.addCoordinateAxis(  makeCoordAxis( ds, "lon", "COL", nx, "XORIG", "XCELL", "degrees east"));
      //ds.addCoordinateAxis(  makeCoordAxis( ds, "lat", "ROW", ny, "YORIG", "YCELL", "degrees north"));

      ds.addCoordinateAxis(makeCoordLLAxis(ds, "lon", "COL", nx, "XORIG", "XCELL", "degrees east"));
      ds.addCoordinateAxis(makeCoordLLAxis(ds, "lat", "ROW", ny, "YORIG", "YCELL", "degrees north"));
      ct = makeLatLongProjection(ds);

      VariableDS v = makeCoordinateTransformVariable(ds, ct);
      ds.addVariable(null, v);
      v.addAttribute(new Attribute(_Coordinate.Axes, "lon lat"));

    } else {
      ds.addCoordinateAxis(makeCoordAxis(ds, "x", "COL", nx, "XORIG", "XCELL", "km"));
      ds.addCoordinateAxis(makeCoordAxis(ds, "y", "ROW", ny, "YORIG", "YCELL", "km"));

      if (projType == 2)
        ct = makeLCProjection(ds);
      else if (projType == 3)
        ct = makeTMProjection(ds);
      else if (projType == 4)
        ct = makeSTProjection(ds);
      else if (projType == 5)
        ct = makeUTMProjection(ds);
      else if (projType == 6) // Was 7. See http://www.baronams.com/products/ioapi/GRIDS.html
        ct = makePolarStereographicProjection(ds);
      else if (projType == 7)
        ct = makeEquitorialMercatorProjection(ds);
      else if (projType == 8)
        ct = makeTransverseMercatorProjection(ds);
      else if (projType == 9)
        ct = makeAlbersProjection(ds);
      else if (projType == 10)
        ct = makeLambertAzimuthalProjection(ds);

      if (ct != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, ct);
        ds.addVariable(null, v);
        v.addAttribute(new Attribute(_Coordinate.Axes, "x y"));
      }
    }

    makeZCoordAxis(ds, "LAY", "VGLVLS", "sigma");
    makeTimeCoordAxis(ds, "TSTEP");
  }

  private CoordinateAxis makeCoordAxis(NetcdfDataset ds, String name, String dimName, int n,
                                       String startName, String incrName, String unitName) {

    double start = .001 * findAttributeDouble(ds, startName); // km
    double incr = .001 * findAttributeDouble(ds, incrName); // km
    start = start + incr / 2.; // shifting x and y to central
    CoordinateAxis v = new CoordinateAxis1D(ds, null, name, DataType.DOUBLE, dimName, unitName,
            "synthesized coordinate from " + startName + " " + incrName + " global attributes");
    v.setValues(n, start, incr);
    return v;
  }

  private CoordinateAxis makeCoordLLAxis(NetcdfDataset ds, String name, String dimName, int n,
                                         String startName, String incrName, String unitName) {
    // Makes coordinate axes for Lat/Lon
    double start = findAttributeDouble(ds, startName); // degrees
    double incr = findAttributeDouble(ds, incrName); // degrees

    CoordinateAxis v = new CoordinateAxis1D(ds, null, name, DataType.DOUBLE, dimName, unitName,
            "synthesized coordinate from " + startName + " " + incrName + " global attributes");
    v.setValues(n, start, incr);
    return v;
  }

  private void makeZCoordAxis(NetcdfDataset ds, String dimName, String levelsName, String unitName) {
    Dimension dimz = ds.findDimension(dimName);
    int nz = dimz.getLength();
    ArrayDouble.D1 dataLev = new ArrayDouble.D1(nz);
    ArrayDouble.D1 dataLayers = new ArrayDouble.D1(nz + 1);

    // layer values are a numeric global attribute array !!
    Attribute layers = ds.findGlobalAttribute("VGLVLS");
    for (int i = 0; i <= nz; i++)
      dataLayers.set(i, layers.getNumericValue(i).doubleValue());

    for (int i = 0; i < nz; i++) {
      double midpoint = (dataLayers.get(i) + dataLayers.get(i + 1)) / 2;
      dataLev.set(i, midpoint);
    }

    CoordinateAxis v = new CoordinateAxis1D(ds, null, "level", DataType.DOUBLE, dimName, unitName,
            "synthesized coordinate from " + levelsName + " global attributes");
    v.setCachedData(dataLev, true);
    v.addAttribute(new Attribute("positive", "down"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));

    // layer edges
    String edge_name = "layer";
    Dimension lay_edge = new Dimension(edge_name, nz + 1);

    ds.addDimension(null, lay_edge);
    CoordinateAxis vedge = new CoordinateAxis1D(ds, null, edge_name, DataType.DOUBLE, edge_name, unitName,
            "synthesized coordinate from " + levelsName + " global attributes");
    vedge.setCachedData(dataLayers, true);
    v.setBoundaryRef(edge_name);

    ds.addCoordinateAxis(v);
    ds.addCoordinateAxis(vedge);
  }

  private void makeTimeCoordAxis(NetcdfDataset ds, String timeName) {
    int start_date = findAttributeInt(ds, "SDATE");
    int start_time = findAttributeInt(ds, "STIME");
    int time_step = findAttributeInt(ds, "TSTEP");

    int year = start_date / 1000;
    int doy = start_date % 1000;
    int hour = start_time / 10000;
    start_time = start_time % 10000;
    int min = start_time / 100;
    int sec = start_time % 100;

    Calendar cal = new GregorianCalendar(new SimpleTimeZone(0, "GMT"));
    cal.clear();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.DAY_OF_YEAR, doy);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, min);
    cal.set(Calendar.SECOND, sec);
    //cal.setTimeZone( new SimpleTimeZone(0, "GMT"));

    java.text.SimpleDateFormat dateFormatOut = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormatOut.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

    String units = "seconds since " + dateFormatOut.format(cal.getTime()) + " UTC";

    // parse the time step
    hour = time_step / 10000;
    time_step = time_step % 10000;
    min = time_step / 100;
    sec = time_step % 100;
    time_step = hour * 3600 + min * 60 + sec;

    Dimension dimt = ds.findDimension(timeName);
    int nt = dimt.getLength();
    ArrayInt.D1 data = new ArrayInt.D1(nt);
    for (int i = 0; i < nt; i++) {
      data.set(i, i * time_step);
    }

    // create the coord axis
    CoordinateAxis1D timeCoord = new CoordinateAxis1D(ds, null, "time", DataType.INT, timeName, units,
            "synthesized time coordinate from SDATE, STIME, STEP global attributes");
    timeCoord.setCachedData(data, true);
    timeCoord.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ds.addCoordinateAxis(timeCoord);
  }

  private CoordinateTransform makeLatLongProjection(NetcdfDataset ds) {
    //double lat0 = findAttributeDouble(ds,  "YCENT");

    // Get lower left and upper right corners of domain in lat/lon
    double x1 = findAttributeDouble(ds, "XORIG");
    double y1 = findAttributeDouble(ds, "YORIG");
    double x2 = x1 + findAttributeDouble(ds, "XCELL") * findAttributeDouble(ds, "NCOLS");
    double y2 = y1 + findAttributeDouble(ds, "YCELL") * findAttributeDouble(ds, "NROWS");

    LatLonProjection ll = new LatLonProjection("LatitudeLongitudeProjection", new ProjectionRect(x1, y1, x2, y2));
    return new ProjectionCT("LatitudeLongitudeProjection", "FGDC", ll);
  }

  private CoordinateTransform makeLCProjection(NetcdfDataset ds) {
    double par1 = findAttributeDouble(ds, "P_ALP");
    double par2 = findAttributeDouble(ds, "P_BET");
    double lon0 = findAttributeDouble(ds, "XCENT");
    double lat0 = findAttributeDouble(ds, "YCENT");

    LambertConformal lc = new LambertConformal(lat0, lon0, par1, par2, 0.0, 0.0, earthRadius);

    return new ProjectionCT("LambertConformalProjection", "FGDC", lc);
  }

  private CoordinateTransform makePolarStereographicProjection(NetcdfDataset ds) {
    //boolean n_polar = (findAttributeDouble(ds, "P_ALP") == 1.0);
    //double central_meridian = findAttributeDouble(ds, "P_GAM");
    double lon0 = findAttributeDouble(ds, "XCENT");
    double lat0 = findAttributeDouble(ds, "YCENT");
    double latts = findAttributeDouble(ds, "P_BET");

    Stereographic sg = new Stereographic(latts, lat0, lon0, 0.0, 0.0, earthRadius);
    //sg.setCentralMeridian(centeral_meridian);

    return new ProjectionCT("PolarStereographic", "FGDC", sg);
  }

  private CoordinateTransform makeEquitorialMercatorProjection(NetcdfDataset ds) {
    double lon0 = findAttributeDouble(ds, "XCENT");
    double par  = findAttributeDouble(ds, "P_ALP");

    Mercator p = new Mercator(lon0, par, 0.0, 0.0, earthRadius);

    return new ProjectionCT("EquitorialMercator", "FGDC", p);
  }

  private CoordinateTransform makeTransverseMercatorProjection(NetcdfDataset ds) {
    double lat0 = findAttributeDouble(ds, "P_ALP");
    double tangentLon = findAttributeDouble(ds, "P_GAM");

    TransverseMercator p = new TransverseMercator(lat0, tangentLon, 1.0, 0.0, 0.0, earthRadius);

    return new ProjectionCT("TransverseMercator", "FGDC", p);
  }

  private CoordinateTransform makeAlbersProjection(NetcdfDataset ds) {
    double lat0 = findAttributeDouble(ds, "YCENT");
    double lon0 = findAttributeDouble(ds, "XCENT");
    double par1 = findAttributeDouble(ds, "P_ALP");
    double par2 = findAttributeDouble(ds, "P_BET");

    AlbersEqualArea p = new AlbersEqualArea(lat0, lon0, par1, par2, 0.0, 0.0, earthRadius);

    return new ProjectionCT("Albers", "FGDC", p);
  }

  private CoordinateTransform makeLambertAzimuthalProjection(NetcdfDataset ds) {
    double lat0 = findAttributeDouble(ds, "YCENT");
    double lon0 = findAttributeDouble(ds, "XCENT");

    LambertAzimuthalEqualArea p =
      new LambertAzimuthalEqualArea(lat0, lon0, 0.0, 0.0, earthRadius);

    return new ProjectionCT("LambertAzimuthal", "FGDC", p);
  }

  private CoordinateTransform makeSTProjection(NetcdfDataset ds) {
    double latt = findAttributeDouble(ds, "PROJ_ALPHA");
    if (Double.isNaN(latt))
      latt = findAttributeDouble(ds, "P_ALP");

    double lont = findAttributeDouble(ds, "PROJ_BETA");
    if (Double.isNaN(lont))
      lont = findAttributeDouble(ds, "P_BET");

    /**
     * Construct a Stereographic Projection.
     * @param latt tangent point of projection, also origin of projecion coord system
     * @param lont tangent point of projection, also origin of projecion coord system
     * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
     */
    Stereographic st = new Stereographic(latt, lont, 1.0, 0.0, 0.0, earthRadius);

    return new ProjectionCT("StereographicProjection", "FGDC", st);
  }

  private CoordinateTransform makeTMProjection(NetcdfDataset ds) {
    double lat0 = findAttributeDouble(ds, "PROJ_ALPHA");
    if (Double.isNaN(lat0))
      lat0 = findAttributeDouble(ds, "P_ALP");

    double tangentLon = findAttributeDouble(ds, "PROJ_BETA");
    if (Double.isNaN(tangentLon))
      tangentLon = findAttributeDouble(ds, "P_BET");

    /**
     * Construct a TransverseMercator Projection.
     * @param lat0 origin of projection coord system is at (lat0, tangentLon)
     * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
     * @param scale scale factor along the central meridian
     */
    TransverseMercator tm = new TransverseMercator(lat0, tangentLon, 1.0, 0.0, 0.0, earthRadius);

    return new ProjectionCT("MercatorProjection", "FGDC", tm);
  }

  /**
   * Intend to use EPSG system parameters
   *
   * @param ds dataset
   * @return CoordinateTransform
   */
  private CoordinateTransform makeUTMProjection(NetcdfDataset ds) {
    int zone = (int) findAttributeDouble(ds, "P_ALP");
    double ycent = findAttributeDouble(ds, "YCENT");
    //double lon0 = findAttributeDouble( "X_CENT");
    //double lat0 = findAttributeDouble( "Y_CENT");

    /**
     * Construct a UTM Projection.
     * @param zone - UTM zone
     * @param if ycent < 0, then isNorth = False
     */
    boolean isNorth = true;
    if (ycent < 0)
      isNorth = false;
    UtmProjection utm = new UtmProjection(zone, isNorth);

    return new ProjectionCT("UTM", "EPSG", utm);
  }

  /////////////////////////////////////////////////////////////////////////

  protected AxisType getAxisType(NetcdfDataset ds, VariableEnhanced ve) {
    Variable v = (Variable) ve;
    String vname = v.getShortName();

    if (vname.equalsIgnoreCase("x"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("y"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("time"))
      return AxisType.Time;

    if (vname.equalsIgnoreCase("level"))
      return AxisType.GeoZ;

    return null;
  }

  protected void makeCoordinateTransforms(NetcdfDataset ds) {
    if (ct != null) {
      VarProcess vp = findVarProcess(ct.getName(), null);
      if (vp != null)
        vp.ct = ct;
    }
    super.makeCoordinateTransforms(ds);
  }

  private double findAttributeDouble(NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    if ((att == null) || (att.isString())) return Double.NaN;
    return att.getNumericValue().doubleValue();
  }

  private int findAttributeInt(NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    return att.getNumericValue().intValue();
  }

}
