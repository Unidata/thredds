package thredds.server.root;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsContentRoot;

import javax.annotation.PostConstruct;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext.xml"},loader=MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
public class RootControllerTest {

	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;

	private RequestBuilder requestBuilder;

	@PostConstruct
	public void init() {
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}
		
	@Test
	public void testRootRedirect() throws Exception{
		requestBuilder = MockMvcRequestBuilders.get("/");
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
		//Check that "/" is redirected
		assertEquals(302, mvc.getResponse().getStatus());		
		assertEquals("redirect:/catalog/catalog.html", mvc.getModelAndView().getViewName());
	}

	@Test
	public void testStaticContent() throws Exception{
		requestBuilder = MockMvcRequestBuilders.get("/tdsCat.css");
		MvcResult mvc = this.mockMvc.perform(requestBuilder).andReturn();
		//Check that "/" is redirected
		Assert.assertEquals(200, mvc.getResponse().getStatus());
		String content = mvc.getResponse().getContentAsString();
		System.out.printf("content='%s'%n", content);
		//Assert.assertNotNull(mvc.getModelAndView());
		//assertEquals("redirect:/catalog/catalog.html", mvc.getModelAndView().getViewName());
	}


}
