package thredds.server.catalogservice;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml", "/WEB-INF/spring-servlet.xml"}, loader = MockTdsContextLoader.class)
public class CatalogControllerTest {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
 	public void setup(){
 		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
 	}

	@SpringJUnit4ParameterizedClassRunner.Parameters
	public static Collection<Object[]> getTestParameters(){
		return Arrays.asList(new Object[][]{
					{"/catalog/scanCdmUnitTests/agg/pointFeatureCollection/netCDFbuoydata/catalog.html", "text/html;charset=UTF-8"},
					{"/catalog/scanCdmUnitTests/agg/pointFeatureCollection/netCDFbuoydata/catalog.xml", ContentType.xml.getContentHeader()},
	});
	}

	String path, expectType;
	public CatalogControllerTest(String path, String expectType) {
	  this.path = path;
	  this.expectType = expectType;
	}

	@Ignore("No longer fails - we ignore command=xxxx")
	@Test
	public void validateCommandTest() throws Exception{

		RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
				.param("dataset", "scanCdmUnitTests/agg/pointFeatureCollection/netCDFbuoydata/BOD001_000_20050627_20051109.nc")
				.param("command", "validate");

		MvcResult result = this.mockMvc.perform( rb )
            .andExpect(MockMvcResultMatchers.status().is(400))
            .andExpect(MockMvcResultMatchers.content().string(new StringContains("UnsupportedOperationException")))
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN))
            .andReturn();

		System.out.printf("%s%n", result.getResponse().getContentAsString());
	}

	@Test
	public void subsetCommandTest() throws Exception{

		RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path)
				.param("dataset", "scanCdmUnitTests/agg/pointFeatureCollection/netCDFbuoydata/BOD001_000_20050627_20051109.nc");

		MvcResult result = this.mockMvc.perform( rb )
            .andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().string(
										new StringContains("scanCdmUnitTests/agg/pointFeatureCollection/netCDFbuoydata/BOD001_000_20050627_20051109.nc"))) // LOOK not actually testing subset
            .andExpect(MockMvcResultMatchers.content().contentType(expectType))
            .andReturn();

		System.out.printf("%s%n", result.getResponse().getContentAsString());
	}

	@Test
	public void showCommandTest() throws Exception{

		RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path);
		MvcResult result = this.mockMvc.perform( rb )
            .andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().string(new StringContains("catalog")))
            .andExpect(MockMvcResultMatchers.content().contentType(expectType))
            .andReturn();

		System.out.printf("%s%n", result.getResponse().getContentAsString());
	}


}
