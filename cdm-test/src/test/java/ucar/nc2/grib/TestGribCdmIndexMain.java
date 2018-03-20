/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.util.test.TestDir;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * test GribCdmIndex.main
 *
 * @author caron
 * @since 3/9/2015
 */
@RunWith(Parameterized.class)
public class TestGribCdmIndexMain {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // file partition
    result.add(new Object[]{TestDir.cdmTestDataDir + "ucar/nc2/grib/collection/gfs80fc.xml"});

    // timeUnit option
    result.add(new Object[]{TestDir.cdmTestDataDir + "ucar/nc2/grib/collection/hrrrConus3surface.xml"});

    return result;
  }


  ///////////////////////////////////////

  String[] args;

  public TestGribCdmIndexMain(String fc) {
    args = new String[2];
    args[0] = "--featureCollection";
    args[1] = fc;
  }

  @Test
  public void testCreateIndex() throws Exception {
    GribCdmIndex.main(args);
  }
}
