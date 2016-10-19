package thredds.server.cdmrfeature;

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
import thredds.util.ContentType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * @author cwardgar
 * @since 2016-10-18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations={"/WEB-INF/applicationContext.xml"})
@Category(NeedsCdmUnitTest.class)
public class CdmrGridControllerTest {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private String datasetPath;

    @Before
    public void setup(){
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        this.datasetPath = "/cdmrfeature/grid/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc";
    }

    @Test
    public void testHeader() throws Exception {
        RequestBuilder rb = MockMvcRequestBuilders.get(datasetPath).servletPath(datasetPath)
                .param("req", "header");

        MvcResult result = this.mockMvc.perform( rb )
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().contentType(ContentType.binary.getContentHeader()))
                .andReturn();

        // We want this statement to succeed without exception.
        // Throws NullPointerException if header doesn't exist
        // Throws IllegalArgumentException if header value is not a valid date.
        result.getResponse().getDateHeader("Last-Modified");
    }

    @Test
    public void testForm() throws Exception {
        RequestBuilder rb = MockMvcRequestBuilders.get(datasetPath).servletPath(datasetPath)
                .param("req", "form");

        MvcResult result = this.mockMvc.perform( rb )
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().contentType(ContentType.text.getContentHeader()))
                .andReturn();

        // We want this statement to succeed without exception.
        // Throws NullPointerException if header doesn't exist
        // Throws IllegalArgumentException if header value is not a valid date.
        result.getResponse().getDateHeader("Last-Modified");
    }
}
