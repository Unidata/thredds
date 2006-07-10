package ucar.nc2.dataset.grid;

import junit.framework.TestCase;
import ucar.ma2.*;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.NCdump;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.vertical.VerticalTransform;

import thredds.catalog.DataType;

public class TestSubset extends TestCase {
  private boolean show = false;

  public TestSubset(String name) {
    super(name);
  }

  public void testRegular() throws Exception {
    GridDataset dataset = GridDataset.open("R:/metapps/test/data/grids/03061219_ruc.nc");

    GeoGrid grid = dataset.findGridByName("T");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    CoordinateAxis zaxis = gcs.getVerticalAxis();
    assert zaxis.getUnitsString().equals("hectopascals");

    GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

    GridCoordSys gcs_section = grid_section.getCoordinateSystem();
    CoordinateAxis zaxis2 = gcs_section.getVerticalAxis();
    assert zaxis2.getSize() == 7;
    assert zaxis2.getUnitsString().equals("hectopascals");
    assert gcs_section.getTimeAxis().equals( gcs.getTimeAxis());

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    assert data.getShape()[0] == 2 : data.getShape()[0];
    assert data.getShape()[1] == 7 : data.getShape()[1];
    assert data.getShape()[2] == 22 : data.getShape()[2];
    assert data.getShape()[3] == 31 : data.getShape()[3];

    // NCdump.printArray( data, "grid_section", System.out,  null);
    dataset.close();
  }

  public void testGrib() throws Exception {
    GridDataset dataset = GridDataset.open("R:/testdata/grid/grib/grib1/data/AVN.wmo");

    GeoGrid grid = dataset.findGridByName("Temperature");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    assert data.getShape()[0] == 3 : data.getShape()[0];
    assert data.getShape()[1] == 3 : data.getShape()[1];
    assert data.getShape()[2] == 13 : data.getShape()[2];
    assert data.getShape()[3] == 15 : data.getShape()[3];

    dataset.close();
  }

  public void testWRF() throws Exception {
    GridDataset dataset = GridDataset.open("R:/testdata/grid/netcdf/wrf/wrfout_v2_Lambert.nc");

    GeoGrid grid = dataset.findGridByName("T");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    CoordinateAxis zaxis = gcs.getVerticalAxis();
    assert zaxis.getSize() == 27;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;
    assert vt.getUnitString().equals("Pa");

    GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    assert data.getShape()[0] == 13 : data.getShape()[0];
    assert data.getShape()[1] == 9 : data.getShape()[1];
    assert data.getShape()[2] == 20 : data.getShape()[2];
    assert data.getShape()[3] == 25 : data.getShape()[3];

    GridCoordSys gcs_section = grid_section.getCoordinateSystem();
    CoordinateAxis zaxis2 = gcs_section.getVerticalAxis();
    assert zaxis2.getSize() == 9 : zaxis2.getSize();

    assert zaxis2.getUnitsString().equals(zaxis.getUnitsString());
    assert gcs_section.getTimeAxis().equals( gcs.getTimeAxis());

    VerticalTransform vt_section = gcs_section.getVerticalTransform();
    assert vt_section != null;
    assert vt_section.getUnitString().equals(vt.getUnitString());

    dataset.close();
  }

  public void testDODS() throws Exception {
    String cat = "http://motherlode.ucar.edu:8080/thredds/catalog/model/NCEP/DGEX/CONUS_12km/catalog.xml";
    String dsid = "#NCEP/DGEX/CONUS_12km/latest.xml";
    ThreddsDataFactory.Result result = new ThreddsDataFactory().openDatatype(cat+dsid, null);
    assert result.dtype == DataType.GRID;
    assert result.gridDataset != null;

    GridDataset dataset = (GridDataset) result.gridDataset;

    GeoGrid grid = dataset.findGridByName("Temperature");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    GeoGrid grid_section = grid.subset(new Range(0, 11, 5), null, null, 3, 3, 3);

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    assert data.getShape()[0] == 3 : data.getShape()[0];
    assert data.getShape()[1] == 2 : data.getShape()[1];
    assert data.getShape()[2] == 101 : data.getShape()[2];
    assert data.getShape()[3] == 164 : data.getShape()[3];

    // NCdump.printArray( data, "grid_section", System.out,  null);
    dataset.close();
  }

  public void utestDODS2() throws Exception {
    String threddsURL = "http://lead.unidata.ucar.edu:8080/thredds/dqcServlet/latestOUADAS?adas";
    ThreddsDataFactory.Result result = new ThreddsDataFactory().openDatatype(threddsURL, null);
    assert result.gridDataset != null;
    GridDataset dataset = (GridDataset) result.gridDataset;

    GeoGrid grid = dataset.findGridByName("PT");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
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

    NCdump.printArray(data, "grid_section", System.out, null);
    dataset.close();
  }

