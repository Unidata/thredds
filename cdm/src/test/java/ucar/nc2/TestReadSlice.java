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

import java.io.*;
import java.util.ArrayList;

/** Test reading variable data */

public class TestReadSlice extends TestCase {

  public TestReadSlice( String name) {
    super(name);
  }

  public void testReadSlice1() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    int[] shape = temp.getShape();

    Variable tempSlice = temp.slice(0, 12);

    // read array section
    Array Asection;
    try {
      Asection = tempSlice.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 1;
    assert shape[1] == Asection.getShape()[0];

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
    Array Asection2 = A.slice( 0, 12);
    assert (Asection2.getRank() == 1);

    TestMA2.testEquals(Asection, Asection2 );

    ncfile.close();
    System.out.println( "*** testReadSlice1 done");
  }

  public void testReadSlice2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    int[] shape = temp.getShape();

    Variable tempSlice = temp.slice(1, 55);

    // read array section
    Array Asection;
    try {
      Asection = tempSlice.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 1;
    assert shape[0] == Asection.getShape()[0];

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
    Array Asection2 = A.slice( 1, 55);
    assert (Asection2.getRank() == 1);

    TestMA2.testEquals(Asection, Asection2 );

    ncfile.close();
    System.out.println( "*** testReadSlice2 done");
  }

  public void testReadSliceCompose() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    int[] shape = temp.getShape();

    Variable tempSlice = temp.slice(1, 55);
    Variable slice2 = tempSlice.slice(0, 12);

    // read array section
    Array Asection;
    try {
      Asection = slice2.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 0;

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
    Array data = A.slice( 1, 55);
    data = data.slice( 0, 12);
    assert (data.getRank() == 0);

    TestMA2.testEquals(Asection, data );

    ncfile.close();
    System.out.println( "*** testReadSliceCompose done");
  }

}
