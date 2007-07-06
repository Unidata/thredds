// $Id: TestOpenInMemory.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import junit.framework.TestCase;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayChar;

import java.io.IOException;

import thredds.util.IO;

public class TestOpenInMemory extends TestCase {

  public TestOpenInMemory( String name) {
    super(name);
  }

  private NetcdfFile openInMemory( String filename) throws IOException {
      String pathname = TestLocal.cdmTestDataDir +filename;
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