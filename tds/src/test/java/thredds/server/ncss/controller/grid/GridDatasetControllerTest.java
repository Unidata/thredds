/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.controller.grid;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.format.SupportedFormat;
import thredds.util.Constants;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridDatasetControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Before
	public void setup(){
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

  @Test
 	public void getGridSubsetOnGridDataset() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.servletPath("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.param("accept", SupportedFormat.NETCDF3.getFormatName())
 				.param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground");

     MvcResult result =  this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
 			.andExpect(MockMvcResultMatchers.content().contentType(SupportedFormat.NETCDF3.getMimeType()))
       .andExpect(MockMvcResultMatchers.header().string(Constants.Content_Disposition, new FilenameMatcher(".nc")))
       .andReturn();

     System.out.printf("Headers%n");
     for (String name : result.getResponse().getHeaderNames()) {
       System.out.printf(  "%s= %s%n", name, result.getResponse().getHeader(name));
     }
 	}

  @Test
 	public void getGridSubsetOnGridDatasetNc4() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.servletPath("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
 				.param("accept", SupportedFormat.NETCDF4.getFormatName())
 				.param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground");

     MvcResult result =  this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
 			.andExpect(MockMvcResultMatchers.content().contentType(SupportedFormat.NETCDF4.getMimeType()))
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
