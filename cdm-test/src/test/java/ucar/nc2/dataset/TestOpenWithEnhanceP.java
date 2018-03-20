/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestOpenWithEnhanceP {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean show = false;

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "conventions", new SuffixFileFilter(".nc"), result);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  String filename;
  public TestOpenWithEnhanceP(String filename) {
    this.filename = filename;
  }

  @Test
  public void openWithEnhance() throws Exception {
    try (NetcdfDataset ncDataset = NetcdfDataset.openDataset(filename, true, null)) {
      if (show) ncDataset.writeNcML(System.out, null);
      Assert.assertEquals(NetcdfDataset.getDefaultEnhanceMode(), ncDataset.getEnhanceMode());
      Assert.assertTrue("size="+ncDataset.getCoordinateSystems().size(), ncDataset.getCoordinateSystems().size() > 0);
    }
  }
}
