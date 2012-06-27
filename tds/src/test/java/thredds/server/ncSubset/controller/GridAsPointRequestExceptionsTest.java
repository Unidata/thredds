package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import thredds.mock.params.PointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dt.GridDataset;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class GridAsPointRequestExceptionsTest {
	
	@Autowired
	PointDataController pointDataController;
	
	private String pathInfo;
	
	@Parameters
	public static Collection<String[]> getTestParameters(){
		
		
		return Arrays.asList( new String[][]{{PointDataParameters.getPathInfo().get(1)}});
	}
	
	@Before
	public void setUp() throws IOException{
		
		//We get all the TdsContext initialized so we could  get an opened grid dataset 
		pointDataController.setGridDataset(DatasetHandlerAdapter.openGridDataset(pathInfo));
		
		
	}
	
	public GridAsPointRequestExceptionsTest(String pathInfo){
		
		this.pathInfo = pathInfo;
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testUnsupportedOperationException() throws Exception{

		//All parameters get the controller validated so set good values 
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		//No vert. levels
		//params.setVar( Arrays.asList("VAR_7-0-2-2_L102","VAR_7-0-2-1_L1","VAR_7-0-2-156_L1") );
		//Different vert levels
		params.setVar( Arrays.asList("Relative_humidity_height_above_ground","Temperature"));
		params.setPoint(true);
		params.setVertCoord(300.0);
		params.setAccept("text/csv");
		params.setTime_start("2012-04-18T12:00:00.000Z");
		params.setTime_duration("PT18H");
		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		
		pointDataController.getPointData(params, result, new MockHttpServletResponse());

	}
	
	@Test(expected=UnsupportedResponseFormatException.class)
	public void testUnsupportedResponseFormatException() throws Exception{

		//All parameters get the controller validated so set good values 
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		//No vert. levels
		//params.setVar( Arrays.asList("VAR_7-0-2-2_L102","VAR_7-0-2-1_L1","VAR_7-0-2-156_L1") );
		//Different vert levels
		params.setVar( Arrays.asList("Relative_humidity_height_above_ground","Temperature"));
		params.setPoint(true);
		params.setVertCoord(300.0);
		params.setAccept("invalidformat");
		params.setTime_start("2012-04-18T12:00:00.000Z");
		params.setTime_duration("PT18H");
		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		
		pointDataController.getPointData(params, result, new MockHttpServletResponse());

	}	
	
	@Test(expected=VariableNotContainedInDatasetException.class)
	public void testVariableNotContainedInDatasetException() throws Exception{

		//All parameters got the controller validated so set good values 
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		//No vert. levels
		//params.setVar( Arrays.asList("VAR_7-0-2-2_L102","VAR_7-0-2-1_L1","VAR_7-0-2-156_L1") );
		//Same vert levels
		params.setVar( Arrays.asList("VAR_7-0-2-11_L100","wrong_var"));
		params.setPoint(true);
		params.setVertCoord(null);
		params.setAccept("text/csv");
		params.setTime_start("2012-04-18T12:00:00.000Z");
		params.setTime_duration("PT18H");
		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		
		pointDataController.getPointData(params, result, new MockHttpServletResponse());

	}
	
	@Test(expected=OutOfBoundariesException.class)
	public void testOutOfBoundariesException() throws Exception{

		//All parameters got the controller validated so set good values 
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(16.74);
		params.setLongitude(-105.0);		
		//params.setVar( Arrays.asList("VAR_7-0-2-2_L102","VAR_7-0-2-1_L1","VAR_7-0-2-156_L1") );
		//Same vert levels
		params.setVar( Arrays.asList("Temperature"));
		params.setPoint(true);
		params.setVertCoord(null);
		params.setAccept("text/csv");
		params.setTime_start("2012-04-18T12:00:00.000Z");
		params.setTime_duration("PT18H");
		
		BindingResult result = new BeanPropertyBindingResult(params, "params");
		
		pointDataController.getPointData(params, result, new MockHttpServletResponse());

	}
	
	@After
	public void tearDown() throws IOException{
		
		GridDataset gds = pointDataController.getGridDataset();
		gds.close();		
		gds = null;
		pointDataController =null;
		
	}	

}
