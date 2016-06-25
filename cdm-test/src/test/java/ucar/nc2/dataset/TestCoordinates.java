// $Id: $
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

package ucar.nc2.dataset;


import junit.framework.*;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.StringReader;

/**
 * Test _Coordinates dataset in the JUnit framework.
 */

public class TestCoordinates extends TestCase {

  public TestCoordinates(String name) {
    super(name);
  }

  @Category(NeedsCdmUnitTest.class)
  public void testAlias() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/demeter/MM_cnrm_129_red.ncml";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    Variable v = ncd.findCoordinateAxis("number");
    assert v != null;
    //assert v.isCoordinateVariable();
    assert v instanceof CoordinateAxis1D;
    assert null != ncd.findDimension("ensemble");
    assert v.getDimension(0) == ncd.findDimension("ensemble");
    ncd.close();
  }


  // test offset only gets applied once
  @Category(NeedsCdmUnitTest.class)
  public void testWrapOnce() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncml/coords/testCoordScaling.ncml";
    System.out.printf("%s%n", filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    Variable v = ncd.findCoordinateAxis("Longitude");
    assert v != null;
    assert v instanceof CoordinateAxis1D;

    // if offset is applied twice, the result is not in +-180 range
    Array data = v.read();
    NCdumpW.printArray(data);
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      assert Math.abs(ii.getDoubleNext()) < 180 : ii.getDoubleCurrent();
    }
    ncd.close();
  }

  // from tom kunicki 1/3/11
  public void testTimeUnitErrorMessage() throws IOException, InvalidRangeException {
    String ncml =
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                    "  <dimension name='time' length='2' />\n" +
                    "  <dimension name='lat' length='3' />\n" +
                    "  <dimension name='lon' length='4' />\n" +
                    "  <dimension name='height' length='5' />\n" +
                    "  <attribute name='title' value='testSimpleTZYXGrid Data' />\n" +
                    "  <attribute name='Conventions' value='CF-1.4' />\n" +
                    "  <variable name='rh' shape='time height lat lon' type='int'>\n" +
                    "    <attribute name='long_name' value='relative humidity' />\n" +
                    "    <attribute name='units' value='percent' />\n" +
                    "    <attribute name='coordinates' value='lat lon' />\n" +
                    "    <values start='10' increment='5' />\n" +
                    "  </variable>\n" +
                    "  <variable name='time' shape='time' type='int'>\n" +
                    "    <attribute name='units' value='hours since 1970-01-01T12:00' />\n" +
                    "    <values start='1' increment='1' />\n" +
                    "  </variable>\n" +
                    "  <variable name='lat' shape='lat' type='float'>\n" +
                    "    <attribute name='units' value='degrees_north' />\n" +
                    "    <values start='40' increment='1' />\n" +
                    "  </variable>\n" +
                    "  <variable name='lon' shape='lon' type='float'>\n" +
                    "    <attribute name='units' value='degrees_east' />\n" +
                    "    <values start='-90' increment='1' />\n" +
                    "  </variable>\n" +
                    "  <variable name='height' shape='height' type='float'>\n" +
                    "    <attribute name='units' value='meters' />\n" +
                    "    <attribute name='positive' value='up'/>\n" +
                    "    <values start='1' increment='1' />\n" +
                    "  </variable>\n" +
                    "</netcdf>";

    String location = "testTimeUnitErrorMessage";
    System.out.println("testTimeUnitErrorMessage=\n" + ncml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);
    NetcdfDataset ncd = new NetcdfDataset(ncfile, true);
    System.out.printf("%n%s%n", ncd);

    VariableDS v = (VariableDS) ncd.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.INT;
    assert v instanceof CoordinateAxis;
    CoordinateAxis axis = (CoordinateAxis) v;
    assert axis.getAxisType() == AxisType.Time;
    
    GridDataset gd = new GridDataset(ncd, null);

    GeoGrid grid = (GeoGrid) gd.findGridByName("rh");
    assert grid != null;
    assert grid.getDataType() == DataType.INT;
    System.out.printf("%s%n", ncd);

    ncfile.close();
  }


}
