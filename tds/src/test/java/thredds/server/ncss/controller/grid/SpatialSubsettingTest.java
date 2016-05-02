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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.params.GridDataParameters;
import thredds.mock.params.GridPathParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.controller.AbstractNcssController;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.dataservice.DatasetHandlerAdapter;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * @author marcos
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class SpatialSubsettingTest {

	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;		
	private RequestBuilder requestBuilder;	
	
	private String accept;
	private String pathInfo;
	private List<String> vars;
	private double[] latlonRectParams;
	private LatLonRect requestedBBOX;
	private LatLonRect datasetBBOX;
	
	@Parameters
	public static Collection<Object[]> getTestParameters(){

		return Arrays.asList( new Object[][]{
				{ SupportedFormat.NETCDF3, GridPathParams.getPathInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(0) },//bounding box contained in the declared dataset bbox
				{ SupportedFormat.NETCDF3, GridPathParams.getPathInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(1) }, //bounding box that intersects the declared bbox
				
				{ SupportedFormat.NETCDF4, GridPathParams.getPathInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(0) },//bounding box contained in the declared dataset bbox
				{ SupportedFormat.NETCDF4, GridPathParams.getPathInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(1) }, //bounding box that intersects the declared bbox
								
			});
	}
	
	public SpatialSubsettingTest( SupportedFormat format, String pathInfo, List<String> vars, double[] latlonRectParams){
		this.accept = format.getAliases().get(0);
		this.pathInfo = pathInfo;
		this.vars = vars;
		this.latlonRectParams = latlonRectParams;
	}
	
	@Before
	public void setUp() throws IOException{
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		String servletPath = pathInfo;
		
		//Creates values for param var
		Iterator<String> it = vars.iterator();
		String varParamVal = it.next();
		while(it.hasNext()){
			String next = it.next();
			varParamVal =varParamVal+","+next;
		}
		
		requestBuilder = MockMvcRequestBuilders.get(servletPath).servletPath(servletPath).param("var", varParamVal)
				.param("west", String.valueOf( latlonRectParams[0]))
				.param("south",String.valueOf( latlonRectParams[1]))
				.param("east", String.valueOf( latlonRectParams[2]))
				.param("north",String.valueOf( latlonRectParams[3]))
				.param("accept", accept);

    String datasetPath = AbstractNcssController.getDatasetPath(this.pathInfo);
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(datasetPath);
    assert (gds != null);
		
		requestedBBOX = new LatLonRect(new LatLonPointImpl(latlonRectParams[1], latlonRectParams[0]), new LatLonPointImpl(latlonRectParams[3], latlonRectParams[2]) );
		datasetBBOX = gds.getBoundingBox();
    gds.close();
	}
	
	@Test
	public void shouldGetVariablesSubset() throws Exception{
				
		//gridDataController.getGridSubset(params, validationResult, response);
		
		this.mockMvc.perform(requestBuilder)
		.andExpect(MockMvcResultMatchers.status().isOk())
		.andExpect( new ResultMatcher(){
			public void match(MvcResult result) throws Exception{
				
				//Open the binary response in memory
				NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", result.getResponse().getContentAsByteArray() );				
				ucar.nc2.dt.grid.GridDataset gdsDataset =new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));
				LatLonRect responseBBox= gdsDataset.getBoundingBox();		

				assertTrue( responseBBox.intersect((datasetBBOX))!= null &&  responseBBox.intersect((requestedBBOX))!= null);
				assertTrue( !responseBBox.equals(datasetBBOX));				
			}
		});
		
	
		

		
	}
	
//	@After
//	public void tearDown() throws IOException{
//		
//		GridDataset gds =gridDataController.getGridDataset();
//		gds.close();
//		gds =null;
//		gridDataController.setGridDataset(null);
//		
//	}
}
