/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.config.TdsContext;
import thredds.server.ncSubset.NCSSPointDataStream;
import thredds.server.ncSubset.NCSSPointDataStreamFactory;
import thredds.server.ncSubset.dataservice.DatasetService;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;

/**
 * @author mhermida
 *
 */
@Controller
@RequestMapping("ncss_new/")
public class FeatureDatasetController {
	
	static private final Logger log = LoggerFactory.getLogger(FeatureDatasetController.class);
	
	protected static final String servletPath = "/ncss_new/";
	
	@Autowired
	DatasetService datasetService;
	
	@Autowired
	TdsContext tdsContext;
	
	/**
	 * 
	 * Handles ncss point data requests for GRID and STATION feature datasets.
	 * 
	 * 
	 * @param req
	 * @param res
	 * @throws IOException
	 * @throws UnsupportedResponseFormatException 
	 * @throws InvalidRangeException 
	 * @throws ParseException 
	 */
	@RequestMapping("**")				
	public void streamPointData(HttpServletRequest req,
			HttpServletResponse res, @Valid PointDataRequestParamsBean params, BindingResult  validationResult) throws IOException, UnsupportedResponseFormatException, NcssException, ParseException, InvalidRangeException{
		
		if( validationResult.hasErrors() ){
			handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult );
		}else{
		
			SupportedFormat format = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.POINT_REQUEST);
			
			String datasetPath = getDatasetPath(req);
			FeatureDataset fd = datasetService.findDatasetByPath(req, res, datasetPath);
		
			if(fd == null){
				//FeatureDataset not supported!!!
				throw new UnsupportedOperationException("Feature Type not supported");
			}
				
			FeatureType ft = fd.getFeatureType();
			//Create a point data streamer depending on the feature
			NCSSPointDataStream pds =   NCSSPointDataStreamFactory.getDataStreamer(ft, tdsContext, format);
			pds.pointDataStream(req, res, fd, datasetPath, params, format);
		}					
	}
	
	private String getDatasetPath(HttpServletRequest req){
		
		String servletPath = req.getServletPath();
		String[] servletPathTokens = servletPath.split("/");
		String lastToken = servletPathTokens[servletPathTokens.length-1];
		if( lastToken.endsWith(".html") || lastToken.endsWith(".xml") ){
			servletPath = servletPath.substring(0, servletPath.length() - lastToken.length() - 1);
		}
		
		return servletPath.substring(FeatureDatasetController.servletPath.length() , servletPath.length());
	}
	
	protected void handleValidationErrorsResponse(HttpServletResponse response, int status, BindingResult  validationResult){
		
		List<ObjectError> errors = validationResult.getAllErrors();
		response.setStatus(status);
		//String responseStr="Validation errors: ";
		StringBuffer responseStr = new StringBuffer();
		responseStr.append("Validation errors: ");
		for(ObjectError err : errors){			
			responseStr.append(err.getDefaultMessage());
			responseStr.append("  -- ");
		}
				
		try{
			
			PrintWriter pw = response.getWriter();
			pw.write(responseStr.toString() );
			pw.flush();
			
		}catch(IOException ioe){
			log.error(ioe.getMessage()); 
		}	
		
	}	

}
