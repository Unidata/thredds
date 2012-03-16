package thredds.servlet;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import thredds.server.config.TdsContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class DataRootHandlerTest {
	
	@Autowired
	private TdsContext tdsContext;
	
	@Autowired
	private DataRootHandler tdsDRH;
	
	@Autowired
	private MockServletContext servletContext;
	
	private String reqPath;
		
	@Before
	public void setUp() throws Exception {		
				
		
		tdsContext.init(servletContext);
		tdsDRH.registerConfigListener( new RestrictedAccessConfigListener() );
		tdsDRH.init();		
	}	
	
	@Test
	public void testAliasExpandersDatasetScan(){
		
		//datasetScan request path 
		reqPath ="/opendapTest/GFS_Puerto_Rico_191km_20100515_0000.grib1";
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
		assertNotNull(match);
								
	}
	
	@Test
	public void testAliasExpandersDatasetFeaturecollection(){
		//featureCollection request path
		reqPath ="/hioos/model/wav/swan/oahu/runs/SWAN_Oahu_Regional_Wave_Model_(500m)_RUN_2011-07-12T00:00:00.000Z";
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
		assertNotNull(match);		
	}

}
