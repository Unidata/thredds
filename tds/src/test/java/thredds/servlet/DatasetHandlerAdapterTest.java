package thredds.servlet;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.params.PathInfoParams;
import thredds.mock.web.MockTdsContextLoader;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dt.GridDataset;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml"}, loader=MockTdsContextLoader.class)
public class DatasetHandlerAdapterTest {

	private String pathInfo;
	
	public DatasetHandlerAdapterTest(String pathInfo){
		this.pathInfo=pathInfo;
	}
	
	@Parameters
	public static List<String[]> getTestParameters(){
		
		return PathInfoParams.getPathInfoAsListOfArrays();
 
	}
	

	@Test
	public void shouldGetGridDataset() throws IOException{
		
		GridDataset gds =DatasetHandlerAdapter.openGridDataset(pathInfo);
		
		assertNotNull(gds);
	}
}
