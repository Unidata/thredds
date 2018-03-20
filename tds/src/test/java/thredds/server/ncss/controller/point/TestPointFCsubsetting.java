/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.controller.point;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
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
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.util.ContentType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Test ncss on point feature collections
 *
 * @author caron
 * @since 10/18/13
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/WEB-INF/applicationContext.xml"}, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class TestPointFCsubsetting {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private WebApplicationContext wac;
    private String dataset = "/ncss/point/testBuoyFeatureCollection/Surface_Buoy_Point_Data_fc.cdmr";
    private String req = "?req=point&var=ICE&var=PRECIP_amt&var=PRECIP_amt24&var=T&north=40&west=-170&east=-100&south" +
            "=-40&time_start=2013-08-04T00:00:00Z&time_end=2013-08-05T00:00:00Z&accept=";
    private MockMvc mockMvc;

    @SpringJUnit4ParameterizedClassRunner.Parameters
    public static List<Object[]> getTestParameters() {
        List<Object[]> result = new ArrayList<Object[]>(10);

        for (SupportedFormat f : SupportedOperation.POINT_REQUEST.getSupportedFormats()) {
            result.add(new Object[]{f});
        }

        return result;
    }


    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    SupportedFormat format;

    public TestPointFCsubsetting(SupportedFormat format) {
        this.format = format;
    }

    @Test
    public void getSubsettedData() throws Exception {
        // problem is that browser wont display text/csv in line, so use tesxt/plain; see thredds.server.ncss.view
        // .dsg.PointWriter.WriterCSV
        String expectFormat = (format == SupportedFormat.CSV_STREAM) ? ContentType.text.getContentHeader() : format
                .getMimeType();

        RequestBuilder rb = MockMvcRequestBuilders.get(dataset + req + format.getFormatName()).servletPath(dataset);
        System.out.printf("%nURL='%s'%n", dataset + req + format.getFormatName());
        MvcResult result = this.mockMvc.perform(rb)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(expectFormat))
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        System.out.printf("getSubsettedData format=%s status = %d type=%s%n", format, response.getStatus(), response.getContentType());
    }
}
