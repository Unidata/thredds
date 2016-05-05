// $Id: TestOpenInMemory.java 51 2006-07-12 17:13:13Z caron $
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

import junit.framework.TestCase;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayChar;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

public class TestOpenInMemory extends TestCase {

  public TestOpenInMemory( String name) {
    super(name);
  }

  private NetcdfFile openInMemory( String filename) throws IOException {
      String pathname = TestDir.cdmLocalTestDataDir +filename;
      System.out.println("**** OpenInMemory "+pathname);

      byte[] ba = IO.readFileToByteArray( pathname);
      NetcdfFile ncfile = NetcdfFile.openInMemory("OpenInMemory", ba);
      System.out.println(ncfile);
      return ncfile;
  }

  public void testRead() throws IOException {

    NetcdfFile ncfile = openInMemory("testWrite.nc");

    assert(null != ncfile.findDimension("lat"));
    assert(null != ncfile.findDimension("lon"));

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));

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

    int i,j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    assert shape[0] == 64;
    assert shape[1] == 128;

    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        double dval = A.getDouble(ima.set(i,j));
        assert(  dval == (double) (i*1000000+j*1000)) : dval;
      }
    }

    // read part of array
    int[] origin2 = new int[2];
    int[] shape2 = new int[2];
    shape2[0] = 1;
    shape2[1] = temp.getShape()[1];
    try {
      A = temp.read(origin2, shape2);
    } catch (InvalidRangeException e) {
      System.err.println("ERROR reading file " +e);
      assert(false);
      return;
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 2);

    for (j=0; j<shape2[1]; j++) {
      assert( A.getDouble(ima.set(0,j)) == (double) (j*1000));
    }

    // rank reduction
    Array Areduce = A.reduce();
    Index ima2 = Areduce.getIndex();
    assert (Areduce.getRank() == 1);

    for (j=0; j<shape2[1]; j++) {
      assert( Areduce.getDouble(ima2.set(j)) == (double) (j*1000));
    }

    // read char variable
    Variable c = null;
    assert(null != (c = ncfile.findVariable("svar")));
    try {
      A = c.read();
    } catch (IOException e) {
      assert(false);
    }
    assert(A instanceof ArrayChar);
    ArrayChar ac = (ArrayChar) A;
    String val = ac.getString(ac.getIndex());
    assert val.equals("Testing 1-2-3") : val;
    //System.out.println( "val = "+ val);

    // read char variable 2
    Variable c2 = null;
    assert(null != (c2 = ncfile.findVariable("svar2")));
    try {
      A = c2.read();
    } catch (IOException e) {
      assert(false);
    }
    assert(A instanceof ArrayChar);
    ArrayChar ac2 = (ArrayChar) A;
    assert(ac2.getString().equals("Two pairs of ladies stockings!"));

    // read String Array
    Variable c3 = null;
    assert(null != (c3 = ncfile.findVariable("names")));
    try {
      A = c3.read();
    } catch (IOException e) {
      assert(false);
    }
    assert(A instanceof ArrayChar);
    ArrayChar ac3 = (ArrayChar) A;
    ima = ac3.getIndex();

    assert(ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
    assert(ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
    assert(ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

    // read String Array - 2
    Variable c4 = null;
    assert(null != (c4 = ncfile.findVariable("names2")));
    try {
      A = c4.read();
    } catch (IOException e) {
      assert(false);
    }
    assert(A instanceof ArrayChar);
    ArrayChar ac4 = (ArrayChar) A;
    ima = ac4.getIndex();

    assert(ac4.getString(0).equals("0 pairs of ladies stockings!"));
    assert(ac4.getString(1).equals("1 pair of ladies stockings!"));
    assert(ac4.getString(2).equals("2 pairs of ladies stockings!"));

    //System.out.println( "ncfile = "+ ncfile);

    ncfile.close();
    System.out.println( "**************TestRead done");
  }

}
