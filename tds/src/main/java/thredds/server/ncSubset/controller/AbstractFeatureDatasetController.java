/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;

import thredds.server.ncSubset.exception.NcssException;

/**
 * @author mhermida
 *
 */
public class AbstractFeatureDatasetController {
	
	protected static final String servletPath = "/ncss";

	protected static final String servletCachePath = "/cache/ncss";
	
	static private final Logger log = LoggerFactory.getLogger(AbstractFeatureDatasetController.class);
	
  protected void handleValidationErrorsResponse(HttpServletResponse response, int status, BindingResult validationResult) {

 		List<ObjectError> errors = validationResult.getAllErrors();
 		response.setStatus(status);
 		// String responseStr="Validation errors: ";
 		StringBuffer responseStr = new StringBuffer();
 		responseStr.append("Validation errors: ");
 		for (ObjectError err : errors) {
 			responseStr.append(err.getDefaultMessage());
 			responseStr.append("  -- ");
 		}

 		try {
 			PrintWriter pw = response.getWriter();
 			pw.write(responseStr.toString());
 			pw.flush();

 		} catch (IOException ioe) {
 			log.error(ioe.getMessage());
 		}

 	}

  protected void handleValidationErrorMessage(HttpServletResponse response, int status, String errorMessage) {

 		response.setStatus(status);

 		try {
 			PrintWriter pw = response.getWriter();
 			pw.write(errorMessage);
 			pw.flush();

 		} catch (IOException ioe) {
 			log.error(ioe.getMessage());
 		}

 	}

	// Exception handlers
	@ExceptionHandler(NcssException.class)
	public ResponseEntity<String> handle(NcssException ncsse) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<String>(
				"NetCDF Subset Service exception handled : " + ncsse.getMessage(), responseHeaders,
				HttpStatus.BAD_REQUEST);
	}
	
	// Exception handlers
	@ExceptionHandler(UnsupportedOperationException.class)
	public ResponseEntity<String> handle(UnsupportedOperationException ex) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<String>(
				"UnsupportedOperationException exception handled : " + ex.getMessage(), responseHeaders,
				HttpStatus.BAD_REQUEST);
	}	
	
	// Exception handlers
	@ExceptionHandler(Throwable.class)
	public ResponseEntity<String> handle(Throwable ex) {
    ex.printStackTrace();
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<String>("Throwable exception handled : " + ex.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public static final String getNCSSServletPath() {
		return servletPath;
	}

	public static final String getServletCachePath() {
		return servletCachePath;
	}	

}
