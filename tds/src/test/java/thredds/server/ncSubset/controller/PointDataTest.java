package thredds.server.ncSubset.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import thredds.mock.params.PointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class PointDataTest {

	private List<String> vars;
	private String accept;
	
	private LatLonPoint point;
	private String pathInfo;
	
	private PointDataRequestParamsBean params;
	private BindingResult result;
	private PointDataController pointDataController;
	
	@Parameters
	public static List<Object[]> getTestParameters(){
		
		return Arrays.asList(new Object[][]{  
				{PointDataParameters.getVars().get(0) , PointDataParameters.getPathInfo().get(0), PointDataParameters.getPoints().get(0), PointDataParameters.getVerticalLevels().get(0) },
				//{PointDataParameters.getVars().get(1) , PointDataParameters.getPathInfo().get(1), PointDataParameters.getPoints().get(1), PointDataParameters.getVerticalLevels().get(1) },
				//{PointDataParameters.getVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				//{PointDataParameters.getVars().get(3) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) }

		});				
	}
	
	public PointDataTest(List<String> vars, String pathInfo, LatLonPoint point, Double verticalLevel){
		
		this.vars = vars;
		this.pathInfo = pathInfo;
		this.point=point;
	}
	
	@Before
	public void setUp() throws IOException{
		
		pointDataController = new PointDataController(); 
		pointDataController.setRequestPathInfo(pathInfo);
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		pointDataController.setGridDataset(gds );
		
		params = new PointDataRequestParamsBean();
		params.setVar(vars);
		params.setLatitude(point.getLatitude());
		params.setLongitude(point.getLongitude());
		GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gds, vars);
		List<CalendarDate> dates = gridAsPointDataset.getDates();		
		Random rand = new Random();
		int randInt =     rand.nextInt( dates.size());
		int randIntNext = rand.nextInt(dates.size());
		int start = Math.min(randInt, randIntNext);
		int end = Math.max(randInt, randIntNext);				
		String startDate= dates.get(start).toString();
		params.setTime_start(startDate);	
		String endDate= dates.get(end).toString();
		params.setTime_end(endDate);
		params.setPoint(true);		
		
	
		params.setAccept("csv");
		//params.setVertCoord(300.0);
		result = new BeanPropertyBindingResult(params, "params");
	}
	
	@After
	public void tearDown() throws IOException{
		
		pointDataController.getGridDataset().close();
		pointDataController.setGridDataset(null);
		pointDataController.setRequestPathInfo("");
	}
	
	
	@Test
	public void shouldGetData() throws ParseException, NcssException, IOException{
	
		//fail("No yet implemented");
		MockHttpServletResponse response = new MockHttpServletResponse();
		pointDataController.getPointData(params, result, response);
		assertEquals(200, response.getStatus());
	}
	
	
	
}
