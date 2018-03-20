/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.controller.grid;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.format.SupportedFormat;

import java.lang.invoke.MethodHandles;

/**
 * Tests NCSS's "/datasetBoundaries.*" endpoints. They are implemented in NcssGridController.
 *
 * @author mhermida
 * @author cwardgar
 */
@RunWith(JUnitParamsRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext.xml" }, loader = MockTdsContextLoader.class)
public class DatasetBoundariesTest {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// Together, these two fields replicate the functionality of "@RunWith(SpringJUnit4ClassRunner.class)".
	// With that out of the way, we can select a different runner: JUnitParamsRunner.
	// See https://github.com/Pragmatists/junitparams-spring-integration-example
	@ClassRule
	public static final SpringClassRule SCR = new SpringClassRule();
	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();


	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;
    private String datasetPath = "/ncss/grid/scanLocal/crossSeamLatLon1D.ncml";

	private String expectedWKT = "POLYGON((" +
				"130.000 0.000, 150.000 0.000, 170.000 0.000, 190.000 0.000, 210.000 0.000, "      +  // Bottom edge
				"230.000 0.000, 230.000 10.000, 230.000 20.000, 230.000 30.000, 230.000 40.000, "  +  // Right edge
				"230.000 50.000, 210.000 50.000, 190.000 50.000, 170.000 50.000, 150.000 50.000, " +  // Top edge
				"130.000 50.000, 130.000 40.000, 130.000 30.000, 130.000 20.000, 130.000 10.000"   +  // Left edge
			"))";

	private String expectedGeoJSON = "{ 'type': 'Polygon', 'coordinates': [ [ " +
				"[130.000, 0.000], [150.000, 0.000], [170.000, 0.000], [190.000, 0.000], [210.000, 0.000], "      +
				"[230.000, 0.000], [230.000, 10.000], [230.000, 20.000], [230.000, 30.000], [230.000, 40.000], "  +
				"[230.000, 50.000], [210.000, 50.000], [190.000, 50.000], [170.000, 50.000], [150.000, 50.000], " +
				"[130.000, 50.000], [130.000, 40.000], [130.000, 30.000], [130.000, 20.000], [130.000, 10.000]"   +
			" ] ] }";

	@Before
	public void setup(){
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	private Object[] parameters() {
		return new Object[] {
				new Object[] {
						"/datasetBoundaries.xml",
						MockMvcRequestBuilders.get(datasetPath + "/datasetBoundaries.xml"),
						SupportedFormat.WKT,
						expectedWKT
				},
				new Object[] {
						"/datasetBoundaries.xml?accept=wkt",
						MockMvcRequestBuilders.get(datasetPath + "/datasetBoundaries.xml").param("accept", "wkt"),
						SupportedFormat.WKT,
						expectedWKT
				},
				new Object[] {
						"/datasetBoundaries.xml?accept=json",
						MockMvcRequestBuilders.get(datasetPath + "/datasetBoundaries.xml").param("accept", "json"),
						SupportedFormat.JSON,
						expectedGeoJSON
				},
				new Object[] {
						"/datasetBoundaries.wkt",
						MockMvcRequestBuilders.get(datasetPath + "/datasetBoundaries.wkt"),
						SupportedFormat.WKT,
						expectedWKT
				},
				new Object[] {
						"/datasetBoundaries.json",
						MockMvcRequestBuilders.get(datasetPath + "/datasetBoundaries.json"),
						SupportedFormat.JSON,
						expectedGeoJSON
				}
		};
	}

	@Test
	@Parameters(method = "parameters")
	@TestCaseName("{method}('{0}')")
	public void test(String testName,RequestBuilder requestBuilder,
			SupportedFormat expectedFormat, String expectedResponse) throws Exception {
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().contentType(expectedFormat.getMimeType()))
				.andExpect(content().string(expectedResponse));
	}
}
