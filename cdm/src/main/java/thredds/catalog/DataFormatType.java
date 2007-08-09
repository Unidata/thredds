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
 * Type-safe enumeration of THREDDS data format types.
 *
 * @author john caron
 */

public final class DataFormatType {
    private static java.util.List<DataFormatType> members = new java.util.ArrayList<DataFormatType>(20);

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
    public final static DataFormatType MCIDAS_AREA = new DataFormatType("McIDAS-AREA");

    public final static DataFormatType GIF = new DataFormatType("image/gif");
    public final static DataFormatType JPEG = new DataFormatType("image/jpeg");
    public final static DataFormatType TIFF = new DataFormatType("image/tiff");

    public final static DataFormatType PLAIN = new DataFormatType("text/plain");
    public final static DataFormatType TSV = new DataFormatType("text/tab-separated-values");
    public final static DataFormatType XML = new DataFormatType("text/xml");

    public final static DataFormatType MPEG = new DataFormatType("video/mpeg");
    public final static DataFormatType QUICKTIME = new DataFormatType("video/quicktime");
    public final static DataFormatType REALTIME = new DataFormatType("video/realtime");
    public final static DataFormatType OTHER_UNKNOWN = new DataFormatType("other/unknown");

    private String name;
    public DataFormatType(String s) {
      this.name = s;
      members.add(this);
    }

    /** Return all DataFormatType objects */
    public static java.util.Collection<DataFormatType> getAllTypes() { return members; }

    /**
     * Find the DataFormatType that matches this name, ignore case.
     * @param name : match this name
     * @return DataFormatType or null if no match.
     */
    public static DataFormatType getType(String name) {
      if (name == null) return null;
      for (DataFormatType m : members) {
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