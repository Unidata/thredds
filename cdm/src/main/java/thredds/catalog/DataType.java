/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package thredds.catalog;

/**
 * Type-safe enumeration of THREDDS Data types.
 *
 * @author john caron
 */

public final class DataType {
    private static java.util.List<DataType> members = new java.util.ArrayList<DataType>(20);

    public final static DataType NONE = new DataType("");

    public final static DataType GRID = new DataType("Grid");
    public final static DataType IMAGE = new DataType("Image");
    public final static DataType POINT = new DataType("Point");
    public final static DataType RADIAL = new DataType("Radial");
    public final static DataType STATION = new DataType("Station");
    public final static DataType SWATH = new DataType("Swath");
    public final static DataType TRAJECTORY = new DataType("Trajectory");

    private String name;
    public DataType(String s) {
      this.name = s;
      members.add(this);
    }

    /** @return all DataType objects */
    public static java.util.Collection<DataType> getAllTypes() { return members; }

    /**
     * Find the DataType that matches this name, ignore case.
     * @param name : match this name
     * @return DataType or null if no match.
     */
    public static DataType getType(String name) {
      if (name == null) return null;
      for (DataType m : members) {
        if (m.name.equalsIgnoreCase( name))
          return m;
      }
      return null;
    }

    /** Return the DataType name. */
     public String toString() { return name; }

     /** Override Object.hashCode() to be consistent with this equals. */
     public int hashCode() { return name.hashCode(); }
     /** DataType with same name are equal. */
     public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof DataType)) return false;
       return o.hashCode() == this.hashCode();
    }
}
