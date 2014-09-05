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
package ucar.nc2.ncml;

import org.junit.Test;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

/** Test TestNcml - AggExisting  in the JUnit framework. */

public class TestAggExisting {

  @Test
  public void testNcmlDirect() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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
    System.out.println(" testNcmlDatasetWcoords.open "+ filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);
    testReadSlice(ncfile);
    ncfile.close();
    System.out.println(" testNcmlDatasetWcoords.closed ");
  }

  // remove test - now we get a coordinate initialized to missing data, but at least testCoordsAdded works!
  // @Test
  public void testNoCoords() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingNoCoords.xml";
    System.out.printf("%s%n", filename);
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
    //System.out.printf("%s%n", ncd);
    //assert false;
  }

  @Test
  public void testNoCoordsDir() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingNoCoordsDir.xml";

    NetcdfDataset ncd = null;
    try {
      ncd = NetcdfDataset.openDataset( filename, true, null);
    } catch (Exception e) {
      assert true;
      return;
    }
    System.out.printf("%s%n", ncd);
    assert false;
  }

  @Test
  public void testCoordsAdded() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExistingAddCoord.ncml";
    System.out.printf("%s%n", filename);
    NetcdfDataset ncd = null;

    try {
      ncd = NetcdfDataset.openDataset( filename, true, null);
      System.out.printf("%s%n", ncd);
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
      assert TestUtils.close(dataI.getDoubleNext(), 41.0);
      assert TestUtils.close(dataI.getDoubleNext(), 40.0);
      assert TestUtils.close(dataI.getDoubleNext(), 39.0);

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
          // System.out.println(" "+val);
          assert TestUtils.close(val, 100*i + 10*j + k) : val;
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
          //System.out.println(" "+val);
          assert TestUtils.close(val, 100*(i+origin[0]) + 10*j + k) : val;
        }

  }

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0}, new int[] {59, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0}, new int[] {2, 3, 2} );
    testReadSlice( ncfile, new int[] {25, 0, 0}, new int[] {10, 3, 4} );
    testReadSlice( ncfile, new int[] {44, 0, 0}, new int[] {10, 2, 3} );
   }
}
