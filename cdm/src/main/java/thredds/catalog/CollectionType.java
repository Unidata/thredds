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

package thredds.catalog;

/**
 * Type-safe enumeration of THREDDS coherent collection types.
 *
 * @author john caron
 */

public final class CollectionType {
    private static java.util.ArrayList<CollectionType> members = new java.util.ArrayList<CollectionType>(20);

    public final static CollectionType NONE = new CollectionType("");
    public final static CollectionType TIMESERIES = new CollectionType("TimeSeries");
    public final static CollectionType STATIONS = new CollectionType("Stations");
    public final static CollectionType FORECASTS = new CollectionType("ForecastModelRuns");

    private String name;
    public CollectionType(String s) {
      this.name = s;
      members.add(this);
    }

    /** @return all CollectionType objects */
    public static java.util.Collection<CollectionType> getAllTypes() { return members; }

    /**
     * Find the CollectionType that matches this name, ignore case.
     * @param name : match this name
     * @return CollectionType or null if no match.
     */
    public static CollectionType getType(String name) {
      if (name == null) return null;
      for (CollectionType m : members) {
        if (m.name.equalsIgnoreCase( name))
          return m;
      }
      return null;
    }

    /**
     * Return the collection name.
     */
     public String toString() {
        return name;
    }

     /** Override Object.hashCode() to be consistent with this equals. */
     public int hashCode() { return name.hashCode(); }
     /** CollectionType with same name are equal. */
     public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof CollectionType)) return false;
       return o.hashCode() == this.hashCode();
    }
}