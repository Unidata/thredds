/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadArray {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
