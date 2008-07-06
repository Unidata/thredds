// $Id:SectionType.java 63 2006-07-12 21:50:51Z edavis $
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

package thredds.wcs;

/**
 * Type-safe enumeration of WCS Section Type.
 *
 * @author john caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

public final class SectionType {
    private static java.util.LinkedHashMap hash = new java.util.LinkedHashMap(10);

    public final static SectionType Service = new SectionType("WCS_Capabilities/Service");
    public final static SectionType Capability = new SectionType("WCS_Capabilities/Capability");
    public final static SectionType ContentMetadata = new SectionType("WCS_Capabilities/ContentMetadata");

    public static java.util.Collection getAllTypes() { return hash.values(); }

    private String name;
    private SectionType(String s) {
      this.name = s;
      hash.put( s, this);
    }

    /**
     * Find the DataType that matches this name.
     * @param name : name to match
     * @return DataType or null if no match.
     */
    public static SectionType getType(String name) {
      if (name == null) return null;
      return (SectionType) hash.get( name);
    }

    /** @return the string name. */
     public String toString() { return name; }

     /** Override Object.hashCode() to be consistent with this equals. */
     public int hashCode() { return name.hashCode(); }
     /** DataType with same name are equal. */
     public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof SectionType)) return false;
       return o.hashCode() == this.hashCode();
    }
}