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

import org.junit.*;
import ucar.nc2.*;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.ma2.*;
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class TestAggSynGrid {

  static GridDataset gds = null;
  static final String filename = "file:./" + TestNcML.topDir + "aggSynGrid.xml";

  @BeforeClass
  public static void setUp() throws IOException {
    if (gds != null) return;
    gds = ucar.nc2.dt.grid.GridDataset.open(filename);
    System.out.printf("open %s%n", filename);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    if (gds != null) gds.close();
    gds = null;
  }

  @Test
  public void testGrid() {
    GridDatatype grid = gds.findGridDatatype("T");
    assert null != grid;
    assert grid.getFullName().equals("T");
    assert grid.getRank() == 3;
    assert grid.getDataType() == DataType.DOUBLE;

    GridCoordSystem gcsys = grid.getCoordinateSystem();
    assert gcsys.getYHorizAxis() != null;
    assert gcsys.getXHorizAxis() != null;
    assert gcsys.getTimeAxis() != null;

    CoordinateAxis1DTime taxis = gcsys.getTimeAxis1D();
    assert taxis.getDataType() == DataType.STRING : taxis.getDataType();

    List names = taxis.getNames();
    java.util.Date[] dates = taxis.getTimeDates();
    assert dates != null;
    for (int i = 0; i < dates.length; i++) {
      Date d = dates[i];
      ucar.nc2.util.NamedObject name = (ucar.nc2.util.NamedObject) names.get(i);
      System.out.println(name.getName() + " == " + d);
    }
  }

  @Test
  public void testDimensions() {
    NetcdfFile ncfile = gds.getNetcdfFile();

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
    assert timeDim.getLength() == 3;
  }

  @Test
  public void testCoordVar() {
    NetcdfFile ncfile = gds.getNetcdfFile();
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

    try {
      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      assert Misc.closeEnough(dataI.getDoubleNext(), 41.0);
      assert Misc.closeEnough(dataI.getDoubleNext(), 40.0);
      assert Misc.closeEnough(dataI.getDoubleNext(), 39.0);
    } catch (IOException io) {
    }

  }

  @Test
  public void testAggCoordVar() throws IOException {
    NetcdfFile ncfile = gds.getNetcdfFile();
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.STRING : time.getDataType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    int count = 0;
    String[] want = new String[]{"2005-11-22 22:19:53Z", "2005-11-22 23:19:53Z", "2005-11-23 00:19:59Z"};
    Array data = time.read();
    assert (data instanceof ArrayObject);
    while (data.hasNext())
      assert want[count++].equals(data.next());

  }

  @Test
  public void testReadData() {
    NetcdfFile ncfile = gds.getNetcdfFile();
    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getShortName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 36 : v.getSize();
    assert v.getShape()[0] == 3;
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
      assert data.getSize() == 36;
      assert data.getShape()[0] == 3;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      int[] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++)
          for (int k = 0; k < shape[2]; k++) {
            double val = data.getDouble(tIndex.set(i, j, k));
            // System.out.println(" "+val);
            assert Misc.closeEnough(val, 100 * i + 10 * j + k) : val;
          }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void readSlice(int[] origin, int[] shape) {
    NetcdfFile ncfile = gds.getNetcdfFile();
    Variable v = ncfile.findVariable("T");

    try {
      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == shape[0] * shape[1] * shape[2];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] + " " + shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getElementType() == double.class;

      Index tIndex = data.getIndex();
      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++)
          for (int k = 0; k < shape[2]; k++) {
            double val = data.getDouble(tIndex.set(i, j, k));
            //System.out.println(" "+val);
            assert Misc.closeEnough(val, 100 * (i + origin[0]) + 10 * j + k) : val;
          }

    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice() {

    readSlice(new int[]{0, 0, 0}, new int[]{3, 3, 4});
    readSlice(new int[]{0, 0, 0}, new int[]{2, 3, 2});
    readSlice(new int[]{2, 0, 0}, new int[]{1, 3, 4});
    readSlice(new int[]{1, 0, 0}, new int[]{2, 2, 3});
  }

}

