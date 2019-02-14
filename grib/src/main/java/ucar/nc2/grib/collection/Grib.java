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

  static public final String VARIABLE_ID_ATTNAME = "Grib_Variable_Id";
  static public final String GRIB_VALID_TIME = "GRIB forecast or observation time";
  static public final String GRIB_RUNTIME = "GRIB reference time";
  static public final String GRIB_STAT_TYPE = "Grib_Statistical_Interval_Type";

  // do not use
  static public boolean debugRead = false;
  static public boolean debugGbxIndexOnly = false;  // we are running with only ncx and gbx index files, no data
  static boolean debugIndexOnlyShow = false;  // debugIndexOnly must be true; show record fetch
  static boolean debugIndexOnly = false;      // we are running with only ncx index files, no data


  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugRead = debugFlag.isSet("Grib/showRead");
    debugIndexOnly = debugFlag.isSet("Grib/indexOnly");
    debugIndexOnlyShow = debugFlag.isSet("Grib/indexOnlyShow");
    debugGbxIndexOnly = debugFlag.isSet("Grib/debugGbxIndexOnly");
  }

  // class not interface, per Bloch edition 2 item 19
  private Grib() {} // disable instantiation
}
