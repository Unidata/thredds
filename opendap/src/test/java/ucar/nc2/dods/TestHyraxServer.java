/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 1/28/2015
 */
public class TestHyraxServer {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Ignore("server not running")
  @org.junit.Test
  public void testGrid() throws IOException, InvalidRangeException {
    try (DODSNetcdfFile dodsfile = TestDODSRead.openAbs("http://data.nodc.noaa.gov/opendap/pathfinder/Version5.0_Climatologies/Monthly/Day/month01_day.hdf")) {

      // should test that we get grids
      // see note1 in DODNetcdfFile
    }
  }
}
