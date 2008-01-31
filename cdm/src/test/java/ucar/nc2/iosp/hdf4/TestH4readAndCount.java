/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.hdf4;

import junit.framework.TestCase;
import ucar.nc2.*;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestH4readAndCount extends TestCase {
  //static public String testDir = "R:/testdata/hdf4/";
  static public String testDir = "C:/data/hdf4/";

  public TestH4readAndCount(String name) {
    super(name);
  }

  public void testReadAndCount() throws IOException {
    System.out.println("  dims  vars gatts  atts strFlds groups");
// ---------------Reading directory C:/data/hdf4/
    read(testDir + "17766010.hdf", 0, 1, 3, 0, 8, 0);
    read(testDir + "96108_08.hdf", 5, 44, 39, 39, 0, 0);
    read(testDir + "balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf", 4, 11, 33, 286, 0, 0);
    read(testDir + "c402_rp_02.diag.sfc.20020122_0130z.hdf", 3, 86, 6, 590, 0, 0);
    read(testDir + "f13_owsa_04010_09A.hdf", 0, 7, 3, 35, 0, 0);
    read(testDir + "MI1B2T_B54_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0);
    read(testDir + "MI1B2T_B55_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0);
    read(testDir + "MI1B2T_B56_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0);
    read(testDir + "TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF", 2, 3, 5, 9, 0, 0);

    System.out.println("\n  dims  vars gatts  atts strFlds groups");

// ---------------Reading directory C:\data\hdf4\eos
    read(testDir + "eos/amsua/amsua16_2008.001_37503_0001_0108_GC.eos", 2, 19, 23, 22, 0, 4);
    read(testDir + "eos/amsua/amsua16_2008.001_37506_0431_0625_WI.eos", 2, 19, 23, 22, 0, 4);
    read(testDir + "eos/amsua/amsua_2000.202_11353_0003_0157_GC.eos", 2, 19, 22, 22, 0, 4);
    read(testDir + "eos/aster/AsterSwath.hdf", 4, 15, 6, 11, 0, 4);
    read(testDir + "eos/aster/PR1B0000-2000101203_010_001.hdf", 25, 67, 4, 39, 351, 14);
    read(testDir + "eos/misr/MI1B2T_B54_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0);
    read(testDir + "eos/misr/MISR_AM1_AGP_P040_F01_24.subset", 6, 14, 44, 5, 7, 6);
    read(testDir + "eos/misr/MISR_AM1_GP_GMP_P040_O003734_05", 3, 24, 56, 20, 7, 3);
    read(testDir + "eos/misr/MISR_AM1_GRP_TERR_GM_P040_AN", 12, 8, 60, 8, 17, 12);
    read(testDir + "eos/modis/MOD02SSH.A2000243.1850.003.hdf", 20, 38, 51, 157, 14, 3);
    read(testDir + "eos/modis/MOD35_L2.A2000243.1850.003.hdf", 6, 14, 19, 96, 0, 4);
    read(testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", 5, 26, 8, 0, 0, 3);
    read(testDir + "eos/tmi/tmi_L2c_2008.001_57703_v04.eos", 2, 16, 12, 13, 0, 4);

    System.out.println("\n  dims  vars gatts  atts strFlds groups");
// ---------------Reading directory C:\data\hdf4\ncidc
    read(testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", 26, 101, 48, 277, 0, 9);
    read(testDir + "ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf", 0, 4, 4, 1, 14, 2);
    read(testDir + "ncidc/AMSR_E_L3_DailyLand_B04_20080101.hdf", 4, 36, 38, 34, 0, 6);
    read(testDir + "ncidc/ESMR-1977131.tne.15", 2, 1, 2, 2, 0, 0);
    read(testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", 16, 22, 53, 64, 14, 3);
    read(testDir + "ncidc/MOD02OBC.A2007001.0005.005.2007307210540.hdf", 46, 139, 51, 7, 865, 0);
    read(testDir + "ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf", 2, 7, 11, 34, 0, 2);

    System.out.println("\n  dims  vars gatts  atts strFlds groups");
// ---------------Reading directory C:\data\hdf4\ssec
    read(testDir + "ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf", 3, 27, 188, 35, 0, 4);
    read(testDir + "ssec/AIRS.2005.08.28.103.L1B.AIRS_Rad.v4.0.9.0.G05241172839.hdf", 9, 216, 284, 20, 0, 4);
    read(testDir + "ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf", 0, 58, 3, 164, 22, 0);
    read(testDir + "ssec/MOD021KM.A2001149.1030.003.2001154234131.hdf", 20, 40, 51, 179, 14, 3);
    read(testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", 13, 62, 63, 560, 0, 4);
  }

  private class Counter {
    int ndims, nvars, natts, ngatts, nstructFields, ngroups;

    private void count(Group g) {
      ndims += g.getDimensions().size();
      nvars += g.getVariables().size();
      ngatts += g.getAttributes().size();
      ngroups += g.getGroups().size();

      for (Variable v : g.getVariables()) {
        natts += v.getAttributes().size();
        if (v instanceof Structure) {
          nstructFields += ((Structure) v).getVariables().size();
        }
      }
      for (Group ng : g.getGroups())
        count(ng);
    }
  }

  private void read(String filename, int ndims, int nvars, int ngatts, int natts, int nstructFields, int ngroups) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(filename);
    Counter c = new Counter();
    c.count(ncfile.getRootGroup());

    if (false) {
      filename = filename.substring(testDir.length());
      filename = StringUtil.replace(filename, '\\', "/");
      System.out.println("read(testDir+\"" + filename + "\"," + c.ndims + "," + c.nvars + "," + c.ngatts + "," + c.natts + "," + c.nstructFields
          + "," + c.ngroups + ");");
      ncfile.close();
      return;
    }

    print(ndims, c.ndims);
    print(nvars, c.nvars);
    print(ngatts, c.ngatts);
    print(natts, c.natts);
    print(nstructFields, c.nstructFields);
    print(ngroups, c.ngroups);
    System.out.println("   " + filename);
    ncfile.close();
  }

  public void readandCountAllInDir(String dirName, FileFilter ff) throws IOException {
    System.out.println("// ---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f))
        read(name, 0, 0, 0, 0, 0, 0);
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        readandCountAllInDir(f.getAbsolutePath(), ff);
    }

  }

  private void print(int want, int have) {
    System.out.format("%5d", have);
    System.out.print((want != have) ? "*" : " ");
  }

  public void makeReadAndCountAll() throws IOException {
    testReadAndCountAllInDir("C:/data/hdf4/", new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".hdf") || pathname.getName().endsWith(".15");
      }
    }); // */
  }

  void testReadAndCountAllInDir(String dirName, FileFilter ff) throws IOException {
    System.out.println("---------------Reading directory " + dirName);
    System.out.println("  dims  vars gatts  atts strFlds groups");
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f))
        read(name, 0, 0, 0, 0, 0, 0);
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        testReadAndCountAllInDir(f.getAbsolutePath(), ff);
    }

  }

  public void problem() throws IOException {
    //TestAll.openAllInDir(testDir, null);

    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag1 H4header/tagDetail H4header/linked H4header/construct"));
    H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag2 H4header/tagDetail H4header/construct"));
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/linked"));

    //TestAll.readAll(testDir+"ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf");
    NetcdfFile ncfile = NetcdfFile.open(testDir + "AIRS.2003.01.24.116.L2.RetStd_H.v5.0.14.0.G07295101113.hdf");
    //Variable v = ncfile.findVariable("Profile_Time");
    //assert v != null;
    //v.read();
    ncfile.close();
  }

}
