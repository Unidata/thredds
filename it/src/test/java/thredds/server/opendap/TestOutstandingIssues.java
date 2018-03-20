/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: $


package thredds.server.opendap;


import junit.framework.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestOutstandingIssues extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testByteAttribute() throws IOException {
    String filename = TestOnLocalServer.withHttpPath("dodsC/scanCdmUnitTests/ft/stationProfile/PROFILER_wind_06min_20091030_2330.nc");
    NetcdfDataset ncd = NetcdfDataset.openDataset(filename, true, null);
    assert ncd != null;
    VariableDS v = (VariableDS) ncd.findVariable("uvQualityCode");
    assert v != null;
    assert v.hasMissing();

    int count = 0;
    Array data = v.read();
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      byte val = ii.getByteNext();
      if (v.isMissing(val)) count++;
      if (val == (byte)-1)
        assert v.isMissing(val);
    }
    System.out.println("size = "+v.getSize()+" missing= "+count);
  }
}
