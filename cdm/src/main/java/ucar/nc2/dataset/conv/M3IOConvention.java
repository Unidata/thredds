// $Id:M3IOConvention.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.projection.*;

import java.util.*;

/**
 * Models-3/EDSS Input/Output netcf format.
 *
 *  The Models-3/EDSS Input/Output Applications Programming Interface (I/O API)
 *  is the standard data access library for both NCSC's EDSS project and EPA's Models-3.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @see "http://www.baronams.com/products/ioapi/index.html"
 */

public class M3IOConvention extends CoordSysBuilder {
  static private java.text.SimpleDateFormat dateFormatOut;
  static {
    dateFormatOut = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormatOut.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /** return true if we think this is a M3IO file. */
  public static boolean isMine( NetcdfFile ncfile) {
    return (null != ncfile.findGlobalAttribute("XORIG"))
      && (null != ncfile.findGlobalAttribute("YORIG"))
      && (null != ncfile.findGlobalAttribute("XCELL"))
      && (null != ncfile.findGlobalAttribute("YCELL"))
      && (null != ncfile.findGlobalAttribute("NCOLS"))
      && (null != ncfile.findGlobalAttribute("NROWS"));
  }

  private boolean isLatLon = false;

  public M3IOConvention() {
    this.conventionName = "M3IO";
  }

  public void augmentDataset( NetcdfDataset ncd, CancelTask cancelTask) {
    constructCoordAxes( ncd);
    ncd.finish();
  }

  private CoordinateTransform ct = null;
  protected void constructCoordAxes(NetcdfDataset ds) {

    Dimension dimx = ds.findDimension("COL");
    int nx = dimx.getLength();

    Dimension dimy = ds.findDimension("ROW");
    int ny = dimy.getLength();

    int projType = findAttributeInt(ds, "GDTYP");
    isLatLon = (projType == 1);
    if (isLatLon) {
      ds.addCoordinateAxis(  makeCoordAxis( ds, "lon", "COL", nx, "XORIG", "XCELL", "degrees east"));
      ds.addCoordinateAxis(  makeCoordAxis( ds, "lat", "ROW", ny, "YORIG", "YCELL", "degrees north"));
    } else {
      ds.addCoordinateAxis(  makeCoordAxis( ds, "x", "COL", nx, "XORIG", "XCELL", "km"));
      ds.addCoordinateAxis(  makeCoordAxis( ds, "y", "ROW", ny, "YORIG", "YCELL", "km"));

      if (projType == 2)
        ct = makeLCProjection(ds);
      else if (projType == 3)
        ct = makeTMProjection(ds);
      else if (projType == 4)
        ct = makeSTProjection(ds);

      if (ct != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, ct);
        ds.addVariable(null, v);
        v.addAttribute( new Attribute(_Coordinate.Axes, "x y"));
      }
    }

    makeZCoordAxis( ds, "LAY", "VGLVLS", "sigma");
    makeTimeCoordAxis( ds, "TSTEP");
  }

  private CoordinateAxis makeCoordAxis( NetcdfDataset ds, String name, String dimName, int n,
    String startName, String incrName, String unitName) {

    double start = .001 * findAttributeDouble( ds, startName); // km
    double incr = .001 * findAttributeDouble( ds, incrName); // km

    CoordinateAxis v = new CoordinateAxis1D( ds, null, name, DataType.DOUBLE, dimName, unitName,
        "synthesized coordinate from "+startName+" "+incrName+" global attributes");
    ds.setValues( v, n, start, incr);
    return v;
  }

  private void makeZCoordAxis( NetcdfDataset ds, String dimName, String levelsName, String unitName) {
    Dimension dimz = ds.findDimension(dimName);
    int nz = dimz.getLength();
    ArrayDouble.D1 dataLev = new ArrayDouble.D1(nz);
    ArrayDouble.D1 dataLayers = new ArrayDouble.D1(nz+1);

    // layer values are a numeric global attribute array !!
    Attribute layers = ds.findGlobalAttribute( "VGLVLS");
    for (int i=0; i<=nz; i++)
      dataLayers.set( i, layers.getNumericValue(i).doubleValue());

    for (int i=0; i<nz; i++) {
      double midpoint = (dataLayers.get(i) + dataLayers.get(i+1))/2;
      dataLev.set( i, midpoint);
    }

    CoordinateAxis v = new CoordinateAxis1D( ds, null, "level", DataType.DOUBLE, dimName, unitName,
       "synthesized coordinate from "+levelsName+" global attributes");
    v.setCachedData( dataLev, true);
    v.addAttribute(new Attribute("positive", "down"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));

    // layer edges
    String edge_name = "layer";
    Dimension lay_edge = new Dimension( edge_name, nz+1, true);
    ds.addDimension( null, lay_edge);
    CoordinateAxis vedge = new CoordinateAxis1D( ds, null, edge_name, DataType.DOUBLE, edge_name, unitName,
       "synthesized coordinate from "+levelsName+" global attributes");
    vedge.setCachedData( dataLayers, true);
    v.setBoundaryRef( edge_name);

    ds.addCoordinateAxis( v);
    ds.addCoordinateAxis( vedge);
  }

