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
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadArray {

  @org.junit.Test
  public void testReadArrayType() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/SDS_array_type.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("IntArray")));
      assert (dset.getDataType() == DataType.INT);

      assert (dset.getRank() == 3);
      assert (dset.getShape()[0] == 10);
      assert (dset.getShape()[1] == 5);
      assert (dset.getShape()[2] == 4);

      // read entire array
      Array A;
      try {
        A = dset.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 3);

      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++)
          for (int k = 0; k < shape[2]; k++)
            if (A.getInt(ima.set(i, j, k)) != i) {
              assert false;
            }


      // read part of array
      dset.setCachedData(null, false); // turn off caching to test read subset
      dset.setCaching(false);
      int[] origin2 = new int[3];
      int[] shape2 = new int[]{
              10, 1, 1};
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
      assert (A.getRank() == 3);
      assert (A.getShape()[0] == 10);
      assert (A.getShape()[1] == 1);
      assert (A.getShape()[2] == 1);

      ima = A.getIndex();
      for (int j = 0; j < shape2[0]; j++) {
        assert (A.getInt(ima.set0(j)) == j) : A.getInt(ima);
      }

      // rank reduction
      Array Areduce = A.reduce();
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);
      ima = A.getIndex();

      for (int j = 0; j < shape2[0]; j++) {
        assert (A.getInt(ima.set0(j)) == j);
      }

    }
  }

}
