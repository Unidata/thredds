/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.controller.point;

import org.junit.Before;
import org.junit.Ignore;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.exception.FeaturesNotFoundException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.util.ContentType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertTrue;

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
public class TestStationFCController {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private WebApplicationContext wac;

  private String dataset = "/ncss/point/testStationFeatureCollection/Metar_Station_Data_fc.cdmr";
  private MockMvc mockMvc;

  @Before
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  public void getClosestStationData() throws Exception {
    long start = System.currentTimeMillis();
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("longitude", "-105.203").param("latitude", "40.019")
            .param("accept", "netcdf4")
            .param("time_start", "2006-03-028T00:00:00Z")
            .param("time_end", "2006-03-29T00:00:00Z")
            .param("var", "air_temperature,dew_point_temperature,precipitation_amount_24,precipitation_amount_hourly,visibility_in_air");

    try {
      this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.content().contentType(SupportedFormat.NETCDF4.getMimeType()));
    } finally {
      long took = System.currentTimeMillis() - start;
      System.out.printf("that took %d msecs%n", took);
    }
  }

  @Test
  public void getStationListData() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "csv")
            .param("time", "2006-03-29T00:00:00Z")
            .param("stns", "BJC,LEST")
            .param("var", "air_temperature,dew_point_temperature,precipitation_amount_24,precipitation_amount_hourly,visibility_in_air");

    this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(ContentType.text.getContentHeader())); // changed to text so it would display in browser
  }

  @Test
  public void getDataForTimeRange() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("time_start", "2006-03-02T00:00:00Z")
            .param("time_end", "2006-03-28T00:00:00Z")
            .param("stns", "BJC,DEN")
            .param("var", "air_temperature,dew_point_temperature,precipitation_amount_24,precipitation_amount_hourly,visibility_in_air");

    this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(ContentType.netcdf.getContentHeader()));
  }

  @Test
  public void testInvalidDateRangeOnStationDataset() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("var", "air_temperature", "dew_point_temperature")
            .param("north", "43.0")
            .param("south", "38.0")
            .param("west", "-107.0")
            .param("east", "-103.0")
            .param("time_start", "2013-08-25T06:00:00Z")
            .param("time_end", "2013-08-26T06:00:00Z");

    org.springframework.test.web.servlet.MvcResult result = this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().is(400))
            .andReturn();
    System.out.printf("%s%n", result.getResponse().getContentAsString());
  }

  @Test
  public void getSubsetOnStationDataset() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("var", "air_temperature", "dew_point_temperature")
            .param("north", "43.0")
            .param("south", "38.0")
            .param("west", "-107.0")
            .param("east", "-103.0")
            .param("time_start", "2006-03-25T00:00:00Z")
            .param("time_end", "2006-03-28T00:00:00Z");

    this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(ContentType.netcdf.getContentHeader()));
  }

  @Test
  @Ignore("takes too long")
  public void getAllStnsOnStationDataset() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("stns", "all")
            .param("var", "air_temperature", "dew_point_temperature")
            .param("time_start", "2006-03-25T00:00:00Z")
            .param("time_end", "2006-03-26T00:00:00Z");


    long start = System.currentTimeMillis();
    try {
      this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.content().contentType(ContentType.netcdf.getContentHeader()));
    } finally {
      long took = System.currentTimeMillis() - start;
      System.out.printf("that took %d msecs%n", took);
    }

  }

  @Test
  public void stationNotFoundStationDataset() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("accept", "netcdf")
            .param("subset", "stns")
            .param("stns", "mock_station")
            .param("var", "air_temperature", "dew_point_temperature")
            .param("time_start", "2006-03-25T00:00:00Z")
            .param("time_end", "2006-04-28T00:00:00Z");

    this.mockMvc.perform(rb).andExpect(new ResultMatcher() {
      public void match(MvcResult result) throws Exception {
        //result.getResponse().getContentAsByteArray()
        Exception ex = result.getResolvedException();
        assertTrue(ex instanceof FeaturesNotFoundException);
      }
    });
  }

  @Test
  public void noFeaturesInPointCollection() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
            .param("longitude", "-105.203").param("latitude", "40.019")
            .param("accept", "netcdf") //
            .param("var", "air_temperature", "dew_point_temperature");

    this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().is(400));
  }

}

