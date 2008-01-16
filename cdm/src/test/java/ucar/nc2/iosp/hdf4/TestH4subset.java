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

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestH4subset extends TestCase {

  public TestH4subset(String name) {
    super(name);
  }

  public void testSpecificVariableSection() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfFile.open(TestH4read.testDir + "96108_08.hdf");

    Variable v = ncfile.findVariable("CalibratedData");
    assert (null != v);
    assert v.getRank() == 3;
    int[] shape = v.getShape();
    assert shape[0] == 810;
    assert shape[1] == 50;
    assert shape[2] == 716;

    Array data = v.read("0:809:10,0:49:5,0:715:2");
    assert data.getRank() == 3;
    int[] dshape = data.getShape();
    assert dshape[0] == 810 / 10;
    assert dshape[1] == 50 / 5;
    assert dshape[2] == 716 / 2;

    // read entire array
    Array A;
    try {
      A = v.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }

    // compare
    Array Asection = A.section(new Section("0:809:10,0:49:5,0:715:2").getRanges());
    assert (Asection.getRank() == 3);
    for (int i = 0; i < 3; i++)
      assert Asection.getShape()[i] == dshape[i];

    TestCompare.compareData(data, Asection);
  }

  public void testSubsetting() throws IOException, InvalidRangeException {
    int ntrials = 100;

    // LayoutRegular
    TestIosp.testVariableSubset(TestH4read.testDir + "MI1B2T_B54_O003734_AN_05.hdf", "Infrared Radiance%2FRDQI", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", "High_Res_B_Swath/Data Fields/Cold_Sky_Mirror_Count_89B", ntrials);

    // LayoutSegmented (linked)
    TestIosp.testVariableSubset(TestH4read.testDir + "96108_08.hdf", "BlackBody1Temperature", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf", "Total_Attenuated_Backscatter_532", ntrials);
    //testVariableSubset(TestH4read.testDir + "ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf", "AMSR-E Level 2B Land Data/Data Vgroup/Land Parameters");

    // PositioningDataInputStream (not linked, compressed)
    TestIosp.testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "DC Restore Change for Reflective 1km Bands", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data Fields/Cloud_Top_Pressure", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data Fields/Quality_Assurance_1km", ntrials);

    // PositioningDataInputStream (linked, compressed)
    TestIosp.testVariableSubset(TestH4read.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data Fields/Surface Pressure Day", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data Fields/Averaging Kernel Night", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf", "MOD_Grid_Snow_500m/Data Fields/Fractional_Snow_Cover", ntrials); // */

     // LayoutBBTiled (chunked and compressed)
    TestIosp.testVariableSubset(TestH4read.testDir + "eos/misr/MISR_AM1_GP_GMP_P040_O003734_05", "GeometricParameters/Data Fields/CaZenith", ntrials);
    TestIosp.testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes", ntrials);
 }

  public void problemSubset() throws IOException, InvalidRangeException {
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag1 H4header/tagDetail H4header/chunked"));
    H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/chunkTable"));
    TestIosp.testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf","MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes",
        new Section("0:3,1049:3957,464:1452"));
  }
}