/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

/**
 * NCAR (center 60) overrides
 *
 * @author caron
 * @since 8/29/13
 */
public class NcarTables extends Grib1Customizer {

  NcarTables(Grib1ParamTables tables) {
    super(60, tables);
  }

  // from http://rda.ucar.edu/docs/formats/grib/gribdoc/
  @Override
  public String getSubCenterName(int subcenter) {
    switch (subcenter) {
      case 1: return "CISL/SCD/Data Support Section";
      case 2: return "NCAR Command Language";
      case 3: return "ESSL/MMM/WRF Model";
      default: return "unknown";
    }
  }
}
