package thredds.servlet;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import ucar.nc2.dt.GridDataset;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
public class DatasetHandlerAdapterTest {

	@Test
	public void shouldGetGridDataset() throws IOException{
		String pathInfo ="/ncss_tests/files/GFS_CONUS_80km_20120229_1200.grib1";
		
		GridDataset gds = DatasetHandlerAdapter.openGridDataset(pathInfo);
		
		assertNotNull(gds);
	}
}
