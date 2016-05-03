package ucar.nc2.iosp.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test coordinate extraction on grib file.
 *
 * @author caron
 * @since 9/10/12
 */
public class TestFindXYcoords {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCoordExtract() throws IOException {
    try (ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "formats/grib2/coordExtract/TestCoordExtract.grib2")) {
      System.out.printf("%s%n", dataset.getLocation());

      GeoGrid grid = dataset.findGridByName("Convective_inhibition_surface");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;

      System.out.printf("%s%n", gcs);

      int result[] = gcs.findXYindexFromLatLon(41.3669944444, -91.140575, null);

      System.out.printf("%d %d %n", result[0], result[1]);
      assert result[0] == 538;
      assert result[1] == 97;
    }
  }

  // franck.reinquin@free.fr
  // 3/11/2015

  @Test
  public void bugReport() throws IOException {

    try (GridDataset dataset = GridDataset.open("http://www.unidata.ucar.edu/software/netcdf/examples/sresa1b_ncar_ccsm3_0_run1_200001.nc")) {

      GridDatatype firstGridInfo = dataset.getGrids().get(0);
      System.out.println("Grid name =" + firstGridInfo.getName());
      GeoGrid firstGrid = (GeoGrid) dataset.getGrids().get(0);

      System.out.println("WHOLE GRID");
      GridCoordSystem gcs = firstGrid.getCoordinateSystem();
      System.out.println("is lat/lon system ? " + gcs.isLatLon());
      assert gcs.isLatLon();

      LatLonRect rect = gcs.getLatLonBoundingBox();
      System.out.println("gcs bounding box : latmin=" + rect.getLatMin() + " latmax=" + rect.getLatMax() + " lonmin=" + rect.getLonMin() + " lonmax=" + rect.getLonMax());
      System.out.println("projection       : " + gcs.getProjection());
      System.out.println("width =" + gcs.getXHorizAxis().getSize() + ", height=" + gcs.getYHorizAxis().getSize());
      System.out.println("X is regular     ? " + ((CoordinateAxis1D) gcs.getXHorizAxis()).isRegular());
      System.out.println("X is contiguous  ? " + gcs.getXHorizAxis().isContiguous());
      System.out.println("X start          : " + ((CoordinateAxis1D) gcs.getXHorizAxis()).getStart());
      System.out.println("X increment      : " + ((CoordinateAxis1D) gcs.getXHorizAxis()).getIncrement());
      System.out.println("Y is regular     ? " + ((CoordinateAxis1D) gcs.getYHorizAxis()).isRegular());
      System.out.println("Y is contiguous  ? " + gcs.getYHorizAxis().isContiguous());
      System.out.println("Y start          : " + ((CoordinateAxis1D) gcs.getYHorizAxis()).getStart());
      System.out.println("Y increment      : " + ((CoordinateAxis1D) gcs.getYHorizAxis()).getIncrement());

      LatLonPoint p = gcs.getLatLon(0, 0);
      System.out.println("index (0,0) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());
      p = gcs.getLatLon(1, 1);
      System.out.println("index (1,1) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());

      System.out.println("looking up lat=" + p.getLatitude() + "  lon=" + p.getLongitude());
      int[] xy = gcs.findXYindexFromLatLon(p.getLatitude(), p.getLongitude(), null);
      System.out.println("index= (" + xy[0] + ", " + xy[1] + ")");
      Assert.assertEquals(xy[0], 1);
      Assert.assertEquals(xy[1], 1);

      // --------------------------------------------------------------------------
      double latMin = -20.D, latMax = -10.D, lonMin = 35.D, lonMax = 45.D;
      System.out.println("\nSUBGRID (latmin=" + latMin + "  latmax=" + latMax + "  lonmin=" + lonMin + "  lonmax=" + lonMax + ")");

      LatLonRect latLonRect = new LatLonRect(
              new LatLonPointImpl(latMin, lonMin),
              new LatLonPointImpl(latMax, lonMax));

      GeoGrid gridSubset = firstGrid.subset(null, null, latLonRect, 0, 1, 1);

      GridCoordSystem gcs2 = gridSubset.getCoordinateSystem();

      rect = gcs2.getLatLonBoundingBox();
      System.out.println("is lat/lon system ? " + gcs2.isLatLon());
      System.out.println("gcs bounding box : latmin=" + rect.getLatMin() + " latmax=" + rect.getLatMax() + " lonmin=" + rect.getLonMin() + " lonmax=" + rect.getLonMax());
      System.out.println("projection       : " + gcs.getProjection());
      System.out.println("width =" + gcs2.getXHorizAxis().getSize() + ", height=" + gcs2.getYHorizAxis().getSize());
      System.out.println("X is regular     ? " + ((CoordinateAxis1D) gcs2.getXHorizAxis()).isRegular());
      System.out.println("X is contiguous  ? " + gcs2.getXHorizAxis().isContiguous());
      System.out.println("X start          : " + ((CoordinateAxis1D) gcs2.getXHorizAxis()).getStart());
      System.out.println("X increment      : " + ((CoordinateAxis1D) gcs2.getXHorizAxis()).getIncrement());
      System.out.println("Y is regular     ? " + ((CoordinateAxis1D) gcs2.getYHorizAxis()).isRegular());
      System.out.println("Y is contiguous  ? " + gcs2.getYHorizAxis().isContiguous());
      System.out.println("Y start          : " + ((CoordinateAxis1D) gcs2.getYHorizAxis()).getStart());
      System.out.println("Y increment      : " + ((CoordinateAxis1D) gcs2.getYHorizAxis()).getIncrement());

      p = gcs2.getLatLon(0, 0);
      System.out.println("index (0,0) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());
      p = gcs2.getLatLon(1, 1);
      System.out.println("index (1,1) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());

      System.out.println("looking up lat=" + p.getLatitude() + "  lon=" + p.getLongitude());
      xy = gcs2.findXYindexFromLatLon(p.getLatitude(), p.getLongitude(), null);
      System.out.println("index= (" + xy[0] + ", " + xy[1] + ")");
      Assert.assertEquals(xy[0], 1);
      Assert.assertEquals(xy[1], 1);

    } catch (IOException | InvalidRangeException e) {
      e.printStackTrace();
    }
  }
}
