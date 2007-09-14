package ucar.nc2.dataset;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml4.NcMLReader;
import ucar.nc2.ncml4.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestSubset extends TestCase {

  public TestSubset( String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;
  String filename = "file:./"+TestNcML.topDir + "subsetAwips.xml";

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

  public void testSubsetData() {
    Variable v = ncfile.findVariable("t");
    Variable vsub = ncfile.findVariable("T-MandatoryLevels");
    Array data, dataSub;

    assert v != null;
    assert vsub != null;

    try {
      dataSub = vsub.read();

      int[] origin = new int[ v.getRank()];
      int[] shape = v.getShape();
      origin[1] = 1;
      shape[1] = 19;

      data = v.read(origin, shape);
      compare(data, dataSub);

      Array data2 = v.read("*,1:19,*,*");
      compare(data2, dataSub);

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  void compare( Array data1, Array data2) {
    assert data1.getSize() == data2.getSize();

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    while (iter1.hasNext() && iter2.hasNext()) {
      double d1 = iter1.getDoubleNext();
      double d2 = iter2.getDoubleNext();

      assert TestUtils.close( d1, d2) : d1+" != "+d2;
    }


  }


}
