/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Read data through the Grib iosp.
 * Though this is superceeded by coverage, we need to keep it working.
 *
 * @author caron
 * @since 4/9/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionReadingIosp {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testReadBest() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    String covName = "Best/Temperature_height_above_ground";
    logger.debug("open {} var={}", endpoint, covName);

    try (NetcdfDataset ds = NetcdfDataset.openDataset(endpoint)) {
      assert ds != null;
      Variable v = ds.findVariable(null, covName);
      assert v != null;
      assert v instanceof VariableDS;

      Variable time = ds.findVariable("Best/time3");
      Array timeData = time.read();
      for (int i=0; i< timeData.getSize(); i++)
        logger.debug("time({}) = {}", i, timeData.getDouble(i));
      logger.debug("{}", time.findAttribute("units"));

      Array data = v.read("30,0,:,:"); // Time  coord : 180 == 2014-10-31T12:00:00Z
      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      logger.debug("data first = {} last = {}", first, last);
      Assert2.assertNearlyEquals(300.33002f, first);
      Assert2.assertNearlyEquals(279.49f, last);
    }
  }

  @Test
  public void testReadMrutpTimeRange() throws IOException, InvalidRangeException {
    // read more than one time coordinate at a time in a MRUTP, no vertical
    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      Variable v = ds.findVariable(null, "Pressure_surface");
      assert v != null;
      Array data = v.read("0:1,50,50");
      assert data != null;
      assert data.getRank() == 3;
      assert data.getDataType() == DataType.FLOAT;
      assert data.getSize() == 2;
      float[] got = (float []) data.copyTo1DJavaArray();
      float[] expect = new float[] {103031.914f, 103064.164f};
      Assert.assertArrayEquals(expect, got, (float) Misc.defaultMaxRelativeDiffFloat);
    }
  }

  @Test
  public void testReadMrutpTimeRangeWithSingleVerticalLevel() throws IOException, InvalidRangeException {
    // read more than one time coordinate at a time in a MRUTP, with vertical
    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      Variable v = ds.findVariable(null, "Relative_humidity_sigma");
      assert v != null;
      Array data = v.read("0:1, 0, 50, 50");
      assert data != null;
      assert data.getRank() == 4;
      assert data.getDataType() == DataType.FLOAT;
      assert data.getSize() == 2;
      logger.debug("{}", NCdumpW.toString(data));
      while (data.hasNext()) {
        float val = data.nextFloat();
        assert !Float.isNaN(val);
      }
      float[] got = (float []) data.copyTo1DJavaArray();
      float[] expect = new float[] {68.0f, 74.0f};
      Assert.assertArrayEquals(expect, got, (float) Misc.defaultMaxRelativeDiffFloat);
    }
  }

  @Test
  public void testReadMrutpTimeRangeWithMultipleVerticalLevel() throws IOException, InvalidRangeException {
    // read more than one time coordinate at a time in a MRUTP. multiple verticals
    try (NetcdfDataset ds = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      Variable v = ds.findVariable(null, "Relative_humidity_isobaric");
      assert v != null;
      Array data = v.read("0:1, 10:20:2, 50, 50");
      assert data != null;
      assert data.getRank() == 4;
      assert data.getDataType() == DataType.FLOAT;
      assert data.getSize() == 12;
      logger.debug("{}", NCdumpW.toString(data));
      while (data.hasNext()) {
        float val = data.nextFloat();
        assert !Float.isNaN(val);
      }
      float[] got = (float []) data.copyTo1DJavaArray();
      float[] expect = new float[] {57.8f, 53.1f, 91.3f, 85.5f, 80.0f, 69.3f, 32.8f, 41.8f, 88.9f, 81.3f, 70.9f, 70.6f};
      Assert.assertArrayEquals(expect, got, (float) Misc.defaultMaxRelativeDiffFloat);
    }
  }

}
