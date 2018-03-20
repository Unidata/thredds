/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.iosp.hdf5.TestH5;
import ucar.nc2.util.TestSubsettingUtils;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caron
 * @since Jan 1, 2008
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestH4subset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
