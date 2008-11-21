/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.jni.netcdf;

import ucar.nc2.*;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.io.PrintWriter;

import junit.framework.TestCase;

/**
 * Read or Compare JNI netcdf with Java.
 *
 * @author caron
 * @since Oct 31, 2008
 */
public class TestJni extends TestCase {
  private JniIosp iosp;
  private boolean showFile = true;
  private boolean showDetail = false;
  private boolean showData = false;

  public void setUp() {
    iosp = new JniIosp();
  }

  public TestJni(String name) {
    super(name);
  }

  String testDir = TestAll.upcShareTestDataDir + "netcdf4/";

  public void testReadAll() throws IOException {
    int count = 0;
    count += scanAllDir(testDir+"vlen", null, new ReadAllData());
    //count += scanAllDir(testDir+"nc4/", null, new ReadAllData());
    //count += scanAllDir(testDir+"nc4-classic/", null, new ReadAllData());
    //count += scanAllDir(testDir+"files/", null, new ReadAllData());
    System.out.println("***READ " + count + " files");
  }


  public void testReadOne() throws IOException {
    new ReadAllData().doClosure(testDir+"vlen/cdm_sea_soundings.nc4");
    //new ReadAllData().doClosure("C:/data/test2.nc");
  }

  public void testCompareAll() throws IOException {
    int count = 0;
    count += scanAllDir("D:/netcdf4/", new NetcdfFileFilter(), new CompareData());
    /* count += scanAllDir(testDir+"compound", null, new CompareData());
    count += scanAllDir(testDir+"nc4/", null, new CompareData());
    count += scanAllDir(testDir+"nc4-classic/", null, new CompareData());
    count += scanAllDir(testDir+"files/", null, new CompareData());  */
    System.out.println("***COMPARE " + count + " files");
  }

  public void testCompareOne() throws IOException {
    new CompareData().doClosure(testDir+"vlen/cdm_sea_soundings.nc4");
  }


