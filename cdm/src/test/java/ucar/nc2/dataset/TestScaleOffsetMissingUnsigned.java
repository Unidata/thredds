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
import ucar.nc2.dataset.NetcdfDataset.Enhance;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;

public class TestScaleOffsetMissingUnsigned {
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
      vs.removeEnhancement(Enhance.ConvertMissing);
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
  
      fooVar.removeEnhancement(Enhance.ConvertMissing);
      double fooValWithoutNaNs = fooVar.read().getDouble(0);

      // "foo" value is equals to fill value. Scale factor has been applied to both.
      Assert2.assertNearlyEquals(actualFillValue, fooValWithoutNaNs);

      // "foo" value is considered a fill.
      Assert.assertTrue(fooVar.isFillValue(fooValWithoutNaNs));


      fooVar.addEnhancement(Enhance.ConvertMissing);
      double fooValWithNaNs = fooVar.read().getDouble(0);

      // "foo" value was converted to NaN because it was equal to _FillValue.
      Assert.assertTrue(Double.isNaN(fooValWithNaNs));

      // Note that we can't use isFillValue() because we've applied the ConvertMissing enhancement.
      // See the EnhanceScaleMissingUnsigned Javadoc.
      Assert.assertTrue(fooVar.isMissing(fooValWithNaNs));
    }
  }

  // Asserts that EnhanceScaleMissingUnsignedImpl compares floating-point values in a "fuzzy" manner.
  // This test demonstrated the bug in https://github.com/Unidata/thredds/issues/1068.
  @Test
  public void testScaleMissingFloatingPointComparisons() throws IOException, URISyntaxException {
    File testResource = new File(getClass().getResource("testScaleMissingFloatingPointComparisons.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS fooVar = (VariableDS) ncd.findVariable("foo");
      fooVar.removeEnhancement(Enhance.ConvertMissing);

      // Values have been multiplied by scale_factor == 0.01f. scale_factor is a float, meaning that we can't compare
      // its products with nearlyEquals() using the default Misc.defaultMaxRelativeDiffDouble.
      Assert2.assertNearlyEquals(0, fooVar.getValidMin(), Misc.defaultMaxRelativeDiffFloat);
      Assert2.assertNearlyEquals(1, fooVar.getValidMax(), Misc.defaultMaxRelativeDiffFloat);

      // Argument is a double, which has higher precision that our scaled _FillValue (float).
      // This assertion failed before the bug was fixed.
      Assert.assertTrue(fooVar.isFillValue(-0.01));

      Array fooVals = fooVar.read();
      Assert.assertEquals(4, fooVals.getSize());

      // foo[0] == -1 (raw); -0.01 (scaled). It is equal to fill value and outside of valid_range.
      Assert2.assertNearlyEquals(-0.01, fooVals.getDouble(0), Misc.defaultMaxRelativeDiffFloat);
      Assert.assertTrue(fooVar.isFillValue(-0.01));
      Assert.assertTrue(fooVar.isMissingValue(-0.01));
      Assert.assertTrue(fooVar.isInvalidData(-0.01));
      Assert.assertTrue(fooVar.isMissing(-0.01));

      // foo[1] == 0 (raw); 0.00 (scaled). It is within valid_range.
      Assert2.assertNearlyEquals(0.00, fooVals.getDouble(1), Misc.defaultMaxRelativeDiffFloat);
      Assert.assertFalse(fooVar.isInvalidData(0.00));
      Assert.assertFalse(fooVar.isMissing(0.00));

      // foo[2] == 100 (raw); 1.00 (scaled). It is within valid_range.
      Assert2.assertNearlyEquals(1.00, fooVals.getDouble(2), Misc.defaultMaxRelativeDiffFloat);
      // These assertions failed before the bug was fixed.
      Assert.assertFalse(fooVar.isInvalidData(1.00));
      Assert.assertFalse(fooVar.isMissing(1.00));

      // foo[3] == 101 (raw); 1.01 (scaled). It is outside of valid_range.
      Assert2.assertNearlyEquals(1.01, fooVals.getDouble(3), Misc.defaultMaxRelativeDiffFloat);
      Assert.assertTrue(fooVar.isMissingValue(1.01));
      Assert.assertTrue(fooVar.isInvalidData(1.01));
      Assert.assertTrue(fooVar.isMissing(1.01));
    }
  }
  
  @Test
  public void testMissingUnsigned() throws URISyntaxException, IOException {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());
  
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("missingUnsigned");
      
      // Packed valid_min == -106. Interpreting bit pattern as unsigned, we get 150.
      Assert2.assertNearlyEquals(150, var.getValidMin());
      
      // Packed valid_min == -6. Interpreting bit pattern as unsigned, we get 250.
      Assert2.assertNearlyEquals(250, var.getValidMax());
      
      // Packed _FillValue and missing_value are -1. Interpreting bit pattern as unsigned, we get 255.
      Assert2.assertNearlyEquals(255, var.getFillValue());
      Assert2.assertNearlyEquals(255, var.getMissingValues()[0]);
      
      // "missingUnsigned" was originally UBYTE, but was widened to accommodate unsigned conversion.
      Assert.assertEquals(DataType.USHORT, var.getDataType());
      
      // Packed values are: -107, -106, -6, -5, -1, 80. Interpreting them as unsigned yields:
      short[] expecteds = new short[] { 149, 150, 250, 251, 255, 80 };
      short[] actuals = (short[]) var.read().getStorage();
      Assert.assertArrayEquals(expecteds, actuals);
    }
  }
  
  @Test
  public void testScaleOffsetMissingUnsigned() throws URISyntaxException, IOException {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());
  
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("scaleOffsetMissingUnsigned");
      
      // These vals are the same as ones from "missingUnsigned", but with a scale_factor of 100 and offset of 1 applied.
      Assert.assertEquals(15001, var.getValidMin(), 0);
      Assert.assertEquals(25001, var.getValidMax(), 0);
    
      Assert.assertEquals(25501, var.getFillValue(), 0);
      Assert.assertEquals(25501, var.getMissingValues()[0], 0);
  
      // "scaleOffsetMissingUnsigned" was originally UBYTE, but scale_factor (SHORT) and add_offset (INT) caused it to
      // be UINT due to:
      /*
       *   <li>The data type of the variable will be set to the
       *       {@link EnhanceScaleMissingUnsignedImpl#largestOf largest of}:
       *     <ul>
       *       <li>the original data type</li>
       *       <li>the unsigned conversion type, if applicable</li>
       *       <li>the {@code scale_factor} attribute type</li>
       *       <li>the {@code add_offset} attribute type</li>
       *     </ul>
       *     The signedness of the variable's data type will be preserved. For example, if the variable was originally
       *     unsigned, then {@link #getScaledOffsetMissingType()} will be unsigned as well.
       *   </li>
       */
      Assert.assertEquals(DataType.UINT, var.getDataType());
  
      // These vals are the same as ones from "missingUnsigned", but with a scale_factor of 100 and offset of 1 applied.
      int[] expecteds = new int[] { 14901, 15001, 25001, 25101, 25501, 8001 };
      int[] actuals = (int[]) var.read().getStorage();
      Assert.assertArrayEquals(expecteds, actuals);
    }
  }
  
  @Test
  public void testScaleValidRange() throws IOException, URISyntaxException {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());

    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("scaleValidRange");
      var.addEnhancement(Enhance.ConvertMissing);

      Assert2.assertNearlyEquals(9.9f,  (float) var.getValidMin());
      Assert2.assertNearlyEquals(10.1f, (float) var.getValidMax());

      Assert.assertEquals(DataType.FLOAT, var.getDataType());  // scale_factor is float.

      float[] expecteds = new float[] { Float.NaN, 9.9f, 10.0f, 10.1f, Float.NaN };
      float[] actuals   = (float[]) var.read().getStorage();
      Assert2.assertArrayNearlyEquals(expecteds, actuals);
    }
  }
  
  @Test
  public void testUnpackedValidRange() throws IOException, URISyntaxException {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());
    DatasetUrl location = DatasetUrl.findDatasetUrl(testResource.getAbsolutePath());
    Set<Enhance> enhanceMode = EnumSet.of(Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset);  // No ConvertMissing!
    
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(location, enhanceMode, -1, null, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("unpackedValidRange");
      
      // valid_range will be interpreted as unpacked because of:
      /*
       * If valid_range is the same type as scale_factor (actually the wider of scale_factor and add_offset) and this
       * is wider than the external data, then it will be interpreted as being in the units of the internal (unpacked)
       * data. Otherwise it is in the units of the external (packed) data.</li>
       */
      // As a result, scale_factor will not be applied to it.
      Assert2.assertNearlyEquals(9.9f,  (float) var.getValidMin());
      Assert2.assertNearlyEquals(10.1f, (float) var.getValidMax());
      
      Assert.assertEquals(DataType.FLOAT, var.getDataType());  // scale_factor is float.
      
      float[] expecteds = new float[] { 9.8f, 9.9f, 10.0f, 10.1f, 10.2f };
      float[] actuals   = (float[]) var.read().getStorage();
      Assert2.assertArrayNearlyEquals(expecteds, actuals);
    }
  }
  
  @Test
  public void testUnsignedOffsetAttribute() throws IOException, URISyntaxException {
    File testResource = new File(getClass().getResource("testScaleOffsetMissingUnsigned.ncml").toURI());
  
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(testResource.getAbsolutePath(), true, null)) {
      VariableDS var = (VariableDS) ncd.findVariable("unsignedOffsetAttribute");
  
      Assert.assertEquals(156, var.getOffset(), 0);
      Assert.assertEquals(DataType.BYTE, var.getDataType());  // No change to data type.
      
      Assert.assertEquals(106, var.read().getByte(0));  // -50 + 156 == 106
    }
  }
}
