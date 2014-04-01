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

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext-tdsConfig.xml"}, loader = MockTdsContextLoader.class)
public class TestRadarServerUrls {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Before
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @SpringJUnit4ParameterizedClassRunner.Parameters
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
            {"/radar/radarCollections.xml"},
            {"/radarServer/radarCollections.xml"},
            {"/radarServer/nexrad/level3/IDD/catalog.xml"},
            {"/radarServer/nexrad/level3/IDD/dataset.xml"},
            {"/radarServer/nexrad/level3/IDD/stations.xml"},
            {"/radarServer/nexrad/level3/IDD?north=50.00&south=20.00&west=-127&east=-66&time=present&var=KPAH"},
            {"/radarServer/nexrad/level2/IDD?stn=KLWX&time=present&var=NCR"},
            {"/radarServer/terminal/level3/IDD/dataset.xml"},
            {"/radarServer/terminal/level3/IDD/stations.xml"},
            {"/radarServer/terminal/level3/IDD?stn=BOS&var=TR0&time=present"},
    });
  }

  String path;

  public TestRadarServerUrls(String path) {
    this.path = path;
  }

  @Test
   public void radarRequest() throws Exception {
     RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path);
     System.out.printf("path = %s%n", path);

     MvcResult result = this.mockMvc.perform( rb )
               .andExpect(MockMvcResultMatchers.status().is(200))
            //   .andExpect(MockMvcResultMatchers.content().contentType(ContentType.xml.getContentHeader()))
               .andReturn();

     System.out.printf("content = %s%n", result.getResponse().getContentAsString());
   }

}

