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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.params.GridPathParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.format.SupportedFormat;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class TemporalSpaceSubsettingTest {
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;		
	private MockHttpServletRequestBuilder requestBuilder;
	
	private String pathInfo;
	private int lengthTimeDim; //Expected time dimension length
	
	@Parameters
	public static Collection<Object[]> getTestParameters(){
				
		
		return Arrays.asList( new Object[][]{
				{ SupportedFormat.NETCDF3,  1, GridPathParams.getPathInfo().get(4), null , null, null, null, null, null }, //No time subset provided
				{ SupportedFormat.NETCDF3, 6, GridPathParams.getPathInfo().get(3), "all", null, null, null, null, null }, //Requesting all
				{ SupportedFormat.NETCDF3, 1, GridPathParams.getPathInfo().get(0), ""   , "2012-04-19T12:00:00.000Z", null, null, null, null }, //Single time on singleDataset
				{ SupportedFormat.NETCDF3, 1, GridPathParams.getPathInfo().get(0), ""   , "2012-04-19T15:30:00.000Z", "PT3H", null, null, null }, //Single time in range with time_window
				{ SupportedFormat.NETCDF3, 6, GridPathParams.getPathInfo().get(3), ""   , null, null, "2012-04-18T12:00:00.000Z", "2012-04-19T18:00:00.000Z", null }, //Time series on Best time series
				{ SupportedFormat.NETCDF3, 5, GridPathParams.getPathInfo().get(3), ""   , null, null, "2012-04-18T12:00:00.000Z", null, "PT24H" }, //Time series on Best time series
		    { SupportedFormat.NETCDF4,  1, GridPathParams.getPathInfo().get(4), null , null, null, null, null, null }, //No time subset provided
				{ SupportedFormat.NETCDF4, 6, GridPathParams.getPathInfo().get(3), "all", null, null, null, null, null }, //Requesting all
				{ SupportedFormat.NETCDF4, 1, GridPathParams.getPathInfo().get(0), ""   , "2012-04-19T12:00:00.000Z", null, null, null, null }, //Single time on singleDataset
				{ SupportedFormat.NETCDF4, 1, GridPathParams.getPathInfo().get(0), ""   , "2012-04-19T15:30:00.000Z", "PT3H", null, null, null }, //Single time in range with time_window
				{ SupportedFormat.NETCDF4, 6, GridPathParams.getPathInfo().get(3), ""   , null, null, "2012-04-18T12:00:00.000Z", "2012-04-19T18:00:00.000Z", null }, //Time series on Best time series
				{ SupportedFormat.NETCDF4, 5, GridPathParams.getPathInfo().get(3), ""   , null, null, "2012-04-18T12:00:00.000Z", null, "PT24H" } //Time series on Best time series
				
			});
	}
	
	public TemporalSpaceSubsettingTest(SupportedFormat format, int expectedLengthTimeDim, String pathInfoForTest,String temporal, String time, String time_window, String time_start, String time_end, String time_duration){
		lengthTimeDim = expectedLengthTimeDim;
		pathInfo = pathInfoForTest;
		String servletPath = pathInfo;
		
		requestBuilder = MockMvcRequestBuilders.get(servletPath).servletPath(servletPath)
				.param("accept", format.getAliases().get(0))
				.param("temporal", temporal)
				.param("time", time)
				.param("time_window", time_window)
				.param("time_duration", time_duration)
				.param("time_start", time_start)
				.param("time_end", time_end)
				.param("var", "Temperature");


	}
	
	@Before
	public void setUp() throws IOException{				
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();				
	}
	
	@Test
	public void shouldGetTimeRange() throws Exception{
		
		
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();

    Formatter f = new Formatter();
    MockHttpServletRequest req = mvc.getRequest();
    Enumeration<String> params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = params.nextElement();
      f.format(" %s=%s%n", name, req.getParameter(name));
    }
    System.out.printf("%s%n%s%n", req.getRequestURI(), f);


		//Open the binary response in memory
		NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", mvc.getResponse().getContentAsByteArray() );
		NetcdfDataset ds = new NetcdfDataset(nf);
		Dimension time =ds.findDimension("time");
		
		assertEquals( lengthTimeDim, time.getLength());
						
	}
		
}
