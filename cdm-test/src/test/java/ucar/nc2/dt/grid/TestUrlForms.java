/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Test forms of the URL in our file opening classes
 *
 * @author caron
 * @since 3/10/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestUrlForms {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static String testfile = TestDir.cdmUnitTestDir + "conventions/avhrr/amsr-avhrr-v2.20040729.nc";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{testfile});
    result.add(new Object[]{"/"+testfile});
    result.add(new Object[]{"file:"+testfile});
    result.add(new Object[]{"file:/"+testfile});

    return result;
  }

  String name;

  public TestUrlForms( String name) {
    this.name = name;
    System.out.printf("%s%n", name);
  }

  @org.junit.Test
  public void openNetcdfFile() throws Exception {
    try (NetcdfFile ncfile = NetcdfFile.open(name)) {
      assert true;
    }
  }

  @org.junit.Test
  public void openNetcdfDataset() throws Exception {
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(name)) {
      assert true;
    }
  }

  @org.junit.Test
  public void openGridDataset() throws Exception {
    try (GridDataset ncd = GridDataset.open(name)) {
      assert true;
    }
  }


}
