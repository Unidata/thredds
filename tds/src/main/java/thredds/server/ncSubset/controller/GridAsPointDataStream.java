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
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.NCSSPointDataStream;
import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.ParamsBean;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import thredds.server.ncSubset.view.PointDataStream;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * @author mhermida
 *
 */
public class GridAsPointDataStream extends GridDatasetSubsetter implements NCSSPointDataStream {
	
	private DiskCache2 diskCache = null; 
	private SupportedFormat format;
	
	private PointDataStream pds;

	public GridAsPointDataStream(DiskCache2 diskCache, SupportedFormat format, OutputStream out ){
		this.diskCache = diskCache;
		this.format = format;
		pds = PointDataStream.createPointDataStream(format,  out);
	}

	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.NCSSPointDataStream#pointDataStream(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, ucar.nc2.constants.FeatureType, java.lang.String, thredds.server.ncSubset.params.ParamsBean)
	 */
	@Override
	public void pointDataStream(HttpServletResponse res, FeatureDataset fd, String requestPathInfo,
			PointDataRequestParamsBean queryParams, SupportedFormat format) throws IOException, VariableNotContainedInDatasetException, UnsupportedOperationException, OutOfBoundariesException, TimeOutOfWindowException, ParseException, DateUnitException, InvalidRangeException {
	
		GridDataset gridDataset =(GridDataset) fd;					
		LatLonPoint point = new LatLonPointImpl(queryParams.getLatitude(), queryParams.getLongitude()); //Check if the point is within boundaries!!
		checkRequestedVars(gridDataset, queryParams);
		Map<String, List<String>> groupVars= groupVarsByVertLevels(gridDataset, queryParams);
								
		if( !isPointWithinBoundaries(gridDataset, point, groupVars ) ){			
			throw  new OutOfBoundariesException("Requested Lat/Lon Point (+" + point + ") is not contained in the Data. "+
					"Data Bounding Box = " + gridDataset.getBoundingBox().toString2());
		}			

		List<CalendarDate> wantedDates = getRequestedDates( gridDataset, queryParams);								
		

		//Get point, wDates, groupedVars and vertCoort from params.				
		pds.stream(gridDataset, point, wantedDates, groupVars, queryParams.getVertCoord());
	}

	protected SupportedFormat getSupportedFormat(ParamsBean params, SupportedOperation operation) throws UnsupportedResponseFormatException{
		
		//Cheking request format...
		SupportedFormat sf;		
		if(params.getAccept() == null){
			//setting the default format
			sf = operation.getDefaultFormat();
			params.setAccept(sf.getFormatName());
		}else{		
			sf = SupportedOperation.isSupportedFormat(params.getAccept(), operation);
			if( sf == null ){			
				throw new UnsupportedResponseFormatException("Requested format: "+params.getAccept()+" is not supported for "+operation.getOperation().toLowerCase() );
			}
		}
		
		return sf;		
	}
	

	private boolean isPointWithinBoundaries(GridDataset  gridDataset, LatLonPoint point, Map<String, List<String>> groupVars){	
		//LatLonRect bbox = gds.getBoundingBox();
		boolean isInData = true;
		List<String> keys = new ArrayList<String>(groupVars.keySet());

		int[] xy = new int[2];
		Iterator<String> it = keys.iterator();

		while( it.hasNext() && isInData ){
			String key = it.next();
			GridDatatype grid = gridDataset.findGridDatatype(groupVars.get(key).get(0));
			GridCoordSystem coordSys = grid.getCoordinateSystem();
			ProjectionPoint p = coordSys.getProjection().latLonToProj(point);
			xy = coordSys.findXYindexFromCoord(p.getX(), p.getY(), null);

			if(xy[0] < 0 || xy[1] < 0  ){
				isInData = false;
			}
		}

		return isInData;
	}

	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.NCSSPointDataStream#setResponseHeaders(ucar.nc2.constants.FeatureType, thredds.server.ncSubset.format.SupportedFormat, java.lang.String)
	 */
	@Override
	public HttpHeaders getResponseHeaders(FeatureDataset fd, SupportedFormat format,
			String datasetPath) {

		return pds.getHttpHeaders((GridDataset)fd, datasetPath);
	}



}
