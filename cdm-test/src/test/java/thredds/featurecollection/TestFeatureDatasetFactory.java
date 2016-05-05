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
package thredds.featurecollection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
