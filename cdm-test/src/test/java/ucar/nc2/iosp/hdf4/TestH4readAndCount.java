/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caron
 * @since Jan 1, 2008
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestH4readAndCount {
  static public String testDir = TestDir.cdmUnitTestDir + "formats/hdf4/";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

// ---------------Reading directory C:/data/hdf4/
    result.add(new Object[]{testDir + "17766010.hdf", 0, 2, 2, 0, 8, 0});
    result.add(new Object[]{testDir + "96108_08.hdf", 5, 49, 39, 39, 0, 0});
    result.add(new Object[]{testDir + "balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf", 4, 11, 33, 286, 0, 0});
    result.add(new Object[]{testDir + "c402_rp_02.diag.sfc.20020122_0130z.hdf", 3, 86, 7, 590, 0, 0});
    result.add(new Object[]{testDir + "f13_owsa_04010_09A.hdf", 0, 7, 3, 35, 0, 0});
    result.add(new Object[]{testDir + "MI1B2T_B54_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0});
    result.add(new Object[]{testDir + "MI1B2T_B55_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0});
    result.add(new Object[]{testDir + "MI1B2T_B56_O003734_AN_05.hdf", 8, 4, 2, 4, 0, 0});
    result.add(new Object[]{testDir + "TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF", 0, 3, 5, 9, 0, 0});
    result.add(new Object[]{testDir + "MYD021KM.A2008349.1800.005.2009329084841.hdf", 20, 41, 54, 179, 14, 3});

// ---------------Reading directory C:\data\hdf4\eos
    result.add(new Object[]{testDir + "eos/amsua/amsua16_2008.001_37503_0001_0108_GC.eos", 2, 19, 23, 23, 0, 4});
    result.add(new Object[]{testDir + "eos/amsua/amsua16_2008.001_37506_0431_0625_WI.eos", 2, 19, 23, 23, 0, 4});
    result.add(new Object[]{testDir + "eos/amsua/amsua_2000.202_11353_0003_0157_GC.eos", 2, 21, 22, 23, 0, 4});
    result.add(new Object[]{testDir + "eos/aster/AsterSwath.hdf", 4, 15, 6, 11, 0, 4});
    result.add(new Object[]{testDir + "eos/aster/PR1B0000-2000101203_010_001.hdf", 25, 67, 4, 39, 351, 14});
    result.add(new Object[]{testDir + "eos/misr/MISR_AM1_AGP_P040_F01_24.subset.eos", 6, 14, 44, 5, 7, 6});
    result.add(new Object[]{testDir + "eos/misr/MISR_AM1_GP_GMP_P040_O003734_05.eos", 3, 24, 56, 20, 7, 3});
    result.add(new Object[]{testDir + "eos/misr/MISR_AM1_GRP_TERR_GM_P040_AN.eos", 12, 8, 60, 8, 17, 12});
    result.add(new Object[]{testDir + "eos/modis/MOD02SSH.A2000243.1850.003.hdf", 20, 39, 50, 157, 14, 3});
    result.add(new Object[]{testDir + "eos/modis/MOD35_L2.A2000243.1850.003.hdf", 6, 15, 18, 96, 0, 4});
    result.add(new Object[]{testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", 5, 26, 8, 0, 0, 3});
    result.add(new Object[]{testDir + "eos/tmi/tmi_L2c_2008.001_57703_v04.eos", 2, 16, 12, 14, 0, 4});

// ---------------Reading directory C:\data\hdf4\ncidc
    result.add(new Object[]{testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", 26, 101, 48, 280, 0, 9});
    result.add(new Object[]{testDir + "ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf", 0, 4, 4, 1, 14, 2});
    result.add(new Object[]{testDir + "ncidc/AMSR_E_L3_DailyLand_B04_20080101.hdf", 4, 36, 38, 34, 0, 6});
    result.add(new Object[]{testDir + "ncidc/ESMR-1977131.tne.15", 0, 1, 2, 2, 0, 0});
    result.add(new Object[]{testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", 16, 23, 52, 64, 14, 3});
    result.add(new Object[]{testDir + "ncidc/MOD02OBC.A2007001.0005.005.2007307210540.hdf", 46, 140, 50, 7, 865, 0});
    result.add(new Object[]{testDir + "ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf", 2, 7, 11, 34, 0, 2});

// ---------------Reading directory C:\data\hdf4\ssec
    result.add(new Object[]{testDir + "ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf", 2, 27, 188, 35, 0, 4});
    result.add(new Object[]{testDir + "ssec/AIRS.2005.08.28.103.L1B.AIRS_Rad.v4.0.9.0.G05241172839.hdf", 9, 216, 284, 21, 0, 4});
    result.add(new Object[]{testDir + "ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf", 112, 59, 2, 164, 22, 0});
    result.add(new Object[]{testDir + "ssec/MOD021KM.A2001149.1030.003.2001154234131.hdf", 20, 41, 50, 179, 14, 3});
    result.add(new Object[]{testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", 13, 62, 63, 560, 0, 4});

    return result;
  }

  String filename;
  int ndims, nvars, ngatts, natts, nstructFields, ngroups;

  public TestH4readAndCount(String filename, int ndims, int nvars, int ngatts, int natts, int nstructFields, int ngroups) {
    this.filename = filename;
    this.ndims = ndims;
    this.nvars = nvars;
    this.ngatts = ngatts;
    this.natts = natts;
    this.nstructFields = nstructFields;
    this.ngroups = ngroups;
  }

  @Test
  public void test() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Counter c = new Counter();
      c.count(ncfile.getRootGroup());

      if (false) {
        filename = filename.substring(testDir.length() + 1);
        filename = StringUtil2.replace(filename, '\\', "/");
        System.out.println("result.add(new Object[]{testDir+\"" + filename + "\"," + c.ndims + "," + c.nvars + "," + c.ngatts + "," + c.natts + "," + c.nstructFields
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
    }
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
      filename = StringUtil2.replace(filename, '\\', "/");
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

  private void print(int want, int have) {
    System.out.format("%5d", have);
    System.out.print((want != have) ? "*" : " ");
  }

}
