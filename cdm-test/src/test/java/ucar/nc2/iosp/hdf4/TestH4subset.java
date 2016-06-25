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
import ucar.ma2.InvalidRangeException;
import ucar.nc2.iosp.hdf5.TestH5;
import ucar.nc2.util.TestSubsettingUtils;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caron
 * @since Jan 1, 2008
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestH4subset {
  static private String dirName = TestH5.testDir;
  static private int nt = 3;
  
  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // H5tiledLayoutBB
    result.add(new Object[]{dirName + "HIRDLS/HIRDLS2-Aura12h_b033_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", nt});

    // LayoutRegular
    result.add(new Object[]{TestH4readAndCount.testDir + "MI1B2T_B54_O003734_AN_05.hdf", "Infrared_Radiance_RDQI", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "ncidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", "High_Res_B_Swath/Data_Fields/Cold_Sky_Mirror_Count_89B", nt});

    // LayoutSegmented (linked)
    result.add(new Object[]{TestH4readAndCount.testDir + "96108_08.hdf", "BlackBody1Temperature", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf", "Total_Attenuated_Backscatter_532", nt});
    //testVariableSubset(TestH4read.testDir + "ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf", "AMSR-E Level 2B Land Data/Data Vgroup/Land Parameters"});

    // PositioningDataInputStream (not linked, compressed)
    result.add(new Object[]{TestH4readAndCount.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "DC_Restore_Change_for_Reflective_1km_Bands", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data_Fields/Cloud_Top_Pressure", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", "mod06/Data_Fields/Quality_Assurance_1km", nt});

    // PositioningDataInputStream (linked, compressed)
    result.add(new Object[]{TestH4readAndCount.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data_Fields/Surface_Pressure_Day", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "eos/mopitt/MOP03M-200501-L3V81.0.1.hdf", "MOP03/Data_Fields/Averaging_Kernel_Night", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf", "MOD_Grid_Snow_500m/Data_Fields/Fractional_Snow_Cover", nt}); // */

     // LayoutBBTiled (chunked and compressed)
    result.add(new Object[]{TestH4readAndCount.testDir + "eos/misr/MISR_AM1_GP_GMP_P040_O003734_05.eos", "GeometricParameters/Data_Fields/CaZenith", nt});
    result.add(new Object[]{TestH4readAndCount.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf", "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_RefSB_Uncert_Indexes", nt});

    return result;
 }
  
  String filename;
  String varName;
  int ntrials;

  public TestH4subset(String filename, String varName, int ntrials) {
    this.filename = filename;
    this.varName = varName;
    this.ntrials = ntrials;
  }

  @Test
  public void test() throws IOException, InvalidRangeException {
    TestSubsettingUtils.subsetVariables(filename, varName, ntrials);
  }  
  
}
