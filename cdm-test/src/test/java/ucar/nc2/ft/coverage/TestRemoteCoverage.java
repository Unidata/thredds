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
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Section;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.lang.invoke.MethodHandles;


/**
 * this needs a TDS 5.0 server. disable for now
 *
 * @author caron
 * @since 10/5/2015.
 */
public class TestRemoteCoverage {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsExternalResource.class)
  public void testCdmRemoteCoverage() throws Exception {
    String ds = "http://thredds-test.unidata.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg_ana/latest.xml";

    try (DataFactory.Result result = new DataFactory().openFeatureDataset("thredds:resolve:" + ds, null)) {
      logger.debug("result errlog = {}", result.errLog);
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
      logger.debug("grid_section.getShape = {}", new Section(shape));
      int[] expectShape = new int[] {1, 31, 241, 480};
      Assert.assertArrayEquals("subset shape", expectShape, shape);
    }
  }

}
