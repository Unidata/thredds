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

package ucar.nc2.grib;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.grib.grib2.Grib2Iosp;
import ucar.nc2.util.Misc;
import ucar.unidata.test.util.TestDir;

/**
 * Test misc GRIB features
 *
 * @author caron
 * @since 11/1/11
 */
public class TestGribMisc {

  @Test
  public void pdsScaleOverflow() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/pdsScale.grib2";
    System.out.printf("%s%n", filename);
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    Variable v = ncfile.findVariable("isobaric");
    float val = v.readScalarFloat();
    assert Misc.closeEnough(val, 92500.0) : val;
    ncfile.close();
  }

  @Test
  public void pdsGenType() throws Exception {
    // this one has a analysis and forecast in same variable
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/08Aug08.12z.cras45_NA.grib2";
    System.out.printf("%s%n", filename);
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L1");
    assert v != null : ncfile.getLocation();
    ncfile.close();

    // this one has a forecast and error = must be seperate variables
    filename = TestDir.cdmUnitTestDir + "formats/grib2/RTMA_CONUS_2p5km_20111225_0000.grib2";
    ncfile = NetcdfFile.open(filename, null);
    assert ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-3-0_L1") != null; // "Pressure_Surface") != null : ncfile.getLocation();
    assert ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-3-0_error_L1") != null; // "Pressure_error_Surface") != null;
    ncfile.close();
  }

  @Test
  public void thinGrid() throws Exception {
    // this one has a analysis and forecast in same variable
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/thinGrid.grib2";
    System.out.printf("%s%n", filename);
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    Variable v = ncfile.findVariableByAttribute(null, GribIosp.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L105");
    assert v != null : ncfile.getLocation();

    Array data = v.read();
    int[] shape = data.getShape();
    assert shape.length == 4;
    assert shape[2] == 1024;
    assert shape[3] == 2048;
    ncfile.close();
  }



}
