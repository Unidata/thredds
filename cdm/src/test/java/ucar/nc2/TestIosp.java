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
package ucar.nc2;

import junit.framework.*;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.Range;
import ucar.nc2.iosp.grib.TestIntervalsTimeCoords;
import ucar.nc2.util.CompareNetcdf;

import java.io.IOException;
import java.util.Random;

/**
 * TestSuite that runs IOSP tests
 *
 */
public class TestIosp {

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest( new TestSuite(ucar.nc2.iosp.grib.TestHybridData.class));
    suite.addTest( new TestSuite(ucar.nc2.iosp.grib.TestOfsData.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.grib.TestIndexUpdating.class));

    //suite.addTest( new TestSuite( ucar.nc2.iosp.dmsp.TestDmspIosp.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.gini.TestGini.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.nexrad2.TestNexrad2.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.nexrad2.TestNexrad2HiResolution.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.nids.TestNids.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.dorade.TestDorade.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.grib.TestGridGribIosp.class));
    suite.addTest( new TestSuite( TestIntervalsTimeCoords.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.gempak.TestReadingGempak.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.TestMiscIosp.class));
    //suite.addTest( new TestSuite( ucar.nc2.iosp.bufr.TestBufrRead.class));

    // slow !!
    suite.addTest( ucar.nc2.iosp.hdf5.TestH5.suite());
    suite.addTest( ucar.nc2.iosp.hdf4.TestH4.suite());

    // slow - comment out if needed
    //suite.addTest(new TestSuite(ucar.nc2.iosp.hdf4.TestH4subset.class)); //
    //suite.addTest(new TestSuite(ucar.nc2.iosp.hdf5.TestH5subset.class)); //
    return suite;
  }

  public static void testVariableSubset(String filename, String varName, int ntrials) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);

    Variable v = ncfile.findVariable(varName);
    assert (null != v);
    int[] shape = v.getShape();

    // read entire array
    Array A;
    try {
      A = v.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }

    int[] dataShape = A.getShape();
    assert dataShape.length == shape.length;
    for (int i = 0; i < shape.length; i++)
      assert dataShape[i] == shape[i];
    Section all = v.getShapeAsSection();
    System.out.println("  Entire dataset="+all);

    for (int k = 0; k < ntrials; k++) {
      // create a random subset, read and compare
      testOne(v, randomSubset(all, 1), A);
      testOne(v, randomSubset(all, 2), A);
      testOne(v, randomSubset(all, 3), A);
    }

    ncfile.close();
  }

   public static void testVariableSubset(String filename, String varName, Section s) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);
    Variable v = ncfile.findVariable(varName);
    assert (null != v);
    testOne(v, s, v.read());
  }

  public static void testOne(Variable v, Section s, Array fullData) throws IOException, InvalidRangeException {
      System.out.println("   section="+s);

      // read just that
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

      CompareNetcdf.compareData(sdata, Asection);
  }

  private static Section randomSubset(Section all, int stride) throws InvalidRangeException {
    Section s = new Section();
    for (Range r : all.getRanges()) {
      int first = random(r.first(), r.last() / 2);
      int last = random(r.last() / 2, r.last());
      s.appendRange(first, last, stride);
    }
    return s;
  }

  private static Random r = new Random(System.currentTimeMillis());
  private static int random(int first, int last) {
    return first + r.nextInt(last - first + 1);
  }


}