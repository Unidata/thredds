package thredds.servlet;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.params.PathInfoParams;
import thredds.mock.web.MockTdsContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
public class DataRootHandlerTest {
	
	
	private String reqPath;
		
	@Test
	public void testAliasExpandersDatasetScan(){
		
		//datasetScan request path 
		//reqPath ="/opendapTest/GFS_Puerto_Rico_191km_20100515_0000.grib1";
		reqPath =PathInfoParams.getPatInfo().get(1);
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
		assertNotNull(match);
								
	}
	
	@Test
	public void testAliasExpandersDatasetFeaturecollection(){
		//featureCollection request path
		reqPath =PathInfoParams.getPatInfo().get(3);
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
		assertNotNull(match);		
	}

}
