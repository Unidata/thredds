// $Id: TestTDSselect.java 51 2006-07-12 17:13:13Z caron $
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
package thredds.server.opendap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

/**
 * @author john
 */
@Category(NeedsCdmUnitTest.class)
public class TestSanity {

  @Ignore("ncml not doing alias subst")
  @Test
  public void testAliasSubst() throws IOException {
    String url = TestWithLocalServer.withPath("/dodsC/ExampleNcML/Modified.nc");
    try (NetcdfDataset dodsfile = NetcdfDataset.openDataset(url)) {
      System.out.printf("OK %s%n", dodsfile.getLocation());
    }
  }

  @Test
  public void testStrings() throws IOException, InvalidRangeException {
    String url = TestWithLocalServer.withPath("/dodsC/scanLocal/testWrite.nc");
    try (NetcdfDataset dodsfile = NetcdfDataset.openDataset(url)) {
      Variable v;

      // string
      v = dodsfile.findVariable("svar");
      assert (null != v);
      assert v.getFullName().equals("svar");
      assert v.getRank() == 1;
      assert v.getSize() == 80;
      assert v.getDataType() == DataType.CHAR : v.getDataType();

      Array a = v.read();
      assert a.getRank() == 1;
      assert a.getSize() == 80 : a.getSize();
      assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

      a = v.read("1:10");
      assert a.getRank() == 1;
      assert a.getSize() == 10 : a.getSize();
      assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

      // string array
      v = dodsfile.findVariable("names");
      assert (null != v);
      assert v.getFullName().equals("names");
      assert v.getRank() == 2;
      assert v.getSize() == 3 * 80;
      assert v.getDataType() == DataType.CHAR : v.getDataType();

      a = v.read();
      assert a.getRank() == 2;
      assert a.getSize() == 3 * 80 : a.getSize();
      assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

      a = v.read("0:1,1:10");
      assert a.getRank() == 2;
      assert a.getSize() == 2 * 10 : a.getSize();
      assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

    }
  }

  @Test
  public void testStridedSubsetSanityCheck() throws Exception {
    String url = TestWithLocalServer.withPath("/dodsC/gribCollection/GFS_CONUS_80km/Best");
    try (GridDataset dataset = GridDataset.open(url)) {
      System.out.printf("%s%n", dataset.getLocation());

      GeoGrid grid = dataset.findGridByName("u-component_of_wind_isobaric");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      // x and y stride 10
      GeoGrid grid_section = grid.subset(null, null, null, 1, 2, 2);
      Array data = grid_section.readDataSlice(0, -1, -1, -1);      // get first time slice
      assert data.getRank() == 3;
      // assert data.getShape()[0] == 6 : data.getShape()[0];
      assert data.getShape()[1] == 33 : data.getShape()[1];
      assert data.getShape()[2] == 47 : data.getShape()[2];

      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        float val = ii.getFloatNext();
        if (grid_section.isMissingData(val)) {
          if (!Float.isNaN(val)) {
            System.out.println(" got not NaN at =" + ii);
          }
          int[] current = ii.getCurrentCounter();
          if ((current[1] > 0) && (current[2] > 1)) {
            System.out.println(" got missing at =" + ii);
            System.out.println(current[1] + " " + current[2]);
          }
        }
      }
    }
  }
}
