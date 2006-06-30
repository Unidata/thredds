// $Id: DataType.java,v 1.10 2006/02/13 19:52:21 caron Exp $
/*
 * Copyright 2002 Unidata Program Center/University Corporation for
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
 * @version $Revision: 1.10 $ $Date: 2006/02/13 19:52:21 $
 */

public final class DataType {
    private static java.util.ArrayList members = new java.util.ArrayList(20);

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

    /** Return all DataType objects */
    public static java.util.Collection getAllTypes() { return members; }

    /**
     * Find the DataType that matches this name, ignore case.
     * @param name : match this name
     * @return DataType or null if no match.
     */
    public static DataType getType(String name) {
      if (name == null) return null;
      for (int i = 0; i < members.size(); i++) {
        DataType m = (DataType) members.get(i);
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


/**
 * $Log: DataType.java,v $
 * Revision 1.10  2006/02/13 19:52:21  caron
 * javadoc
 *
 * Revision 1.9  2005/07/21 18:50:47  caron
 * no message
 *
 * Revision 1.8  2005/04/26 15:22:09  caron
 * consolidate Debug handling
 * clean up initialContent catalogs
 *
 * Revision 1.7  2005/01/24 16:56:58  caron
 * *** empty log message ***
 *
 * Revision 1.6  2004/05/11 23:30:26  caron
 * release 2.0a
 *
 * Revision 1.5  2004/02/20 00:49:49  caron
 * 1.3 changes
 *
 * Revision 1.4  2003/05/29 21:22:42  john
 * getAllTypes()
 *
 * Revision 1.3  2003/03/17 21:29:14  john
 * fix bugs
 *
 * Revision 1.2  2003/03/07 21:12:34  edavis
 * Added GIF instance.
 *
 * Revision 1.1.1.1  2002/11/23 17:49:45  caron
 * thredds reorg
 *
 */
