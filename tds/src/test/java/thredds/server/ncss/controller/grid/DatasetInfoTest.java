/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.controller.grid;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import thredds.mock.web.MockTdsContextLoader;
import thredds.util.ContentType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;

/**
 * Test NcssDatasetInfo controller
 *
 * @author caron
 * @since 10/20/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml", "/WEB-INF/spring-servlet.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class DatasetInfoTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;
  private String xmlpath = "/ncss/grid/testGFSfmrc/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/dataset.xml";
  private String htmlpath = "/ncss/grid/testGFSfmrc/runs/GFS_CONUS_80km_RUN_2012-04-18T12:00:00.000Z/dataset.html";

  @Before
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void getDatasetXml() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(xmlpath).servletPath(xmlpath);
    MvcResult result = this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(ContentType.HEADER, ContentType.xml.getContentHeader()))
            .andReturn();

    System.out.printf("%s%n", result.getResponse().getContentAsString());
  }

  @Test
  public void getDatasetHtml() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get(htmlpath).servletPath(htmlpath);
    MvcResult result = this.mockMvc.perform(rb)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(ContentType.HEADER, ContentType.html.getContentHeader()))
            .andReturn();

    System.out.printf("%s%n", result.getResponse().getContentAsString());
  }


}
