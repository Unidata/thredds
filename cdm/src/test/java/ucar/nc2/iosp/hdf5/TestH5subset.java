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
package ucar.nc2.iosp.hdf5;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.hdf4.TestH4read;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;

import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestH5subset extends TestCase {

  public TestH5subset(String name) {
    super(name);
  }

  private String dirName = "C:/data/hdf5/";
  public void testReadAll() throws IOException {
    TestAll.readAllDir(dirName, new FileFilter() {
      public boolean accept(File file) {
        String name = file.getPath();
        return (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"));
      }
    });

  }

  public void testSubsetting() throws IOException, InvalidRangeException {
    int ntrials = 100;

    // H5chunkFilterLayout
    TestIosp.testVariableSubset(dirName + "HIRDLS2-Aura12h_b033_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation Fields/CloudContamination", ntrials);
    TestIosp.testVariableSubset(dirName + "aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/GRIDS/O3Descending/Data Fields/L3dmValue", ntrials);
    TestIosp.testVariableSubset(dirName + "aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/SWATHS/O3AscendingResiduals/Data Fields/L2gpValue", ntrials);
    TestIosp.testVariableSubset(dirName + "aura/OMI-Aura_L3-OMTO3e_2005m1214_v002-2006m0929t143855.he5", "HDFEOS/GRIDS/OMI Column Amount O3/Data Fields/ColumnAmountO3", ntrials);
    TestIosp.testVariableSubset(dirName + "HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation Fields/CloudContamination", ntrials);
    TestIosp.testVariableSubset(dirName + "HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation Fields/CloudContamination", ntrials);
    TestIosp.testVariableSubset(dirName + "ssec-h5/I3A_CCD_13FEB2007_0501_L1B_STD.h5", "CCD/Image Data/CCD_VIS", ntrials);
    TestIosp.testVariableSubset(dirName + "ssec-h5/K01_VHR_28AUG2007_0000_L02_IND.h5", "VHRR/Image Data/VHRR_WV", ntrials);
    TestIosp.testVariableSubset(dirName + "HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data Fields/Temperature", ntrials); // */

    // H5chunkLayout
    TestIosp.testVariableSubset(dirName + "aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO/Data Fields/L2gpValue", ntrials);
    TestIosp.testVariableSubset(dirName + "aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO column/Geolocation Fields/Latitude", ntrials);

    // RegularSectionLayout
    TestIosp.testVariableSubset(dirName + "HIRDLS/HIR2ARSP_c3_na.he5", "HDFEOS/SWATHS/H2SO4_H2O_Tisdale/Data Fields/EXTC", ntrials);
    TestIosp.testVariableSubset(dirName + "HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data Fields/AERO01", ntrials);
    TestIosp.testVariableSubset(dirName + "HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Data Fields/7%2E10MicronAerosolExtinction", ntrials);
    TestIosp.testVariableSubset(dirName + "oon/TES-Aura_L3-CH4-M2007m08_F01_04.he5", "HDFEOS/GRIDS/NadirGrid/Data Fields/CH4", ntrials); // */

 }

  public void problemSubset() throws IOException, InvalidRangeException {
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag1 H4header/tagDetail H4header/chunked"));
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/chunkTable"));
    TestIosp.testVariableSubset(TestH4read.testDir + "ncidc/MOD02HKM.A2007016.0245.005.2007312120020.hdf","MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes",
        new Section("0:3,1049:3957,464:1452"));
  }

}