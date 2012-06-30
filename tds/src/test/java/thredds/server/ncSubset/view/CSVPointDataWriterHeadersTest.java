package thredds.server.ncSubset.view;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.params.PointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.controller.SupportedFormat;
import thredds.servlet.DatasetHandlerAdapter;
import ucar.nc2.dt.GridDataset;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class CSVPointDataWriterHeadersTest {
	
	private static final String CONTENT_LOCATION_HEADER_KEY = "Content-Location";	
	private static final String CONTENT_DISPOSITION_HEADER_KEY = "Content-Disposition";
	
	private PointDataWriter pointDataWriter;
	
	private String pathInfo = "unitTests/GFS/CONUS_80km/GFS_CONUS_80km_20120419_0000.nc";
	private GridDataset gridDataset;
	private String location;
	
	@Before
	public void setUp() throws IOException{
		pointDataWriter = AbstractPointDataWriterFactory.createPointDataWriterFactory(SupportedFormat.CSV).createPointDataWriter(new ByteArrayOutputStream());		
		gridDataset = DatasetHandlerAdapter.openGridDataset(pathInfo);
		location =gridDataset.getLocationURI();
	}
	
	@Test
	public void shouldHaveContentHeaders(){
		
		pointDataWriter.setHTTPHeaders(gridDataset);
		assertTrue(pointDataWriter.getResponseHeaders().containsKey(CONTENT_LOCATION_HEADER_KEY) );
		assertTrue(pointDataWriter.getResponseHeaders().containsKey(CONTENT_DISPOSITION_HEADER_KEY) );
	}
	
	@Test
	public void contentLocationMustContaintDatasetName(){
		 
		pointDataWriter.setHTTPHeaders(gridDataset);
		String content_location = pointDataWriter.getResponseHeaders().get(CONTENT_LOCATION_HEADER_KEY).get(0);
		assertTrue( content_location.endsWith(".csv") );
		String content_disposition = pointDataWriter.getResponseHeaders().get(CONTENT_DISPOSITION_HEADER_KEY).get(0);
		content_disposition.contains(gridDataset.getLocationURI());
	} 

}