  class NetcdfFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".nc") || pathname.getName().endsWith(".nc4");
    }
  }

  /////////////////////////////////////////////////////////////////////////////

  /*
ncdump:
netcdf tst_solar_2 {
   types:
     int(*) unimaginatively_named_vlen_type ;
 // global attributes:
     unimaginatively_named_vlen_type :equally_unimaginatively_named_attribute_YAWN = {-99}, {-99, -99};
}

jni:
netcdf R:/testdata/netcdf4/nc4/tst_solar_2.nc {
 :equally_unimaginatively_named_attribute_YAWN = -99, -99, -99; // int
}

java:
netcdf R:/testdata/netcdf4/nc4/tst_solar_2.nc {
 :equally_unimaginatively_named_attribute_YAWN = -99, -99, -99; // int
}
    */

   public void testAttVlen() throws IOException {
     new ReadAllData().doClosure("R:/testdata/netcdf4/nc4/tst_solar_2.nc");
   }

  /////////////////////////////////////////////////////////////////////////////

  /*
  netcdf tst_enums {
types:
  ubyte enum Bradys {Mike = 8, Carol = 7, Greg = 6, Marsha = 5, Peter = 4,
      Jan = 3, Bobby = 2, Whats-her-face = 1, Alice = 0} ;

// global attributes:
  Bradys :brady_attribute = Alice, Peter, Mike ;
}

jni:
netcdf R:/testdata/netcdf4/nc4/tst_enums.nc {
 types:
  enum Bradys { 'Alice' = 0, 'Whats-her-face' = 1, 'Bobby' = 2, 'Jan' = 3, 'Peter' = 4, 'Marsha' = 5, 'Greg' = 6,
    'Carol' = 7, 'Mike' = 8};

 :brady_attribute = "Alice", "Peter", "Mike";
}

java:
netcdf R:/testdata/netcdf4/nc4/tst_enums.nc {
 types:
  enum Bradys { 'Alice' = 0, 'Whats-her-face' = 1, 'Bobby' = 2, 'Jan' = 3, 'Peter' = 4, 'Marsha' = 5,
    'Greg' = 6, 'Carol' = 7, 'Mike' = 8};


 :brady_attribute = "Alice", "Peter", "Mike";
}
   */

  public void testAttEnum() throws IOException {
    new ReadAllData().doClosure("R:/testdata/netcdf4/nc4/tst_enums.nc");
  }

  /////////////////////////////////////////////////////////////////////////////


  /*
  netcdf tst_solar_cmp {
types:
  compound wind_vector {
    float u ;
    float v ;
  } // wind_vector

// global attributes:
   wind_vector :my_favorite_wind_speeds = {13.3, 12.2}, {13.3, 12.2}, {13.3, 12.2} ;
}

jni:
netcdf R:/testdata/netcdf4/nc4/tst_solar_cmp.nc {
 :my_favorite_wind_speeds.u = 13.3f, 13.3f, 13.3f; // float
 :my_favorite_wind_speeds.v = 12.2f, 12.2f, 12.2f; // float
}

java:
netcdf R:/testdata/netcdf4/files/tst_solar_cmp.nc {
 :my_favorite_wind_speeds.u = 13.3f, 13.3f, 13.3f; // float
 :my_favorite_wind_speeds.v = 12.2f, 12.2f, 12.2f; // float
}
   */


  public void testAttCompound() throws IOException {
    new ReadAllData().doClosure("R:/testdata/netcdf4/nc4/tst_solar_cmp.nc");
  }

  /////////////////////////////////////////////////////////////////////////////

  /*
$ ncdump tst_vl.nc
netcdf tst_vl {
types:
  int(*) name1 ;
dimensions:
        dim = 3 ;
variables:
        name1 var(dim) ;
data:

 var = {-99}, {-99, -99}, {-99, -99, -99} ;
}

java:
netcdf R:/testdata/netcdf4/vlen/tst_vl.nc {
 dimensions:
   dim = 3;
 variables:
   int var(dim=3);
 data:
var =
  {-99 , -99 -99 , -99 -99 -99 }
}


jni:
netcdf //zero/share/testdata/netcdf4/vlen/tst_vl.nc {
 dimensions:
   dim = 3;
 variables:
   int var(dim=3, *);
}
   */


  public void testVarVlen() throws IOException {
    new ReadAllData().doClosure("R:/testdata/netcdf4/nc4/tst_vl.nc");
  }

  /*
  netcdf tst_grps {
dimensions:
        dim = 3 ;
variables:
        float var(dim) ;
                var:units = "m/s" ;

// global attributes:
                :title = "for testing groups" ;

group: group-1 {
  dimensions:
        dim = 3 ;
  variables:
        float var(dim) ;
                var:units = "m/s" ;

  // global attributes:
                :title = "for testing groups" ;
  } // group group-1

group: group-2 {
  dimensions:
        dim = 3 ;
  variables:
        float var(dim) ;
                var:units = "m/s" ;
                :title = "for testing groups" ;

  group: group-3 {
    dimensions:
        dim = 3 ;
    variables:
        float var(dim) ;
                var:units = "m/s" ;

    // global attributes:
                :title = "for testing groups" ;
    } // group group-3
  } // group group-2
}
   */

  public void testDims() throws IOException {
    new ReadAllData().doClosure("R:/testdata/netcdf4/nc4/tst_grps.nc");
  }

  ///////////////////////////////////////////////////////////////////////////

  private int scanAllDir(String dirName, FileFilter ff, Closure c) {
    int count = 0;

    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude"))
        count += c.doClosure(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += scanAllDir(f.getAbsolutePath(), ff, c);
    }

    return count;
  }

  private interface Closure {
    int doClosure(String filename);
  }

  private class CompareData implements Closure {
    public int doClosure(String filename) {
      System.out.println("\n------Compare filename " + filename);
      NetcdfFile ncfile = null;
      NetcdfFile ncfileC = null;
      try {
        ncfileC = iosp.open(filename);
        ncfile = NetcdfFile.open(filename);
        TestCompare.compareFiles(ncfile, ncfileC, true, false, false);

      } catch (Throwable e) {
        e.printStackTrace();

      } finally {

        if (ncfileC != null)
          try {
            ncfileC.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }

        if (ncfile != null)
          try {
            ncfile.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }

      }
      return 1;
    }
  }

  private class ReadAllData implements Closure {
    public int doClosure(String filename) {
      System.out.println("\n------Reading filename " + filename);
      PrintWriter pw = (showData) ? new PrintWriter(System.out) : null;
      NetcdfFile ncfile = null;
      try {
        ncfile = iosp.open(filename);
        if (showFile) System.out.println("\n"+ncfile.toString());

        for (Variable v : ncfile.getVariables()) {
          if (v.getSize() > max_size) {
            Section s = makeSubset(v);
            if (showDetail)
              System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
            v.read(s);
          } else {
            if (showDetail)
              System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
            Array data = v.read();
            if (showData)
              NCdumpW.printArray(data, v.getName(), pw, null);
          }
        }

      } catch (Exception e) {
        e.printStackTrace();

      } finally {
        if (ncfile != null)
          try {
            ncfile.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }
      }

      return 1;
    }

    int max_size = 1000 * 1000 * 10;

    Section makeSubset(Variable v) throws InvalidRangeException {
      int[] shape = v.getShape();
      shape[0] = 1;
      Section s = new Section(shape);
      long size = s.computeSize();
      shape[0] = (int) Math.max(1, max_size / size);
      return new Section(shape);
    }
  }


}
