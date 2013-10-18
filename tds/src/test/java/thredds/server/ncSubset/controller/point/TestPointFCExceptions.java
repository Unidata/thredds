/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.server.ncSubset.controller.point;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.exception.FeaturesNotFoundException;
import thredds.server.ncSubset.format.SupportedFormat;

import static org.junit.Assert.assertTrue;

/**
 * Test ncss on station feature collections
 *
 * @author caron
 * @since 10/18/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class TestPointFCExceptions{

	@Autowired
	private WebApplicationContext wac;

  private String dataset =  "/ncss/testBuoyFeatureCollection/Surface_Buoy_Point_Data_fc.cdmr";
 	private String req =  "?req=point&var=ICE&var=PRECIP_amt&var=PRECIP_amt24&var=T&north=40&west=-170&east=-100&south=-40&time_start=2013-08-04T00:00:00Z&time_end=2013-08-07T00:00:00Z&accept=";

	private MockMvc mockMvc;

	@Before
	public void setup(){
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@Test
	public void noBB() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
				.param("longitude", "-105.203").param("latitude", "40.019")
				.param("accept", "netcdf4")
				.param("var", "ICE");

		this.mockMvc.perform( rb )
      .andExpect(MockMvcResultMatchers.status().is(400))
			.andExpect(MockMvcResultMatchers.content().contentType( SupportedFormat.NETCDF4.getResponseContentType() ));
	}

	@Test
	public void getStationListData() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
				.param("accept", "csv")
        .param("time","2006-03-29T00:00:00Z")
				.param("subset", "stns")
				.param("stns", "BJC,LEST")
				.param("var", "air_temperature,dew_point_temperature,precipitation_amount_24,precipitation_amount_hourly,visibility_in_air");

		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.content().contentType(SupportedFormat.CSV_STREAM.getResponseContentType()));
	}

	@Test
	public void getDataForTimeRange() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
				.param("accept", "netcdf")
        .param("time_start","2006-03-02T00:00:00Z")
        .param("time_end","2006-03-28T00:00:00Z")
				.param("subset", "stns")
				.param("stns", "BJC,DEN")
				.param("var", "air_temperature,dew_point_temperature,precipitation_amount_24,precipitation_amount_hourly,visibility_in_air");

		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().isOk())
		.andExpect(MockMvcResultMatchers.content().contentType( SupportedFormat.NETCDF3.getResponseContentType() )) ;
	}

	@Test
  public void testInvalidDateRangeOnStationDataset() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
 				.param("accept", "netcdf")
 				.param("var", "air_temperature", "dew_point_temperature")
 				.param("subset", "bb")
 				.param("north", "43.0")
 				.param("south", "38.0")
 				.param("west", "-107.0")
 				.param("east", "-103.0")
 				.param("time_start","2013-08-25T06:00:00Z")
 				.param("time_end","2013-08-26T06:00:00Z");

    org.springframework.test.web.servlet.MvcResult result = this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().is(400))
            .andReturn();
		System.out.printf("%s%n", result.getResponse().getContentAsString());
 	}

  @Test
  public void getSubsetOnStationDataset() throws Exception{
 		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
 				.param("accept", "netcdf")
 				.param("var", "air_temperature", "dew_point_temperature")
 				.param("subset", "bb")
 				.param("north", "43.0")
 				.param("south", "38.0")
 				.param("west", "-107.0")
 				.param("east", "-103.0")
 				.param("time_start","2006-03-25T00:00:00Z")
 				.param("time_end","2006-03-28T00:00:00Z");

 		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().isOk())
 			.andExpect(MockMvcResultMatchers.content().contentType( SupportedFormat.NETCDF3.getResponseContentType() ));
 	}

	@Test
	public void getAllStnsOnStationDataset() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
				.param("accept", "netcdf")
				.param("subset", "stns")
				.param("stns", "all")
				.param("var", "air_temperature", "dew_point_temperature")
        .param("time_start","2006-03-25T00:00:00Z")
        .param("time_end","2006-03-26T00:00:00Z");


		this.mockMvc.perform( rb ).andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.content().contentType( SupportedFormat.NETCDF3.getResponseContentType() ));

	}

	@Test
	public void stationNotFoundStationDataset() throws Exception{
		RequestBuilder rb = MockMvcRequestBuilders.get(dataset).servletPath(dataset)
				.param("accept", "netcdf")
				.param("subset", "stns")
				.param("stns", "mock_station")
				.param("var", "air_temperature", "dew_point_temperature")
            .param("time_start","2006-03-25T00:00:00Z")
            .param("time_end","2006-04-28T00:00:00Z");

		this.mockMvc.perform( rb ).andExpect(new ResultMatcher(){
			public void match(MvcResult result) throws Exception{
				//result.getResponse().getContentAsByteArray()
				Exception ex =  result.getResolvedException();
				assertTrue( ex instanceof FeaturesNotFoundException);
			}
		} );
	}

}

