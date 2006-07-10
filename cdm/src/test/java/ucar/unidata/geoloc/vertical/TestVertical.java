package ucar.unidata.geoloc.vertical;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.TestAll;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;

/**
 * Test basic projection methods
 */

public class TestVertical extends TestCase {

  public TestVertical(String name) {
    super(name);
  }

  public void testOceanS() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dataset.grid.GridDataset.open(TestAll.testdataDir + "grid/netcdf/cf/roms_ocean_s_coordinate.nc");

    GridDatatype grid = gds.findGridDatatype("temp");
    assert grid != null;

    GridCoordSystem gcs = grid.getGridCoordSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.OceanS;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testOceanSigma() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dataset.grid.GridDataset.open("R:/testdata/grid/netcdf/cf/gomoos_cf.nc");

    GridDatatype grid = gds.findGridDatatype("temp");
    assert grid != null;

    GridCoordSystem gcs = grid.getGridCoordSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.OceanSigma;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testAtmSigma() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dataset.grid.GridDataset.open("R:/testdata/grid/netcdf/cf/temperature.nc");

    GridDatatype grid = gds.findGridDatatype("Temperature");
    assert grid != null;

    GridCoordSystem gcs = grid.getGridCoordSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.Sigma;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testAtmHybrid() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dataset.grid.GridDataset.open("R:/testdata/grid/netcdf/cf/ccsm2.nc");

    GridDatatype grid = gds.findGridDatatype("T");
    assert grid != null;

    GridCoordSystem gcs = grid.getGridCoordSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.HybridSigmaPressure : vct.getVerticalTransformType();

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  public void testWrfEta() throws java.io.IOException, InvalidRangeException {
    GridDataset gds = ucar.nc2.dataset.grid.GridDataset.open("R:/testdata/grid/netcdf/wrf/wrfout_v2_Lambert.nc");

    GridDatatype grid = gds.findGridDatatype("T");
    assert grid != null;

    GridCoordSystem gcs = grid.getGridCoordSystem();
    assert gcs != null;

    VerticalCT vct = gcs.getVerticalCT();
    assert vct != null;
    assert vct.getVerticalTransformType() == VerticalCT.Type.WRFEta : vct.getVerticalTransformType();

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    gds.close();
  }

  //R:/testdata/grid/netcdf/wrf/wrfout_v2_Lambert.nc

  public void testStride() throws java.io.IOException, InvalidRangeException {
    String url= "dods://lead4.unidata.ucar.edu:8080/thredds/dodsC/model/UCAR/UNIDATA/WRF/STEERED/wrfout_d01_2006-04-20_00_00_00.nc";
    GridDataset gds = ucar.nc2.dataset.grid.GridDataset.open( url);

    GridDatatype grid = gds.findGridDatatype("T");
    assert grid != null;

    grid = grid.makeSubset(null,null,null,1,2,4);

    GridCoordSystem gcs = grid.getGridCoordSystem();
    assert gcs != null;

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;

    ArrayDouble.D3 ca = vt.getCoordinateArray(0);
    assert ca != null;
    assert ca.getRank() == 3 : ca.getRank();

    int[] shape = ca.getShape();
    for (int i = 0; i < 3; i++)
      System.out.println(" shape " + i + " = " + shape[i]);

    assert shape[0] == 30;
    assert shape[1] == 143/2 + 1;
    assert shape[2] == 143/4 + 1;

    gds.close();
  }
}
