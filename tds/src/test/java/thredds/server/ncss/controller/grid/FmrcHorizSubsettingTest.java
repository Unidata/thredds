/* Copyright */
package thredds.server.ncss.controller.grid;

import com.beust.jcommander.internal.Lists;
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
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.mock.params.GridDataParameters;
import thredds.mock.params.GridPathParams;
import thredds.mock.web.MockTdsContextLoader;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class FmrcHorizSubsettingTest {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @SpringJUnit4ParameterizedClassRunner.Parameters
  public static Collection<Object[]> getTestParameters() {

    return Arrays.asList(new Object[][]{
            {"/ncss/grid/testGFSfmrc/files/GFS_CONUS_80km_20120418_1200.nc", Lists.newArrayList("Pressure", "Pressure_reduced_to_MSL"),
                    new int[][]{{1, 2, 2}, {1, 2, 2}}, new ProjectionRect(-4226.106971141345, -832.6983183345455, -4126.106971141345, -732.6983183345455)}, //No vertical levels

            {"/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd", Lists.newArrayList("Relative_humidity_height_above_ground", "Temperature_height_above_ground"),
                    new int[][]{{1, 1, 16, 15}, {1, 1, 16, 15}}, new ProjectionRect(-600, -600, 600, 600)}, //Same vertical level (one level)

            {"/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd", Lists.newArrayList("Temperature", "Relative_humidity"),
                    new int[][]{{1, 29, 2, 93}, {1, 29, 2, 93}}, new ProjectionRect(-4226.106971141345, 4268.6456816654545, 3250.825028858655, 4368.6456816654545)}, //Same vertical level (multiple level)

            {"/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd", Lists.newArrayList("Pressure", "Temperature", "Relative_humidity_height_above_ground"),
                    new int[][]{{1, 2, 93}, {1, 29, 2, 93}, {1, 1, 2, 93}}, new ProjectionRect(-4226.106971141345, 4268.6456816654545, 3250.825028858655, 4368.6456816654545)}, //No vertical levels and vertical levels

            {"/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd", Lists.newArrayList("Relative_humidity_height_above_ground", "Temperature"),
                    new int[][]{{1, 1, 65, 93}, {1, 29, 65, 93}}, new ProjectionRect(-4264.248291015625, -872.8428344726562, 3293.955078125, 4409.772216796875)}, //Full extension

            {"/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd", Lists.newArrayList("Relative_humidity_height_above_ground", "Temperature"),
                    new int[][]{{1, 1, 11, 53}, {1, 29, 11, 53}}, new ProjectionRect(-4864.248291015625, -1272.8428344726562, 0, 0)}  //Intersection
    });
  }

  @Before
  public void setUp() throws IOException {
    mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  ////////////////////////////////////////////////////////
  private String pathInfo;
  private int[][] expectedShapes;
  private List<String> vars;
  private ProjectionRect prect;

  public FmrcHorizSubsettingTest(String pathInfo, List<String> vars, int[][] result, ProjectionRect prect) {
    this.expectedShapes = result;
    this.pathInfo = pathInfo;
    this.vars = vars;
    this.prect = prect;
  }

  @Test
  public void shouldSubsetGrid() throws Exception {
    System.out.printf("path=%s rect=%s%n", pathInfo, prect.toString2(4));

    Iterator<String> it = vars.iterator();
    String varParamVal = it.next();
    while (it.hasNext()) {
      String next = it.next();
      varParamVal = varParamVal + "," + next;
    }

    RequestBuilder requestBuilder = MockMvcRequestBuilders.get(pathInfo).servletPath(pathInfo)
            .param("var", varParamVal)
            .param("minx", Double.toString(prect.getMinX()))
            .param("miny", Double.toString(prect.getMinY()))
            .param("maxx", Double.toString(prect.getMaxX()))
            .param("maxy", Double.toString(prect.getMaxY()));

    System.out.printf("Request=%s%n", requestBuilder);
    System.out.printf("%n%s vars=%s%n", pathInfo, varParamVal);

    MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
    SpatialSubsettingTest.showRequest(mvc.getRequest());
    assertEquals(200, mvc.getResponse().getStatus());

    // Open the binary response in memory
    try (NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", mvc.getResponse().getContentAsByteArray())) {
    /* System.out.printf("%s%n", nf);
    Variable v = nf.findVariable(null, "x");
    assert v != null;
    System.out.printf("x= ");
    NCdumpW.printArray(v.read());
    System.out.printf("%n");
    v = nf.findVariable(null, "y");
    assert v != null;
    System.out.printf("y= ");
    NCdumpW.printArray(v.read());
    System.out.printf("%n"); */

      ucar.nc2.dt.grid.GridDataset gdsDataset = new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));
      assertTrue(gdsDataset.getCalendarDateRange().isPoint());

      int count = 0;
      for (String varName : vars) {
        GeoGrid grid = gdsDataset.findGridByShortName(varName);
        System.out.printf("%s grid.getShape()=%s%n", varName, Misc.showInts(grid.getShape()));
        System.out.printf("%s        expected=%s%n", varName, Misc.showInts(expectedShapes[count]));
        assertArrayEquals(expectedShapes[count], grid.getShape());
        count++;
      }
    }
  }
}