  public void test2D() throws Exception {
    GridDataset dataset = GridDataset.open("R:/testdata/grid/netcdf/cf/mississippi.nc");

    GeoGrid grid = dataset.findGridByName("salt");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    GeoGrid grid_section = grid.subset(null, null, null, 5, 5, 5);

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    assert data.getShape()[0] == 1 : data.getShape()[0];
    assert data.getShape()[1] == 4 : data.getShape()[1];
    assert data.getShape()[2] == 13 : data.getShape()[2];
    assert data.getShape()[3] == 26 : data.getShape()[3];

    grid_section = grid.subset(null, new Range(0, 0), null, 0, 2, 2);
    data = grid_section.readDataSlice(-1, -1, -1, -1);
    assert data.getShape()[0] == 1 : data.getShape()[0];
    assert data.getShape()[1] == 1 : data.getShape()[1];
    assert data.getShape()[2] == 32 : data.getShape()[2];
    assert data.getShape()[3] == 64 : data.getShape()[3];

    NCdump.printArray(data, "grid_section", System.out, null);

    LatLonPoint p0 = new LatLonPointImpl(29.0, -90.0);
    LatLonRect bbox = new LatLonRect(p0, 1.0, 2.0);
    grid_section = grid.subset(null, null, bbox, 1, 1, 1);
    data = grid_section.readDataSlice(-1, -1, -1, -1);

    assert data.getShape()[0] == 1 : data.getShape()[0];
    assert data.getShape()[1] == 20 : data.getShape()[1];
    assert data.getShape()[2] == 63 : data.getShape()[2];
    assert data.getShape()[3] == 53 : data.getShape()[3];

    gcs = grid_section.getCoordinateSystem();
    ProjectionRect rect = gcs.getBoundingBox();
    System.out.println(" rect= " + rect);

    p0 = new LatLonPointImpl(30.0, -90.0);
    bbox = new LatLonRect(p0, 1.0, 2.0);
    grid_section = grid.subset(null, null, bbox, 1, 1, 1);
    data = grid_section.readDataSlice(-1, -1, -1, -1);

    assert data.getShape()[0] == 1 : data.getShape()[0];
    assert data.getShape()[1] == 20 : data.getShape()[1];
    assert data.getShape()[2] == 18 : data.getShape()[2];
    assert data.getShape()[3] == 17 : data.getShape()[3];

    gcs = grid_section.getCoordinateSystem();
    System.out.println(" rect= " + gcs.getBoundingBox());


    dataset.close();
  }

  public void test3D() throws Exception {
    GridDataset dataset = GridDataset.open("thredds:resolve:http://motherlode.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_12km/latest.xml");

    GeoGrid grid = dataset.findGridByName("Relative_humidity");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    GeoGrid grid_section = grid.subset(null, null, null, 1, 10, 10);

    Array data = grid_section.readDataSlice(0, -1, -1, -1);
    assert data.getRank() == 3;
    // assert data.getShape()[0] == 6 : data.getShape()[0];
    assert data.getShape()[1] == 43 : data.getShape()[1];
    assert data.getShape()[2] == 62 : data.getShape()[2];

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      float val = ii.getFloatNext();
      if (grid_section.isMissingData(val)) {
        if (!Float.isNaN(val)) {
          System.out.println(" got not NaN at =" + ii);
        }
        int[] current = ii.getCurrentCounter();
        if ((current[1] > 0) && (current[2] > 1)) {
          System.out.println(" got missing at =" + ii);
          System.out.println(current[1] + " " + current[2]);
        }
      }
    }

    dataset.close();
  }

  public void utestLatLonSubset() throws Exception {
    GridDataset dataset = GridDataset.open("thredds:resolve:http://motherlode.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_12km/latest.xml");
    //GridDataset dataset = GridDataset.open("dods://motherlode.ucar.edu:8080/thredds/dodsC/model/NCEP/NAM/CONUS_12km/NAM_CONUS_12km_20060305_1200.grib2");
    // GridDataset dataset = GridDataset.open("R:/testdata/grid/grib/grib2/test/NAM_CONUS_12km_20060305_1200.grib2");
    GeoGrid grid = dataset.findGridByName("Relative_humidity");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    Array data = null;

    long start, took;
    for (int time =0; time < 5; time++) {
      start = System.currentTimeMillis();
      data = grid.readDataSlice(0, -1, -1, -1);
      took = System.currentTimeMillis() -start;
      System.out.println(" whole took= "+took+" msec");
    }

    LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
    GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);
    for (int time =0; time < 5; time++) {
      start = System.currentTimeMillis();
      data = grid_section.readDataSlice(0, -1, -1, -1);
      took = System.currentTimeMillis() -start;
      System.out.println(" subset took= "+took+" msec");
    }

    assert data.getRank() == 3;
    //assert data.getShape()[0] == 6 : data.getShape()[0];
    //assert data.getShape()[1] == 101 : data.getShape()[1];
    //assert data.getShape()[2] == 164 : data.getShape()[2];

    /* IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      float val = ii.getFloatNext();
      if (grid_section.isMissingData(val)) {
        if (!Float.isNaN(val)) {
          System.out.println(" got not NaN at =" + ii);
        }
        int[] current = ii.getCurrentCounter();
        if ((current[1] > 0) && (current[2] > 1)) {
          System.out.println(" got missing at =" + ii);
          System.out.println(current[1] + " " + current[2]);
        }
      }
    }  */

    dataset.close();
  }


}

