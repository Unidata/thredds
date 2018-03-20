/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.constants;

/**
 * redo thredds.catalog.DataFormatType as enum
 * break dependency of ucar.nc2 on server catalog
 *
 * @author caron
 * @since 1/7/2015
 */
public enum DataFormatType {
  BUFR(null),
  ESML(null),
  GEMPAK(null),
  GINI(null),
  GRIB1("GRIB-1"),
  GRIB2("GRIB-2"),
  HDF4(null),
  HDF5(null),
  MCIDAS_AREA("McIDAS-AREA"),
  NCML("NcML"),
  NETCDF("NetCDF"),
  NETCDF4("NetCDF-4"),
  NEXRAD2(null),
  NIDS(null),

  GIF("image/gif"),
  JPEG("image/jpeg"),
  TIFF("image/tiff"),

  CSV("text/csv"),
  HTML("text/html"),
  PLAIN("text/plain"),
  TSV("text/tab-separated-values"),
  XML("text/xml"),

  MPEG("video/mpeg"),
  QUICKTIME("video/quicktime"),
  REALTIME("video/realtime");

  private final String desc;

  DataFormatType(String desc) {
    this.desc = (desc == null) ? toString() : desc;
  }

  // case insensitive
  public static DataFormatType getType(String name) {
    if (name == null) return null;
    for (DataFormatType m : values()) {
      if (m.desc.equalsIgnoreCase( name))
        return m;
      if (m.toString().equalsIgnoreCase( name))
        return m;
    }
    return null;
  }

  public String getDescription() {
    return desc;
  }
}
