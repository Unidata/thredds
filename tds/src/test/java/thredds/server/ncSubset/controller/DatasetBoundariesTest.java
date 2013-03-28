/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.format.SupportedFormat;
import ucar.nc2.NetcdfFile;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class DatasetBoundariesTest {
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Before
	public void setup(){
		this.mockMvc = webAppContextSetup(this.wac).build(); 
	}
	
	@Test
	public void getDatasetBoundaries() throws Exception{						
		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries")
				.servletPath("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries")
				.param("accept", "json");
		
		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().isOk());
		
	}
	
	@Test
	public void datasetWasOpenendAndClosed() throws Exception{
		
		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries")
				.servletPath("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries");
		
		MvcResult mvc = this.mockMvc.perform(rb).andReturn();		
		
		AbstractNcssController handler = (AbstractNcssController) ((HandlerMethod)mvc.getHandler()).getBean();
		
		assertNotNull(handler.getGridDataset());
		assertNull(handler.getGridDataset().getNetcdfFile() );
		
	}
	
	
	@Test
	public void defaultContentType() throws Exception{
		
		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries")
				.servletPath("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries");
		
		this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.header().string("content-type", SupportedFormat.WKT.getResponseContentType() ) );		
		
	}	
	
	@Test
	public void jsonResponseHasContentType() throws Exception{
		
		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries")
				.servletPath("/ncss/grid/testFeatureCollection/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/datasetBoundaries").param("accept", "json");
		
		this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.header().string("content-type", SupportedFormat.JSON.getResponseContentType() ) );		
		
	}
	
	private MockMvcBuilder webAppContextSetup(WebApplicationContext wac){
		
		return MockMvcBuilders.webAppContextSetup(wac);
		
	}

}
