/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
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
package thredds.server.ncss.dataservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Formatter;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.servlet.DataRootHandler;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * 
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class FeatureDatasetTypeTest {

	@Autowired
	FeatureDatasetService featureDatasetService;
	
	@Test
	public void getFDForSingleFileGridDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		FeatureDataset fd = featureDatasetService.findDatasetByPath(req, res, "cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc");
    assertNotNull(fd);
		assertEquals(FeatureType.GRID, fd.getFeatureType());
	}
			
	@Test
	public void getFDForSingleFileStationDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		FeatureDataset fd = featureDatasetService.findDatasetByPath(req, res, "cdmUnitTest/ncss/point_features/metar/Surface_METAR_20130826_0000.nc");
		assertNotNull(fd);
		assertEquals(FeatureType.STATION, fd.getFeatureType());
	}
	
	
	@Test
	public void getFDForDatasetScanGridDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		FeatureDataset fd = featureDatasetService.findDatasetByPath(req, res, "testGridScan/GFS_CONUS_80km_20120227_0000.grib1");
    assertNotNull(fd);
		assertEquals(FeatureType.GRID, fd.getFeatureType());
	}
	
	@Test
	public void getFDForDatasetScanStationDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();

    String reqPath =  "testStationScan/Surface_METAR_20130826_0000.nc";
		FeatureDataset fd = featureDatasetService.findDatasetByPath(req, res, reqPath);
    if (fd == null) {
      DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
      if (match == null) {
        Formatter f = new Formatter();
        DataRootHandler.getInstance().showRoots(f);
        System.out.printf("DataRoots%n%s%n", f);
      }

    }
    assertNotNull(fd);
		assertEquals(FeatureType.STATION, fd.getFeatureType());
	}
	
	@Test
	public void getFDForFeatureCollectionGridDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();

		FeatureDataset fd = featureDatasetService.findDatasetByPath(req, res, "testGFSfmrc/GFS_CONUS_80km_nc_best.ncd");
    assertNotNull(fd);
		assertEquals(FeatureType.GRID, fd.getFeatureType());
	}
	
	@Test
	public void getFDForFeatureCollectionStationDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		
		FeatureDataset fd = featureDatasetService.findDatasetByPath(req, res, "testStationFeatureCollection/Metar_Station_Data_fc.cdmr");
    assertNotNull(fd);
		assertEquals(FeatureType.STATION, fd.getFeatureType());
	}
}
