/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grib;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test misc GRIB features
 *
 * @author caron
 * @since 11/1/11
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribMisc {

  @Test
  public void pdsScaleOverflow() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/pdsScale.grib2";
    System.out.printf("%s%n", filename);
    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      Variable v = ncfile.findVariable("isobaric");
      float val = v.readScalarFloat();
      assert Misc.closeEnough(val, 92500.0) : val;
    }
  }

  @Test
  public void pdsGenType() throws Exception {
    // this one has a analysis and forecast in same variable
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/08Aug08.12z.cras45_NA.grib2";
    System.out.printf("%s%n", filename);
    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L1");
      assert v != null : ncfile.getLocation();
    }

    // this one has a forecast and error = must be seperate variables
    filename = TestDir.cdmUnitTestDir + "formats/grib2/RTMA_CONUS_2p5km_20111225_0000.grib2";
    System.out.printf("%s%n", filename);
    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      assert ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-3-0_L1") != null; // Pressure_Surface
      assert ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-0-0_error_L103") != null; // Temperature_error_height_above_ground
    }
  }

  @Test
  public void thinGrid() throws Exception {
    // this one has a analysis and forecast in same variable
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/thinGrid.grib2";
    System.out.printf("%s%n", filename);
    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L105");
      assert v != null : ncfile.getLocation();

      Array data = v.read();
      int[] shape = data.getShape();
      assert shape.length == 4;
      assert shape[shape.length - 2] == 1024;
      assert shape[shape.length - 1] == 2048;
    }
  }

  @Test
  public void testJPEG2K() throws Exception {
    // Tests specifically if the land-sea mask from GFS is decoded properly.
    // Not only does this test generally if the decoding works, but covers
    // a corner case with a single-bit field; this case was broken in the
    // original jj2000 code
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    System.out.printf("testJPEG2K %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_2-0-0_L1"); // Land_cover_0__sea_1__land_surface
      int[] origin = {0, 38, 281};
      int[] shape = {1, 1, 2};
      Array vals = v.read(origin, shape);
      assert Misc.closeEnough(vals.getFloat(0), 0.0) : vals.getFloat(0);
      assert Misc.closeEnough(vals.getFloat(1), 1.0) : vals.getFloat(1);
    }
  }

  @Test
  public void testNBits0() throws IOException {
    // Tests of GRIB2 nbits=0; should be reference value (0.0), not missing value
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/SingleRecordNbits0.grib2";
    System.out.printf("testNBits0 %s%n", filename);

    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-1-194_L1");
      assert v != null : ncfile.getLocation();
      Array vals = v.read();
      while (vals.hasNext()) {
        assert 0.0 == vals.nextDouble();
      }
    }
  }

  @Ignore("NCEP may be miscoding. Withdraw unit test until we have more info")
  @Test
   public void testScanMode() throws IOException, InvalidRangeException {
     // Robert.C.Lipschutz@noaa.gov
     // we are setting the value of scanMode to 64, which per GRIB2 Table 3.4 indicates "points scan in the +j direction", and so filling
     // the data arrays from south to north.
    /*
    Hi Bob:

    You might think that if scanmode = 64, one should just invert the grids. As it turns out, on all projections except for latlon (that i have sample of),
    the right thing to do is to ignore the flipping, because the coordinate system (the assignment of lat,lon values to each grid point) correctly adjusts
    for it. So its just on latlon grids that this issue arises.

    So on your file:

     C:/Users/caron/Downloads/grid174_scanmode_64_example.grb2

      latlon scan mode=64 dLat=0.125000 lat=(89.938004,-89.938004)

    Now, the only other example of a latlon Grid that I seem to have with scan mode 64 is

     Q:/cdmUnitTest/tds/ncep/SREF_PacificNE_0p4_ensprod_20120213_2100.grib2

        latlon scan 64 lat=(10.000000 , 50.000000)

    its over the pacific and much harder to tell if its flipped, but im guessing not. Note that its lat range is consistent with scan mode 64.

    Im loath to generalize from a sample size of 2. Do you have a sample of GRIB2 files with various encodings? Perhaps I could test them to see if we
    can guess when to flip or not.

    thanks,
    John
     */
     String filename = TestDir.cdmUnitTestDir + "formats/grib2/grid174_scanmode_64_example.grb2";

     try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
       Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L1");
       assert v != null : ncfile.getLocation();
       ArrayFloat vals = (ArrayFloat) (v.read("0,:,0").reduce());   // read first column - its flipped
       System.out.printf("%s: first=%f last=%f%n", v.getFullName(), vals.getFloat(0), vals.getFloat((int)vals.getSize()-1));
       assert Misc.closeEnough( vals.getFloat(0), 243.289993);
       assert Misc.closeEnough( vals.getFloat((int)vals.getSize()-1), 242.080002);
     }
   }

  // Tests reading a bad ecmwf encoded grib 1 file.
  // gaussian thin grid to boot.
  @Test
  public void testReadBadEcmwf() throws IOException {
    Grib1RecordScanner.setAllowBadDsLength(true);
    Grib1RecordScanner.setAllowBadIsLength(true);

    String filename = TestDir.cdmUnitTestDir + "formats/grib1/problem/badEcmwf.grib1";
    try (NetcdfFile nc = NetcdfFile.open(filename)) {

      Variable var = nc.findVariable("2_metre_temperature_surface");
      Array data = var.read();
      int npts = 2560 * 5136;
      Assert.assertEquals(npts, data.getSize());

      float first = data.getFloat(0);
      float last = data.getFloat(npts-1);

      Assert.assertEquals(273.260162, first, 1e-6);
      Assert.assertEquals(224.599670, last, 1e-6);
    }

    Grib1RecordScanner.setAllowBadDsLength(false);
    Grib1RecordScanner.setAllowBadIsLength(false);
  }



}
