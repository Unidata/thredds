/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.*;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test netcdf dataset in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggReadGridDataset extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestOffAggReadGridDataset( String name) {
    super(name);
  }

  GridDataset gds = null;
  String location = "file:"+ TestDir.cdmUnitTestDir + "conventions/cf/bora_test_agg.ncml";

  public void setUp() {
    try {
      gds = GridDataset.open(location);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }
  }

  protected void tearDown() throws IOException {
    if (gds != null) gds.close();
  }

  public void testStructure() {
    System.out.println("gds opened = "+location+"\n"+ gds);
  }
}