  private void makeTimeCoordAxis( NetcdfDataset ds, String timeName) {
    int start_date = findAttributeInt(ds,  "SDATE");
    int start_time = findAttributeInt(ds,  "STIME");
    int time_step = (int) findAttributeInt(ds,  "TSTEP");

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
    cal.set(Calendar.HOUR, hour);
    cal.set(Calendar.MINUTE, min);
    cal.set(Calendar.SECOND, sec);
    //cal.setTimeZone( new SimpleTimeZone(0, "GMT"));

    String units = "seconds since "+dateFormatOut.format( cal.getTime())+" UTC";

    // parse the time step
    hour = time_step / 10000;
    time_step = time_step % 10000;
    min = time_step / 100;
    sec = time_step % 100;
    time_step = hour * 3600 + min * 60 + sec;

    Dimension dimt = ds.findDimension(timeName);
    int nt = dimt.getLength();
    ArrayInt.D1 data = new ArrayInt.D1(nt);
    for (int i=0; i<nt; i++) {
      data.set(i, i * time_step);
    }

    // create the coord axis
    CoordinateAxis1D timeCoord = new CoordinateAxis1D( ds, null, "time", DataType.INT, timeName, units,
       "synthesized time coordinate from SDATE, STIME, STEP global attributes");
    timeCoord.setCachedData( data, true);
    timeCoord.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    ds.addCoordinateAxis( timeCoord);
  }


  private CoordinateTransform makeLCProjection(NetcdfDataset ds) {
    double par1 = findAttributeDouble(ds,  "P_ALP");
    double par2 = findAttributeDouble(ds,  "P_BET");
    double lon0 = findAttributeDouble(ds,  "XCENT");
    double lat0 = findAttributeDouble(ds,  "YCENT");

    LambertConformal lc = new LambertConformal(lat0, lon0, par1, par2);
    CoordinateTransform ct = new ProjectionCT("LambertConformalProjection", "FGDC", lc);

    return ct;
  }

  private CoordinateTransform makeTMProjection(NetcdfDataset ds) {
    double lat0 = findAttributeDouble(ds,  "PROJ_ALPHA");
    double tangentLon = findAttributeDouble(ds,  "PROJ_BETA");
    //double lon0 = findAttributeDouble( "X_CENT");
    //double lat0 = findAttributeDouble( "Y_CENT");

    /**
     * Construct a TransverseMercator Projection.
     * @param lat0 origin of projection coord system is at (lat0, tangentLon)
     * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
     * @param scale scale factor along the central meridian
     */
    TransverseMercator tm = new TransverseMercator(lat0, tangentLon, 1.0);
    CoordinateTransform ct = new ProjectionCT("MercatorProjection", "FGDC", tm);

    return ct;
  }

  private CoordinateTransform makeSTProjection(NetcdfDataset ds) {
    double latt = findAttributeDouble(ds,  "PROJ_ALPHA");
    double lont = findAttributeDouble(ds,  "PROJ_BETA");
    //double lon0 = findAttributeDouble( "X_CENT");
    //double lat0 = findAttributeDouble( "Y_CENT");

    /**
     * Construct a Stereographic Projection.
     * @param latt tangent point of projection, also origin of projecion coord system
     * @param lont tangent point of projection, also origin of projecion coord system
     * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
     */
    Stereographic st= new Stereographic(latt, lont, 1.0);
    CoordinateTransform ct = new ProjectionCT("StereographicProjection", "FGDC", st);

    return ct;
  }

  /////////////////////////////////////////////////////////////////////////

    protected AxisType getAxisType( NetcdfDataset ds, VariableEnhanced ve) {
      Variable v = (Variable) ve;
      String vname = v.getName();

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

   protected void makeCoordinateTransforms( NetcdfDataset ds) {
     if (ct != null) {
      VarProcess vp = findVarProcess(ct.getName());
      if (vp != null)
        vp.ct = ct;
     }
     super.makeCoordinateTransforms(  ds);
   }

  private double findAttributeDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    return att.getNumericValue().doubleValue();
  }

  private int findAttributeInt( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    return att.getNumericValue().intValue();
  }

}
