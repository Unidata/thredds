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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
import thredds.server.ncss.format.SupportedFormat;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.NetcdfFile;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;


/**
 * @author marcos
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class VariableSpaceSubsettingTest {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;		
	private RequestBuilder requestBuilder;	

	private String accept;
	private String pathInfo;
	private int[][] expectedShapes;
	private List<String> vars;

	@Parameters
	public static Collection<Object[]> getTestParameters(){


		return Arrays.asList( new Object[][]{
				{ SupportedFormat.NETCDF3, new int[][]{ {1,65,93}, {1,65,93} } , GridPathParams.getPathInfo().get(4), GridDataParameters.getVars().get(0)}, //No vertical levels
				{ SupportedFormat.NETCDF3, new int[][]{ {1,1,65,93}, {1,1,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(1)}, //Same vertical level (one level)
				{ SupportedFormat.NETCDF3, new int[][]{ {1,29,65,93}, {1,29,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(2)}, //Same vertical level (multiple level)
				{ SupportedFormat.NETCDF3, new int[][]{ {1,65,93}, {1,29,65,93}, {1,1,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(3)}, //No vertical levels and vertical levels
				{ SupportedFormat.NETCDF3, new int[][]{ {1,1,65,93}, {1,29,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(4)}, //Different vertical levels

				{ SupportedFormat.NETCDF4,  new int[][]{ {1,65,93}, {1,65,93} } , GridPathParams.getPathInfo().get(4), GridDataParameters.getVars().get(0)}, //No vertical levels
				{ SupportedFormat.NETCDF4, new int[][]{ {1,1,65,93}, {1,1,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(1)}, //Same vertical level (one level)
				{ SupportedFormat.NETCDF4, new int[][]{ {1,29,65,93}, {1,29,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(2)}, //Same vertical level (multiple level)
				{ SupportedFormat.NETCDF4, new int[][]{ {1,65,93}, {1,29,65,93}, {1,1,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(3)}, //No vertical levels and vertical levels
				{ SupportedFormat.NETCDF4, new int[][]{ {1,1,65,93}, {1,29,65,93} }, GridPathParams.getPathInfo().get(3), GridDataParameters.getVars().get(4)}, //Different vertical levels

		});
	}

	public VariableSpaceSubsettingTest(SupportedFormat format, int[][] result, String pathInfo, List<String> vars){
		this.accept = format.getAliases().get(0);
		this.expectedShapes= result;
		this.pathInfo = pathInfo;
		this.vars = vars;
	}

	@Before
	public void setUp() throws IOException{
		String servletPath = pathInfo;
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();

		Iterator<String> it = vars.iterator();
		String varParamVal = it.next();
		while(it.hasNext()){
			String next = it.next();
			varParamVal =varParamVal+","+next;
		}		

		requestBuilder = MockMvcRequestBuilders.get(servletPath).servletPath(servletPath)
				.param("accept", accept)
				.param("var", varParamVal);


	}

	@Test
	public void shoudGetVariablesSubset() throws Exception{

		mockMvc.perform(requestBuilder)
		.andExpect( MockMvcResultMatchers.status().isOk() )
		.andExpect( new ResultMatcher(){
			public void match(MvcResult result) throws Exception{
				//Open the binary response in memory
				NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", result.getResponse().getContentAsByteArray() );
				ucar.nc2.dt.grid.GridDataset gdsDataset =new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));		
				assertTrue( gdsDataset.getCalendarDateRange().isPoint());

				List<VariableSimpleIF> vars = gdsDataset.getDataVariables();
				int[][] shapes = new int[vars.size()][];
				int cont = 0;
				for(VariableSimpleIF var : vars){
					shapes[cont] = var.getShape();
					cont++;

				}					
				assertArrayEquals(expectedShapes, shapes);										
			}
		});								
	}

}
