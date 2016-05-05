/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package thredds.server.ncss.controller.gridaspoint;

import org.junit.Assert;
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
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "/WEB-INF/applicationContext.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridAsPointMisc {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Before
  public void setup(){
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  public void fileNotFound() throws Exception {
    RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/cdmUnitTest/ncss/GFS/CONUS_80km/baddie.nc")
            .servletPath("/ncss/grid/cdmUnitTest/ncss/GFS/CONUS_80km/baddie.nc")
            .param("accept", "netcdf" )
            .param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground")
            .param("latitude", "40.019")
            .param("longitude", "-105.293");

    this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().is(404));
  }

  @Test
  public void getGridAsPointSubsetAllSupportedFormats() throws Exception{
    for (SupportedFormat sf : SupportedOperation.GRID_AS_POINT_REQUEST.getSupportedFormats()) {
      RequestBuilder rb = MockMvcRequestBuilders.get("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
              .servletPath("/ncss/grid/testGFSfmrc/GFS_CONUS_80km_nc_best.ncd")
              .param("accept", sf.toString())
              .param("var", "Relative_humidity_height_above_ground", "Temperature_height_above_ground")
              .param("latitude", "40.019")
              .param("longitude", "-105.293");

      System.out.printf("getGridAsPointSubsetAllSupportedFormats return type=%s%n", sf);

      MvcResult mvcResult = this.mockMvc.perform(rb).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
      String ct = mvcResult.getResponse().getContentType();
      Assert.assertTrue(ct.startsWith(sf.getMimeType()));
    }
  }

}
