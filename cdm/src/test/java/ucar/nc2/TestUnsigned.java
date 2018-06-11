/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

/**
 * Test Unsigned values and attributes in NcML works correctly
 *
 * @author caron
 * @since Aug 7, 2008
 */
public class TestUnsigned {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testSigned() throws IOException {
    try (NetcdfFile ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testWrite.nc")) {
      Variable v = ncfile.findVariable("bvar");
      Assert.assertNotNull(v);
      Assert.assertTrue(!v.getDataType().isUnsigned());
      Assert.assertEquals(DataType.BYTE, v.getDataType());

      boolean hasSigned = false;
      Array data = v.read();
      while (data.hasNext()) {
        byte b = data.nextByte();
        if (b < 0) hasSigned = true;
      }
      assert hasSigned;
    }
  }

  @Test
  public void testUnsigned() throws IOException {
    try (NetcdfFile ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testUnsignedByte.ncml")) {
      Variable v = ncfile.findVariable("bvar");
      Assert.assertNotNull(v);
      Assert.assertEquals(DataType.FLOAT, v.getDataType()); // has float scale_factor

      boolean hasSigned = false;
      Array data = v.read();
      while (data.hasNext()) {
        float b = data.nextFloat();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }

  @Test
  public void testUnsignedWrap() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
        "  <variable name='bvar' shape='lat' type='byte'>\n" +
        "    <attribute name='_Unsigned' value='true' />\n" +
        "    <attribute name='scale_factor' type='float' value='2.0' />\n" +
        "   </variable>\n" +
        "</netcdf>";

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
      NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll())) {

      Variable v = ncd.findVariable("bvar");
      Assert.assertNotNull(v);
      Assert.assertEquals(DataType.FLOAT, v.getDataType());

      boolean hasSigned = false;
      Array data = v.read();
      while (data.hasNext()) {
        float b = data.nextFloat();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }

  @Test
  public void testVarWithUnsignedAttribute() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
            "  <variable name='bvar' shape='lat' type='byte'>\n" +
            "    <attribute name='_Unsigned' value='true' />\n" +
            "   </variable>\n" +
            "</netcdf>";

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
         NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll())) {

      Variable v = ncd.findVariable("bvar");
      Assert.assertNotNull(v);
      Assert.assertEquals(DataType.USHORT, v.getDataType());

      boolean hasSigned = false;
      Array data = v.read();
      while (data.hasNext()) {
        float b = data.nextFloat();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }

  @Test
  public void testVarWithUnsignedType() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
            "  <variable name='bvar' shape='lat' type='ubyte'/>" +
            "</netcdf>";

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
         NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll())) {

      Variable v = ncd.findVariable("bvar");
      Assert.assertNotNull(v);
      Assert.assertEquals(DataType.USHORT, v.getDataType());

      boolean hasSigned = false;
      Array data = v.read();
      while (data.hasNext()) {
        float b = data.nextFloat();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }

  @Test
  public void testAttWithUnsignedType() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
            "  <attribute name='gatt' type='ubyte'>1 0 -1</attribute>" +
            "</netcdf>";

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
         NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll())) {

      Attribute att = ncd.findGlobalAttribute("gatt");
      Assert.assertNotNull(att);
      Assert.assertEquals(DataType.UBYTE, att.getDataType());

      Assert.assertEquals(3, att.getLength());
      Array gattValues = att.getValues();

      boolean hasSigned = false;
      while (gattValues.hasNext()) {
        short b = gattValues.nextShort();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }

  @Test
  public void testAttWithUnsignedAtt() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
            "  <attribute name='gatt' type='byte' isUnsigned='true'>1 0 -1</attribute>" +
            "</netcdf>";

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
         NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll())) {

      Attribute att = ncd.findGlobalAttribute("gatt");
      Assert.assertNotNull(att);
      Assert.assertEquals(DataType.UBYTE, att.getDataType());

      Assert.assertEquals(3, att.getLength());
      Array gattValues = att.getValues();

      boolean hasSigned = false;
      while (gattValues.hasNext()) {
        short b = gattValues.nextShort();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }

  @Test
  public void testAttWithUnsignedType2() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"+TestDir.cdmLocalTestDataDir +"testWrite.nc'>\n" +
            "  <attribute name='gatt' type='ubyte' value='1 0 -1' />" +
            "</netcdf>";

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
         NetcdfFile ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getEnhanceAll())) {

      Attribute att = ncd.findGlobalAttribute("gatt");
      Assert.assertNotNull(att);
      Assert.assertEquals(DataType.UBYTE, att.getDataType());

      Assert.assertEquals(3, att.getLength());
      Array gattValues = att.getValues();

      boolean hasSigned = false;
      while (gattValues.hasNext()) {
        short b = gattValues.nextShort();
        if (b < 0) hasSigned = true;
      }
      Assert.assertTrue(!hasSigned);
    }
  }
}
