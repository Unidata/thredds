/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.IOException;

/**
 * ATD Radar file (ad hoc guesses).
 *
 * @author caron
 */

public class ATDRadarConvention extends CoordSysBuilder {

  /**
   * @param ncfile test this NetcdfFile
   * @return true if we think this is a ATDRadarConvention file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    // not really sure until we can examine more files
    String s = ncfile.findAttValueIgnoreCase(null, "sensor_name", "none");
    return s.equalsIgnoreCase("CRAFT/NEXRAD");
  }

  public ATDRadarConvention() {
    this.conventionName = "ATDRadar";
  }

  public void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
    NcMLReader.wrapNcMLresource(ncDataset, CoordSysBuilder.resourcesDir + "ATDRadar.ncml", cancelTask);
  }

}