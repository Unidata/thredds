package ucar.nc2.ncml_old;

import junit.framework.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml_old.NcMLReader;
import ucar.nc2.ncml_old.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLequals extends TestCase {

  public TestNcMLequals( String name) {
    super(name);
  }

  public void testEquals() throws IOException {
    String filename = "file:"+TestNcML.topDir + "testEquals.xml";
    NetcdfDataset ncd = new NcMLReader().readNcML(filename, null);

    String locref  = ncd.getReferencedFile().getLocation();
    NetcdfDataset ncdref = NetcdfDataset.openDataset(locref, false, null);

    TestCompare.compareFiles(ncd, ncdref);

    ncd.close();
    ncdref.close();
  }

}
