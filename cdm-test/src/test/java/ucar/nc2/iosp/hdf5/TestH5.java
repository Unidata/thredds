/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * TestSuite that runs all the sample testsNew
 *
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static boolean dumpFile = false;
  public static String testDir = TestDir.cdmUnitTestDir + "formats/hdf5/";

 public static NetcdfFile open( String filename) {
    try {
      System.out.println("**** Open "+filename);
      NetcdfFile ncfile = NetcdfFile.open(filename);
      if (TestH5.dumpFile) System.out.println("open "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static NetcdfFile openH5( String filename) {
    try {
      System.out.println("**** Open "+ testDir+filename);
      NetcdfFile ncfile = NetcdfFile.open( testDir+filename);
      if (TestH5.dumpFile) System.out.println("open H5 "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static NetcdfDataset openH5dataset( String filename) {
    try {
      System.out.println("**** Open "+ testDir+filename);
      NetcdfDataset ncfile = NetcdfDataset.openDataset( testDir+filename);
      if (TestH5.dumpFile) System.out.println("open H5 "+ncfile);
      return ncfile;

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
      return null;
    }
  }

  public static class H5FileFilter implements FileFilter {
    public boolean accept(File file) {
      String name = file.getPath();
      return (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"));
    }
  }

  //////////////////////////////////////////////////////////////////////////

  // file that is offset 2048 bytes - NPP!
  @org.junit.Test
  public void testSuperblockIsOffset() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("superblockIsOffsetNPP.h5")) {

      Variable v = ncfile.findVariable("BeamTime");
      System.out.printf("%s%n", v);

      Array data = v.read();
      logger.debug("{}", NCdumpW.toString(data, "offset data", null));
      Index ii = data.getIndex();
      assert (data.getLong(ii.set(11, 93)) == 1718796166693743L);

    }
  }

  // file that is offset 512 bytes - MatLab, using compact layout (!)
  @org.junit.Test
  public void testOffsetCompactLayout() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("matlab_cols.mat")) {

      Variable v = ncfile.findVariable("b");
      System.out.printf("%s%n", v);

      Array data = v.read();
      logger.debug("{}", NCdumpW.toString(data, "offset data", null));
      Index ii = data.getIndex();
      assert (data.getDouble(ii.set(3, 2)) == 12.0);

    }
  }

  // groups have a cycle using hard link
  /*
  $ h5dump h5ex_g_traverse.h5
  HDF5 "h5ex_g_traverse.h5" {
  GROUP "/" {
     GROUP "group1" {
        DATASET "dset1" {
           DATATYPE  H5T_STD_I32LE
           DATASPACE  SIMPLE { ( 1, 1 ) / ( 1, 1 ) }
           DATA {
           (0,0): 0
           }
        }
        GROUP "group3" {
           DATASET "dset2" {
              HARDLINK "/group1/dset1"
           }
           GROUP "group4" {
              GROUP "group1" {
                 GROUP "group5" {
                    HARDLINK "/group1"
                 }
              }
              GROUP "group2" {
              }
           }
        }
     }
     GROUP "group2" {
        HARDLINK "/group1/group3"
     }
  }
   */
  @org.junit.Test
  public void testGroupHardLinks() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("groupHasCycle.h5")) {
      System.out.printf("%s%n", ncfile);
    }
  }

}
