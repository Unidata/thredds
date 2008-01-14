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
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.TestLocal;
import ucar.nc2.TestCompare;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestReadSubset extends TestCase {

  public TestReadSubset(String name) {
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
    // LayoutRegular
    testVariableSubset(TestH4read.testDir + "MI1B2T_B54_O003734_AN_05.hdf", "Infrared Radiance%2FRDQI");
    testVariableSubset(TestH4read.testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", "High_Res_B_Swath/Data Fields/Cold_Sky_Mirror_Count_89B");

    // LayoutSegmented (linked)
    testVariableSubset(TestH4read.testDir + "96108_08.hdf", "BlackBody1Temperature");
    testVariableSubset(TestH4read.testDir + "ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf", "Total_Attenuated_Backscatter_532");
    //testVariableSubset(TestH4read.testDir + "ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf", "AMSR-E Level 2B Land Data/Data Vgroup/Land Parameters");

    // PositioningDataInputStream (not linked, compressed)
    testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "DC Restore Change for Reflective 1km Bands");
    testVariableSubset(TestH4read.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data Fields/Cloud_Top_Pressure");
    testVariableSubset(TestH4read.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data Fields/Quality_Assurance_1km");

    // PositioningDataInputStream (linked, compressed)
    testVariableSubset(TestH4read.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data Fields/Surface Pressure Day");
    testVariableSubset(TestH4read.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data Fields/Averaging Kernel Night");
    testVariableSubset(TestH4read.testDir + "ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf", "MOD_Grid_Snow_500m/Data Fields/Fractional_Snow_Cover"); // */

     // LayoutBBTiled (chunked and compressed)
    testVariableSubset(TestH4read.testDir + "eos/misr/MISR_AM1_GP_GMP_P040_O003734_05", "GeometricParameters/Data Fields/CaZenith");
    testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes");
 }

  public void problemSubset() throws IOException, InvalidRangeException {
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag1 H4header/tagDetail H4header/chunked"));
    H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/chunkTable"));
    testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf","MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes",
        new Section("0:3,1049:3957,464:1452"));
  }

  void testVariableSubset(String filename, String varName, Section s) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);
    Variable v = ncfile.findVariable(varName);
    assert (null != v);
    testOne(v, s, v.read());
  }

  void testVariableSubset(String filename, String varName) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);

    Variable v = ncfile.findVariable(varName);
    assert (null != v);
    int[] shape = v.getShape();

    // read entire array
    Array A;
    try {
      A = v.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }

    int[] dataShape = A.getShape();
    assert dataShape.length == shape.length;
    for (int i = 0; i < shape.length; i++)
      assert dataShape[i] == shape[i];
    Section all = v.getShapeAsSection();
    System.out.println("  Entire dataset="+all);

    int ntrials = 22;
    for (int k = 0; k < ntrials; k++) {
      // create a random subset, read and compare
      testOne(v, randomSubset(all, 1), A);
      testOne(v, randomSubset(all, 2), A);
      testOne(v, randomSubset(all, 3), A);
    }

    ncfile.close();
  }

  public void problem() throws InvalidRangeException, IOException {
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/chunkTable")); // H4header/construct"));

    NetcdfFile ncfile = NetcdfFile.open(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf");
    Variable v = ncfile.findVariable("MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB");
    Section s= new Section("0:2:1,22:3152:1,0:1350:1"); // "0:2:1,28:3152:1,530:1358:1"
    testOne(v, s, v.read());
  }

  void testOne(Variable v, Section s, Array fullData) throws IOException, InvalidRangeException {
      System.out.println("   section="+s);

      // read just that
      Array sdata = v.read(s);
      assert sdata.getRank() == s.getRank();
      int[] sshape = sdata.getShape();
      for (int i = 0; i < sshape.length; i++)
        assert sshape[i] == s.getShape(i);

      // compare with logical section
      Array Asection = fullData.sectionNoReduce(s.getRanges());
      int[] ashape = Asection.getShape();
      assert (ashape.length == sdata.getRank());
      for (int i = 0; i < ashape.length; i++)
        assert sshape[i] == ashape[i];

      TestCompare.compareData(sdata, Asection);
  }

  Section randomSubset(Section all, int stride) throws InvalidRangeException {
    Section s = new Section();
    for (Range r : all.getRanges()) {
      int first = random(r.first(), r.last() / 2);
      int last = random(r.last() / 2, r.last());
      s.appendRange(first, last, stride);
    }
    return s;
  }

  Random r = new Random(System.currentTimeMillis());

  int random(int first, int last) {
    return first + r.nextInt(last - first + 1);
  }


}