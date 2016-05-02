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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.controller.NcssController;
import thredds.server.ncss.exception.RequestTooLargeException;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class GridRequestException2Test {

	@Autowired
	private NcssController featureDatasetController;

	private MockHttpServletResponse response ;
	private MockHttpServletRequest request;

  //   <featureCollection featureType="GRIB" name="GFS_CONUS_80km" path="gribCollection/GFS_CONUS_80km">
	private String pathInfo="/ncss/gribCollection/GFS_CONUS_80km/best";

	@Before
	public void setUp() throws IOException{

		response = new MockHttpServletResponse();
		request = new MockHttpServletRequest();
		request.setPathInfo(pathInfo);
		request.setServletPath(pathInfo);

	}

	@Test(expected=RequestTooLargeException.class)
	public void testRequestTooLargeException() throws Exception{

    NcssParamsBean params;
		BindingResult validationResult;
		params = new NcssParamsBean();
		params.setTemporal("all");
		List<String> vars = new ArrayList<>();
		vars.add("u-component_of_wind_isobaric");
		vars.add("v-component_of_wind_isobaric");
		vars.add("Geopotential_height_isobaric");
		params.setVar(vars);
		validationResult = new BeanPropertyBindingResult(params, "params");
		featureDatasetController.handleRequest(request, response, params, validationResult);
	}

	@After
	public void tearDown() throws IOException{

		//GridDataset gds = gridDataController.getGridDataset();
		//gds.close();
		//gds = null;
		//gridDataController =null;

	}

}
