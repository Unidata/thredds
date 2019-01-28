/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.dataservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.core.DataRootManager;
import thredds.core.TdsRequestedDataset;
import thredds.mock.web.MockTdsContextLoader;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryFeatureDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * 
 * @author mhermida
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class FeatureDatasetTypeTest {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    DataRootManager matcher;
	
	@Test
	public void getFDForSingleFileGridDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		FeatureDataset fd = TdsRequestedDataset.getGridDataset(req, res, "cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc");
    assertNotNull(fd);
		assertEquals(FeatureType.GRID, fd.getFeatureType());
	}
			
	@Test
	public void getFDForSingleFileStationDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		FeatureDataset fd = TdsRequestedDataset.getPointDataset(req, res, "cdmUnitTest/ncss/point_features/metar/Surface_METAR_20130826_0000.nc");
		assertNotNull(fd);
		assertEquals(FeatureType.STATION, fd.getFeatureType());
	}
	
	
	@Test
	public void getFDForDatasetScanGridDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		FeatureDataset fd = TdsRequestedDataset.getGridDataset(req, res, "testGridScan/GFS_CONUS_80km_20120227_0000.grib1");
		assertNotNull(fd);
		assertEquals(FeatureType.GRID, fd.getFeatureType());
	}
	
	@Test
	public void getFDForDatasetScanStationDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();

    String reqPath =  "testStationScan/Surface_METAR_20130826_0000.nc";
		FeatureDataset fd = TdsRequestedDataset.getPointDataset(req, res, reqPath);
    if (fd == null) {
      DataRootManager.DataRootMatch match = matcher.findDataRootMatch(reqPath);
      if (match == null) {
        Formatter f = new Formatter();
        matcher.showRoots(f);
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

		FeatureDataset fd = TdsRequestedDataset.getGridDataset(req, res, "testGFSfmrc/GFS_CONUS_80km_nc_best.ncd");
		assertNotNull(fd);
		assertEquals(FeatureType.GRID, fd.getFeatureType());
	}
	
	@Test
	public void getFDForFeatureCollectionStationDataset() throws IOException{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		
		FeatureDataset fd = TdsRequestedDataset.getPointDataset(req, res, "testStationFeatureCollection/Metar_Station_Data_fc.cdmr");
		assertNotNull(fd);
		assertEquals(FeatureType.STATION, fd.getFeatureType());
	}

}
