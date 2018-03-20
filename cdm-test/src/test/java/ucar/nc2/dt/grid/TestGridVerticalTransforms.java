/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.grid;

import junit.framework.TestCase;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Section;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestGridVerticalTransforms extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestGridVerticalTransforms(String name) {
    super(name);
  }

  public void testWRF() throws Exception {
    testDataset( TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc");
    testDataset( TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_d01_2006-03-08_21-00-00");
  }

  private void testDataset( String location) throws IOException, InvalidRangeException {
    ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open(location);
    assert dataset != null;

    testGrid( dataset.findGridByName("U"));
    testGrid( dataset.findGridByName("V"));
    testGrid( dataset.findGridByName("W"));
    testGrid( dataset.findGridByName("T"));

    dataset.close();
  }

  private void testGrid( GeoGrid grid) throws IOException, InvalidRangeException {
    assert null != grid;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    Array data = grid.readDataSlice(0, -1, -1, -1);
    assert data.getRank() == 3;

    CoordinateAxis zaxis = gcs.getVerticalAxis();
    assert data.getShape()[0] == zaxis.getSize() : zaxis.getSize();

    CoordinateAxis yaxis = gcs.getYHorizAxis();
    assert data.getShape()[1] == yaxis.getSize() : yaxis.getSize();

    CoordinateAxis xaxis = gcs.getXHorizAxis();
    assert data.getShape()[2] == xaxis.getSize() : xaxis.getSize();

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;
    assert vt.getUnitString() != null;

    ucar.ma2.ArrayDouble.D3 vcoord = vt.getCoordinateArray(0);
    assert vcoord.getShape()[0] ==  zaxis.getSize() : vcoord.getShape()[0];
    assert vcoord.getShape()[1] ==  yaxis.getSize() : vcoord.getShape()[1];
    assert vcoord.getShape()[2] ==  xaxis.getSize() : vcoord.getShape()[2];
  }


/*  The 3D coordinate array does not return correct shape and values. Just running this simple code to get z values..

url=http://coast-enviro.er.usgs.gov/models/share/erie_test.ncml;
var='temp';

z is of shape 20x2x87, it should be 20x87x193.
*/

  public void testErie() throws IOException, InvalidRangeException {
    String uri = TestDir.cdmUnitTestDir + "transforms/erie_test.ncml";
    String var = "temp";

    GridDataset ds = GridDataset.open(uri);
    GeoGrid grid = ds.findGridByName(var);
    Section s = new Section(grid.getShape());
    System.out.printf("var = %s %n", s);

    GridCoordSystem gcs = grid.getCoordinateSystem();
    VerticalTransform vt = gcs.getVerticalTransform();
    ArrayDouble.D3 z = vt.getCoordinateArray(0);
    Section sv = new Section(z.getShape());
    System.out.printf("3dcoord = %s %n", sv);

    s = s.removeRange(0);
    assert s.equals(sv);
  }

}
