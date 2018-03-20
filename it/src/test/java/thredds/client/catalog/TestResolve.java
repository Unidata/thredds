/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/** Test relative URL resolution. */
public class TestResolve {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testResolver() throws IOException {

    String remoteDataset = TestOnLocalServer.withProtocolAndPath(
            "thredds:resolve:http", "catalog/gribCollection/GFS_CONUS_80km/latest.xml");

    try (NetcdfFile ncd = NetcdfDataset.openFile(remoteDataset, null)) {
      List<Attribute> globalAttrs = ncd.getGlobalAttributes();
      String testMessage = "";

      for (Attribute attr : globalAttrs) {
        testMessage = testMessage + "\n" + attr;
      }

      logger.debug(testMessage);
    }
  }
}
