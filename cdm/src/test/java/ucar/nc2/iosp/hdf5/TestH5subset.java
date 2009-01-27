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
package ucar.nc2.iosp.hdf5;

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestH5subset extends TestCase {

  public TestH5subset(String name) {
    super(name);
  }

  private String dirName = TestH5.testDir; // "C:/data/hdf5/";

  public void testSubsetting() throws IOException, InvalidRangeException {
    int ntrials = 37;

    // H5tiledLayoutBB
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIRDLS2-Aura12h_b033_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation Fields/CloudContamination", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/GRIDS/O3Descending/Data Fields/L3dmValue", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/SWATHS/O3AscendingResiduals/Data Fields/L2gpValue", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/aura/OMI-Aura_L3-OMTO3e_2005m1214_v002-2006m0929t143855.he5", "HDFEOS/GRIDS/OMI Column Amount O3/Data Fields/ColumnAmountO3", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation Fields/CloudContamination", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation Fields/CloudContamination", ntrials);
    TestIosp.testVariableSubset(dirName + "ssec/I3A_CCD_13FEB2007_0501_L1B_STD.h5", "CCD/Image Data/CCD_VIS", ntrials);
    TestIosp.testVariableSubset(dirName + "ssec/K01_VHR_28AUG2007_0000_L02_IND.h5", "VHRR/Image Data/VHRR_WV", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data Fields/Temperature", ntrials); // */

    // H5tiledLayout
    TestIosp.testVariableSubset(dirName + "eos/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO/Data Fields/L2gpValue", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO column/Geolocation Fields/Latitude", ntrials);
    TestIosp.testVariableSubset(dirName + "IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5","U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA", ntrials);

    // LayoutRegular
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIR2ARSP_c3_na.he5", "HDFEOS/SWATHS/H2SO4_H2O_Tisdale/Data Fields/EXTC", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data Fields/AERO01", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Data Fields/7%2E10MicronAerosolExtinction", ntrials);
    TestIosp.testVariableSubset(dirName + "eos/aura/TES-Aura_L3-CH4-M2007m08_F01_04.he5", "HDFEOS/GRIDS/NadirGrid/Data Fields/CH4", ntrials); // */

 }

  public void problemSubset() throws IOException, InvalidRangeException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/dataBtree H5header/dataChunk"));
    TestIosp.testVariableSubset(dirName + "IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5",
            "U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA", 100);
  }

}