package thredds.server.serverinfo;

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
import org.springframework.web.servlet.ModelAndView;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.config.TdsServerInfo;
import ucar.unidata.util.test.category.NeedsContentRoot;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"},loader=MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
public class ServerInfoControllerTest{

	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;		
	private RequestBuilder requestBuilder;	
	
	
	@Before
	public void setUpTdsContext(){			
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();	
	}
		
	@Test
	public void serverInfoHTMLRequestTest() throws Exception{
		
		requestBuilder = MockMvcRequestBuilders.get("/serverInfo.html");
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
		assertEquals(200, mvc.getResponse().getStatus());		
		checkModelAndView(mvc.getModelAndView(), "thredds/server/serverinfo/serverInfo_html");
		       
	}
	
	@Test
	public void serverInfoXMLRequestTest() throws Exception{
		
		requestBuilder = MockMvcRequestBuilders.get("/serverInfo.xml");
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
		assertEquals(200, mvc.getResponse().getStatus());		
		checkModelAndView(mvc.getModelAndView(), "thredds/server/serverinfo/serverInfo_xml");
		       
	}	
	
	@Test
	public void serverVersionRequestTest() throws Exception{
		
		requestBuilder = MockMvcRequestBuilders.get("/serverVersion.txt");
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
		assertEquals(200, mvc.getResponse().getStatus());		
		checkModelAndView(mvc.getModelAndView(), "thredds/server/serverinfo/serverVersion_txt");        
		
	}
	
	private void checkModelAndView(ModelAndView mv, String view){
		
        assertViewName(mv, view);                
        assertAndReturnModelAttributeOfType(mv,"serverInfo" , TdsServerInfo.class);
        assertAndReturnModelAttributeOfType(mv,"webappVersion" , String.class);
        assertAndReturnModelAttributeOfType(mv,"webappName" , String.class);
        assertAndReturnModelAttributeOfType(mv,"webappVersionBuildDate" , String.class);		
		
	}

	
}
