/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.constants;

/**
 * Type-safe enumeration of Scientific Feature types.
 *
 * @author john caron
 */

public final class FeatureType {
  private static java.util.List<FeatureType> members = new java.util.ArrayList<FeatureType>(20);

  public final static FeatureType NONE = new FeatureType("");

  public final static FeatureType GRID = new FeatureType("Grid");
  public final static FeatureType IMAGE = new FeatureType("Image");
  public final static FeatureType POINT = new FeatureType("Point");
  public final static FeatureType PROFILE = new FeatureType("Profile");
  public final static FeatureType RADIAL = new FeatureType("Radial");
  public final static FeatureType SECTION = new FeatureType("Section");
  public final static FeatureType STATION = new FeatureType("Station");
  public final static FeatureType SWATH = new FeatureType("Swath");
  public final static FeatureType TRAJECTORY = new FeatureType("Trajectory");
  public final static FeatureType OTHER_UNKNOWN = new FeatureType("other/unknown");

  // experimental
  public final static FeatureType STATION_PROFILE = new FeatureType("StationProfile");
  public final static FeatureType STATION_RADIAL = new FeatureType("StationRadial");
  public final static FeatureType ANY_POINT = new FeatureType("AnyPointFeatureCollection");

  private String name;

  public FeatureType(String s) {
    this.name = s;
    members.add(this);
  }

  /**
   * @return all FeatureType objects
   */
  public static java.util.Collection<FeatureType> getAllTypes() {
    return members;
  }

  /**
   * Find the FeatureType that matches this name, ignore case.
   *
   * @param name : match this name
   * @return FeatureType or null if no match.
   */
  public static FeatureType getType(String name) {
    if (name == null) return null;
    for (FeatureType m : members) {
      if (m.name.equalsIgnoreCase(name))
        return m;
    }
    return null;
  }

  /**
   * Return the FeatureType name.
   */
  public String toString() {
    return name;
  }

  /**
   * Override Object.hashCode() to be consistent with this equals.
   */
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * FeatureType with same name are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeatureType)) return false;
    return o.hashCode() == this.hashCode();
  }
}
