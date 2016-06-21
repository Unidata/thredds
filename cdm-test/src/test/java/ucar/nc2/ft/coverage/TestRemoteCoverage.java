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
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Section;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.util.test.category.NeedsExternalResource;


/**
 * this needs a TDS 5.0 server. disable for now
 *
 * @author caron
 * @since 10/5/2015.
 */
public class TestRemoteCoverage {

  @Test
  @Category(NeedsExternalResource.class)
  public void testCdmRemoteCoverage() throws Exception {
    String ds = "http://thredds-dev.unidata.ucar.edu/thredds/catalog/grib/NCEP/DGEX/CONUS_12km/files/latest.xml";

    try (DataFactory.Result result = new DataFactory().openFeatureDataset("thredds:resolve:" + ds, null)) {
      System.out.println("result errlog= " + result.errLog);
      assert !result.fatalError;
      assert result.featureType == FeatureType.GRID;
      assert result.featureDataset != null;

      String gridName = "Temperature_isobaric";
      FeatureDatasetCoverage dataset = (FeatureDatasetCoverage) result.featureDataset;
      Assert.assertNotNull(gridName, dataset.getDataVariable(gridName));
      Assert.assertEquals(1, dataset.getCoverageCollections().size());

      CoverageCollection cc = dataset.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", gcs);
      Assert.assertEquals("CoverageCoordSys rank", 5, gcs.getAxes().size());

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).setHorizStride(3);

      GeoReferencedArray geo = grid.readData(params);
      CoverageCoordSys geoCoordsys = geo.getCoordSysForData();
      Assert.assertNotNull("geoCoordsys", geoCoordsys);

      int[] shape = geoCoordsys.getShape();
      System.out.println("grid_section.getShape= " + new Section(shape));
      int[] expectShape = new int[] {1, 3, 101, 164};
      Assert.assertArrayEquals("subset shape", expectShape, shape);
    }
  }

}
