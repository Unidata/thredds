/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.LastModified;

import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.params.RequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DataRootHandler;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

public abstract class AbstractNcssController implements LastModified{
	
	static private final Logger log = LoggerFactory.getLogger(AbstractNcssController.class);
	
	protected static final String NETCDF_FORMAT_NAME = "NETCDF";
	
	protected static final String servletPath = "/ncss/grid";
	
	protected static final String servletCachePath = "/cache/ncss";
	
	//The interceptor sets these properties 
	protected String requestPathInfo = null;
	protected GridDataset gridDataset = null;		
	
	void setRequestPathInfo(String requestPathInfo) {
		this.requestPathInfo = requestPathInfo;
	}

	public String getRequestPathInfo() {
		return this.requestPathInfo;
	}

	void setGridDataset(GridDataset gds) {
		this.gridDataset = gds;
	}

	public GridDataset getGridDataset() {
		return this.gridDataset;
	}
	
	
	
	/**
	 *   
	 * @param gds
	 * @param params
	 * @return
	 * @throws OutOfBoundariesException
	 * @throws ParseException
	 * @throws TimeOutOfWindowException 
	 */
	protected List<CalendarDate> getRequestedDates(GridDataset gds, RequestParamsBean params) throws OutOfBoundariesException, ParseException, TimeOutOfWindowException{
		
		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds, params.getVar());
		long time_window =0;
		if( params.getTime_window() != null ){
			TimeDuration dTW = new TimeDuration(params.getTime_window());
			time_window = (long)dTW.getValueInSeconds()*1000;
		}		
				
		//Check param temporal=all (ignore any other value) --> returns all dates
		if(params.getTemporal()!= null && params.getTemporal().equals("all") ){			
			return gap.getDates();			
		}else{ //Check if some time param was provided, if not closest time to current
			if(params.getTime()==null && params.getTime_start()==null && params.getTime_end()==null && params.getTime_duration()==null ){
				//Closest to present
				List<CalendarDate> closestToPresent = new ArrayList<CalendarDate>();
				
				CalendarDate now = CalendarDate.of(new Date());
				CalendarDate start = gap.getDates().get(0);
				CalendarDate end  = gap.getDates().get(gap.getDates().size()-1);
				if( now.isBefore(start) ){ 
					//now = start;
					if( time_window <= 0 || Math.abs(now.getDifferenceInMsecs(start)) < time_window ){
						closestToPresent.add(start);					
						return closestToPresent;
					}else{
						throw new TimeOutOfWindowException("There is not time within the provided time window");
					}	
				}
				if( now.isAfter(end) ){
					//now = end;
					if( time_window <=0 || Math.abs(now.getDifferenceInMsecs(end)) < time_window ){
						closestToPresent.add(end);
						return closestToPresent;
					}else{
						throw new TimeOutOfWindowException("There is not time within the provided time window");
					}	
				}
								
				return  NcssRequestUtils.wantedDates(gap, CalendarDateRange.of(now,now), time_window);				
				
				}
			}
		//We should have a time or a timeRange...
		if(params.getTime_window()!=null && params.getTime()!=null){
			DateRange dr = new DateRange( new DateType(params.getTime(), null, null ), null, new TimeDuration(params.getTime_window()), null );
			time_window = CalendarDateRange.of(dr).getDurationInSecs()*1000;			
		}
		CalendarDateRange dates = getRequestedDateRange(params);		
		return NcssRequestUtils.wantedDates(gap, dates, time_window );
	}
	
	/** 
	 * 
	 * @param params
	 * @return
	 * @throws ParseException
	 */
	CalendarDateRange getRequestedDateRange(RequestParamsBean params) throws ParseException{
						
		if(params.getTime()!=null){			
			CalendarDate date=null;			
			if( params.getTime().equalsIgnoreCase("present") ){
				date =CalendarDate.of(new Date());
			}else{
				date = CalendarDate.of( CalendarDateFormatter.isoStringToDate(params.getTime())  );				
			}						
			return CalendarDateRange.of(date,date);		
		}	
		//We should have valid params here...
		CalendarDateRange dates=null;
		DateRange dr = new DateRange( new DateType(params.getTime_start() , null, null), new DateType(params.getTime_end(), null, null), new TimeDuration(params.getTime_duration()), null );		
	    dates = CalendarDateRange.of(dr);
				
		return dates;
	}
	
	protected void handleValidationErrorsResponse(HttpServletResponse response, int status, BindingResult  validationResult){
		
		List<ObjectError> errors = validationResult.getAllErrors();
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
	
	protected void setResponseHeaders(HttpServletResponse response, HttpHeaders httpHeaders){
		
		Set<String> keySet = httpHeaders.keySet();
		Iterator<String> it = keySet.iterator();
		while( it.hasNext() ){
			String key = it.next();
			if(httpHeaders.containsKey(key)){
				response.setHeader(key, httpHeaders.get(key).get(0)  );
			}
			
		}	
	}
	
	protected SupportedFormat getSupportedFormat(RequestParamsBean params, SupportedOperation operation) throws UnsupportedResponseFormatException{
		
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
	
	
	
	public static final String getServletPath() {
		return AbstractNcssController.servletPath;
	}
	
	public static final String getServletCachePath() {
		return AbstractNcssController.servletCachePath;
	}	
	
	public static final String buildCacheUrl(String fileName){
		 return NcssRequestUtils.getTdsContext().getContextPath() + AbstractNcssController.getServletCachePath() + "/" + fileName;
	}
	
	public long getLastModified(HttpServletRequest req) {
		File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo());
		if ((file != null) && file.exists())
			return file.lastModified();
		return -1;
	}	
	
}
