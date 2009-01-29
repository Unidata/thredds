// $Id:RadialCoordSys.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.dt.radial;

import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.dataset.*;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.*;
import java.io.IOException;

/**
 * A radial CoordinateSystem has azimuth, elevation and radial axes.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class RadialCoordSys {

  /**
   * Determine if this CoordinateSystem can be made into a RadialCoordSys.
   * @param parseInfo put debug information into this Formatter; may be null.
   * @param cs the CoordinateSystem to test
   * @return true if it can be made into a RadialCoordSys.
   */
  public static boolean isRadialCoordSys( Formatter parseInfo, CoordinateSystem cs) {
    return (cs.getAzimuthAxis() != null) && (cs.getRadialAxis() != null) && (cs.getElevationAxis() != null);
  }

  /**
   * Determine if the CoordinateSystem cs can be made into a GridCoordSys for the Variable v.
   * @param parseInfo put debug information into this StringBuffer; may be null.
   * @param cs CoordinateSystem to check.
   * @param v Variable to check.
   * @return the RadialCoordSys made from cs, else null.
   */
  public static RadialCoordSys makeRadialCoordSys( Formatter parseInfo, CoordinateSystem cs, VariableEnhanced v) {
    if (parseInfo != null) {
      parseInfo.format(" ");
      v.getNameAndDimensions(parseInfo, true, false);
      parseInfo.format(" check CS " + cs.getName());
    }
    if (isRadialCoordSys( parseInfo, cs)) {
      RadialCoordSys rcs = new RadialCoordSys( cs);
      if (cs.isComplete( v)) {
        if (parseInfo != null) parseInfo.format(" OK\n");
        return rcs;
      } else {
        if (parseInfo != null) parseInfo.format(" NOT complete\n");
      }
    }

    return null;
  }

  /////////////////////////////////////////////////////////////////////////////
  private CoordinateAxis aziAxis, elevAxis, radialAxis, timeAxis;
  private Array aziData, elevData, radialData, timeData;

  private String name;
  private ArrayList coordAxes = new ArrayList();
  private ucar.nc2.units.DateUnit dateUnit;
  private ucar.unidata.geoloc.EarthLocation origin;
  private LatLonRect bb;
  private double maxRadial;

  /** Create a RadialCoordSys from an existing Coordinate System.
   */
  public RadialCoordSys( CoordinateSystem cs) {
    super();

    aziAxis = cs.getAzimuthAxis();
    radialAxis = cs.getRadialAxis();
    elevAxis = cs.getElevationAxis();
    timeAxis = cs.getTaxis();

    coordAxes.add( aziAxis);
    coordAxes.add( radialAxis);
    coordAxes.add( elevAxis);

    // make name based on coordinate
    Collections.sort( coordAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = CoordinateSystem.makeName( coordAxes);

  }

  public String getName() { return name; }
  public List getCoordAxes() { return coordAxes; }

  /** get the Azimuth axis */
  public CoordinateAxis getAzimuthAxis() { return aziAxis; }
  /** get the Elevation axis  */
  public CoordinateAxis getElevationAxis() { return elevAxis; }
  /** get the Radial axis */
  public CoordinateAxis getRadialAxis() { return radialAxis; }
  /** get the Time axis */
  public CoordinateAxis getTimeAxis() { return timeAxis; }

    /** get the Azimuth axis data. Calling this will force the data to be cached. */
  public Array getAzimuthAxisDataCached() throws IOException {
    if (aziData == null)
      aziData = aziAxis.read();
    return aziData;
  }

    /** get the Elevation axis data. Calling this will force the data to be cached. */
  public Array getElevationAxisDataCached() throws IOException {
    if (elevData == null)
      elevData = elevAxis.read();
    return elevData;
  }

    /** get the Radial axis data. Calling this will force the data to be cached. */
  public Array getRadialAxisDataCached() throws IOException {
    if (radialData == null)
      radialData = radialAxis.read();
    return radialData;
  }

    /** get the Time axis data. Calling this will force the data to be cached. */
  public Array getTimeAxisDataCached() throws IOException {
    if (timeData == null)
      timeData = timeAxis.read();
    return timeData;
  }

    /** Origin of the coordinate system */
  public ucar.unidata.geoloc.EarthLocation getOrigin() {
    return origin;
  }

  public void setOrigin(ucar.unidata.geoloc.EarthLocation origin) {
    this.origin = origin;
  }

  /**
   * Get the maximum radial distance, in km.
   */
  public double getMaximumRadial() {
    if (maxRadial == 0.0) {
      try {
        Array radialData = getRadialAxisDataCached();
        maxRadial = MAMath.getMaximum( radialData);

        String units = getRadialAxis().getUnitsString();
        SimpleUnit radialUnit = SimpleUnit.factory(units);
        maxRadial = radialUnit.convertTo(maxRadial, SimpleUnit.kmUnit); // convert to km

      } catch (IOException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      }
    }
    return maxRadial;
  }

  public LatLonRect getBoundingBox() {
    if (bb != null)
      return bb;
    if (origin == null)
      return null;

    double dLat = Math.toDegrees( getMaximumRadial() / Earth.getRadius());
    double latRadians = Math.toRadians( origin.getLatitude());
    double dLon = dLat * Math.cos(latRadians);

    double lat1 = origin.getLatitude() - dLat/2;
    double lon1 = origin.getLongitude() - dLon/2;
    bb = new LatLonRect( new LatLonPointImpl( lat1, lon1), dLat, dLon);

    return bb;
  }


  /** Get the units of Calendar time.
   *  To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   *  To get units as a String, call DateUnit.getUnitsString().
   */
  public ucar.nc2.units.DateUnit getTimeUnits() throws Exception {
    if (null == dateUnit) {
      dateUnit = new DateUnit( timeAxis.getUnitsString());
    }
    return dateUnit;
  }

  /** debug */
  static public void main( String[] args) {
    System.out.println("1 Deg="+Math.toDegrees( 1000 * 111.0 / Earth.getRadius()));
  }

}