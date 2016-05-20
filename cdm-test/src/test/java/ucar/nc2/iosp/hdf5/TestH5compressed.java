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

/** Test compressed data from H5  read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5compressed {

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
      assert (dset.getDataType() == DataType.BYTE);

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
