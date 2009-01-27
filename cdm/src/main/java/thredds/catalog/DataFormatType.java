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
    private DataFormatType(String s) {
      this.name = s;
      members.add(this);
    }
  
    private DataFormatType(String s, boolean fake)
    {
      this.name = s;
    }

    /**
     * Return all DataFormatType objects
     *
     * @return Collection of known DataFormatType-s
     */
    public static java.util.Collection<DataFormatType> getAllTypes() { return members; }

    /**
     * Find the known DataFormatType that matches the given name (ignoring case)
     * or null if the name is unknown.
     *
     * @param name name of the desired DataFormatType.
     * @return DataFormatType or null if no match.
     */
    public static DataFormatType findType(String name) {
      if (name == null) return null;
      for (DataFormatType m : members) {
        if (m.name.equalsIgnoreCase( name))
          return m;
      }
      return null;
    }
    /**
     * Return a DataFormatType for the given name by either matching
     * a known type (ignoring case) or creating an unknown type.
     *
     * @param name name of the desired DataFormatType.
     * @return the named DataFormatType or null if given name is null.
     */
    public static DataFormatType getType(String name)
    {
      if (name == null)
        return null;
      DataFormatType t = findType( name );
      return t != null ? t : new DataFormatType( name, false);
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