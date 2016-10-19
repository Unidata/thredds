package thredds.server.fileserver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * @author cwardgar
 * @since 2016-10-18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations={"/WEB-INF/applicationContext.xml"})
@Category(NeedsCdmUnitTest.class)
public class FileServerControllerTest {
    private MockMvc mockMvc;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new FileServerController()).build();
    }

    @Test
    public void testLastModified() throws Exception {
        String path = "/fileServer/testNAMfmrc/files/20060925_0600.nc";
        RequestBuilder rb = MockMvcRequestBuilders.get(path).servletPath(path);

        MvcResult result = mockMvc.perform(rb).andReturn();
        Assert.assertEquals(200, result.getResponse().getStatus());

        // We want this statement to succeed without exception.
        // Throws NullPointerException if header doesn't exist
        // Throws IllegalArgumentException if header value is not a valid date.
        result.getResponse().getDateHeader("Last-Modified");
    }
}
