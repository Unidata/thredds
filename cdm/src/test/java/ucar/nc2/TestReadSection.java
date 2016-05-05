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
import ucar.ma2.*;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.ArrayList;

/** Test reading variable data */

public class TestReadSection extends TestCase {

  public TestReadSection( String name) {
    super(name);
  }

  public void testReadVariableSection() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    Variable temp = ncfile.findVariable("temperature");
    assert(null != temp);

    int[] origin = {3,6};
    int[] shape = {12,17};

    Variable tempSection = temp.section(new Section(origin, shape));

    // read array section
    Array Asection;
    try {
      Asection = tempSection.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 2;
    assert shape[0] == Asection.getShape()[0];
    assert shape[1] == Asection.getShape()[1];

    // read entire array
    Array A;
    try {
      A = temp.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 2);

    // compare
    Array Asection2 = A.section( origin, shape, null);
    assert (Asection2.getRank() == 2);
    assert (shape[0] == Asection2.getShape()[0]);
    assert (shape[1] == Asection2.getShape()[1]);

    IndexIterator s1 = Asection.getIndexIterator();
    IndexIterator s2 = Asection2.getIndexIterator();
    int count = 0;
    while (s1.hasNext()) {
      double d1 = s1.getDoubleNext();
      double d2 = s2.getDoubleNext();
      assert Misc.closeEnough(d1, d2) : count+" "+d1 +" != "+d2;
      count++;
    }

    ncfile.close();
    System.out.println( "*** testReadVariableSection done");
  }


  public void testReadVariableSection2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));

    ArrayList<Range> ranges = new ArrayList<Range>();
    Range r0 = new Range(3,14);
    Range r1 = new Range(6,22);
    ranges.add( r0);
    ranges.add( r1);

    Variable tempSection = temp.section(ranges);
    assert tempSection.getRank() == 2;
    int[] vshape = tempSection.getShape();
    assert r0.length() == vshape[0];
    assert r1.length() == vshape[1];

    // read array section
    Array Asection;
    try {
      Asection = tempSection.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 2;
    assert r0.length() == Asection.getShape()[0];
    assert r1.length() == Asection.getShape()[1];

    // read entire array
    Array A;
    try {
      A = temp.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 2);

    // compare
    Array Asection2 = A.section( ranges);
    assert (Asection2.getRank() == 2);
    assert (r0.length() == Asection2.getShape()[0]);
    assert (r1.length() == Asection2.getShape()[1]);

    IndexIterator s1 = Asection.getIndexIterator();
    IndexIterator s2 = Asection2.getIndexIterator();
    int count = 0;
    while (s1.hasNext()) {
      double d1 = s1.getDoubleNext();
      double d2 = s2.getDoubleNext();
      assert Misc.closeEnough( d1, d2) : count+" "+d1 +" != "+d2;
      count++;
    }

    ncfile.close();
    System.out.println( "*** testReadVariableSection2 done");
  }

}
