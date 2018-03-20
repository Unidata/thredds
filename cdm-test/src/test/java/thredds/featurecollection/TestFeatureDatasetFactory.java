/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.featurecollection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test open FeatureDatasets through FeatureDatasetFactoryManager
 *
 * @author caron
 * @since 9/26/2015.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestFeatureDatasetFactory {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    //result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf", FeatureType.ANY});
    // coverage would give FeatureType.CURVILINEAR, but we have reverted
    result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf", FeatureType.CURVILINEAR});

    return result;
  }

  String ds;
  FeatureType what;

  public TestFeatureDatasetFactory(String ds, FeatureType what) {
    this.ds = ds;
    this.what = what;
  }

  @Test
  public void testOpen() throws IOException, InvalidRangeException {
    System.out.printf("FeatureDatasetFactoryManager.open %s%n", ds);
    Formatter errlog = new Formatter();
    try (FeatureDataset fd = FeatureDatasetFactoryManager.open(what, ds, null, errlog)) {
      Assert.assertNotNull(errlog.toString()+" "+ds, fd);
      if (fd.getFeatureType().isCoverageFeatureType())
        testCoverage(ds);
    }
  }

  void testCoverage(String endpoint) throws IOException, InvalidRangeException {
    System.out.printf("open CoverageDatasetFactory %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      CoverageCollection gds = cc.findCoverageDataset(what);
      Assert.assertEquals(what, gds.getCoverageType());
    }
  }

}
