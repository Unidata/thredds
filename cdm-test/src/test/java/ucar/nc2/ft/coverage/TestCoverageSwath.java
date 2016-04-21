/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Test swath data as coverage
 *
 * @author caron
 * @since 3/17/2016.
 */
public class TestCoverageSwath {

  @Category(NeedsCdmUnitTest.class)
  @Test
  public void TestCoverageSize() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "formats/dmsp/F14200307192230.s.OIS";
    System.out.printf("open %s%n", endpoint);

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
      System.out.printf("%s%n", Misc.showInts(shape));
      Assert.assertArrayEquals(new int[]{1631, 1465}, shape);

      long size = cover.getSizeInBytes();
      Assert.assertEquals(1631*1465, size);

      CoverageCoordSys csys = cover.getCoordSys();
      LatLonRect llbb = gds.getLatlonBoundingBox();
      Assert.assertNotNull("getLatlonBoundingBox", llbb);
      System.out.printf("llbb=%s (%s)%n", llbb.toString2(), llbb);

      SubsetParams subset = new SubsetParams().setLatLonBoundingBox(gds.getLatlonBoundingBox()); // should be the same!
      Optional<CoverageCoordSys> opt = csys.subset(subset);
      Assert.assertTrue(opt.isPresent());

      CoverageCoordSys csyss = opt.get();
      Assert.assertEquals(csys.getXAxis().getNcoords(), csyss.getXAxis().getNcoords());
      Assert.assertEquals(csys.getYAxis().getNcoords(), csyss.getYAxis().getNcoords());

      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
    }
  }

}
