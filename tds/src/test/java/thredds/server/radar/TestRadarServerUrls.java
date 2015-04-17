package thredds.server.radar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.mock.web.MockTdsContextLoader;
import thredds.util.ContentType;

import java.util.Arrays;
import java.util.Collection;


/**
 * Test RadarServer sanity check
 *
 * @author caron
 * @since 11/15/13
 */

//@RunWith(SpringJUnit4ParameterizedClassRunner.class)
//@WebAppConfiguration
//@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml"}, loader = MockTdsContextLoader.class)
public class TestRadarServerUrls {

  //@Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  //@Before
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  //@SpringJUnit4ParameterizedClassRunner.Parameters
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{

            {"/radarServer2/nexrad/level2/IDD/dataset.xml", null},
            {"/radarServer2/nexrad/level2/IDD/stations.xml", null},
            {"/radarServer2/nexrad/level2/IDD?stn=KDGX&time_start=2014-06-05T12:47:17&time_end=2014-06-05T16:07:17", null},
            //{"/radarServer2/nexrad/level2/IDD", "stn=KDGX&time=present"},

            {"/radarServer2/nexrad/level3/IDD/catalog.xml", null},
            {"/radarServer2/nexrad/level3/IDD/dataset.xml", null},
            {"/radarServer2/nexrad/level3/IDD/stations.xml", null},
            //{"/radarServer2/nexrad/level3/IDD","stn=UDX&var=N0R&time=present"},

            {"/radarServer2/nexrad/level3/IDD/N0R/catalog.xml", null},
            {"/radarServer2/nexrad/level3/IDD/N0R/UDX/catalog.xml", null},
            {"/radarServer2/nexrad/level3/IDD/N0R/UDX/20131114/catalog.xml", null},
            {"/radarServer2/nexrad/level3/IDD/dataset.xml", null},
            {"/radarServer2/nexrad/level3/IDD/stations.xml", null},
            //{"/radarServer2/nexrad/level3/IDD", "north=50.00&south=20.00&west=-127&east=-66&time=present&var=KPAH"},

         /*   {"/radarServer2/terminal/level3/IDD/dataset.xml", null},
            {"/radarServer2/terminal/level3/IDD/stations.xml", null},
            {"/radarServer2/terminal/level3/IDD", "stn=ORD&var=NTP&time=present"},
            {"/radarServer2/terminal/level3/IDD", "stn=OKC&time=present&var=NTP"}, */

    });
  }

  String path, query;

  public TestRadarServerUrls(String path, String query) {
    this.path = path;
    this.query = query;
  }

 //@Test   //LOOK BAIL out
  public void radarRequest() throws Exception {
    String url = (query == null) ? path : path + "?" + query;
    RequestBuilder rb = MockMvcRequestBuilders.get(url).servletPath(path);
    System.err.printf("url = %s%n", url);

    MvcResult result = this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().is(200))
                    //   .andExpect(MockMvcResultMatchers.content().contentType(ContentType.xml.getContentHeader()))
            .andReturn();

    System.err.printf("content for %s=%n%s%n", url, result.getResponse().getContentAsString());
  }

}

