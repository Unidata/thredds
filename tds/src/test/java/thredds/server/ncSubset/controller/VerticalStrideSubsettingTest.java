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

import static org.junit.Assert.assertArrayEquals;
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
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;


/**
 * @author marcos
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class VerticalStrideSubsettingTest {
	
	@Autowired
	private GridDataController gridDataController;
	
	private GridDataRequestParamsBean params;	
	private BindingResult validationResult;
	private MockHttpServletResponse response ;	
	
	private String pathInfo;
	private int[][] expectedShapes;	
	private List<String> vars;
	private Integer vertStride;
	
	@Parameters
	public static Collection<Object[]> getTestParameters(){
				
		return Arrays.asList( new Object[][]{
				{ new int[][]{ {1,65,93}, {1,65,93} } , PathInfoParams.getPatInfo().get(4), GridDataParameters.getVars().get(0), 1 }, //No vertical levels 
				{ new int[][]{ {1,1,65,93}, {1,1,65,93} }, PathInfoParams.getPatInfo().get(3), GridDataParameters.getVars().get(1), 1}, //Same vertical level (one level)
				{ new int[][]{ {1,10,65,93}, {1,10,65,93} }, PathInfoParams.getPatInfo().get(3), GridDataParameters.getVars().get(2), 3}, //Same vertical level (multiple level)
				{ new int[][]{ {1,65,93}, {1,10,65,93}, {1,1,65,93} }, PathInfoParams.getPatInfo().get(3), GridDataParameters.getVars().get(3), 3 }, //No vertical levels and vertical levels
				{ new int[][]{ {1,1,65,93}, {1,6,65,93} }, PathInfoParams.getPatInfo().get(3), GridDataParameters.getVars().get(4), 5}, //Different vertical levels
								
			});
	}
	
	public VerticalStrideSubsettingTest(int[][] result, String pathInfo, List<String> vars, Integer vertStride){
		this.expectedShapes= result;
		this.pathInfo = pathInfo;
		this.vars = vars;
		this.vertStride = vertStride;
	}
	
	@Before
	public void setUp() throws IOException{
		
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		gridDataController.setGridDataset(gds);
		params = new GridDataRequestParamsBean(); 
		params.setVar(vars);
		params.setVertStride(vertStride);
		
		validationResult = new BeanPropertyBindingResult(params, "params");
		response = new MockHttpServletResponse();
	}
	
	@Test
	public void shoudGetVerticalStridedSubset() throws NcssException, InvalidRangeException, ParseException, IOException{
				
		gridDataController.getGridSubset(params, validationResult, response);
		
		assertEquals(200, response.getStatus());
		//Open the binary response in memory
		NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getContentAsByteArray() );	
		
		ucar.nc2.dt.grid.GridDataset gdsDataset =new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));		
		assertTrue( gdsDataset.getCalendarDateRange().isPoint());		
		//assertEquals(expectedValue, Integer.valueOf( gdsDataset.getDataVariables().size()));
		
		List<VariableSimpleIF> vars = gdsDataset.getDataVariables();
		int[][] shapes = new int[vars.size()][];
		int cont = 0;
		for(VariableSimpleIF var : vars){
			//int[] shape =var.getShape();
			shapes[cont] = var.getShape();
			cont++;
			//String dimensions =var.getDimensions().toString();
			//int rank =var.getRank();
		}
		
		assertArrayEquals(expectedShapes, shapes);
		
	}
	
	@After
	public void tearDown() throws IOException{
		
		GridDataset gds = gridDataController.getGridDataset();
		gds.close();
		gds = null;
		gridDataController =null;
		
	}	

}	

