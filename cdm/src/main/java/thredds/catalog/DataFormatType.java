// $Id$
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
 * Type-safe enumeration of THREDDS data format types.
 *
 * @author john caron
 * @version $Revision$ $Date$
 */

public final class DataFormatType {
      private static java.util.ArrayList members = new java.util.ArrayList(20);

    public final static DataFormatType NONE = new DataFormatType("");

    public final static DataFormatType BUFR = new DataFormatType("BUFR");
    public final static DataFormatType ESML = new DataFormatType("ESML");
    public final static DataFormatType GEMPAK = new DataFormatType("GEMPAK");
    public final static DataFormatType GINI = new DataFormatType("GINI");
    public final static DataFormatType GRIB1 = new DataFormatType("GRIB-1");
    public final static DataFormatType GRIB2 = new DataFormatType("GRIB-2");
    public final static DataFormatType HDF4 = new DataFormatType("HDF4");
    public final static DataFormatType HDF5 = new DataFormatType("HDF5");
    public final static DataFormatType NETCDF = new DataFormatType("NetCDF");
    public final static DataFormatType NEXRAD2 = new DataFormatType("NEXRAD2");
    public final static DataFormatType NCML = new DataFormatType("NcML");
    public final static DataFormatType NIDS = new DataFormatType("NIDS");

    public final static DataFormatType GIF = new DataFormatType("image/gif");
    public final static DataFormatType JPEG = new DataFormatType("image/jpeg");
    public final static DataFormatType TIFF = new DataFormatType("image/tiff");

    public final static DataFormatType PLAIN = new DataFormatType("text/plain");
    public final static DataFormatType TSV = new DataFormatType("text/tab-separated-values");
    public final static DataFormatType XML = new DataFormatType("text/xml");

    public final static DataFormatType MPEG = new DataFormatType("video/mpeg");
    public final static DataFormatType QUICKTIME = new DataFormatType("video/quicktime");
    public final static DataFormatType REALTIME = new DataFormatType("video/realtime");

    private String name;
    public DataFormatType(String s) {
      this.name = s;
      members.add(this);
    }

    /** Return all DataFormatType objects */
    public static java.util.Collection getAllTypes() { return members; }

    /**
     * Find the DataFormatType that matches this name, ignore case.
     * @param name : match this name
     * @return DataFormatType or null if no match.
     */
    public static DataFormatType getType(String name) {
      if (name == null) return null;
      for (int i = 0; i < members.size(); i++) {
        DataFormatType m = (DataFormatType) members.get(i);
        if (m.name.equalsIgnoreCase( name))
          return m;
      }
      return null;
    }

    /**
     * Return the DataFormatType name.
     */
    public String toString() { return name; }

     /** Override Object.hashCode() to be consistent with this equals. */
     public int hashCode() { return name.hashCode(); }

     /** DataFormatType with same name are equal. */
     public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof DataFormatType)) return false;
       return o.hashCode() == this.hashCode();
    }

}

/**
 * $Log: DataFormatType.java,v $
 * Revision 1.6  2006/02/13 19:52:21  caron
 * javadoc
 *
 * Revision 1.5  2006/01/17 01:46:52  caron
 * use jdom instead of dom everywhere
 *
 * Revision 1.4  2005/07/21 18:50:47  caron
 * no message
 *
 * Revision 1.3  2005/04/28 16:46:33  caron
 * server cleanup
 *
 * Revision 1.2  2004/06/04 00:51:53  caron
 * release 2.0b
 *
 * Revision 1.1  2004/05/11 23:30:26  caron
 * release 2.0a
 *
 * Revision 1.6  2004/02/20 00:49:51  caron
 * 1.3 changes
 *
 * Revision 1.5  2003/05/29 22:51:37  john
 * add NcML
 *
 * Revision 1.4  2003/05/01 17:47:56  edavis
 * Added Resolver instance.
 *
 * Revision 1.3  2003/03/17 21:29:15  john
 * fix bugs
 *
 * Revision 1.2  2003/03/07 21:12:52  edavis
 * Added HTTP instance.
 *
 * Revision 1.1.1.1  2002/11/23 17:49:45  caron
 * thredds reorg
 *
 *
 */
