/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncss.controller.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.web.MockTdsContextLoader;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.ma2.Array;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridCoverageSubsettingTest {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Parameters
  public static Collection<Object[]> getTestParameters() {

    return Arrays.asList(new Object[][]{
            //			{ new int[]{1,2,2} , "/ncss/grid/scanCdmUnitTests/ft/grid/GFS_Global_onedeg_20081229_1800.grib2.nc", "Pressure_surface", true, 50.0, -10.0, 50, 110 }, //No vertical levels
            //			{ new int[]{1,2,2} , "/ncss/grid/scanCdmUnitTests/tds/ncep/RR_CONUS_13km_20121028_0000.grib2", "Pressure_surface", true, 45.0, 15.0, -120, -90 }, //No vertical levels
            {"/ncss/grid/scanCdmUnitTests/tds/ncep/RR_CONUS_13km_20121028_0000.grib2", "Pressure_surface", false, 700.0, 2700.0, -2000, 666,
                    new Expected(new int[]{1, 149, 198}, new ProjectionRect(-2004.745, 697.882, 663.620, 2702.542)) },
            {"/ncss/grid/scanCdmUnitTests/tds/ncep/RR_CONUS_13km_20121028_0000.grib2", "Pressure_surface", false, 0, 4000.0, 1234, 4000,
                    new Expected(new int[]{1, 294, 114}, new ProjectionRect(1232.509766, -6.457754, 2763.094727, 3962.227295)) },
            {"/ncss/grid/scanCdmUnitTests/tds/ncep/RR_CONUS_13km_20121028_0000.grib2", "Pressure_surface", false, -4000, 444, 1234, 4000,
                    new Expected(new int[]{1, 77, 114}, new ProjectionRect(1232.510, -588.893, 2763.095, 440.527)) },
            {"/ncss/grid/scanCdmUnitTests/tds/ncep/RR_CONUS_13km_20121028_0000.grib2", "Pressure_surface", false, 2000, 4000, -4000, -833.102753,
                    new Expected(new int[]{1, 146, 186}, new ProjectionRect(-3332.155, 1998.202, -826.330, 3962.227)) },
    });
  }

  private static class Expected {
    int[] shape;
    ProjectionRect rect;

    public Expected(int[] shape, ProjectionRect rect) {
      this.shape = shape;
      this.rect = rect;
    }
  }

  @Before
  public void setUp() throws IOException {
    mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  private static AtomicInteger count = new AtomicInteger();

  ////////////////////////////////////////////////////////
  private String pathInfo;
  private Expected expect;
  private String vars;
  private double north, south, east, west;
  private boolean isLatLon;

  public GridCoverageSubsettingTest(String pathInfo, String vars, boolean isLatLon, double south, double north, double west, double east, Expected expect) {
    this.pathInfo = pathInfo;
    this.vars = vars;
    this.north = north;
    this.south = south;
    this.east = east;
    this.west = west;
    this.expect = expect;
  }

  @Test
  public void shouldSubsetGrid() throws Exception {
    if (isLatLon)
      System.out.printf("path=%s lat=[%f,%f] lon=[%f,%f]%n", pathInfo, south, north, west, east);
    else
      System.out.printf("path=%s y=[%f,%f] x=[%f,%f]%n", pathInfo, north, south, west, east);


    RequestBuilder requestBuilder;
    if (isLatLon)
      requestBuilder = MockMvcRequestBuilders.get(pathInfo).servletPath(pathInfo)
              .param("var", vars)
              .param("north", Double.valueOf(north).toString())
              .param("south", Double.valueOf(south).toString())
              .param("east", Double.valueOf(east).toString())
              .param("west", Double.valueOf(west).toString());
    else
      requestBuilder = MockMvcRequestBuilders.get(pathInfo).servletPath(pathInfo)
              .param("var", vars)
              .param("maxy", Double.valueOf(north).toString())
              .param("miny", Double.valueOf(south).toString())
              .param("maxx", Double.valueOf(east).toString())
              .param("minx", Double.valueOf(west).toString());

    System.out.printf("%n%s vars=%s%n", pathInfo, vars);

    MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
    if (mvc.getResponse().getStatus() != 200)
      System.out.printf("BAD %d == %s%n", mvc.getResponse().getStatus(), mvc.getResponse().getContentAsString());
    assertEquals(200, mvc.getResponse().getStatus());

    // Save the result
    String fileOut = TestDir.temporaryLocalDataDir + "GridCoverageSubsettingTest" + count.incrementAndGet() + ".nc";
    System.out.printf("Write to %s%n", fileOut);
    try (FileOutputStream fout = new FileOutputStream(fileOut)) {
      ByteArrayInputStream bis = new ByteArrayInputStream(mvc.getResponse().getContentAsByteArray());
      IO.copy(bis, fout);
    }

    // Open the binary response in memory
    NetcdfFile nf = NetcdfFile.open(fileOut);
    System.out.printf("%s%n", nf);
    Variable v = nf.findVariable(null, "x");
    assert v != null;
    Array x = v.read();
    NCdumpW.printArray(x);
    System.out.printf("%n");
    v = nf.findVariable(null, "y");
    assert v != null;
    Array y = v.read();
    NCdumpW.printArray(y);
    System.out.printf("%n");

    int nx = (int) x.getSize();
    int ny = (int) y.getSize();
    ProjectionRect prect = new ProjectionRect(x.getDouble(0), y.getDouble(0), x.getDouble(nx - 1), y.getDouble(ny-1));

    if (expect != null) {
      v = nf.findVariable(null, vars);
      System.out.printf("v.getShape()=%s%n", Misc.showInts(v.getShape()));
      assertArrayEquals(expect.shape, v.getShape());

      System.out.printf("Expected ProjectionRect=%s%n", expect.rect.toString2(6));
      System.out.printf("Actual ProjectionRect=%s%n", prect.toString2(6));
      assertTrue(expect.rect.closeEnough(prect));
    }

    //ucar.nc2.dt.grid.GridDataset gdsDataset = new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));
    //assertTrue( gdsDataset.getCalendarDateRange().isPoint());

		/* int[][] shapes = new int[vars.size()][];
    int count = 0;
			GeoGrid grid = gdsDataset.findGridByShortName(varName);
			shapes[count++] = grid.getShape();
		assertArrayEquals(expectedShapes, shapes);  */
  }


}
