package ucar.nc2.ncml4;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.nc2.*;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

public class TestNcmlAggNewSync extends TestCase {

  public TestNcmlAggNewSync( String name) {
    super(name);
  }

  String dataDir = "//zero/share/testdata/image/testSync/";

  public void testMove() throws IOException, InterruptedException {
     move(dataDir+"SUPER-NATIONAL_8km_WV_20051128_2100.gini");

     String filename = "file:./"+TestNcML.topDir + "offsite/aggNewSync.xml";
     NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
     testAggCoordVar( ncfile, 7);
     ncfile.close();

     moveBack(dataDir+"SUPER-NATIONAL_8km_WV_20051128_2100.gini");

     ncfile = new NcMLReader().readNcML(filename, null);
     testAggCoordVar( ncfile, 8);
     ncfile.close();

   }

  public void testSync() throws IOException, InterruptedException {
     move(dataDir+"SUPER-NATIONAL_8km_WV_20051128_2100.gini");

     String filename = "file:./"+TestNcML.topDir + "offsite/aggNewSync.xml";
     NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
     testAggCoordVar( ncfile, 7);

     moveBack(dataDir+"SUPER-NATIONAL_8km_WV_20051128_2100.gini");
     Thread.currentThread().sleep(2000);

     ncfile.sync();
     testAggCoordVar( ncfile, 8);
     ncfile.close();
   }


  public void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize();
    assert time.getShape()[0] == n;
    assert time.getDataType() == DataType.STRING;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == String.class;

    String prev = null;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      String curr = (String) dataI.getObjectNext();
      System.out.println(" coord="+curr);
      assert (prev == null) || prev.compareTo( curr) < 0;
      prev = curr;
    }
  }

  void move( String filename) {
    File f = new File( filename);
    if (f.exists())
      f.renameTo( new File(filename+".save"));
  }

  void moveBack( String filename) {
    File f = new File( filename+".save");
    f.renameTo( new File(filename));
  }

}


