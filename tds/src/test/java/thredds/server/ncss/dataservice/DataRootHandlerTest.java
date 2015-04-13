package thredds.server.ncss.dataservice;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.mock.params.GridPathParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.core.DataRootManager;
import thredds.server.ncss.controller.AbstractNcssController;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.util.List;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext.xml"}, loader=MockTdsContextLoader.class)
@Category(NeedsCdmUnitTest.class)
public class DataRootHandlerTest {

  @Autowired
  DataRootManager matcher;

  @SpringJUnit4ParameterizedClassRunner.Parameters
 	public static List<String[]> getTestParameters(){
 		return GridPathParams.getPathInfoAsListOfArrays();
 	}
  private String pathInfo;

 	public DataRootHandlerTest(String pathInfo){
 		this.pathInfo=pathInfo;
 	}

  @Test
  public void testAllRoots() {
    System.out.printf("pathInfo=%s%n", pathInfo);
    String datasetPath = AbstractNcssController.getDatasetPath(this.pathInfo);
    DataRootManager.DataRootMatch match = matcher.findDataRootMatch(datasetPath);
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
