package thredds.tds;

import junit.framework.*;

import thredds.catalog.*;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;
import ucar.nc2.constants.DataType;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.ma2.Array;

import java.io.IOException;
import java.util.List;

public class TestDodsServer extends TestCase {

  public TestDodsServer( String name) {
    super(name);
  }

  String dataset = "http://localhost:8080/thredds/dodsC/gribCollection/NAM_CONUS_20km_surface_20060315_1800.grib1";
  public void testGrid() {
    String grid = dataset + ".ascii?Visibility[0:1:0][0:1:0][0:1:0]";
    System.out.println(" request= "+grid);
    String result = IO.readURLcontents(grid);
    System.out.println(" result= "+result);
    assert result.indexOf("Error") < 0;  // not an error message
  }

  public void testGridArray() {
    String array = dataset + ".asc?Visibility.Visibility[0:1:0][0:1:0][0:1:0]";
    System.out.println(" request= "+array);
    String result = IO.readURLcontents(array);
    System.out.println(" result= "+result);
    assert result.indexOf("Error") < 0;  // not an error message
  }

  public void testSingleDataset() throws IOException {
    InvCatalogImpl cat = TestTDSAll.open(null);

    InvDataset ds = cat.findDatasetByID("testSingleDataset");
    assert (ds != null) : "cant find dataset 'testSingleDataset'";
    assert ds.getDataType() == DataType.GRID;

    ThreddsDataFactory fac = new ThreddsDataFactory();

    ThreddsDataFactory.Result dataResult = fac.openDatatype( ds, null);

    assert dataResult != null;
    assert !dataResult.fatalError;
    assert dataResult.tds != null;

    GridDataset gds = (GridDataset) dataResult.tds;
    GridDatatype grid = gds.findGridDatatype("Z_sfc");
    assert grid != null;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;
    assert null == gcs.getVerticalAxis();

    CoordinateAxis1D time = gcs.getTimeAxis1D();
    assert time != null;
    assert time.getSize() == 1;
    assert 102840.0 == time.readScalarDouble();

    dataResult.tds.close();
  }


  private void doOne(String urlString) throws IOException {
    System.out.println("Open and read "+urlString);

    NetcdfFile ncd = NetcdfDataset.openFile(urlString, null);
    assert ncd != null;

    // pick a random variable to read
    List vlist = ncd.getVariables();
    int n = vlist.size();
    assert n > 0;
    Variable v = (Variable) vlist.get(n/2);
    Array data = v.read();
    assert data.getSize() == v.getSize();

    ncd.close();
  }

  public void testUrlReading() throws IOException {
    doOne("http://localhost:8080/thredds/dodsC/testEnhanced/2004050412_eta_211.nc");
    doOne("http://localhost:8080/thredds/dodsC/testNestedCatalog/ocean.nc");
  }


}