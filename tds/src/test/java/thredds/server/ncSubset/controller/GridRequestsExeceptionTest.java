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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.exception.RequestTooLargeException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.params.GridDataRequestParamsBean;
import thredds.servlet.DatasetHandlerAdapter;
import ucar.nc2.dt.GridDataset;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class GridRequestsExeceptionTest {
	
	@Autowired
	private GridDataController gridDataController;
	
	//private GridDataRequestParamsBean params;	
	//private BindingResult validationResult;
	private MockHttpServletResponse response ;
	private String pathInfo="testFeatureCollection/Test_Feature_Collection_best.ncd";
	
	@Before
	public void setUp() throws IOException{

		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		gridDataController.setGridDataset(gds);
		response = new MockHttpServletResponse();
	}
	
	@Test(expected=RequestTooLargeException.class)
	public void testRequestTooLargeException() throws Exception{
		GridDataRequestParamsBean params;
		BindingResult validationResult;
		params = new GridDataRequestParamsBean();		
		params.setTemporal("all");
		List<String> vars = new ArrayList<String>();
		vars.add("Relative_humidity");
		vars.add("Temperature");
		params.setVar(vars);		
		validationResult = new BeanPropertyBindingResult(params, "params");
		gridDataController.getGridSubset(params, validationResult, response);
		
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testUnsupportedOperationException() throws Exception{
		GridDataRequestParamsBean params;
		BindingResult validationResult;
		params = new GridDataRequestParamsBean();		
		List<String> vars = new ArrayList<String>();
		vars.add("all");
		params.setVar(vars);
		params.setVertCoord(200.);
		validationResult = new BeanPropertyBindingResult(params, "params");
		gridDataController.getGridSubset(params, validationResult, response);		
	}
	
	@Test(expected=TimeOutOfWindowException.class)
	public void testTimeOutOfWindowException() throws Exception{
		GridDataRequestParamsBean params;
		BindingResult validationResult;
		params = new GridDataRequestParamsBean();		
		List<String> vars = new ArrayList<String>();
		vars.add("all");
		params.setVar(vars);
		params.setTime("2012-04-18T15:00:00Z");
		params.setTime_window("PT1H");
		validationResult = new BeanPropertyBindingResult(params, "params");
		gridDataController.getGridSubset(params, validationResult, response);		
	}	
	
	@After
	public void tearDown() throws IOException{
		
		GridDataset gds = gridDataController.getGridDataset();
		gds.close();		
		gds = null;
		gridDataController =null;
		
	}	

}
