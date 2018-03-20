/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.controller.point;

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
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;

/**
 * Test ncss on station feature collections
 *
 * @author caron
 * @since 10/18/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class TestPointFCExceptions {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private WebApplicationContext wac;

  private String dataset = "/ncss/point/testBuoyFeatureCollection/Surface_Buoy_Point_Data_fc.cdmr";

  private MockMvc mockMvc;

  @Before
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  public void noFeaturesInPointCollectionCSV() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("longitude", "-105.203").param("latitude", "40.019")
            .param("accept", "csv") //
            .param("var", "ICE");

    this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().is(400));
  }

  @Test
  public void noFeaturesInPointCollectionNetcdf() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("longitude", "-105.203").param("latitude", "40.019")
            .param("accept", "netcdf") // empty - netcdf fails
            .param("var", "ICE");

    this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().is(400));
  }

  @Test
  public void invalidTimeRange() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("north", "43.0")
            .param("south", "38.0")
            .param("west", "-107.0")
            .param("east", "-103.0")
            .param("time_start", "2006-03-02T00:00:00Z")
            .param("time_end", "2006-03-28T00:00:00Z")
            .param("var", "ICE, PRECIP_amt");

    this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().is(400));
  }

  @Test
  public void invalidVariables() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("var", "air_temperature", "dew_point_temperature")
            .param("north", "43.0")
            .param("south", "38.0")
            .param("west", "-107.0")
            .param("east", "-103.0");

    MvcResult result = this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().is(400))
            .andReturn();
    System.out.printf("%s%n", result.getResponse().getContentAsString());
  }

}

