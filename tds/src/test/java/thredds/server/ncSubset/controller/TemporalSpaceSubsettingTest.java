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
package thredds.server.ncSubset.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import thredds.mock.params.PathInfoParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.exception.InvalidBBOXException;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.RequestTooLargeException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.GridDataRequestParamsBean;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class TemporalSpaceSubsettingTest {
	
	
	@Autowired
	private GridDataController gridDataController;
	
	private GridDataRequestParamsBean params;	
	private BindingResult validationResult;
	private MockHttpServletResponse response ;
	
	private String pathInfo;
	private int lengthTimeDim; //Expected time dimension length
	
	@Parameters
	public static Collection<Object[]> getTestParameters(){
		
		
		return Arrays.asList( new Object[][]{
				{ 1, PathInfoParams.getPatInfo().get(4), null , null, null, null, null, null }, //No time subset provided
				{ 6, PathInfoParams.getPatInfo().get(3), "all", null, null, null, null, null }, //Requesting all
				{ 1, PathInfoParams.getPatInfo().get(0), ""   , "2012-04-19T12:00:00.000Z", null, null, null, null }, //Single time on singleDataset
				{ 1, PathInfoParams.getPatInfo().get(0), ""   , "2012-04-19T15:30:00.000Z", "PT3H", null, null, null }, //Single time in range with time_window 
				{ 6, PathInfoParams.getPatInfo().get(3), ""   , null, null, "2012-04-18T12:00:00.000Z", "2012-04-19T18:00:00.000Z", null }, //Time series on Best time series
				{ 5, PathInfoParams.getPatInfo().get(3), ""   , null, null, "2012-04-18T12:00:00.000Z", null, "PT24H" } //Time series on Best time series
				
			});
	}
	
	public TemporalSpaceSubsettingTest(int expectedLengthTimeDim, String pathInfoForTest,String temporal, String time, String time_window, String time_start, String time_end, String time_duration){
		lengthTimeDim = expectedLengthTimeDim;
		pathInfo = pathInfoForTest;		
		params = new GridDataRequestParamsBean();
		params.setTemporal(temporal);
		params.setTime(time);
		params.setTime_window(time_window);
		params.setTime_duration(time_duration);
		params.setTime_start(time_start);
		params.setTime_end(time_end);
		params.setTime_duration(time_duration);
	}
	
	@Before
	public void setUp() throws IOException{
		
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		gridDataController.setGridDataset(gds);
		
		List<String> var = new ArrayList<String>();
		//var.add("Pressure");
		var.add("Temperature");				
		params.setVar(var);
				
		validationResult = new BeanPropertyBindingResult(params, "params");
		response = new MockHttpServletResponse();
	}
	
	@Test
	public void shouldGetTimeRange() throws NcssException, InvalidRangeException, ParseException, IOException{
		
		gridDataController.getGridSubset(params, validationResult, response);
		
		assertEquals(200, response.getStatus());
		//Open the binary response in memory
		NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getContentAsByteArray() );
		NetcdfDataset ds = new NetcdfDataset(nf);
		Dimension time =ds.findDimension("time");
		
		assertEquals( lengthTimeDim, time.getLength());
						
	}
	
	@After
	public void tearDown() throws IOException{
		
		GridDataset gds = gridDataController.getGridDataset();
		gds.close();
		gds = null;
		gridDataController =null;
		
	}	

}
