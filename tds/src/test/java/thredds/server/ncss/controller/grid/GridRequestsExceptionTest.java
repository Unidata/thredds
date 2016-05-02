/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.controller.grid;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridRequestsExceptionTest {

  @Autowired
 	private WebApplicationContext wac;

 	private MockMvc mockMvc;
	private String path="/ncss/gribCollection/GFS_CONUS_80km/best";

  @Before
 	public void setUp() throws IOException{
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
 	}

  @Test
 	public void checkBadGridRequestWhenNoParams() throws Exception{
    System.out.printf("path= %s%n", path);
    MvcResult result = this.mockMvc.perform(get(path).servletPath(path)) // note make it both the request an the servlet path (!)
            .andExpect(status().is(400))
            .andExpect(content().string(containsString("No variables requested")))
            .andReturn();

    System.out.printf("content= %s%n", result.getResponse().getContentAsString());
 	}
	
  @Test
 	public void checkBadGridRequestWhenEmptyVarParams() throws Exception{
    System.out.printf("path= %s%n", path);
    MvcResult result = this.mockMvc.perform(get(path).servletPath(path)
            .param("var", ""))
            .andExpect(status().is(400))
            .andExpect(content().string(containsString("No variables requested")))
            .andReturn();

    System.out.printf("content= %s%n", result.getResponse().getContentAsString());
 	}

  @Test
 	public void testMultipleVerticalCoordinates() throws Exception{
    System.out.printf("path= %s%n", path);
    MvcResult result = this.mockMvc.perform(get(path).servletPath(path)
            .param("var", "all")
            .param("vertCoord", "200.0"))
            .andExpect(status().is(400))
            .andExpect(content().string(containsString("must have variables with same vertical levels")))
            .andReturn();

    System.out.printf("content= %s%n", result.getResponse().getContentAsString());
 	}

  @Test
 	public void testTimeDoesNotIntersect() throws Exception{
    System.out.printf("path= %s%n", path);
    MvcResult result = this.mockMvc.perform(get(path).servletPath(path)
            .param("var", "Pressure_reduced_to_MSL_msl")
            .param("time", "2012-04-18T15:00:00Z"))
            // .param("time_window", "PT1H"))
            .andExpect(status().is(400))
            .andExpect(content().string(containsString("does not intersect actual time range")))
            .andReturn();

    System.out.printf("content= %s%n", result.getResponse().getContentAsString());
 	}

}
