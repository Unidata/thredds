/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.util.TestSubsettingUtils;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestH5subset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static private String dirName = TestH5.testDir;
  static private int nt = 3;
  
  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // H5tiledLayoutBB
    result.add(new Object[]{dirName + "HIRDLS/HIRDLS2-Aura12h_b033_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", nt});
    result.add(new Object[]{dirName + "aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/GRIDS/O3Descending/Data_Fields/L3dmValue", nt});
    result.add(new Object[]{dirName + "aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/SWATHS/O3AscendingResiduals/Data_Fields/L2gpValue", nt});
    result.add(new Object[]{dirName + "aura/OMI-Aura_L3-OMTO3e_2005m1214_v002-2006m0929t143855.he5", "HDFEOS/GRIDS/OMI_Column_Amount_O3/Data_Fields/ColumnAmountO3", nt});
    result.add(new Object[]{dirName + "HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", nt});
    result.add(new Object[]{dirName + "HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", nt});
    result.add(new Object[]{dirName + "ssec-h5/I3A_CCD_13FEB2007_0501_L1B_STD.h5", "CCD/Image_Data/CCD_VIS", nt});
    result.add(new Object[]{dirName + "ssec-h5/K01_VHR_28AUG2007_0000_L02_IND.h5", "VHRR/Image_Data/VHRR_WV", nt});
    result.add(new Object[]{dirName + "HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/Temperature", nt}); // */

    // H5tiledLayout
    result.add(new Object[]{dirName + "aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO/Data_Fields/L2gpValue", nt});
    result.add(new Object[]{dirName + "aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO_column/Geolocation_Fields/Latitude", nt});
    result.add(new Object[]{dirName + "IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5","U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA", nt});

    // LayoutRegular
    result.add(new Object[]{dirName + "HIRDLS/HIR2ARSP_c3_na.he5", "HDFEOS/SWATHS/H2SO4_H2O_Tisdale/Data_Fields/EXTC", nt});
    result.add(new Object[]{dirName + "HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/AERO01", nt});
    result.add(new Object[]{dirName + "HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/7\\.10MicronAerosolExtinction", nt});
    result.add(new Object[]{dirName + "aura/TES-Aura_L3-CH4-M2007m08_F01_04.he5", "HDFEOS/GRIDS/NadirGrid/Data_Fields/CH4", nt}); // */

    // netcdf4
    result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4", "water_temp", nt}); // */

    return result;
 }

  String filename;
  String varName;
  int ntrials;

  public TestH5subset(String filename, String varName, int ntrials) {
    this.filename = filename;
    this.varName = varName;
    this.ntrials = ntrials;
  }

  @Test
  public void test() throws IOException, InvalidRangeException {
    TestSubsettingUtils.subsetVariables(filename, varName, ntrials);
  }


}
