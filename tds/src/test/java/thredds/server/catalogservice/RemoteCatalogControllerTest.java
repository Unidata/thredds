package thredds.server.catalogservice;

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import thredds.core.AllowedServices;
import thredds.core.StandardService;
import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsContentRoot;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext.xml" }, loader = MockTdsContextLoader.class)
@Category({NeedsContentRoot.class, NeedsExternalResource.class})
public class RemoteCatalogControllerTest {

	@Autowired
	private WebApplicationContext wac;
	private MockMvc mockMvc;

	@Autowired
 private AllowedServices allowedServices;

	@Before
 	public void setup(){
 		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		allowedServices.setAllowService(StandardService.catalogRemote, true);
 	}

	String dataset="casestudies/ccs039/grids/netCDF/1998062912_eta.nc";
	String catalog="http://thredds.ucar.edu/thredds/catalog/casestudies/ccs039/grids/netCDF/catalog.xml";
	String path ="/remoteCatalogService";
	String htmlContent = "text/html;charset=UTF-8";

	@Test
	public void showCommandTest() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
						.param("command", "SHOW")
						.param("catalog", catalog);

		MvcResult result = this.mockMvc.perform( rb )
            .andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().contentType(htmlContent))
            .andReturn();

		System.out.printf("showCommandTest status=%d%n", result.getResponse().getStatus());
		System.out.printf("%s%n", result.getResponse().getContentAsString());
	}

	@Test
	public void subsetCommandTest() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
						.param("command", "SUBSET")
						.param("catalog", catalog)
						.param("dataset", dataset);

		MvcResult result = this.mockMvc.perform( rb )
            .andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().contentType(htmlContent))
            .andReturn();

		System.out.printf("subsetCommandTest status=%d%n", result.getResponse().getStatus());
		System.out.printf("%s%n", result.getResponse().getContentAsString());
	}

	@Test
	public void validateCommandTest() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
						.param("command", "VALIDATE")
						.param("catalog", catalog)
						.param("dataset", dataset);

		MvcResult result = this.mockMvc.perform( rb )
            .andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().contentType(htmlContent))
            .andReturn();

		System.out.printf("validateCommandTest status=%d%n", result.getResponse().getStatus());
		System.out.printf("%s%n", result.getResponse().getContentAsString());
	}

}
