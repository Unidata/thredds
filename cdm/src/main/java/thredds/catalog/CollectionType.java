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
    private CollectionType(String s) {
      this.name = s;
      members.add(this);
    }

    private CollectionType(String name, boolean fake) {
      this.name = name;
    }

    /** @return all CollectionType objects */
    public static java.util.Collection<CollectionType> getAllTypes() { return members; }

    /**
     * Return the known CollectionType that matches the given name (ignoring case)
     * or null if the name is unknown.
     *
     * @param name name of the desired CollectionType.
     * @return CollectionType or null if no match.
     */
    public static CollectionType findType(String name) {
      if (name == null) return null;
      for (CollectionType m : members) {
        if (m.name.equalsIgnoreCase( name))
          return m;
      }
      return null;
    }

  /**
   * Return a CollectionType that matches the given name by either matching
   * a known type (ignoring case) or creating an unknown type.
   *
   * @param name name of the desired CollectionType
   * @return the named CollectionType or null if given name is null.
   */
    public static CollectionType getType(String name) {
      if (name == null) return null;
      CollectionType type = findType( name);
      return type != null ? type : new CollectionType( name, false);
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