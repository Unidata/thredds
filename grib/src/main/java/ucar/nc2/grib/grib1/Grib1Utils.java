/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1;

/**
 * static utilities for Grib-1
 *
 * @author caron
 * @since 8/30/11
 */
public class Grib1Utils {

  public static String extractParameterCode(Grib1Record record) {
    Grib1SectionProductDefinition pds = record.getPDSsection();
    return pds.getCenter()+"-"+pds.getSubCenter()+"-"+pds.getTableVersion()+"-"+pds.getParameterNumber();
  }

}
