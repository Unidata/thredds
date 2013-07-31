package thredds.server.ncSubset.controller;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handle(Exception e, HttpServletResponse response){
		
		log.error("Exception handled in AbstractNcssDataRequestController", e);
		response.reset();
		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<String>( "Exception handled: "+e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);		
		
	}	
}
