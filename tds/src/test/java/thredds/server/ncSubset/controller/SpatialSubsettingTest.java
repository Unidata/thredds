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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
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

import thredds.mock.params.GridDataParameters;
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
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 * @author marcos
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class SpatialSubsettingTest {

	@Autowired
	private GridDataController gridDataController;
	
	private GridDataRequestParamsBean params;	
	private BindingResult validationResult;
	private MockHttpServletResponse response ;	
	
	private String pathInfo;
	private List<String> vars;
	private double[] latlonRectParams;
	private LatLonRect requestedBBOX;
	private LatLonRect datasetBBOX;
	
	@Parameters
	public static Collection<Object[]> getTestParameters(){
				
		return Arrays.asList( new Object[][]{
				{ PathInfoParams.getPatInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(0) },//bounding box contained in the declared dataset bbox
				{ PathInfoParams.getPatInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(1) }, //bounding box that intersects the declared bbox
				//{ PathInfoParams.getPatInfo().get(4), GridDataParameters.getVars().get(0), GridDataParameters.getLatLonRect().get(2) } //bounding box that contains data but doesn't intersect the declared bbox
								
			});
	}
	
	public SpatialSubsettingTest( String pathInfo, List<String> vars, double[] latlonRectParams){

		this.pathInfo = pathInfo;
		this.vars = vars;
		this.latlonRectParams = latlonRectParams;
	}
	
	@Before
	public void setUp() throws IOException{
		
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		gridDataController.setGridDataset(gds);
		params = new GridDataRequestParamsBean(); 
		params.setVar(vars);
		
		params.setWest(latlonRectParams[0]);
		params.setSouth(latlonRectParams[1]);
		params.setEast(latlonRectParams[2]);
		params.setNorth(latlonRectParams[3]);
		requestedBBOX = new LatLonRect(new LatLonPointImpl(latlonRectParams[1], latlonRectParams[0]), new LatLonPointImpl(latlonRectParams[3], latlonRectParams[2]) );
		datasetBBOX = gds.getBoundingBox();
		validationResult = new BeanPropertyBindingResult(params, "params");
		response = new MockHttpServletResponse();
	}
	
	@Test
	public void shoudGetVariablesSubset() throws NcssException, InvalidRangeException, ParseException, IOException{
				
		gridDataController.getGridSubset(params, validationResult, response);
		
		assertEquals(200, response.getStatus());
		//Open the binary response in memory
		NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getContentAsByteArray() );	
		
		ucar.nc2.dt.grid.GridDataset gdsDataset =new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));
		LatLonRect responseBBox= gdsDataset.getBoundingBox();		

		assertTrue( responseBBox.intersect((datasetBBOX))!= null &&  responseBBox.intersect((requestedBBOX))!= null);
		assertTrue( !responseBBox.equals(datasetBBOX));
		
	}
	
	@After
	public void tearDown() throws IOException{
		
		GridDataset gds =gridDataController.getGridDataset();
		gds.close();
		gds =null;
		gridDataController.setGridDataset(null);
		
	}
}
