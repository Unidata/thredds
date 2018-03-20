/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test compressed data from H5  read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5compressed {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @org.junit.Test
  public void testReadCompressedInt() throws IOException {
    // actually doesnt seem to be compressed ??

    try (NetcdfFile ncfile = TestH5.openH5("support/zip.h5")) {
      Variable dset = ncfile.findVariable("Data/Compressed_Data");
      assert dset != null;
      assert (dset.getDataType() == DataType.INT);

      assert (dset.getRank() == 2);
      assert (dset.getShape()[0] == 1000);
      assert (dset.getShape()[1] == 20);

      // read entire array
      Array A = dset.read();
      assert (A.getRank() == 2);
      assert (A.getShape()[0] == 1000);
      assert (A.getShape()[1] == 20);

      int[] shape = A.getShape();
      Index ima = A.getIndex();
      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++)
          assert (A.getInt(ima.set(i, j)) == i + j) : i + " " + j + " " + A.getInt(ima);

    }
  }

  @org.junit.Test
  public void testReadCompressedByte() throws IOException {
    // actually doesnt seem to be compressed ??

    try (NetcdfFile ncfile = TestH5.openH5("msg/MSG1_8bit_HRV.H5")) {
      Variable dset = ncfile.findVariable("image1/image_preview");
      assert dset != null;
      assert (dset.getDataType() == DataType.UBYTE);

      assert (dset.getRank() == 2);
      assert (dset.getShape()[0] == 64);
      assert (dset.getShape()[1] == 96);

      // read entire array
      Array A = dset.read();
      assert (A.getRank() == 2);
      assert (A.getShape()[0] == 64);
      assert (A.getShape()[1] == 96);

      byte[] firstRow = new byte[]{
              0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
              31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
              31, 31, 31, 31, 31, 31, 31, 31, 31, 32, 32, 33, 34, 35, 36, 37, 39, 39,
              40, 42, 42, 44, 44, 57, 59, 52, 52, 53, 55, 59, 62, 63, 66, 71, 79, 81,
              83, 85, 87, 89, 90, 87, 84, 84, 87, 94, 82, 80, 76, 77, 68, 59, 57, 61,
              68, 81, 42};

      byte[] lastRow = new byte[]{
              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 31, 31,
              31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
              31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
              31, 31, 30, 31, 28, 20, 11, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

      int[] shape = A.getShape();
      Index ima = A.getIndex();
      int lrow = shape[0] - 1;
      for (int j = 0; j < shape[1]; j++) {
        assert (A.getByte(ima.set(0, j)) == firstRow[j]) : A.getByte(ima) + " should be " + firstRow[j];
        assert (A.getByte(ima.set(lrow, j)) == lastRow[j]) : A.getByte(ima) + " should be " + lastRow[j];
      }
    }
  }

  @org.junit.Test
  public void testEndian() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestN4reading.testDir + "endianTest.nc4")) {
      Variable v = ncfile.findVariable("TMP");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;

      Array data = v.read();
      assert data.getElementType() == float.class;

      //large values indicate incorrect inflate or byte swapping
      while (data.hasNext()) {
        float val = data.nextFloat();
        assert Math.abs(val) < 100.0 : val;
      }
    }
  }


}
