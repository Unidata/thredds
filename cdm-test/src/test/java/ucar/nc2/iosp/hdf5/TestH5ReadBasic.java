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

import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadBasic {

  @org.junit.Test
  /* attribute array of String */
  public void testReadH5attributeArrayString() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/astrarr.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("dset")));
      assert (dset.getDataType() == DataType.INT);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 4);
      d = dset.getDimension(1);
      assert (d.getLength() == 6);

      Attribute att = dset.findAttribute("string-att");
      assert (null != att);
      assert (att.isArray());
      assert (att.getLength() == 4);
      assert (att.isString());
      assert (att.getStringValue().equals("test "));
      assert (att.getStringValue(0).equals("test "));
      assert (att.getStringValue(1).equals("left "));
      assert (att.getStringValue(2).equals("call "));
      assert (att.getStringValue(3).equals("mesh "));

      // read entire array
      Array A;
      try {
        A = dset.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (A.getInt(ima.set(i, j)) == 0);
        }
      }

    }
  }

  @org.junit.Test
  public void testReadH5attributeString() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/attstr.h5")) {

      Group g = ncfile.getRootGroup().findGroup("MyGroup");
      assert null != g;
      Attribute att = g.findAttribute("data_contents");
      assert (null != att);
      assert (!att.isArray());
      assert (att.isString());
      assert (att.getStringValue().equals("important_data"));

    }
  }

  @org.junit.Test
  public void testReadH5boolean() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/bool.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("dset")));
      assert (dset.getDataType() == DataType.INT);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 4);
      d = dset.getDimension(1);
      assert (d.getLength() == 6);

      // read entire array
      Array A;
      try {
        A = dset.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (A.getInt(ima.set(i, j)) == (j < 3 ? 1 : 0));
        }
      }

    }
  }

  @org.junit.Test
   public void testReadH5StringFixed() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/dstr.h5")) {

      Variable v = null;
      assert (null != (v = ncfile.findVariable("Char_Data")));
      assert (v.getDataType() == DataType.CHAR);

      Dimension d = v.getDimension(0);
      assert (d.getLength() == 16);

      // read entire array
      Array A;
      try {
        A = v.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 1);
      assert (A instanceof ArrayChar);

      ArrayChar ca = (ArrayChar) A;
      assert (ca.getString().equals("This is a test."));

    }
  }

  @org.junit.Test
  public void testReadH5StringArray() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/dstrarr.h5")) {

      Variable v = null;
      assert (null != (v = ncfile.findVariable("strdata")));
      assert (v.getRank() == 3);
      assert (v.getDataType() == DataType.CHAR);

      int[] shape = v.getShape();
      assert (shape[0] == 2);
      assert (shape[1] == 2);
      assert (shape[2] == 5);

      // read entire array
      Array A;
      try {
        A = v.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 3);
      assert (A instanceof ArrayChar);

      ArrayChar ca = (ArrayChar) A;
      ArrayChar.StringIterator siter = ca.getStringIterator();
      assert (siter.next().equals("test "));
      assert (siter.next().equals("left "));
      assert (siter.next().equals("call "));
      assert (siter.next().equals("mesh "));

    }
  }

  @org.junit.Test
  public void testReadH5ShortArray() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/short.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("IntArray")));
      assert (dset.getDataType() == DataType.SHORT);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 5);
      d = dset.getDimension(1);
      assert (d.getLength() == 6);

      // read entire array
      Array A;
      try {
        A = dset.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          assert (A.getInt(ima.set(i, j)) == i + j);
        }
      }

      // read part of array
      dset.setCachedData(null, false); // turn off caching to test read subset
      dset.setCaching(false);
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = dset.getShape()[1];
      try {
        A = dset.read(origin2, shape2);
      } catch (InvalidRangeException e) {
        System.err.println("ERROR reading file " + e);
        assert (false);
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 2);

      for (j = 0; j < shape2[1]; j++) {
        assert (A.getInt(ima.set(0, j)) == j);
      }

      // rank reduction
      Array Areduce = A.reduce();
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (j = 0; j < shape2[1]; j++) {
        assert (Areduce.getInt(ima2.set(j)) == j);
      }

    }
  }

}
