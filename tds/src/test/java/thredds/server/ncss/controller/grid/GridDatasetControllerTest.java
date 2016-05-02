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
package thredds.server.ncss.controller.grid;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.format.SupportedFormat;
import thredds.util.Constants;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridDatasetControllerTest {
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Before
	public void setup(){
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}
  @Test
 	public void fileNotFound() throws Exception {
 		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/cdmUnitTest/ncss/GFS/CONUS_80km/baddie.nc")
 				.servletPath("/ncss/cdmUnitTest/ncss/GFS/CONUS_80km/baddie.nc")
 				.param("accept", "netcdf" )
 				.param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground")
 				.param("latitude", "40.019")
 				.param("longitude", "-105.293");

 		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().is(404));
  }

  @Test
 	public void getGridAsPointSubset() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.servletPath("/ncss/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.param("accept", "netcdf" )
 				.param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground")
 				.param("latitude", "40.019")
 				.param("longitude", "-105.293");

 		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().isOk())
 			.andExpect(MockMvcResultMatchers.content().contentType( SupportedFormat.NETCDF3.getResponseContentType() )).andReturn() ;

 	}

  @Test
 	public void getGridSubsetOnGridDataset() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.servletPath("/ncss/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.param("accept", SupportedFormat.NETCDF3.getFormatName())
 				.param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground");

     MvcResult result =  this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
 			.andExpect(MockMvcResultMatchers.content().contentType(SupportedFormat.NETCDF3.getResponseContentType()))
       .andExpect(MockMvcResultMatchers.header().string(Constants.Content_Disposition, new FilenameMatcher(".nc")))
       .andReturn();

     System.out.printf("Headers%n");
     for (String name : result.getResponse().getHeaderNames()) {
       System.out.printf(  "%s= %s%n", name, result.getResponse().getHeader(name));
     }
 	}

  @Test
 	public void getGridSubsetOnGridDatasetNc4() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.servletPath("/ncss/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.param("accept", SupportedFormat.NETCDF4.getFormatName())
 				.param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground");

     MvcResult result =  this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
 			.andExpect(MockMvcResultMatchers.content().contentType(SupportedFormat.NETCDF4.getResponseContentType()))
      .andExpect(MockMvcResultMatchers.header().string(Constants.Content_Disposition, new FilenameMatcher(".nc4")))
      .andReturn();

     System.out.printf("Headers%n");
     for (String name : result.getResponse().getHeaderNames()) {
       System.out.printf(  "%s= %s%n", name, result.getResponse().getHeader(name));
     }
 	}

  private class FilenameMatcher extends BaseMatcher<String> {
    String suffix;
    FilenameMatcher(String suffix) {
      this.suffix = suffix;
    }

    @Override
    public void describeTo(Description description) {
    }

    @Override
    public boolean matches(Object item) {
      String value = (String) item;
      return value.endsWith(suffix);
    }
  }

}
