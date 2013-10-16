package thredds.server.ncSubset.dataservice;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.mock.params.PathInfoParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.controller.AbstractFeatureDatasetController;
import thredds.servlet.DataRootHandler;

import java.util.List;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
public class DataRootHandlerTest {

  @SpringJUnit4ParameterizedClassRunner.Parameters
 	public static List<String[]> getTestParameters(){
 		return PathInfoParams.getPathInfoAsListOfArrays();
 	}
  private String pathInfo;

 	public DataRootHandlerTest(String pathInfo){
 		this.pathInfo=pathInfo;
 	}

  @Test
  public void testAllRoots() {
    System.out.printf("pathInfo=%s%n", pathInfo);
    String datasetPath = AbstractFeatureDatasetController.getDatasetPath(this.pathInfo);
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(datasetPath);
		assertNotNull(match);
 	}
		
	/* @Test
	public void testAliasExpandersDatasetScan(){
		
		//datasetScan request path 
		//reqPath ="/opendapTest/GFS_Puerto_Rico_191km_20100515_0000.grib1";
		reqPath =PathInfoParams.getPathInfo().get(1);
    System.out.printf("reqPath=%s%n", reqPath);
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
		assertNotNull(match);
								
	}
	
	@Test
	public void testAliasExpandersDatasetFeaturecollection(){
		//featureCollection request path
		reqPath =PathInfoParams.getPathInfo().get(3);
    System.out.printf("reqPath=%s%n", reqPath);
		DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);
		assertNotNull(match);		
	}    */

}
