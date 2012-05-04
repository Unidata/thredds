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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.LastModified;

import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.RequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DataRootHandler;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

public class AbstractNcssController implements LastModified{
	
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
	 */
	protected List<CalendarDate> getRequestedDates(GridDataset gds, RequestParamsBean params) throws OutOfBoundariesException, ParseException{
		
		GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gds, params.getVar());
		
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
					closestToPresent.add(start);
					return closestToPresent;
				}
				if( now.isAfter(end) ){ 
					closestToPresent.add(end);
					return closestToPresent;
				}
				//now is in range				
				return  NcssRequestUtils.wantedDates(gap, CalendarDateRange.of(now,now));				
				
				}
			}
		//We should have a time or a timeRange...
		CalendarDateRange dates = getRequestedDateRange(params);		
		return NcssRequestUtils.wantedDates(gap, dates );
				

		
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
				
				DateTime dt = new DateTime(params.getTime());
				date = CalendarDate.of(dt.getMillis());				
			}
						
			return CalendarDateRange.of(date,date);
		
		}/*else{
			if (params.getTime_start()==null && params.getTime_end()==null && params.getTime_duration()==null) {
				//No param times were provided --> closest time to present
				CalendarDate date= CalendarDate.of(new Date());
				return CalendarDateRange.of(date,date);			
			}
		}*/
	
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
	
	/**
	 * Checks all requested variables are in the dataset for a Ncss requests. 
	 * 
	 * @param gds
	 * @param params
	 * @throws VariableNotContainedInDatasetException
	 */
	protected void checkRequestedVars(GridDataset gds, RequestParamsBean params) throws VariableNotContainedInDatasetException{
		//Check vars --> all variables requested
		if(params.getVar().get(0).equals("all")){
			params.setVar(NcssRequestUtils.getAllVarsAsList(getGridDataset()));
		}else{ //all the requested are in dataset
			for(String var : params.getVar()){						
				GridDatatype grid = getGridDataset().findGridDatatype(var);
				if(grid == null) 
					throw new VariableNotContainedInDatasetException("Variable: "+var+" is not contained in the requested dataset"); 
			}
		}		
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
