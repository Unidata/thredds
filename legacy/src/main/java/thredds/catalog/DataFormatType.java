/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
    public final static DataFormatType NETCDF = new DataFormatType("netCDF");
    public final static DataFormatType NETCDF4 = new DataFormatType("netCDF-4");
    public final static DataFormatType NEXRAD2 = new DataFormatType("NEXRAD-2");
    public final static DataFormatType NCML = new DataFormatType("NcML");
    public final static DataFormatType NIDS = new DataFormatType("NEXRAD-3");
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