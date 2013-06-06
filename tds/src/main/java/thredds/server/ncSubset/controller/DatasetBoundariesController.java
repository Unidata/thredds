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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.ParamsBean;
import ucar.nc2.dt.grid.gis.GridBoundariesExtractor;



/**
 * @author mhermida
 *
 */
@Controller
@Scope("request")
@RequestMapping(value="/ncss/grid/**")
public class DatasetBoundariesController extends AbstractNcssController{ 

	static private final Logger log = LoggerFactory.getLogger(DatasetBoundariesController.class);
	
	@RequestMapping(value = { "datasetBoundaries" } )
	void getDatasetBoundaries(ParamsBean params, HttpServletRequest req, HttpServletResponse res) throws IOException, UnsupportedResponseFormatException{
		
		//Checking request format...			
		SupportedFormat sf = getSupportedFormat( params, SupportedOperation.DATASET_BOUNDARIES_REQUEST  );				
		String boundaries = getBoundaries( sf );		

		res.setContentType(sf.getResponseContentType());
		
		res.getWriter().write(boundaries);
		res.getWriter().flush();
		
	}
	
	private String getBoundaries(SupportedFormat format){
		
		String boundaries ="";
		GridBoundariesExtractor gbe =GridBoundariesExtractor.valueOf(gridDataset);
		
		if( format == SupportedFormat.WKT )
			boundaries = gbe.getDatasetBoundariesWKT();
		if( format == SupportedFormat.JSON )
			boundaries = gbe.getDatasetBoundariesGeoJSON();
						
		return boundaries;
	}
	


	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.controller.AbstractNcssController#extractRequestPathInfo(java.lang.String)
	 */
	@Override
	String extractRequestPathInfo(String requestPathInfo) {

		if( requestPathInfo.endsWith("datasetBoundaries")  ){
			requestPathInfo = requestPathInfo.trim(); 
			String[] pathInfoArr = requestPathInfo.split("/");			  
			StringBuilder sb = new StringBuilder();
			int len = pathInfoArr.length;
			sb.append(pathInfoArr[1]);
			for(int i= 2;  i<len-1; i++  ){
				sb.append("/"+pathInfoArr[i]);
			}
			requestPathInfo = sb.toString();
		}
		
		this.requestPathInfo = requestPathInfo;
		return requestPathInfo;
	}

	
}
