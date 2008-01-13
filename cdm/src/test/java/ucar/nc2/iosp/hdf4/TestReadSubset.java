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

  public void testReadVariableSection() throws InvalidRangeException, IOException {
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
    testVariableSubset(TestH4read.testDir + "96108_08.hdf", "BlackBody1Temperature");
    testVariableSubset(TestH4read.testDir + "MI1B2T_B54_O003734_AN_05.hdf", "Infrared Radiance%2FRDQI");
    testVariableSubset(TestH4read.testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", "High_Res_B_Swath/Data Fields/Cold_Sky_Mirror_Count_89B");

    // PositioningDataInputStream
    testVariableSubset(TestH4read.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data Fields/Surface Pressure Day");
    testVariableSubset(TestH4read.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data Fields/Averaging Kernel Night");
    testVariableSubset(TestH4read.testDir + "ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf", "MOD_Grid_Snow_500m/Data Fields/Fractional_Snow_Cover");
    testVariableSubset(TestH4read.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data Fields/Cloud_Top_Pressure");
    testVariableSubset(TestH4read.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data Fields/Quality_Assurance_1km");

    // LayoutBBTiled
    testVariableSubset(TestH4read.testDir + "eos/misr/MISR_AM1_GP_GMP_P040_O003734_05", "GeometricParameters/Data Fields/CaZenith");
    testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes");

    // LayoutSegmented
    testVariableSubset(TestH4read.testDir + "ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf", "Total_Attenuated_Backscatter_532");
    testVariableSubset(TestH4read.testDir + "eos/misr/MISR_AM1_AGP_P040_F01_24.subset", "Standard/Data Fields/AveSceneElev");

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
    NetcdfFile ncfile = NetcdfFile.open(TestH4read.testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf");
    Variable v = ncfile.findVariable("High_Res_B_Swath/Data Fields/Cold_Sky_Mirror_Count_89B");
    Section s= new Section("624:1536:2,15:25:2,0:0:2");
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

/* ------Reading filename C:\data\hdf4\96108_08.hdf
  Try to read variable BlackBody1Temperature(NumberOfScanlines=810, NumberOfChannels=50) size= 40500
***RAF LayoutType=ucar.nc2.iosp.LayoutSegmented

------Reading filename C:\data\hdf4\MI1B2T_B54_O003734_AN_05.hdf
  Try to read variable Infrared Radiance/RDQI(fakeDim0=512, fakeDim1=2048) size= 1048576
***RAF LayoutType=ucar.nc2.iosp.LayoutRegular

------Reading filename C:\data\hdf4\eos\misr\MISR_AM1_AGP_P040_F01_24.subset
  Try to read variable Standard/Data Fields/AveSceneElev(SOMBlockDim=180, XDim=128, YDim=512) size= 11796480 section= 0:151:1,0:127:1,0:511:1
***RAF LayoutType=ucar.nc2.iosp.LayoutSegmented
***BB LayoutType=ucar.nc2.iosp.LayoutBBTiled

------Reading filename C:\data\hdf4\eos\misr\MISR_AM1_GP_GMP_P040_O003734_05
  Try to read variable GeometricParameters/Data Fields/CaZenith(SOMBlockDim=180, XDim=8, YDim=32) size= 46080
***RAF LayoutType=ucar.nc2.iosp.LayoutSegmented
***BB LayoutType=ucar.nc2.iosp.LayoutBBTiled

------Reading filename C:\data\hdf4\eos\mopitt\MOP03M-200501-L3V81.0.1.hdf
  Try to read variable MOP03/Data Fields/Surface Pressure Day(nlat=180, nlon=360) size= 64800
***PositioningDataInputStream LayoutType=ucar.nc2.iosp.LayoutRegular

  Try to read variable MOP03/Data Fields/Averaging Kernel Night(nlat=180, nlon=360, nprs=7, nprs=7) size= 3175200
***PositioningDataInputStream LayoutType=ucar.nc2.iosp.LayoutRegular

------Reading filename C:\data\hdf4\ncidc\AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf
  Try to read variable High_Res_B_Swath/Data Fields/Cold_Sky_Mirror_Count_89B(DataTrack_lo=2006, High_Cal_Counts=32, Level1A_High_Chan=2) size= 128384
***RAF LayoutType=ucar.nc2.iosp.LayoutRegular

------Reading filename C:\data\hdf4\ncidc\MOD02HKM.A2007016.0245.005.2007312120020.hdf
  Try to read variable MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes(Band_500M=5, 20*nscans=4060, 2*Max_EV_frames=2708) size= 54972400 section= 0:0:1,0:4059:1,0:2707:1
***RAF LayoutType=ucar.nc2.iosp.LayoutSegmented
***BB LayoutType=ucar.nc2.iosp.LayoutBBTiled

------Reading filename C:\data\hdf4\ncidc\MOD10A1.A2008001.h23v15.005.2008003161138.hdf
  Try to read variable MOD_Grid_Snow_500m/Data Fields/Fractional_Snow_Cover(YDim=2400, XDim=2400) size= 5760000
***PositioningDataInputStream LayoutType=ucar.nc2.iosp.LayoutRegular

------Reading filename C:\data\hdf4\ssec\CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf
  Try to read variable Total_Attenuated_Backscatter_532(63270, 583) size= 36886410 section= 0:17151:1,0:582:1
***RAF LayoutType=ucar.nc2.iosp.LayoutSegmented

------Reading filename C:\data\hdf4\ssec\MYD06_L2.A2006188.1655.005.2006194124315.hdf
  Try to read variable mod06/Data Fields/Cloud_Top_Pressure(Cell_Along_Swath_5km=406, Cell_Across_Swath_5km=270) size= 109620
***PositioningDataInputStream LayoutType=ucar.nc2.iosp.LayoutRegular

  Try to read variable mod06/Data Fields/Quality_Assurance_1km(Cell_Along_Swath_1km=2030, Cell_Across_Swath_1km=1354, QA_Parameter_1km=5) size= 13743100 section= 0:1476:1,0:1353:1,0:4:1
***PositioningDataInputStream LayoutType=ucar.nc2.iosp.LayoutRegular
*/
