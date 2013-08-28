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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import thredds.server.ncSubset.params.RequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.server.ncSubset.view.PointDataStream;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * @author mhermida
 *
 */
public class GridAsPointDataStream implements NCSSPointDataStream {
	
	private DiskCache2 diskCache = null; 
	private SupportedFormat format;
	
	public GridAsPointDataStream(DiskCache2 diskCache, SupportedFormat format){
		this.diskCache = diskCache;
		this.format = format;
	}

	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.NCSSPointDataStream#pointDataStream(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, ucar.nc2.constants.FeatureType, java.lang.String, thredds.server.ncSubset.params.ParamsBean)
	 */
	@Override
	public void pointDataStream(HttpServletRequest req,
			HttpServletResponse res, FeatureDataset fd, String requestPathInfo,
			PointDataRequestParamsBean queryParams, SupportedFormat format) throws IOException, VariableNotContainedInDatasetException, UnsupportedOperationException, OutOfBoundariesException, TimeOutOfWindowException, ParseException, DateUnitException, InvalidRangeException {

		//Checking request format...			
		//SupportedFormat sf = getSupportedFormat(queryParams, SupportedOperation.POINT_REQUEST  );
		GridDataset gridDataset =(GridDataset) fd;					
		LatLonPoint point = new LatLonPointImpl(queryParams.getLatitude(), queryParams.getLongitude()); //Check if the point is within boundaries!!
		checkRequestedVars(gridDataset,  queryParams);
		Map<String, List<String>> groupVars= groupVarsByVertLevels(gridDataset, queryParams);
								
		if( !isPointWithinBoundaries(gridDataset, point, groupVars ) ){			
			throw  new OutOfBoundariesException("Requested Lat/Lon Point (+" + point + ") is not contained in the Data. "+
					"Data Bounding Box = " + gridDataset.getBoundingBox().toString2());
		}			

		List<CalendarDate> wantedDates = getRequestedDates( gridDataset, queryParams);		
		
				
		PointDataStream pds = PointDataStream.createPointDataStream(format, res.getOutputStream() );
		
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
	

	protected void checkRequestedVars(GridDataset gds, RequestParamsBean params) throws VariableNotContainedInDatasetException, UnsupportedOperationException{
		//Check vars
		//if var = all--> all variables requested
		if(params.getVar().get(0).equals("all")){
			params.setVar(NcssRequestUtils.getAllVarsAsList(gds));					
		}		

		//Check not only all vars are contained in the grid, also they have the same vertical coords
		Iterator<String> it = params.getVar().iterator();
		String varName = it.next();
		//GridDatatype grid = gds.findGridByShortName(varName);
		GridDatatype grid = gds.findGridDatatype(varName);
		if(grid == null) 
			throw new VariableNotContainedInDatasetException("Variable: "+varName+" is not contained in the requested dataset");

		CoordinateAxis1D vertAxis = grid.getCoordinateSystem().getVerticalAxis();
		CoordinateAxis1D newVertAxis = null;
		boolean sameVertCoord = true;

		while(sameVertCoord && it.hasNext()){
			varName = it.next();
			//grid = gds.findGridByShortName(varName);
			grid = gds.findGridDatatype(varName);
			if(grid == null) 
				throw new VariableNotContainedInDatasetException("Variable: "+varName+" is not contained in the requested dataset");

			newVertAxis = grid.getCoordinateSystem().getVerticalAxis();

			if( vertAxis != null ){
				if( vertAxis.equals(newVertAxis)){
					vertAxis = newVertAxis;
				}else{
					sameVertCoord = false;
				}
			}else{
				if(newVertAxis != null) sameVertCoord = false;
			}	
		}

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

	private Map<String, List<String>> groupVarsByVertLevels(GridDataset gds, PointDataRequestParamsBean params) throws VariableNotContainedInDatasetException{
		String no_vert_levels ="no_vert_level";
		List<String> vars = params.getVar();
		Map<String, List<String>> varsGroupsByLevels = new HashMap<String, List<String>>();

		for(String var :vars ){
			GridDatatype grid =gds.findGridDatatype(var);

			//Variables should have been checked before...  
			if(grid == null ){
				throw new VariableNotContainedInDatasetException("Variable: "+var+" is not contained in the requested dataset");
			}			


			CoordinateAxis1D axis = grid.getCoordinateSystem().getVerticalAxis();					

			String axisKey = null;
			if(axis == null){
				axisKey = no_vert_levels;
			}else{
				axisKey = axis.getShortName();
			}

			if( varsGroupsByLevels.containsKey(axisKey) ){
				varsGroupsByLevels.get(axisKey).add(var);
			}else{
				List<String> varListForVerlLevel = new ArrayList<String>();
				varListForVerlLevel.add(var);
				varsGroupsByLevels.put(axisKey, varListForVerlLevel);
			} 			 			 
		}

		return varsGroupsByLevels;
	}
	
	protected List<CalendarDate> getRequestedDates(GridDataset gds, RequestParamsBean params) throws OutOfBoundariesException, ParseException, TimeOutOfWindowException{
		
		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds, params.getVar());
		List<CalendarDate> dates = gap.getDates();	
		
		if(dates.isEmpty() ) return dates;
		
		long time_window =0;
		if( params.getTime_window() != null ){
			TimeDuration dTW = new TimeDuration(params.getTime_window());
			time_window = (long)dTW.getValueInSeconds()*1000;
		}		
				
		//Check param temporal=all (ignore any other value) --> returns all dates
		if(params.getTemporal()!= null && params.getTemporal().equals("all") ){			
			return dates;			
		}else{ //Check if some time param was provided, if not closest time to current
			if(params.getTime()==null && params.getTime_start()==null && params.getTime_end()==null && params.getTime_duration()==null ){
				//Closest to present
				List<CalendarDate> closestToPresent = new ArrayList<CalendarDate>();
				
				CalendarDate now = CalendarDate.of(new Date());
				CalendarDate start = dates.get(0);
				CalendarDate end  = dates.get(dates.size()-1);
				if( now.isBefore(start) ){ 
					//now = start;
					if( time_window <= 0 || Math.abs(now.getDifferenceInMsecs(start)) < time_window ){
						closestToPresent.add(start);					
						return closestToPresent;
					}else{
						throw new TimeOutOfWindowException("There is no time within the provided time window");
					}	
				}
				if( now.isAfter(end) ){
					//now = end;
					if( time_window <=0 || Math.abs(now.getDifferenceInMsecs(end)) < time_window ){
						closestToPresent.add(end);
						return closestToPresent;
					}else{
						throw new TimeOutOfWindowException("There is no time within the provided time window");
					}	
				}
								
				return  NcssRequestUtils.wantedDates(gap, CalendarDateRange.of(now,now), time_window);				
				
				}
			}
		//We should have a time or a timeRange...
		if(params.getTime_window()!=null && params.getTime()!=null){
			DateRange dr = new DateRange( new DateType(params.getTime(), null, null ), null, new TimeDuration(params.getTime_window()), null );			
			//time_window = CalendarDateRange.of(dr).getDurationInSecs()*1000;			
			time_window = CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate()).getDurationInSecs()*1000; 
									
		}
		CalendarDateRange dateRange = getRequestedDateRange(params);		
		return NcssRequestUtils.wantedDates(gap, dateRange, time_window );	
	}	
	
	
	
	CalendarDateRange getRequestedDateRange(RequestParamsBean params) throws ParseException{
		
		if(params.getTime()!=null){			
			CalendarDate date=null;			
			if( params.getTime().equalsIgnoreCase("present") ){
				date =CalendarDate.of(new Date());
			}else{			
				
				//date = CalendarDate.of( CalendarDateFormatter.isoStringToDate(params.getTime())  );	
				date =  CalendarDateFormatter.isoStringToCalendarDate(null, params.getTime());
			}						
			return CalendarDateRange.of(date,date);		
		}	
		//We should have valid params here...
		CalendarDateRange dates=null;
		DateRange dr = new DateRange( new DateType(params.getTime_start() , null, null), new DateType(params.getTime_end(), null, null), new TimeDuration(params.getTime_duration()), null );		
	    //dates = CalendarDateRange.of(dr);
		dates = CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate()  );
				
		return dates;
	}	
	
	
	
	
}
