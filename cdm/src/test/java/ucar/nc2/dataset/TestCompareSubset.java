package ucar.nc2.dataset;

import junit.framework.*;
import ucar.nc2.*;
import ucar.nc2.ncml4.NcMLReader;
import ucar.nc2.ncml4.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestCompareSubset extends TestCase {

  public TestCompareSubset( String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;
  String filename = "file:./"+TestNcML.topDir + "aggSynthetic.xml";

  public void setUp() {
    if (ncfile != null) return;

    try {
      ncfile = new NcMLReader().readNcML(filename, null);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }
  }

  public void testStructure() {
    //System.out.println("TestNested = \n"+ncfile);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 3;
    assert timeDim.isUnlimited();
  }

  public void testSubsetData() {
    NetcdfFile subset = null;
    try {
      subset = ucar.nc2.NetcdfFile.open("C:/dev/netcdf/test/data/time0.nc");
      System.out.println("testSubsetData = "+TestCompareNetcdf.compareSubset( ncfile, subset));
      subset.close();
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }

  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    if (d1 != 0.0)
      return Math.abs((d1-d2)/d1) < 1.0e-5;
    else
      return Math.abs(d1-d2) < 1.0e-5;
  }
}
