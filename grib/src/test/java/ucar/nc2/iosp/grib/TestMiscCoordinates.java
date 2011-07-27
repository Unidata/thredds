package ucar.nc2.iosp.grib;

/**
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
/**
 * test grib files with hybrid vert coords
 * LOOK: vert coord transform not getting made!
 */

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;

import java.io.IOException;

import junit.framework.TestCase;


public class TestMiscCoordinates extends TestCase {
  public TestMiscCoordinates(String name) {
    super(name);
  }

  public void testHybridVertCOord() throws IOException {
    doOne(TestAll.cdmUnitTestDir + "formats/grib1/ECMWF.hybrid.grib1");
    doOne(TestAll.cdmUnitTestDir + "formats/grib1/07010418_arw_d01.GrbF01500");
  }

  void doOne(String filename) throws IOException {
    System.out.println("\n\nReading File " + filename);

    NetcdfFile ncfile = NetcdfFile.open(filename);

    if (filename.equals("ECMWF.hybrid.grib1")) {
      Variable Hybrid = ncfile.findVariable("hybrid");
      assert (Hybrid.getNameAndDimensions().equals("hybrid(hybrid=91)"));
      Variable Hybrida = ncfile.findVariable("hybrida");
      assert (Hybrida.getNameAndDimensions().equals("hybrida(hybrid=91)"));
      Variable Hybridb = ncfile.findVariable("hybridb");
      assert (Hybridb.getNameAndDimensions().equals("hybridb(hybrid=91)"));

      int idx = Hybrid.findDimensionIndex("hybrid");
      Dimension dim = Hybrid.getDimension(idx);

    } else if (filename.equals("07010418_arw_d01.GrbF01500")) {
      Variable Hybrid = ncfile.findVariable("hybrid");
      assert (Hybrid.getNameAndDimensions().equals("hybrid(hybrid=2)"));
    }
    ncfile.close();
  }

  public void testGaussianLats() throws IOException {
    String filename = TestAll.cdmUnitTestDir + "formats/grib1/CCCma_SRES_A2_HGT500_1-10.grb";
    System.out.println("\n\nReading File " + filename);

    NetcdfFile ncfile = NetcdfFile.open(filename);

    Variable lat = ncfile.findVariable("lat");
    assert lat.getSize() == 48;
    ncfile.close();
  }



  static public void main(String args[]) throws IOException {
    TestMiscCoordinates ggi = new TestMiscCoordinates("");
    ggi.testHybridVertCOord();
  }


}
