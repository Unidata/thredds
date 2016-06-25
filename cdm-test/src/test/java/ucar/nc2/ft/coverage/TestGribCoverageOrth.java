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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * testing non-othogonal 2D time coords
 *
 * @author caron
 * @since 1/6/2016.
 */
@RunWith(Parameterized.class)
@Category(NeedsRdaData.class)
public class TestGribCoverageOrth {
  private static String topdir = "D:/work/rdavm/";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{topdir + "ds277.6/monthly/ds277.6.ncx4"});
    return result;
  }

  String filename;
  public TestGribCoverageOrth(String filename) {
    this.filename = filename;
  }

  @Ignore("files not present")
  @Test
  public void testGridCoverageDatasetFmrc() throws IOException, InvalidRangeException {
    System.out.printf("%s%n", filename);
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cc);

      System.out.printf(" %s type=%s%n", cc.getName(), cc.getCoverageType());
      for (CoverageCoordSys coordSys : cc.getCoordSys()) {
        Assert.assertTrue( coordSys.isTime2D(coordSys.getAxis(AxisType.RunTime)));
        Assert.assertTrue( coordSys.isTime2D(coordSys.getTimeAxis()));
      }

      for (CoverageCoordAxis axis : cc.getCoordAxes()) {
        if (axis.getAxisType().isTime())
          System.out.printf("  %12s %10s %5d %10s %s%n", axis.getName(), axis.getAxisType(), axis.getNcoords(), axis.getDependenceType(), axis.getSpacing());
      }
    }
  }
}
