package thredds.server.serverinfo;

import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.config.TdsContext;
import thredds.server.config.TdsServerInfo;
import thredds.server.serverinfo.ServerInfoController;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/serverInfo-servlet.xml" },loader=MockTdsContextLoader.class)
public class ServerInfoControllerTest{

	@Autowired
	private TdsContext tdsContext;
	
	@Autowired
	private ServerInfoController serverInfoController;
	
	@Before
	public void setUpTdsContext(){
		serverInfoController.setTdsContext(tdsContext);				
	}
		
	@Test
	public void serverInfoRequestTest() throws Exception{

		//HTML request test
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/serverInfo.html");
		request.setServletPath("/serverInfo.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =serverInfoController.handleRequest(request, response);        
        
        checkModelAndView(mv, "thredds/server/serverinfo/serverInfo_html");
          
        //XML request test
        request.setServletPath("/serverInfo.xml");
        mv =serverInfoController.handleRequest(request, response);
        checkModelAndView(mv, "thredds/server/serverinfo/serverInfo_xml");
        
	}	
	
	@Test
	public void serverVersionRequestTest() throws Exception{
		
		//HTML request test
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/serverVersion.txt");
		request.setServletPath("/serverVersion.txt");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =serverInfoController.handleRequest(request, response);        
        
        checkModelAndView(mv, "thredds/server/serverinfo/serverVersion_txt");		
		
	}
	
	private void checkModelAndView(ModelAndView mv, String view){
		
        assertViewName(mv, view);                
        assertAndReturnModelAttributeOfType(mv,"serverInfo" , TdsServerInfo.class);
        assertAndReturnModelAttributeOfType(mv,"webappVersion" , String.class);
        assertAndReturnModelAttributeOfType(mv,"webappName" , String.class);
        assertAndReturnModelAttributeOfType(mv,"webappVersionBuildDate" , String.class);		
		
	}

	
}
