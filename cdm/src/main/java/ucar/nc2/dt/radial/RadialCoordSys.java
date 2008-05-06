// $Id:RadialCoordSys.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.dt.radial;

import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.dataset.*;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.units.ConversionException;

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
   * @param parseInfo put debug information into this StringBuffer; may be null.
   * @param cs the CoordinateSystem to test
   * @return true if it can be made into a RadialCoordSys.
   */
  public static boolean isRadialCoordSys( StringBuilder parseInfo, CoordinateSystem cs) {
    return (cs.getAzimuthAxis() != null) && (cs.getRadialAxis() != null) && (cs.getElevationAxis() != null);
  }

  /**
   * Determine if the CoordinateSystem cs can be made into a GridCoordSys for the Variable v.
   * @param parseInfo put debug information into this StringBuffer; may be null.
   * @param cs CoordinateSystem to check.
   * @param v Variable to check.
   * @return the RadialCoordSys made from cs, else null.
   */
  public static RadialCoordSys makeRadialCoordSys( StringBuilder parseInfo, CoordinateSystem cs, VariableEnhanced v) {
    if (parseInfo != null) {
      parseInfo.append(" ");
      v.getNameAndDimensions(parseInfo, false, true);
      parseInfo.append(" check CS " + cs.getName());
    }
    if (isRadialCoordSys( parseInfo, cs)) {
      RadialCoordSys rcs = new RadialCoordSys( cs);
      if (cs.isComplete( v)) {
        if (parseInfo != null) parseInfo.append(" OK\n");
        return rcs;
      } else {
        if (parseInfo != null) parseInfo.append(" NOT complete\n");
      }
    }

    return null;
  }

  private static SimpleUnit kmUnit = SimpleUnit.factory("km");

  /////////////////////////////////////////////////////////////////////////////
  private CoordinateAxis aziAxis, elevAxis, radialAxis, timeAxis;
  private Array aziData, elevData, radialData, timeData;

  private String name;
  private ArrayList coordAxes = new ArrayList();
  private ucar.nc2.units.DateUnit dateUnit;
  private ucar.nc2.dt.EarthLocation origin;
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
  public ucar.nc2.dt.EarthLocation getOrigin() {
    return origin;
  }

  public void setOrigin(ucar.nc2.dt.EarthLocation origin) {
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
        maxRadial =  radialUnit.convertTo(maxRadial, kmUnit); // convert to km

      } catch (IOException e) {
        e.printStackTrace();
      } catch (ConversionException e) {
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
  public ucar.nc2.units.DateUnit getTimeUnits() {
    if (null == dateUnit) {
      dateUnit = (DateUnit) SimpleUnit.factory( timeAxis.getUnitsString());
    }
    return dateUnit;
  }

  /** debug */
  static public void main( String[] args) {
    System.out.println("1 Deg="+Math.toDegrees( 1000 * 111.0 / Earth.getRadius()));
  }

}