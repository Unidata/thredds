/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestTDSselect.java 51 2006-07-12 17:13:13Z caron $

package thredds.server.opendap;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * miscellaneous opendap tests
 */
@Category(NeedsCdmUnitTest.class)
public class TestOpendapMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // @Ignore("Fails because we dont have alias subst in ncml")
  @Test
  public void testAliasSubst() throws IOException {
    String url = TestOnLocalServer.withHttpPath("/dodsC/ExampleNcML/Modified.nc");
    try (NetcdfDataset dodsfile = NetcdfDataset.openDataset(url)) {
      System.out.printf("OK %s%n", dodsfile.getLocation());
    }
  }

  @Test
  public void testStrings() throws IOException, InvalidRangeException {
    String url = TestOnLocalServer.withHttpPath("/dodsC/scanLocal/testWrite.nc");
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
    String url = TestOnLocalServer.withHttpPath("/dodsC/gribCollection/GFS_CONUS_80km/Best");
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

  @Test
  public void testByteAttribute() throws IOException {
    String filename = TestOnLocalServer.withHttpPath("dodsC/scanCdmUnitTests/ft/stationProfile/PROFILER_wind_06min_20091030_2330.nc");
    NetcdfDataset ncd = NetcdfDataset.openDataset(filename, true, null);
    assert ncd != null;
    VariableDS v = (VariableDS) ncd.findVariable("uvQualityCode");
    assert v != null;
    assert v.hasMissing();

    int count = 0;
    Array data = v.read();
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      byte val = ii.getByteNext();
      if (v.isMissing(val)) count++;
      if (val == (byte)-1)
        assert v.isMissing(val);
    }
    System.out.println("size = "+v.getSize()+" missing= "+count);
  }
}
