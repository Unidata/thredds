/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.collection;

/**
 * GRIB constants.
 *
 * @author caron
 * @since 2/23/2016.
 */
public class Grib {

  public static final String VARIABLE_ID_ATTNAME = "Grib_Variable_Id";
  public static final String GRIB_VALID_TIME = "GRIB forecast or observation time";
  public static final String GRIB_RUNTIME = "GRIB reference time";
  public static final String GRIB_STAT_TYPE = "Grib_Statistical_Interval_Type";

  // do not use
  public static boolean debugRead = false;
  public static boolean debugGbxIndexOnly = false;  // we are running with only ncx and gbx index files, no data
  static boolean debugIndexOnlyShow = false;  // debugIndexOnly must be true; show record fetch
  static boolean debugIndexOnly = false;      // we are running with only ncx index files, no data


  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugRead = debugFlag.isSet("Grib/showRead");
    debugIndexOnly = debugFlag.isSet("Grib/indexOnly");
    debugIndexOnlyShow = debugFlag.isSet("Grib/indexOnlyShow");
    debugGbxIndexOnly = debugFlag.isSet("Grib/debugGbxIndexOnly");
  }

  // Class, not interface, per Bloch edition 2 item 19
  private Grib() {} // disable instantiation
}
