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

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcNetcdf extends TestCase {

  public TestOffAggFmrcNetcdf( String name) {
    super(name);
  }

  public void testNUWGdatasets() throws IOException, InvalidRangeException {
    String filename = "file:"+ TestDir.cdmUnitTestDir + "ncml/nc/ncmodels/aggFmrcNetcdf.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);

    int nagg = 15;

    testDimensions(ncfile, nagg);
    testYCoordVar(ncfile);
    testRunCoordVar(ncfile, nagg);
    testTimeCoordVar(ncfile, nagg, 11);
    testReadData(ncfile, nagg);
    testReadSlice(ncfile);

    ncfile.close();
    
  }

  private void testDimensions(NetcdfFile ncfile, int nagg) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getShortName().equals("x");
    assert latDim.getLength() == 93;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getShortName().equals("y");
    assert lonDim.getLength() == 65;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("run");
    assert null != timeDim;
    assert timeDim.getShortName().equals("run");
    assert timeDim.getLength() == nagg : timeDim.getLength();
  }

 private void testYCoordVar(NetcdfFile ncfile) throws IOException {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getShortName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == 65;
    assert lat.getShape()[0] == 65;
    assert lat.getDataType() == DataType.DOUBLE;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("y"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("km");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 65;
      assert data.getShape()[0] == 65;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert Misc.closeEnough(dataI.getDoubleNext(), -832.6983183345455);
      assert Misc.closeEnough(dataI.getDoubleNext(), -751.4273183345456);
      assert Misc.closeEnough(dataI.getDoubleNext(), -670.1563183345455);

  }

  private void testRunCoordVar(NetcdfFile ncfile, int nagg) {
    Variable time = ncfile.findVariable("run");
    assert null != time;
    assert time.getShortName().equals("run");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.DOUBLE;

    DateFormatter formatter = new DateFormatter();
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == double.class;

      NCdumpW.printArray(data);

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        assert val == count * 12 : val +" != "+ count * 12;
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  private void testTimeCoordVar(NetcdfFile ncfile, int nagg, int noff) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 2;
    assert time.getSize() == nagg * noff;
    assert time.getShape()[0] == nagg;
    assert time.getShape()[1] == noff;
    assert time.getDataType() == DataType.DOUBLE;

    Array data = time.read();
    assert data.getRank() == 2;
    assert data.getSize() == nagg * noff;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == noff;
    assert data.getElementType() == double.class;

    double[][] result =  new double[][]  {
      {0.0, 6.0, 12.0, 18.0, 24.0, 30.0, 36.0, 42.0, 48.0, 54.0, 60.0},
      {12.0, 18.0, 24.0, 30.0, 36.0, 42.0, 48.0, 54.0, 60.0, 66.0, 72.0},
      {24.0, 30.0, 36.0, 42.0, 48.0, 54.0, 60.0, 66.0, 72.0, 78.0, 84.0},
      {36.0, 42.0, 48.0, 54.0, 60.0, 66.0, 72.0, 78.0, 84.0, 90.0, 96.0},
      {48.0, 54.0, 60.0, 66.0, 72.0, 78.0, 84.0, 90.0, 96.0, 102.0, 108.0},
      {60.0, 66.0, 72.0, 78.0, 84.0, 90.0, 96.0, 102.0, 108.0, 114.0, 120.0},
      {72.0, 78.0, 84.0, 90.0, 96.0, 102.0, 108.0, 114.0, 120.0, 126.0, 132.0},
      {84.0, 90.0, 96.0, 102.0, 108.0, 114.0, 120.0, 126.0, 132.0, 138.0, 144.0},
      {96.0, 102.0, 108.0, 114.0, 120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0},
      {108.0, 114.0, 120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0},
      {120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0},
      {132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0, 186.0, 192.0},
      {144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0, 186.0, 192.0, 198.0, 204.0},
      {156.0, 162.0, 168.0, 174.0, 180.0, 186.0, 192.0, 198.0, 204.0, 210.0, 216.0},
            {168.0, 174.0, 180.0, 186.0, 192.0, 198.0, 204.0, 210.0, 216.0, 222.0, 228.0}
    };

    Index ima = data.getIndex();
    for (int i=0; i < nagg; i++)
      for (int j=0; j < noff; j++) {
        double val = data.getDouble(ima.set(i,j));
        assert Misc.closeEnough(val, result[i][j]);
      }


  }



  private void testReadData(NetcdfFile ncfile, int nagg) throws IOException {
    Variable v = ncfile.findVariable("P_sfc");
    assert null != v;
    assert v.getShortName().equals("P_sfc");
    assert v.getRank() == 4;
    assert v.getShape()[0] == nagg;
    assert v.getShape()[1] == 11;
    assert v.getShape()[2] == 65;
    assert v.getShape()[3] == 93;
    assert v.getDataType() == DataType.FLOAT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("run");
    assert v.getDimension(1) == ncfile.findDimension("time");
    assert v.getDimension(2) == ncfile.findDimension("y");
    assert v.getDimension(3) == ncfile.findDimension("x");

    Array data = v.read();
    assert data.getRank() == 4;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == 11;
    assert data.getShape()[2] == 65;
    assert data.getShape()[3] == 93;

    double sum = MAMath.sumDoubleSkipMissingData(data, 0.0);

    /* float sum = 0.0f;
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      sum += ii.getFloatNext();
    } */
    System.out.println(" sum= "+sum);

  }

  private void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("P_sfc");

      Array data = v.read(origin, shape);
      assert data.getRank() == 4;
      assert data.getSize() == shape[0] * shape[1] * shape[2] * shape[3];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getShape()[3] == shape[3];
      assert data.getElementType() == float.class;

      /* Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert TestUtils.close(val, 100*(i+origin[0]) + 10*j + k) : val;
        } */

  }

  private void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {1, 11, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {3, 2, 3, 2} );
    testReadSlice( ncfile, new int[] {3, 5, 0, 0}, new int[] {1, 5, 3, 4} );
    testReadSlice( ncfile, new int[] {3, 9, 0, 0}, new int[] {5, 2, 2, 3} );
   }
}

