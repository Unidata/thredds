/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.Assert2;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;

public class TestScaleOffsetMissing {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWrite() throws Exception {
    String filename = tempFolder.newFile().getAbsolutePath();
    ArrayDouble unpacked;
    MAMath.ScaleOffset so;
    Array packed;

    try (NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(filename, true)) {
      // define dimensions
      Dimension latDim = ncfile.addDimension("lat", 200);
      Dimension lonDim = ncfile.addDimension("lon", 300);
      int       n      = lonDim.getLength();

      // create an array
      unpacked = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
      Index ima = unpacked.getIndex();

      for (int i = 0; i < latDim.getLength(); i++) {
        for (int j = 0; j < lonDim.getLength(); j++) {
          unpacked.setDouble(ima.set(i, j), (i * n + j) + 30.0);
        }
      }

      double  missingValue = -9999;
      int     nbits        = 16;

      // convert to packed form
      so = MAMath.calcScaleOffsetSkipMissingData(unpacked, missingValue, nbits);
      ncfile.addVariable("unpacked", DataType.DOUBLE, "lat lon");

      ncfile.addVariable("packed", DataType.SHORT, "lat lon");
      ncfile.addVariableAttribute("packed", CDM.MISSING_VALUE, (short) -9999);
      ncfile.addVariableAttribute("packed", CDM.SCALE_FACTOR, so.scale);
      ncfile.addVariableAttribute("packed", "add_offset", so.offset);

      // create the file
      ncfile.create();

      ncfile.write("unpacked", unpacked);

      packed = MAMath.convert2packed(unpacked, missingValue, nbits, DataType.SHORT);
      ncfile.write("packed", packed);
    }

    Array readPacked;

    // read the packed form, compare to original
    try (NetcdfFile ncfileRead = NetcdfFile.open(filename)) {
      Variable v = ncfileRead.findVariable("packed");
      assert v != null;
      readPacked = v.read();
      ucar.unidata.util.test.CompareNetcdf.compareData(readPacked, packed);
    }

    Array readEnhanced;

    // read the packed form, enhance using scale/offset, compare to original
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {
      VariableDS vs = (VariableDS) ncd.findVariable("packed");
      vs.setUseNaNs(false);
      readEnhanced = vs.read();

      nearlyEquals(packed, unpacked, readEnhanced, 1.0 / so.scale);
    }

    Array convertPacked = MAMath.convert2Unpacked(readPacked, so);
    nearlyEquals(packed, convertPacked, readEnhanced, 1.0 / so.scale);

    doSubset(filename);
  }

  void nearlyEquals(Array packed, Array data1, Array data2, double close) {
    IndexIterator iterp = packed.getIndexIterator();
    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    while (iter1.hasNext()) {
      double v1 = iter1.getDoubleNext();
      double v2 = iter2.getDoubleNext();
      double p = iterp.getDoubleNext();
      double diff = Math.abs(v1 - v2);
      assert (diff < close) : v1 + " != " + v2 + " index=" + iter1+" packed="+p;
    }
  }

  // check section of scale/offset only applies it once
  private void doSubset(String filename) throws IOException, InvalidRangeException {
    // read the packed form, enhance using scale/offset, compare to original
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {
      Variable vs = ncd.findVariable("packed");
      assert vs != null;

      Section s            = new Section().appendRange(1, 1).appendRange(1, 1);
      Array   readEnhanced = vs.read(s);
      logger.debug(NCdumpW.toString(readEnhanced));

      Variable sec         = vs.section(s);
      Array    readSection = sec.read();
      logger.debug(NCdumpW.toString(readSection));

      ucar.unidata.util.test.CompareNetcdf.compareData(readEnhanced, readSection);
    }
  }


  // Asserts that "scale_factor" is applied to "_FillValue".
  // This test demonstrated the bug in https://github.com/Unidata/thredds/issues/1065.
  @Test
  public void testScaledFillValue() throws URISyntaxException, IOException {
    File testResource = new File(getClass().getResource("testScaledFillValue.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");

      double expectedFillValue = .99999;
      double actualFillValue = fooVar.getFillValue();

      // Scale factor of "1.e-05" has been applied to original "99999".
      Assert2.assertNearlyEquals(expectedFillValue, actualFillValue);

      fooVar.setUseNaNs(false);
      double fooValWithoutNaNs = fooVar.read().getDouble(0);

      // "foo" value is equals to fill value. Scale factor has been applied to both.
      Assert2.assertNearlyEquals(actualFillValue, fooValWithoutNaNs);

      // "foo" value is considered a fill.
      Assert.assertTrue(fooVar.isFillValue(fooValWithoutNaNs));


      fooVar.setUseNaNs(true);
      double fooValWithNaNs = fooVar.read().getDouble(0);

      // "foo" value was converted to NaN because it was equal to _FillValue.
      Assert.assertTrue(Double.isNaN(fooValWithNaNs));

      // Note that we can't use isFillValue() because we've set useNaNs to "true". See the EnhanceScaleMissing Javadoc.
      Assert.assertTrue(fooVar.isMissing(fooValWithNaNs));
    }
  }

  // Asserts that EnhanceScaleMissingImpl compares floating-point values in a "fuzzy" manner.
  // This test demonstrated the bug in https://github.com/Unidata/thredds/issues/1068.
  @Test
  public void testScaleMissingFloatingPointComparisons() throws IOException, URISyntaxException {
    File testResource = new File(getClass().getResource("testScaleMissingFloatingPointComparisons.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");
      fooVar.setUseNaNs(false);

      // Values have been multiplied by scale_factor == 0.01f. scale_factor is a float, meaning that we can't compare
      // its products with nearlyEquals() using the default Misc.defaultMaxRelativeDiffDouble.
      Assert2.assertNearlyEquals(0, fooVar.getValidMin(), Misc.defaultMaxRelativeDiffFloat);
      Assert2.assertNearlyEquals(1, fooVar.getValidMax(), Misc.defaultMaxRelativeDiffFloat);

      // Argument is a double, which has higher precision that our scaled _FillValue (float).
      // This assertion failed before the bug was fixed.
      Assert.assertTrue(fooVar.isFillValue(-.01));

      Array fooVals = fooVar.read();
      Assert.assertEquals(4, fooVals.getSize());

      // foo[0] == -1 (raw); -.01 (scaled). It is equal to fill value and outside of valid_range.
      double actualFooVal = fooVals.getDouble(0);
      Assert.assertTrue(fooVar.isFillValue(actualFooVal));
      Assert.assertTrue(fooVar.isInvalidData(actualFooVal));
      Assert.assertTrue(fooVar.isMissing(actualFooVal));

      // foo[1] == 0 (raw); 0.0 (scaled). It is within valid_range.
      actualFooVal = fooVals.getDouble(1);
      Assert.assertFalse(fooVar.isInvalidData(actualFooVal));
      Assert.assertFalse(fooVar.isMissing(actualFooVal));

      // foo[2] == 100 (raw); 1.0 (scaled). It is within valid_range.
      actualFooVal = fooVals.getDouble(2);
      // These assertions failed before the bug was fixed.
      Assert.assertFalse(fooVar.isInvalidData(actualFooVal));
      Assert.assertFalse(fooVar.isMissing(actualFooVal));

      // foo[3] == 101 (raw); 1.01 (scaled). It is outside of valid_range.
      actualFooVal = fooVals.getDouble(0);
      Assert.assertTrue(fooVar.isInvalidData(actualFooVal));
      Assert.assertTrue(fooVar.isMissing(actualFooVal));
    }
  }
}
