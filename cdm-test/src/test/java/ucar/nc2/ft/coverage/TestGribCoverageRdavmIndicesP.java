/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsRdaData;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * sanity check on all rdavm indices in the directory
 *
 * @author caron
 * @since 1/5/2016.
 */
@RunWith(Parameterized.class)
@Category(NeedsRdaData.class)
public class TestGribCoverageRdavmIndicesP {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static String topdir = "D:/work/rdavm/index/";
  private static boolean showDetails = false;

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(30);
    try {
      TestDir.actOnAllParameterized(topdir, new SuffixFileFilter(".ncx4"), result);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  String filename;
  public TestGribCoverageRdavmIndicesP(String filename) {
    this.filename = filename;
  }

  @Test
  public void testGridCoverageDatasetRdavm() throws IOException, InvalidRangeException {
    if (showDetails) System.out.printf("%s%n", filename);
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, fdc);
      for (CoverageCollection cc : fdc.getCoverageCollections()) {
        System.out.printf(" %s type=%s%n", cc.getName(), cc.getCoverageType());
        //for (CoverageCoordSys coordSys : cc.getCoordSys()) {
        //Assert.assertTrue( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        //Assert.assertTrue( coordSys.isTime2D(coordSys.getTimeAxis()));
        //}

        if (showDetails)
          for (CoverageCoordAxis axis : cc.getCoordAxes()) {
            if (axis.getAxisType().isTime())
              System.out.printf("  %12s %10s %5d %10s %s%n", axis.getName(), axis.getAxisType(), axis.getNcoords(), axis.getDependenceType(), axis.getSpacing());
          }
      }
    }
  }

}
