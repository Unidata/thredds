// $Id: AxisType.java,v 1.3 2006/02/13 19:51:26 caron Exp $
/*
 * Copyright 2002-2004 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset;

/**
 * Type-safe enumeration of netCDF Coordinate Axis types. These are used for tagging
 * georeferencing axes.
 *
 * @author john caron
 * @version $Revision: 1.3 $ $Date: 2006/02/13 19:51:26 $
 */

public class AxisType {

  private static java.util.HashMap hash = new java.util.HashMap(10);

  /**
   * represents the time coordinate
   */
  public final static AxisType Time = new AxisType("Time", 0);
  /**
   * represents a x coordinate
   */
  public final static AxisType GeoX = new AxisType("GeoX", 3);
  /**
   * represents a y coordinate
   */
  public final static AxisType GeoY = new AxisType("GeoY", 2);
  /**
   * represents a z coordinate
   */
  public final static AxisType GeoZ = new AxisType("GeoZ", 1);
  /**
   * represents a latitude coordinate
   */
  public final static AxisType Lat = new AxisType("Lat", 2);
  /**
   * represents a longitude coordinate
   */
  public final static AxisType Lon = new AxisType("Lon", 3);
  /**
   * represents a vertical height coordinate
   */
  public final static AxisType Height = new AxisType("Height", 1);
  /**
   * represents a vertical pressure coordinate
   */
  public final static AxisType Pressure = new AxisType("Pressure", 1);
  /**
   * represents a radial azimuth coordinate
   */
  public final static AxisType RadialAzimuth = new AxisType("RadialAzimuth", 2);
  /**
   * represents a radial distance coordinate
   */
  public final static AxisType RadialDistance = new AxisType("RadialDistance", 1);
  /**
   * represents a radial elevation coordinate
   */
  public final static AxisType RadialElevation = new AxisType("RadialElevation", 3);

  private int order; // canonical ordering time - z - y - x  or elev - azimuth - distance
  private String _AxisType;

  private AxisType(String s, int order) {
    this._AxisType = s;
    this.order = order;
    hash.put(s, this);
  }

  /**
   * Find the AxisType that matches this name.
   *
   * @param name match this name
   * @return AxisType or null if no match.
   */
  public static AxisType getType(String name) {
    if (name == null) return null;
    return (AxisType) hash.get(name);
  }

  /**
   * Axis name.
   *
   * @return the string name.
   */
  public String toString() {
    return _AxisType;
  }

  /**
   * canonical ordering: (time, z, x, y) or (time, elevation, azimuth, distance)
   */
  public int compareTo(AxisType o) {
    return order - o.order;
  }

}


/**
 * $Log: AxisType.java,v $
 * Revision 1.3  2006/02/13 19:51:26  caron
 * javadoc
 *
 * Revision 1.2  2005/02/23 20:01:02  caron
 * *** empty log message ***
 *
 * Revision 1.1  2004/08/16 20:53:47  caron
 * 2.2 alpha (2)
 *
 * Revision 1.2  2003/06/03 20:06:07  caron
 * fix javadocs
 *
 * Revision 1.1  2003/04/08 15:06:23  caron
 * nc2 version 2.1
 *
 */
