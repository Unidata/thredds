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
import ucar.nc2.util.TestSubsettingUtils;
import ucar.unidata.test.util.TestDir;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestH5subset extends TestCase {

  public TestH5subset(String name) {
    super(name);
  }

  private String dirName = TestH5.testDir;

 public void testSubsetting() throws IOException, InvalidRangeException {
    int ntrials = 3;

    // H5tiledLayoutBB
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRDLS2-Aura12h_b033_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/GRIDS/O3Descending/Data_Fields/L3dmValue", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", "HDFEOS/SWATHS/O3AscendingResiduals/Data_Fields/L2gpValue", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "aura/OMI-Aura_L3-OMTO3e_2005m1214_v002-2006m0929t143855.he5", "HDFEOS/GRIDS/OMI_Column_Amount_O3/Data_Fields/ColumnAmountO3", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Geolocation_Fields/CloudContamination", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "ssec-h5/I3A_CCD_13FEB2007_0501_L1B_STD.h5", "CCD/Image_Data/CCD_VIS", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "ssec-h5/K01_VHR_28AUG2007_0000_L02_IND.h5", "VHRR/Image_Data/VHRR_WV", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/Temperature", ntrials); // */

    // H5tiledLayout
    TestSubsettingUtils.subsetVariables(dirName + "aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO/Data_Fields/L2gpValue", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", "HDFEOS/SWATHS/BrO_column/Geolocation_Fields/Latitude", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5","U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA", ntrials);

    // LayoutRegular
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIR2ARSP_c3_na.he5", "HDFEOS/SWATHS/H2SO4_H2O_Tisdale/Data_Fields/EXTC", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRPROF-AFGL_b038_na.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/AERO01", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/7\\.10MicronAerosolExtinction", ntrials);
    TestSubsettingUtils.subsetVariables(dirName + "aura/TES-Aura_L3-CH4-M2007m08_F01_04.he5", "HDFEOS/GRIDS/NadirGrid/Data_Fields/CH4", ntrials); // */

    // netcdf4
    TestSubsettingUtils.subsetVariables(TestDir.cdmUnitTestDir + "formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4", "water_temp", ntrials); // */
 }

  public void testProblem() throws IOException, InvalidRangeException {
    TestSubsettingUtils.subsetVariables(dirName + "HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5", "HDFEOS/SWATHS/HIRDLS/Data_Fields/7\\.10MicronAerosolExtinction", 1);
  }

  private void testVariableSubset(String filename, String varName, Section s) throws InvalidRangeException, IOException {
    System.out.printf("%ntestVariableSubset=%s,%s%n",filename,varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);

    Variable v = ncfile.findVariable(varName);
    assert (null != v);
    int[] shape = v.getShape();

    // read entire array
    Array fullData;
    try {
      fullData = v.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }

    int[] dataShape = fullData.getShape();
    assert dataShape.length == shape.length;
    for (int i = 0; i < shape.length; i++)
      assert dataShape[i] == shape[i];
    System.out.println("  Entire dataset   ="+v.getShapeAsSection());
    System.out.println("  Test read section="+s);

    // read section
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

    ucar.unidata.test.util.CompareNetcdf.compareData(sdata, Asection);

    ncfile.close();
  }


}