package thredds.server.ncss.controller.grid;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.params.GridAsPointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.controller.AbstractNcssController;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.server.ncss.dataservice.DatasetHandlerAdapter;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext-tdsConfig.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridAsPointDataTest {


  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;
  private RequestBuilder requestBuilder;

  private List<String> vars;
  private String accept;

  private LatLonPoint point;
  private String pathInfo;

  @Parameters
  public static List<Object[]> getTestParameters() {


    return Arrays.asList(new Object[][]{
      {SupportedFormat.NETCDF3, GridAsPointDataParameters.getVars().get(0), GridAsPointDataParameters.getPathInfo().get(0), GridAsPointDataParameters.getPoints().get(0), GridAsPointDataParameters.getVerticalLevels().get(0)},
      {SupportedFormat.NETCDF3, GridAsPointDataParameters.getVars().get(1), GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1), GridAsPointDataParameters.getVerticalLevels().get(1)},
      {SupportedFormat.NETCDF3, GridAsPointDataParameters.getVars().get(2), GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2)},
      {SupportedFormat.NETCDF3, GridAsPointDataParameters.getVars().get(3), GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2)},

      {SupportedFormat.NETCDF4, GridAsPointDataParameters.getVars().get(0), GridAsPointDataParameters.getPathInfo().get(0), GridAsPointDataParameters.getPoints().get(0), GridAsPointDataParameters.getVerticalLevels().get(0)},
      {SupportedFormat.NETCDF4, GridAsPointDataParameters.getVars().get(1), GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1), GridAsPointDataParameters.getVerticalLevels().get(1)},
      {SupportedFormat.NETCDF4, GridAsPointDataParameters.getVars().get(2), GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2)},
      {SupportedFormat.NETCDF4, GridAsPointDataParameters.getVars().get(3), GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2)}

    });
  }

  public GridAsPointDataTest(SupportedFormat format, List<String> vars, String pathInfo, LatLonPoint point, Double verticalLevel) {

    this.vars = vars;
    this.pathInfo = pathInfo;
    this.point = point;
    this.accept = format.getAliases().get(0);
  }

  @Before
  public void setUp() throws IOException {

    mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    String servletPath = pathInfo;

    //Creates values for param var
    Iterator<String> it = vars.iterator();
    String varParamVal = it.next();
    while (it.hasNext()) {
      String next = it.next();
      varParamVal = varParamVal + "," + next;
    }
    System.out.printf("path=%s vars=%s point=%s%n", pathInfo, varParamVal, point);

    //Values for time subsetting
    String datasetPath = AbstractNcssController.getDatasetPath(this.pathInfo);
    GridDataset gds = DatasetHandlerAdapter.openGridDataset(datasetPath);
    GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gds, vars);
    List<CalendarDate> dates = gridAsPointDataset.getDates();
    Random rand = new Random();
    int randInt = rand.nextInt(dates.size());
    int randIntNext = rand.nextInt(dates.size());
    int start = Math.min(randInt, randIntNext);
    int end = Math.max(randInt, randIntNext);
    String startDate = dates.get(start).toString();
    String endDate = dates.get(end).toString();
    gds.close();

    requestBuilder = MockMvcRequestBuilders.get(servletPath).servletPath(servletPath)
            .param("var", varParamVal)
            .param("latitude", String.valueOf(point.getLatitude()))
            .param("longitude", String.valueOf(point.getLongitude()))
            .param("time_start", startDate)
            .param("time_end", endDate)
            .param("accept", accept);

  }


  @Test
  public void shouldGetData() throws Exception {
    this.mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk());
  }
}
