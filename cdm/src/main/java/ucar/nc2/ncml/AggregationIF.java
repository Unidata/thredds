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
package ucar.nc2.ncml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author caron
 * @since Aug 10, 2007
 */
public interface AggregationIF {

  public void close() throws IOException;

  public String getDimensionName();

  public void persist() throws IOException;

  public boolean sync() throws IOException;

  public boolean syncExtend(boolean force) throws IOException;

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class Type {
    private static ArrayList<Type> members = new ArrayList<Type>(20);

    public final static Type JOIN_EXISTING_ONE = new Type("joinExistingOne");
    public final static Type JOIN_EXISTING = new Type("joinExisting");
    public final static Type JOIN_NEW = new Type("joinNew");
    public final static Type UNION = new Type("union");
    public final static Type FORECAST_MODEL = new Type("forecastModelRun"); // deprecated
    public final static Type FORECAST_MODEL_COLLECTION = new Type("forecastModelRunCollection");
    public final static Type FORECAST_MODEL_SINGLE = new Type("forecastModelRunSingleCollection");

    private String name;

    public Type(String s) {
      this.name = s;
      members.add(this);
    }

    public static Collection getAllTypes() {
      return members;
    }

    /**
     * Find the CollectionType that matches this name, ignore case.
     *
     * @param name : match this name
     * @return CollectionType or null if no match.
     */
    public static Type getType(String name) {
      if (name == null) return null;
      for (Type m : members) {
        if (m.name.equalsIgnoreCase(name))
          return m;
      }
      return null;
    }

    /**
     * @return the string name.
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
     * CollectionType with same name are equal.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Type)) return false;
      return o.hashCode() == this.hashCode();
    }
  }

}
