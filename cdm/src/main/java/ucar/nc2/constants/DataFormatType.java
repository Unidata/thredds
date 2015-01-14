/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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

  private DataFormatType(String desc) {
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
