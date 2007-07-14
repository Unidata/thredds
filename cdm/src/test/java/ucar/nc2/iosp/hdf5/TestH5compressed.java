package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.*;

/** Test nc2 read JUnit framework. */

public class TestH5compressed extends TestCase {

  public TestH5compressed(String name) {
    super(name);
  }

  public void testReadCompressedInt() throws IOException {
    // actually doesnt seem to be compressed ??

    NetcdfFile ncfile = TestH5.openH5("support/zip.h5");
    Variable dset = null;
    assert(null != (dset = ncfile.findVariable("Data/Compressed_Data")));
    assert(dset.getDataType() == DataType.INT);

    assert(dset.getRank() == 2);
    assert(dset.getShape()[0] == 1000);
    assert(dset.getShape()[1] == 20);

    // read entire array
    Array A = null;
    try {
      A = dset.read();
    }
    catch (IOException e) {
      System.err.println("ERROR reading file ");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert(A.getRank() == 2);
    assert(A.getShape()[0] == 1000);
    assert(A.getShape()[1] == 20);

    int[] shape = A.getShape();
    Index ima = A.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        assert(  A.getInt(ima.set(i,j)) == i+j) : i+" "+j+" "+A.getInt(ima);

    ncfile.close();
  }

  public void testReadCompressedByte() throws IOException {
    // actually doesnt seem to be compressed ??

    NetcdfFile ncfile = TestH5.openH5("support/MSG1_8bit_HRV.H5");
    Variable dset = null;
    assert(null != (dset = ncfile.findVariable("image1/image_preview")));
    assert(dset.getDataType() == DataType.BYTE);

    assert(dset.getRank() == 2);
    assert(dset.getShape()[0] == 64);
    assert(dset.getShape()[1] == 96);

    // read entire array
    Array A = null;
    try {
      A = dset.read();
    }
    catch (IOException e) {
      System.err.println("ERROR reading file ");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert(A.getRank() == 2);
    assert(A.getShape()[0] == 64);
    assert(A.getShape()[1] == 96);

    byte[] firstRow = new byte[] {
       0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 32, 32, 33, 34, 35, 36, 37, 39, 39,
      40, 42, 42, 44, 44, 57, 59, 52, 52, 53, 55, 59, 62, 63, 66, 71, 79, 81,
      83, 85, 87, 89, 90, 87, 84, 84, 87, 94, 82, 80, 76, 77, 68, 59, 57, 61,
      68, 81, 42};

    byte[] lastRow = new byte[] {
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 31, 31,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
      31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
      31, 31, 30, 31, 28, 20, 11, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    int[] shape = A.getShape();
    Index ima = A.getIndex();
    int lrow = shape[0]-1;
    for (int j = 0; j < shape[1]; j++) {
      assert(  A.getByte(ima.set(0,j)) == firstRow[j]) : A.getByte(ima)+" should be "+firstRow[j];
      assert(  A.getByte(ima.set(lrow,j)) == lastRow[j]) : A.getByte(ima)+" should be "+lastRow[j];
    }

    ncfile.close();
  }

}
