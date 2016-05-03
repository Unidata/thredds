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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.params.GridPathParams;
import thredds.mock.web.MockTdsContextLoader;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class AllVariablesSubsettingTest {

	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;		
	private RequestBuilder requestBuilder;
		
	
	@Before
	public void setUp() throws IOException{
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();	
		String servletPath = GridPathParams.getPathInfo().get(0);
		requestBuilder = MockMvcRequestBuilders.get(servletPath).servletPath(servletPath).param("var", "all");
	}
	
	@Test
	public void shouldGetAllVariables() throws Exception{
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
		assertEquals(200, mvc.getResponse().getStatus());
		
		//Open the binary response in memory
		//NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getContentAsByteArray() );
		NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", mvc.getResponse().getContentAsByteArray() );
		
		ucar.nc2.dt.grid.GridDataset gdsDataset = new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));
		assertTrue( gdsDataset.getCalendarDateRange().isPoint());
		assertEquals(7, gdsDataset.getDataVariables().size());
		
	}
	
}
