package ucar.nc2.dods;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;

/** Test nc2 dods in the JUnit framework.
 * Dataset {
    Grid {
     ARRAY:
        Float64 amp[10];
     MAPS:
        Float64 x[10];
    } OneD;
} Simple;*/

public class TestBennoGrid extends TestCase {

  public TestBennoGrid( String name) {
    super(name);
  }

  public void testGrid() throws IOException, InvalidRangeException {
    DODSNetcdfFile dodsfile = TestDODSRead.openAbs("http://iridl.ldeo.columbia.edu/SOURCES/.NOAA/.NCEP/.CPC/.GLOBAL/.daily/dods");

    Variable dataV = null;

    // array
    assert(null != (dataV = dodsfile.findVariable("olr")));
    assert dataV instanceof DODSVariable;

    // maps
    Variable v = null;
    assert(null != (v = dodsfile.findVariable("time")));
    assert(null != (v = dodsfile.findVariable("lat")));
    assert(null != (v = dodsfile.findVariable("lon")));

    // read data
    Array data = dataV.read("0, 0:72:1, 0:143:1" );
    assert null != data;
  }

}
