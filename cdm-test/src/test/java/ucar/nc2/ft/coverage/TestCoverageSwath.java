/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test swath data as coverage
 *
 * @author caron
 * @since 3/17/2016.
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageSwath {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  @Test
  public void TestCoverageSize() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "formats/dmsp/F14200307192230.s.OIS";
    logger.debug("open {}", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.SWATH, gds.getCoverageType());

      String covName = "visibleImagery";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      int[] shape = cover.getShape();
      logger.debug("{}", Misc.showInts(shape));
      Assert.assertArrayEquals(new int[]{1631, 1465}, shape);

      long size = cover.getSizeInBytes();
      Assert.assertEquals(1631 * 1465 * DataType.USHORT.getSize(), size);

      CoverageCoordSys csys = cover.getCoordSys();
      LatLonRect llbb = gds.getLatlonBoundingBox();
      Assert.assertNotNull("getLatlonBoundingBox", llbb);
      logger.debug("llbb={} ({})", llbb.toString2(), llbb);

      SubsetParams subset = new SubsetParams().setLatLonBoundingBox(gds.getLatlonBoundingBox()); // should be the same!
      Optional<CoverageCoordSys> opt = csys.subset(subset);
      Assert.assertTrue(opt.isPresent());

      CoverageCoordSys csyss = opt.get();
      Assert.assertEquals(csys.getXAxis().getNcoords(), csyss.getXAxis().getNcoords());
      Assert.assertEquals(csys.getYAxis().getNcoords(), csyss.getYAxis().getNcoords());

      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      logger.debug("{}", Misc.showInts(data.getShape()));
    }
  }
}
