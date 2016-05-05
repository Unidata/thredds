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
package ucar.nc2.iosp.dorade;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestDorade {

  public static String groundDoradeFile = TestDir.cdmUnitTestDir + "formats/dorade/swp.1020511015815.SP0L.573.1.2_SUR_v1";
  public static String airDoradeFile = TestDir.cdmUnitTestDir + "formats/dorade/swp.1030524195200.TA-ELDR.291.-16.5_AIR_v-999";

  public static boolean show = false;

  @Test
  public void testDoradeGround() throws IOException {

    System.out.println("**** Open " + groundDoradeFile);
    try (NetcdfFile ncfile = NetcdfFile.open(groundDoradeFile)) {

      for (Variable v : ncfile.getVariables()) {
        System.out.println(v.getFullName());
      }

    /* test both gate and radial dimension */
      Dimension gateDim = ncfile.getRootGroup().findDimension("gate_1");

      assert (gateDim.getLength() == 1008);

      Dimension radialDim = ncfile.getRootGroup().findDimension("radial");

      assert (radialDim.getLength() == 439);

    /* test some att  */
      Attribute testAtt = ncfile.getRootGroup().findAttribute("Conventions");
      assert (testAtt.getStringValue().equals(_Coordinate.Convention));

      testAtt = ncfile.getRootGroup().findAttribute("format");
      assert (testAtt.getStringValue().equals("Unidata/netCDF/Dorade"));

      testAtt = ncfile.getRootGroup().findAttribute("Project_name");
      assert (testAtt.getStringValue().equalsIgnoreCase("IHOP_2002"));

      testAtt = ncfile.getRootGroup().findAttribute("Radar_Name");
      assert (testAtt.getStringValue().equalsIgnoreCase("SPOL"));

      testAtt = ncfile.getRootGroup().findAttribute("VolumeCoveragePatternName");
      assert (testAtt.getStringValue().equalsIgnoreCase("SUR"));

      testAtt = ncfile.getRootGroup().findAttribute("Volume_Number");
      assert (testAtt.getStringValue().equalsIgnoreCase("1"));

      testAtt = ncfile.getRootGroup().findAttribute("Sweep_Number");
      assert (testAtt.getStringValue().equalsIgnoreCase("2"));

      testAtt = ncfile.getRootGroup().findAttribute("Sweep_Date");
      assert (testAtt.getStringValue().equalsIgnoreCase("2002-05-11 01:58:15Z"));

      Variable var;
      var = ncfile.findVariable("elevation");
      testReadData(var);
      var = ncfile.findVariable("azimuth");
      testReadData(var);
      var = ncfile.findVariable("distance_1");
      testReadData(var);
      var = ncfile.findVariable("latitudes_1");
      testReadData(var);
      var = ncfile.findVariable("longitudes_1");
      testReadData(var);
      var = ncfile.findVariable("altitudes_1");
      testReadData(var);
      var = ncfile.findVariable("rays_time");
      testReadData(var);
      var = ncfile.findVariable("Range_to_First_Cell");
      float t = testReadScalar(var);
      assert (t == (float) 150.0);

      var = ncfile.findVariable("Cell_Spacing");
      t = testReadScalar(var);
      assert (t == (float) 149.89624);
      var = ncfile.findVariable("Fixed_Angle");
      t = testReadScalar(var);
      assert (t == (float) 1.1975098);
      var = ncfile.findVariable("Nyquist_Velocity");
      t = testReadScalar(var);
      assert (t == (float) 25.618269);
      var = ncfile.findVariable("Unambiguous_Range");
      t = testReadScalar(var);
      assert (t == (float) 156.11694);
      var = ncfile.findVariable("Radar_Constant");
      t = testReadScalar(var);
      assert (t == (float) 70.325195);
      var = ncfile.findVariable("rcvr_gain");
      t = testReadScalar(var);
      assert (t == (float) 46.95);
      var = ncfile.findVariable("ant_gain");
      t = testReadScalar(var);
      assert (t == (float) 45.58);
      var = ncfile.findVariable("sys_gain");
      t = testReadScalar(var);
      assert (t == (float) 46.95);
      var = ncfile.findVariable("bm_width");
      t = testReadScalar(var);
      assert (t == (float) 0.92);

      var = ncfile.findVariable("VE");
      testReadData(var);
      var = ncfile.findVariable("DM");
      testReadData(var);
      var = ncfile.findVariable("NCP");
      testReadData(var);
      var = ncfile.findVariable("SW");
      testReadData(var);
      var = ncfile.findVariable("DZ");
      testReadData(var);
      var = ncfile.findVariable("DCZ");
      testReadData(var);
      var = ncfile.findVariable("LVDR");
      testReadData(var);
      var = ncfile.findVariable("NIQ");
      testReadData(var);
      var = ncfile.findVariable("AIQ");
      testReadData(var);
      var = ncfile.findVariable("CH");
      testReadData(var);
      var = ncfile.findVariable("AH");
      testReadData(var);
      var = ncfile.findVariable("CV");
      testReadData(var);
      var = ncfile.findVariable("AV");
      testReadData(var);
      var = ncfile.findVariable("RHOHV");
      testReadData(var);
      var = ncfile.findVariable("LDR");
      testReadData(var);
      var = ncfile.findVariable("DL");
      testReadData(var);
      var = ncfile.findVariable("DX");
      testReadData(var);
      var = ncfile.findVariable("ZDR");
      testReadData(var);
      var = ncfile.findVariable("PHI");
      testReadData(var);
      var = ncfile.findVariable("KDP");
      testReadData(var);

      assert (0 == var.findDimensionIndex("radial"));
      assert (1 == var.findDimensionIndex("gate_1"));
    }
  }

  @Test
  public void testDoradeAir() throws IOException {
    System.out.println("**** Open " + airDoradeFile);
    try (NetcdfFile ncfile = NetcdfFile.open(airDoradeFile)) {

      for (Variable v : ncfile.getVariables()) {
        System.out.println(v.getFullName());
      }

      Dimension gateDim = ncfile.getRootGroup().findDimension("gate_1");
      assert (gateDim.getLength() == 320);
      Dimension radialDim = ncfile.getRootGroup().findDimension("radial");
      assert (radialDim.getLength() == 274);

      Variable var;
      var = ncfile.findVariable("elevation");
      testReadData(var);
      var = ncfile.findVariable("azimuth");
      testReadData(var);
      var = ncfile.findVariable("distance_1");
      testReadData(var);
      var = ncfile.findVariable("latitudes_1");
      testReadData(var);
      var = ncfile.findVariable("longitudes_1");
      testReadData(var);
      var = ncfile.findVariable("altitudes_1");
      testReadData(var);
      var = ncfile.findVariable("rays_time");
      testReadData(var);

      var = ncfile.findVariable("Range_to_First_Cell");
      float t = testReadScalar(var);
      assert (t == (float) -2.0);
      var = ncfile.findVariable("Cell_Spacing");
      t = testReadScalar(var);
      assert (t == (float) 150.0);
      var = ncfile.findVariable("Fixed_Angle");
      t = testReadScalar(var);
      assert (t == (float) -16.53);
      var = ncfile.findVariable("Nyquist_Velocity");
      t = testReadScalar(var);
      assert (t == (float) 78.03032);
      var = ncfile.findVariable("Unambiguous_Range");
      t = testReadScalar(var);
      assert (t == (float) 60.0);
      var = ncfile.findVariable("Radar_Constant");
      t = testReadScalar(var);
      assert (t == (float) -81.17389);
      var = ncfile.findVariable("rcvr_gain");
      t = testReadScalar(var);
      assert (t == (float) 32.64);
      var = ncfile.findVariable("ant_gain");
      t = testReadScalar(var);
      assert (t == (float) 39.35);
      var = ncfile.findVariable("sys_gain");
      t = testReadScalar(var);
      assert (t == (float) 52.34);
      var = ncfile.findVariable("bm_width");
      t = testReadScalar(var);
      assert (t == (float) 1.79);

      var = ncfile.findVariable("VS");
      testReadData(var);
      var = ncfile.findVariable("VL");
      testReadData(var);
      var = ncfile.findVariable("SW");
      testReadData(var);
      var = ncfile.findVariable("VR");
      testReadData(var);
      var = ncfile.findVariable("NCP");
      testReadData(var);
      var = ncfile.findVariable("DBZ");
      testReadData(var);
      var = ncfile.findVariable("VG");
      testReadData(var);
      var = ncfile.findVariable("VT");
      testReadData(var);
      assert (null != var.getDimension(0));
      assert (null != var.getDimension(1));
    }

  }

  private void testReadData(Variable v) throws IOException {
    if (show) System.out.printf(" read %s%n", v.getNameAndDimensions());
    assert (null != v);

    assert (null != v.getDimension(0));
    Array a = v.read();
    assert (null != a);
    assert (v.getSize() == a.getSize());
  }

  private float testReadScalar(Variable v) throws IOException {
    if (show) System.out.printf(" read %s%n", v.getNameAndDimensions());
    assert (null != v);
    Array a = v.read();
    assert (null != a);
    IndexIterator ii = a.getIndexIterator();
    return ii.getFloatNext();
  }

}
