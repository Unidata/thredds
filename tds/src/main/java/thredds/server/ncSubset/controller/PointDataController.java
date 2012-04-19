package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import thredds.server.ncSubset.view.PointDataStream;
import thredds.servlet.UsageLog;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

@Controller
@RequestMapping(value="/ncss/grid/**")
class PointDataController extends AbstractNcssController{ 

	static private final Logger log = LoggerFactory.getLogger(PointDataController.class);

	
	@RequestMapping(value = "**", params = { "latitude", "longitude", "var" })
	void getPointData(@Valid PointDataRequestParamsBean params, BindingResult  validationResult, HttpServletResponse response ) throws ParseException, NcssException, IOException{

		if( validationResult.hasErrors() ){
			
			handleValidationErrorsResponse(response, HttpServletResponse.SC_BAD_REQUEST, validationResult );
			
		}else{
			
			//Cheking request format...			
			SupportedFormat sf = getSupportedFormat(params, SupportedOperation.POINT_REQUEST  );
			
			
			LatLonPoint point = params.getLatLonPoint(); //Check if the point is within boundaries!!		
			if( !isPointWithinBoundaries(params.getLatLonPoint(), getGridDataset() ) ){			
				throw  new OutOfBoundariesException("Requested Lat/Lon Point (+" + point + ") is not contained in the Data. "+
						"Data Bounding Box = " + getGridDataset().getBoundingBox().toString2());
			}
				

			List<Double> verticalLevels = getRequestedVertLevel(gridDataset, params);
			List<CalendarDate> wantedDates = getRequestedDates( gridDataset, params);
	
			response.setContentType(sf.getResponseContentType() );
			PointDataStream pds = PointDataStream.createPointDataStream(  sf, response.getOutputStream() );
			boolean allWritten = pds.stream( getGridDataset(), point, wantedDates, params.getVar(), verticalLevels);
			if(allWritten){				
				setResponseHeaders(response, pds.getHttpHeaders() );
				response.flushBuffer();
				response.getOutputStream().close();
				response.setStatus(HttpServletResponse.SC_OK);
				log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
				
			}else{
				//Something went wrong...
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR , -1));				
			}
		}	
	}
	
	/**
	 * 
	 * Checks the vertical levels for the variables requested.
	 * If some of the variables requested is not in the dataset throws a VariableNotContainedInDatasetException
	 * and if the vertical levels are different throws an UnsupportedOperationException.
	 * Returns the values of the common vertical level or an empty list if the variables don't have vertical levels
	 * 
	 * @param gds
	 * @param params
	 * @return
	 * @throws NcssException
	 */
	private List<Double> getRequestedVertLevel(GridDataset gds, PointDataRequestParamsBean params ) throws NcssException{
		
		// Some var should have vertical axis here --> use the first one
		List<CoordinateAxis1D> zAxis = new ArrayList<CoordinateAxis1D>();
		//Iterator<String> varsIt = params.getVar().iterator();
		ListIterator<String> varsIt = params.getVar().listIterator();
		boolean variablesWithNoLevels = false;
		boolean hasDifferentVertLevels = false;
		while ( varsIt.hasNext()) {
			String var = varsIt.next();
			
			//Check the requested variables too. We need all of them in the dataset to check the vertical levels
			GridDatatype gdt =gds.findGridDatatype(var);
			if(gdt == null ){
				throw new VariableNotContainedInDatasetException("Variable: "+var+" is not contained in the requested dataset");
			}
			
			CoordinateAxis1D tmp = gdt.getCoordinateSystem().getVerticalAxis();
			if(tmp != null){
				zAxis.add(tmp);
			}else{
				variablesWithNoLevels = true;
			}
			//Compare new Axis with the previous found			
			//At least one variable with vert levels and other without them
			if( zAxis.size() > 0  && variablesWithNoLevels ) hasDifferentVertLevels = true;
			//Different vertical levels
			if(zAxis.size() > 1 ){
				if(!zAxis.get( zAxis.size()-1).equals( zAxis.get(zAxis.size()-2) ) )
					hasDifferentVertLevels = true;
			}	

			
			if(hasDifferentVertLevels)
				throw new UnsupportedOperationException("The variables requested: "+ params.getVar() +" have different vertical levels. Only requests on variables with same vertical levels are supported.");
		}		
		
		//No vertical levels
		if(zAxis.isEmpty() ) return Collections.emptyList();
		
		List<Double> vertLevels = new ArrayList<Double>();
		int levelIdx = 0;
		if(params.getVertCoord() != null){ //Only one vertical level
			levelIdx = zAxis.get(0).findCoordElement(params.getVertCoord());
			if( levelIdx > 0 ){
				vertLevels.add( zAxis.get(0).getCoordValue(levelIdx) );
			}else{
				throw new OutOfBoundariesException("Vertical level is out of range");
			}
		}else{				
			for(double vertLevel : zAxis.get(0).getCoordValues()){
				vertLevels.add(vertLevel);
			}	
		}
		
		
        
		return vertLevels;
	}
	
	
	private boolean isPointWithinBoundaries(LatLonPoint point, GridDataset gds){	
		LatLonRect bbox = gds.getBoundingBox();		
		return bbox.contains(point);
	}
	
	

	//Exception handlers
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody String handle(NcssException ncsse ){
		return "NetCDF Subset Service exception handled : "+ncsse.getMessage();
	}
	
	
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody String handle(ParseException pe){
		return "Parse exception handled: "+pe.getMessage();
	}
	
	
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody String handle(IOException ioe){
		return "IO exception handled: "+ioe.getMessage();
	}	
	
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody String handleValidationException(MethodArgumentNotValidException ve){
		return "Bad request: "+ve.getMessage();
	}	

}
