package thredds.server.ncSubset.dataservice;

import java.io.IOException;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import thredds.server.ncSubset.controller.AbstractNcssController;
import thredds.servlet.DatasetHandler;
import ucar.nc2.dt.GridDataset;

/**
 * 
 * Adapter class that opens GridDataset only taking the pathtInfo.
 * It mocks a request and a response 
 * 
 * @author mhermida
 *
 */
public final class DatasetHandlerAdapter {
	
	private DatasetHandlerAdapter(){}
	
	public static final GridDataset openGridDataset(String pathInfo) throws IOException{
		
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res= new MockHttpServletResponse();
		
		req.setPathInfo(pathInfo);
		
    String datasetPath = AbstractNcssController.getDatasetPath(pathInfo);
		return DatasetHandler.openGridDataset(req, res, datasetPath);
	}

}
