package thredds.server.ncSubset.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.RequestParamsBean;
import ucar.nc2.dt.GridDataset;

public abstract class AbstractNcssDataRequestController extends AbstractNcssController {

	static private final Logger log = LoggerFactory.getLogger("threddsServlet");
	
	/**
	 * Checks all requested variables are in the dataset for a Ncss requests. 
	 * 
	 * @param gds
	 * @param params
	 * @throws VariableNotContainedInDatasetException
	 * @throws UnsupportedOperationException 
	 */
	protected abstract void checkRequestedVars(GridDataset gds, RequestParamsBean params) throws VariableNotContainedInDatasetException, UnsupportedOperationException;
	
	String extractRequestPathInfo(String requestPathInfo){
		
		/*if( requestPathInfo.endsWith("xml") || requestPathInfo.endsWith("html")   ){
			requestPathInfo = requestPathInfo.trim(); 
			String[] pathInfoArr = requestPathInfo.split("/");			  
			StringBuilder sb = new StringBuilder();
			int len = pathInfoArr.length;
			sb.append(pathInfoArr[1]);
			for(int i= 2;  i<len-1; i++  ){
				sb.append("/"+pathInfoArr[i]);
			}
			requestPathInfo = sb.toString();
		}*/
		this.requestPathInfo = requestPathInfo;
		
		return requestPathInfo;
		
	}
	
	/**
	 * 
	 * Handles internal errors and writes the stacktrace to the threddsServlet log.
	 * 
	 * @param e
	 * @return
	 */
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody String handle(Exception e){
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		log.error( errors.toString() );
		return "Exception handled: "+e.getMessage();
	}	
}
