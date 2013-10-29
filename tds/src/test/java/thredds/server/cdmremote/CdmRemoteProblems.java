package thredds.server.cdmremote;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import thredds.mock.web.MockTdsContextLoader;
import thredds.util.ContentType;

/**
 * Describe
 *
 * @author caron
 * @since 10/29/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext-tdsConfig.xml"}, loader = MockTdsContextLoader.class)
public class CdmRemoteProblems {

  @Autowired
 	private org.springframework.web.context.WebApplicationContext wac;

 	private MockMvc mockMvc;
  // private String path = "/cdmremote/NCOF/POLCOMS/IRISH_SEA/files/20060925_0600.nc";
  private String path = "/cdmremote/testStationFeatureCollection/files/Surface_METAR_20060325_0000.nc";

 	@Before
 	public void setup(){
 		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
 	}


  @Test
   public void cdmRemoteRequestCapabilitiesTest() throws Exception {
     RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
   				.param("req", "capabilities");

     MvcResult result = this.mockMvc.perform( rb )
               .andExpect(MockMvcResultMatchers.status().is(200))
               .andExpect(MockMvcResultMatchers.content().contentType(ContentType.xml.toString()))
               .andReturn();

    System.out.printf("content = %s%n", result.getResponse().getContentAsString());
   }

}