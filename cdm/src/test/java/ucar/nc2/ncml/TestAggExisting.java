/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.Assert2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test TestNcml - AggExisting  in the JUnit framework. */

public class TestAggExisting {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testNcmlDirect() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }


  @Test
  public void testNcmlDataset() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetNoProtocolInFilename() throws IOException, InvalidRangeException {
    String filename = "./"+TestNcML.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test(expected = IOException.class)
  public void testNcmlDatasetNoProtocolInNcmlAbsPath() throws IOException, InvalidRangeException {
    // if using an absolute path in the NcML file location attr of the element netcdf, then
    // you must prepend file:
    // this should fail with an IOException
    String filename = "file:./"+TestNcML.topDir + "aggExisting6.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test(expected = IOException.class)
  public void testNcmlDatasetNoProtocolInFilenameOrNcmlAbsPath() throws IOException, InvalidRangeException {
    // if using an absolute path in the NcML file location attr of the element netcdf, then
    // you must prepend file:
    // this should fail with an IOException
    String filename = "./"+TestNcML.topDir + "aggExisting6.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetNoProtocolInNcmlRelPath() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting7.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetNoProtocolInFilenameOrNcmlRelPath() throws IOException, InvalidRangeException {
    String filename = "./"+TestNcML.topDir + "aggExisting7.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
  }

  @Test
  public void testNcmlDatasetWcoords() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingWcoords.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" testNcmlDatasetWcoords.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
    logger.debug(" testNcmlDatasetWcoords.closed ");
  }

  // remove test - now we get a coordinate initialized to missing data, but at least testCoordsAdded works!
  // @Test
  public void testNoCoords() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingNoCoords.xml";
    logger.debug("{}", filename);
    NetcdfDataset ncd = null;

    try {
      ncd = NetcdfDataset.openDataset( filename, true, null);
      Variable time = ncd.findVariable(null, "time");
      Array data = time.read();
      // all missing
      // assert data.getInt(0) ==
    } finally {
      if (ncd != null) ncd.close();
    }
    //logger.debug("{}", ncd);
    //assert false;
  }

  @Test
  public void testNoCoordsDir() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingNoCoordsDir.xml";

    NetcdfDataset ncd = null;
    try {
      ncd = NetcdfDataset.openDataset( filename, true, null);
    } catch (Exception e) {
      assert true;
      return;
    } finally {
      if (ncd != null) ncd.close();
    }
    
    logger.debug("{}", ncd);
    assert false;
  }

  @Test
  public void testCoordsAdded() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingAddCoord.ncml";
    logger.debug("{}", filename);
    NetcdfDataset ncd = null;

    try {
      ncd = NetcdfDataset.openDataset( filename, true, null);
      logger.debug("{}", ncd);
    } finally {
      if (ncd != null) ncd.close();
    }
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getShortName().equals("lon");
    assert lonDim.getLength() == 4;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 59;
  }

 public void testCoordVar(NetcdfFile ncfile) throws IOException {

    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getDataType() == DataType.FLOAT;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      Assert2.assertNearlyEquals(dataI.getDoubleNext(), 41.0);
      Assert2.assertNearlyEquals(dataI.getDoubleNext(), 40.0);
      Assert2.assertNearlyEquals(dataI.getDoubleNext(), 39.0);
  }

  public void testAggCoordVar(NetcdfFile ncfile) {
    Variable time = ncfile.findVariable("time");
    assert null != time;

    String testAtt = ncfile.findAttValueIgnoreCase(time, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("timeAtt");

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 59;
      assert data.getShape()[0] == 59;
      assert data.getElementType() == int.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext())
        assert dataI.getIntNext() == count++ : dataI.getIntCurrent();

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  public void testReadData(NetcdfFile ncfile) {
    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getShortName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 708 : v.getSize();
    assert v.getShape()[0] == 59;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 708;
      assert data.getShape()[0] == 59;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      int [] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          Assert2.assertNearlyEquals(val, 100*i + 10*j + k);
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("T");

      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == shape[0] * shape[1] * shape[2];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getElementType() == double.class;

      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          Assert2.assertNearlyEquals(val, 100*(i+origin[0]) + 10*j + k);
        }

  }

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0}, new int[] {59, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0}, new int[] {2, 3, 2} );
    testReadSlice( ncfile, new int[] {25, 0, 0}, new int[] {10, 3, 4} );
    testReadSlice( ncfile, new int[] {44, 0, 0}, new int[] {10, 2, 3} );
   }
}
