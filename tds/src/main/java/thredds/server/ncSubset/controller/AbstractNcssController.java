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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.LastModified;

import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.ParamsBean;
import thredds.server.ncSubset.params.RequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DatasetHandler;
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
	
	/*void setRequestPathInfo(String requestPathInfo) {
		this.requestPathInfo = requestPathInfo;
	}*/

	public String getRequestPathInfo() {
		return this.requestPathInfo;
	}

	void setGridDataset(GridDataset gds) {
		this.gridDataset = gds;
	}
	
	/**
	 * Extracts and set the requestPathInfo from the request servlet path for the controllers. 
	 * It depends on the controller how to get it.
	 * @param requestPathInfo 
	 */
	abstract String extractRequestPathInfo(String requestPathInfo);

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
	
	
	protected GridDataset openGridDataset(HttpServletRequest req, HttpServletResponse res, String pathInfo) throws IOException{
		  GridDataset gds = null;
		  
		 /* if( pathInfo.endsWith("xml") || pathInfo.endsWith("html") || pathInfo.endsWith("datasetBoundaries")  ){
			  pathInfo = pathInfo.trim(); 
			  String[] pathInfoArr = pathInfo.split("/");			  
			  StringBuilder sb = new StringBuilder();
			  int len = pathInfoArr.length;
			  sb.append(pathInfoArr[1]);
			  for(int i= 2;  i<len-1; i++  ){
				  sb.append("/"+pathInfoArr[i]);
			  }
			  pathInfo = sb.toString();
		  }*/
		  
	      try {
	          gds = DatasetHandler.openGridDataset(req, res, pathInfo);
	          if (null == gds) {
	            res.sendError(HttpServletResponse.SC_NOT_FOUND);
	          }

	        } catch (java.io.FileNotFoundException ioe) {
	          if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_NOT_FOUND);

	        } catch (Throwable e) {
	          log.error("GridServlet.showForm", e);
	          if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

	        }
	      
	      return gds;
	}	
	
	
	//Exception handlers
	@ExceptionHandler(NcssException.class)
	public ResponseEntity<String> handle(NcssException ncsse ){
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<String>( "NetCDF Subset Service exception handled : "+ncsse.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
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
