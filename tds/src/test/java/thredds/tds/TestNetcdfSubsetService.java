package thredds.tds;

import junit.framework.*;

import ucar.nc2.dataset.*;
import ucar.nc2.*;

import java.io.IOException;
import java.io.File;

public class TestNetcdfSubsetService extends TestCase {

  public TestNetcdfSubsetService( String name) {
    super(name);
  }

  public void testNetcdfSubsetService() throws IOException {
    String url = "/ncServer/gribCollection/NAM_CONUS_20km_surface_20060316_0000.grib1.nc?grid=K_index&grid=Sweat_index&west=-140&east=-90&north=50&south=20&time_start=3&time_end=12";
    File fileSave = new File("testNetcdfSubsetService.nc");

    thredds.util.IO.readURLtoFile(TestTDSAll.topCatalog+url, fileSave);
    System.out.println("Copied "+TestTDSAll.topCatalog+url+" to "+fileSave.getPath());

    NetcdfFile ncd = NetcdfDataset.openFile(fileSave.getPath(), null);
    assert ncd != null;

    assert ncd.findVariable("K_index") != null;
    assert ncd.findVariable("Sweat_index") != null;
    assert ncd.findVariable("time") != null;
    assert ncd.findVariable("y") != null;
    assert ncd.findVariable("x") != null;

    Variable v = ncd.findVariable("time");
    assert v.getSize() == 4;

    v = ncd.findVariable("x");
    assert v.getSize() == 235;

    v = ncd.findVariable("y");
    assert v.getSize() == 199;

    ncd.close();
  }


}