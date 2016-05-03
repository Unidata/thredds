package thredds.server.ncss.controller.grid;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.params.GridAsPointDataParameters;
import thredds.mock.params.GridPathParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.exception.OutOfBoundariesException;
import thredds.server.ncss.exception.UnsupportedResponseFormatException;
import thredds.server.ncss.exception.VariableNotContainedInDatasetException;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridAsPointRequestExceptionsTest {
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	private RequestBuilder requestBuilder;
	private String pathInfo;
	
	@Parameters
	public static Collection<String[]> getTestParameters(){		
		return Arrays.asList( new String[][]{{GridAsPointDataParameters.getPathInfo().get(1)}});
	}
	
	@Before
	public void setUp() throws IOException{
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}
	
	public GridAsPointRequestExceptionsTest(String pathInfo){
		this.pathInfo = GridPathParams.getPathInfo().get(0);
	}
	
	
	//@Test(expected=UnsupportedResponseFormatException.class)
	@Test
	public void testUnsupportedResponseFormatException() throws Exception{

		requestBuilder = MockMvcRequestBuilders.get(pathInfo).servletPath(pathInfo)
				.param("latitude", "42.02" )
				.param("longitude", "-105.0" )
				.param("var", "Relative_humidity_height_above_ground","Temperature")
				.param("vertCoord", "300")
				.param("accept", "unsupported")
				.param("time_start", "2012-04-18T12:00:00.000Z")
				.param("time_duration", "PT18H");
		
		this.mockMvc.perform(requestBuilder).andExpect(new ResultMatcher(){
			public void match(MvcResult result) throws Exception{
				Exception ex =  result.getResolvedException();
				assertTrue( ex instanceof  UnsupportedResponseFormatException);
			}
		} );
		
	}	
	
//	@Test(expected=VariableNotContainedInDatasetException.class)
	@Test
	public void testVariableNotContainedInDatasetException() throws Exception{
		
		requestBuilder = MockMvcRequestBuilders.get(pathInfo).servletPath(pathInfo)
				.param("latitude", "42.02" )
				.param("longitude", "-105.0" )
				.param("var", "VAR_7-0-2-11_L100,wrong_var")
				.param("accept", "text/csv")
				.param("time_start", "2012-04-18T12:00:00.000Z")
				.param("time_duration", "PT18H");
		
		this.mockMvc.perform(requestBuilder).andExpect(new ResultMatcher(){
			public void match(MvcResult result) throws Exception{
				Exception ex =  result.getResolvedException();
				assert ( ex instanceof  VariableNotContainedInDatasetException) : ex.getMessage();
			}
		} );

	}
	
//	@Test(expected=OutOfBoundariesException.class)
	@Test
	public void testOutOfBoundariesException() throws Exception{

		requestBuilder = MockMvcRequestBuilders.get(pathInfo).servletPath(pathInfo)
				.param("latitude", "16.74" )
				.param("longitude", "-105.0" )
				.param("var", "Temperature")
				.param("accept", "text/csv")
				.param("time_start", "2012-04-18T12:00:00.000Z")
				.param("time_duration", "PT18H");
		
		this.mockMvc.perform(requestBuilder).andExpect(new ResultMatcher(){
			public void match(MvcResult result) throws Exception{
				Exception ex =  result.getResolvedException();
        assert ex != null;
				assert ( ex instanceof OutOfBoundariesException) : ex.getMessage();
			}
		} );
		
	}	
	 
}
