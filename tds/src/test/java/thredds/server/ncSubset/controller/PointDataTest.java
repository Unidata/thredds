package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
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

import thredds.mock.params.PointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class PointDataTest {

	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;		
	private RequestBuilder requestBuilder;
	
	private List<String> vars;
	private String accept;
	
	private LatLonPoint point;
	private String pathInfo;
	
	//private PointDataRequestParamsBean params;
	//private BindingResult result;
	//private PointDataController pointDataController;
	
	@Parameters
	public static List<Object[]> getTestParameters(){
		
	
		return Arrays.asList(new Object[][]{  
				{SupportedFormat.NETCDF3, PointDataParameters.getVars().get(0) , PointDataParameters.getPathInfo().get(0), PointDataParameters.getPoints().get(0), PointDataParameters.getVerticalLevels().get(0) },
				{SupportedFormat.NETCDF3, PointDataParameters.getVars().get(1) , PointDataParameters.getPathInfo().get(1), PointDataParameters.getPoints().get(1), PointDataParameters.getVerticalLevels().get(1) },
				{SupportedFormat.NETCDF3, PointDataParameters.getVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF3, PointDataParameters.getVars().get(3) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				
				{SupportedFormat.NETCDF4, PointDataParameters.getVars().get(0) , PointDataParameters.getPathInfo().get(0), PointDataParameters.getPoints().get(0), PointDataParameters.getVerticalLevels().get(0) },
				{SupportedFormat.NETCDF4, PointDataParameters.getVars().get(1) , PointDataParameters.getPathInfo().get(1), PointDataParameters.getPoints().get(1), PointDataParameters.getVerticalLevels().get(1) },
				{SupportedFormat.NETCDF4, PointDataParameters.getVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF4, PointDataParameters.getVars().get(3) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) }				

		});				
	}
	
	public PointDataTest(SupportedFormat format,  List<String> vars, String pathInfo, LatLonPoint point, Double verticalLevel){
		
		this.vars = vars;
		this.pathInfo = pathInfo;
		this.point=point;
		this.accept = format.getAliases().get(0);
	}
	
	@Before
	public void setUp() throws IOException{
		
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();	
		String servletPath = AbstractNcssDataRequestController.servletPath+pathInfo;		
		
		//Creates values for param var
		Iterator<String> it = vars.iterator();
		String varParamVal = it.next();
		while(it.hasNext()){
			String next = it.next();
			varParamVal =varParamVal+","+next;
		}
		
		//Values for time subsetting
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gds, vars);
		List<CalendarDate> dates = gridAsPointDataset.getDates();		
		Random rand = new Random();
		int randInt =     rand.nextInt( dates.size());
		int randIntNext = rand.nextInt(dates.size());
		int start = Math.min(randInt, randIntNext);
		int end = Math.max(randInt, randIntNext);				
		String startDate= dates.get(start).toString();
		String endDate= dates.get(end).toString();
		
		requestBuilder = MockMvcRequestBuilders.get(servletPath).servletPath(servletPath)
				.param("var", varParamVal)
				.param("latitude", String.valueOf(point.getLatitude()) )
				.param("longitude", String.valueOf(point.getLongitude() ) )
				.param("time_start", startDate)
				.param("time_end", endDate)
				.param("accept", accept);		
		
	}
	
	
	
	@Test
	public void shouldGetData() throws Exception{	
		this.mockMvc.perform(requestBuilder)
		.andExpect(MockMvcResultMatchers.status().isOk());
	}	
}
