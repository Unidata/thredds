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
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggDirDateFormat extends TestCase {
  private int ntimes = 8;
  public TestOffAggDirDateFormat( String name) {
    super(name);
  }

  public void testNcmlGrid() throws IOException {
    String filename = "file:"+ TestDir.cdmUnitTestDir + "formats/gini/aggDateFormat.ncml";

    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( filename);
    System.out.println(" TestNcmlAggDirDateFormat.openGrid "+ filename);

    NetcdfFile ncfile = gds.getNetcdfFile();
    testDimensions( ncfile);
    testAggCoordVar( ncfile);
    testReadData( gds);

    gds.close();
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("y");
    assert null != latDim;
    assert latDim.getShortName().equals("y");
    assert latDim.getLength() == 1008 : latDim.getLength() +"!="+ 1008;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("x");
    assert null != lonDim;
    assert lonDim.getShortName().equals("x");
    assert lonDim.getLength() == 1536;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == ntimes : timeDim.getLength() +"!="+ ntimes;
  }


  public void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == ntimes;
    assert time.getShape()[0] == ntimes;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == ntimes;
    assert data.getShape()[0] == ntimes;
    assert data.getElementType() == double.class;

    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      System.out.println(" coord="+dataI.getObjectNext());
  }

  public void testReadData(GridDataset gds) throws IOException {
    GridDatatype g = gds.findGridDatatype("IR_WV");
    assert null != g;
    assert g.getFullName().equals("IR_WV");
    assert g.getRank() == 3;
    assert g.getShape()[0] == ntimes;
    assert g.getShape()[1] == 1008;
    assert g.getShape()[2] == 1536;
    assert g.getDataType() == DataType.SHORT : g.getDataType();

    GridCoordSystem gsys = g.getCoordinateSystem();
    assert gsys.getXHorizAxis() != null;
    assert gsys.getYHorizAxis() != null;
    assert gsys.getTimeAxis() != null;
    assert gsys.getVerticalAxis() == null;
    assert gsys.getProjection() != null;

    Array data = g.readVolumeData(0);
    assert data.getRank() == 2;
    assert data.getShape()[0] == 1008;
    assert data.getShape()[1] == 1536;
    assert data.getElementType() == short.class;
  }


}


