/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;

public class TestRemoteAggregation {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testAggExisting() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncml/remote.ncml";

    System.out.printf(" testAggExisting: read %s%n%n%s", filename, IO.readFile(filename));

    NetcdfDataset ncd = NetcdfDataset.openDataset(filename);
    System.out.println(" testAggExisting.open "+ ncd);

    Variable sst_time = ncd.findVariable("sst_time");
    assert sst_time != null;
    assert sst_time.getRank() == 2;
    int[] shape =  sst_time.getShape();
    assert shape[0] == 6;
    assert shape[1] == 1;

    ncd.close();
  }
}

