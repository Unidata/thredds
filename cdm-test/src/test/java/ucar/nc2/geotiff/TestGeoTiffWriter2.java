/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geotiff;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoTiffWriter2 writing geotiffs
 *
 * @author caron
 * @since 7/31/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGeoTiffWriter2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();
  static public String topdir = TestDir.cdmUnitTestDir;

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{topdir + "formats/dmsp/F14200307192230.n.OIS", "infraredImagery", new LatLonRect(new LatLonPointImpl(-5, -52.0), new LatLonPointImpl(25, -20.0))});

    // this fails
    //result.add(new Object[]{topdir + "formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4", "surf_salt_flux", new LatLonRect(new LatLonPointImpl(43, 141), 5, 5)});

    return result;
  }

  String filename, field;
  LatLonRect llbb;

  public TestGeoTiffWriter2(String filename, String field, LatLonRect llbb) {
    this.filename = filename;
    this.field = field;
    this.llbb = llbb;
  }

  @Test
  public void testWrite() throws IOException {
    String fileOut = tempFolder.newFile().getAbsolutePath();

    try (GeoTiffWriter2 writer = new GeoTiffWriter2(fileOut)) {
      writer.writeGrid(filename, field, 0, 0, true, llbb);
    }

    // read it back in
    try (GeoTiff geotiff = new GeoTiff(fileOut)) {
      geotiff.read();
      System.out.println("geotiff read in = " + geotiff.showInfo());
      //geotiff.testReadData();
    }
  }
}
