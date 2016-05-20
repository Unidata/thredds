/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 4/9/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionReading {

  @Test
  public void testReadTimeRange() throws IOException, InvalidRangeException {
    // read more than one time coordinate at a time in a TP (multiple runtimes, single offset) partition
    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3")) {
      Variable v = ds.findVariable(null, "Pressure_surface");
      assert v != null;
      Array data = v.read("0:1,50,50");
      assert data != null;
      assert data.getRank() == 3;
      assert data.getDataType() == DataType.FLOAT;
      assert data.getSize() == 2;
      float[] got = (float []) data.copyTo1DJavaArray();
      float[] expect = new float[] {103031.914f, 103064.164f};
      Assert.assertArrayEquals(expect, got, (float) Misc.maxReletiveError);
    }
  }

  @Test
  public void testReadTimeRangeWithSingleVerticalLevel() throws IOException, InvalidRangeException {
    // read more than one time coordinate at a time in a TP (multiple runtimes, single offset) partition
    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3")) {
      Variable v = ds.findVariable(null, "Relative_humidity_sigma");
      assert v != null;
      Array data = v.read("0:1, 0, 50, 50");
      assert data != null;
      assert data.getRank() == 4;
      assert data.getDataType() == DataType.FLOAT;
      assert data.getSize() == 2;
      System.out.printf("%s%n", NCdumpW.toString(data));
      while (data.hasNext()) {
        float val = data.nextFloat();
        assert !Float.isNaN(val);
      }
      float[] got = (float []) data.copyTo1DJavaArray();
      float[] expect = new float[] {68.0f, 74.0f};
      Assert.assertArrayEquals(expect, got, (float) Misc.maxReletiveError);
    }
  }


  @Test
  public void testReadTimeRangeWithMultipleVerticalLevel() throws IOException, InvalidRangeException {
    // read more than one time coordinate at a time in a TP (multiple runtimes, single offset) partition
    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3")) {
      Variable v = ds.findVariable(null, "Relative_humidity_isobaric");
      assert v != null;
      Array data = v.read("0:1, 10:20:2, 50, 50");
      assert data != null;
      assert data.getRank() == 4;
      assert data.getDataType() == DataType.FLOAT;
      assert data.getSize() == 12;
      System.out.printf("%s%n", NCdumpW.toString(data));
      while (data.hasNext()) {
        float val = data.nextFloat();
        assert !Float.isNaN(val);
      }
      float[] got = (float []) data.copyTo1DJavaArray();
      float[] expect = new float[] {57.8f, 53.1f, 91.3f, 85.5f, 80.0f, 69.3f, 32.8f, 41.8f, 88.9f, 81.3f, 70.9f, 70.6f};
      Assert.assertArrayEquals(expect, got, (float) Misc.maxReletiveError);
    }
  }

}
