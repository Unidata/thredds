/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.MAMath;
import ucar.ma2.Range;
import ucar.nc2.NCdumpW;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.lang.invoke.MethodHandles;

public class TestGridSubsetThredds {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Ignore("Bad URL, as of 2015/03/11.")
  @Category(NeedsExternalResource.class)
  public void testDODS2() throws Exception {
    String threddsURL = "http://lead.unidata.ucar.edu:8080/thredds/dqcServlet/latestOUADAS?adas";
    GridDataset dataset = null;

    try {
      DataFactory.Result result = new DataFactory().openFeatureDataset(threddsURL, null);
      assert result.featureDataset != null;
      dataset = (GridDataset) result.featureDataset;

      GeoGrid grid = dataset.findGridByName("PT");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      GeoGrid grid_section = grid.subset(null, null, null, 5, 5, 5);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 11 : data.getShape()[1];
      assert data.getShape()[2] == 26 : data.getShape()[2];
      assert data.getShape()[3] == 43 : data.getShape()[3];

      grid_section = grid.subset(null, new Range(0, 0), null, 0, 2, 2);
      data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 1 : data.getShape()[1];
      assert data.getShape()[2] == 65 : data.getShape()[2];
      assert data.getShape()[3] == 106 : data.getShape()[3];

      logger.debug(NCdumpW.toString(data, "grid_section", null));
    } finally {
      if (dataset != null) dataset.close();
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void test3D() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestOnLocalServer.withDodsPath(
            "dodsC/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc"))) {
      logger.debug("{}", dataset.getLocation());

      GeoGrid grid = dataset.findGridByName("Relative_humidity");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      // x and y stride 10
      GeoGrid grid_section = grid.subset(null, null, null, 1, 10, 10);
      Array data = grid_section.readDataSlice(0, -1, -1, -1);      // get first time slice
      assert data.getRank() == 3;
      assert data.getShape()[1] == 7  : data.getShape()[1];
      assert data.getShape()[2] == 10 : data.getShape()[2];

      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        float val = ii.getFloatNext();
        if (grid_section.isMissingData(val)) {
          if (!Float.isNaN(val)) {
            logger.debug(" got not NaN at = {}", ii);
          }
          int[] current = ii.getCurrentCounter();
          if ((current[1] > 0) && (current[2] > 1)) {
            logger.debug("got missing at = {}", ii);
            logger.debug("{} {} ", current[1], current[2]);
          }
        }
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBBSubset() throws Exception {
    try (GridDataset dataset = GridDataset.open(
            "dods://localhost:8081/thredds/dodsC/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc")) {
      GeoGrid grid = dataset.findGridByName("Pressure");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {}",  gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox());

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      LatLonRect llbb_subset = new LatLonRect(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2);
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("result lat/lon bbox = {}", gcs2.getLatLonBoundingBox());
      logger.debug("result bbox = " + gcs2.getBoundingBox());

      ProjectionRect pr = gcs2.getProjection().getDefaultMapArea();
      logger.debug("projection mapArea = {}", pr);
      assert (pr.closeEnough(gcs2.getBoundingBox()));
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBBSubset2() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestOnLocalServer.withDodsPath(
            "dodsC/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc"))) {
      GeoGrid grid = dataset.findGridByName("Pressure");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {}", gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox());

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      LatLonRect llbb_subset = new LatLonRect(new LatLonPointImpl(-15, -140), new LatLonPointImpl(55, 30));
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("result lat/lon bbox = {}", gcs2.getLatLonBoundingBox());
      logger.debug("result bbox = {}", gcs2.getBoundingBox());

      ProjectionRect pr = gcs2.getProjection().getDefaultMapArea();
      logger.debug("projection mapArea = {}", pr);
      assert (pr.closeEnough(gcs2.getBoundingBox()));
    }
  }

  @Test
  @Ignore("Does this dataset exist on a public server?")
  public void testFMRCSubset() throws Exception {
    try (GridDataset dataset = GridDataset.open("dods://localhost:8080/thredds/dodsC/data/cip/fmrcCase1/CIPFMRCCase1_best.ncd")) {
      GeoGrid grid = dataset.findGridByName("Latitude__90_to_+90");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      Range timeRange = new Range(2, 2);
      int bestZIndex = 5;

      GeoGrid subset = grid.subset(timeRange, new Range(bestZIndex, bestZIndex), null, null);
      Array yxData = subset.readYXData(0, 0);

      logger.debug(NCdumpW.toString(yxData, "xyData", null));
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testFindVerticalCoordinate() throws Exception {
    try (GridDataset dataset = GridDataset.open(
            "dods://localhost:8081/thredds/dodsC/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc")) {
      GeoGrid grid = dataset.findGridByName("Relative_humidity");
      assert null != grid;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1D zaxis = gcs.getVerticalAxis();
      float zCoord = 1000;
      int zidx = zaxis.findCoordElement(zCoord);
      assert zidx == 0 : zidx;

      zCoord = 800;
      zidx = zaxis.findCoordElement(zCoord);
      assert zidx == 8 : zidx;

      zCoord = 100;
      zidx = zaxis.findCoordElement(zCoord);
      assert zidx == 28 : zidx;
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testScaleOffset() throws Exception {
    try (GridDataset dataset = GridDataset.open("http://esrl.noaa.gov/psd/thredds/dodsC/Datasets/noaa.oisst.v2/sst.wkmean.1990-present.nc")) {
      GeoGrid grid = dataset.findGridByName("sst");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {} ({})", gcs.getBoundingBox(), gcs.getLatLonBoundingBox());

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      LatLonRect llbb_subset = new LatLonRect(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2);

      GeoGrid grid2 = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid2.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("subset bbox = {} ({})", gcs2.getBoundingBox(), gcs2.getLatLonBoundingBox());

      logger.debug("original grid var = {}", grid.getVariable());
      logger.debug("subset grid var = {}", grid2.getVariable());

      Array data = grid.readDataSlice(0, 0, 159, 0);
      Array data2 = grid2.readDataSlice(0, 0, 0, 0);

      logger.debug(NCdumpW.toString(data, "org", null));
      logger.debug(NCdumpW.toString(data2, "subset", null));

      ucar.unidata.util.test.CompareNetcdf.compareData(data, data2);
    }
  }

  @Test
  @Ignore("Keeps failing on nomads URL.")
  @Category(NeedsExternalResource.class)
  public void testScaleOffset2() throws Exception {
    try (GridDataset dataset = GridDataset.open("dods://nomads.ncdc.noaa.gov/thredds/dodsC/cr20sixhr/air.1936.nc")) {
      GeoGrid grid = dataset.findGridByName("air");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {} ({})", gcs.getBoundingBox(), gcs.getLatLonBoundingBox());

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      LatLonRect llbb_subset = new LatLonRect(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2);

      GeoGrid grid2 = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid2.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("subset bbox = {} ({})", gcs2.getBoundingBox(), gcs2.getLatLonBoundingBox());

      logger.debug("original grid var = {}", grid.getVariable());
      logger.debug("subset grid var = {}", grid2.getVariable());

      Array data = grid.readVolumeData(0);
      Array data2 = grid2.readVolumeData(0);

      logger.debug("minmax org data = {}", MAMath.getMinMax(data));
      logger.debug("minmax subset data = {}", MAMath.getMinMax(data2));
    }
  }
}
