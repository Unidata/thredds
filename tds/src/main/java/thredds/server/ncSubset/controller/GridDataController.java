package thredds.server.ncSubset.controller;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import thredds.server.ncSubset.exception.InvalidBBOXException;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.RequestTooLargeException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.GridDataRequestParamsBean;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.UsageLog;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;

@Controller
@RequestMapping(value="/ncss/grid/")
class GridDataController extends AbstractNcssController{

	static private final Logger log = LoggerFactory.getLogger(GridDataController.class);
	
	private HttpHeaders httpHeaders = new HttpHeaders(); 
	private File netcdfResult;
	private long maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
	
	@RequestMapping(value = "**", params = { "!latitude","!longitude", "var"})
	void getGridSubset(@Valid GridDataRequestParamsBean params, BindingResult  validationResult, HttpServletResponse response )throws UnsupportedResponseFormatException, RequestTooLargeException, OutOfBoundariesException, VariableNotContainedInDatasetException, InvalidBBOXException, InvalidRangeException, ParseException, IOException, UnsupportedOperationException{
		
		if( validationResult.hasErrors() ){			
			handleValidationErrorsResponse(response, HttpServletResponse.SC_BAD_REQUEST, validationResult );			
		}else{
			//Only netcdf format is supported for Grid subssetting
			SupportedFormat sf = getSupportedFormat(params, SupportedOperation.GRID_REQUEST  );
		
			checkRequestedVars(gridDataset,  params);
			
			if( isSpatialSubset(params) ){
				spatialSubset(params, response);	
			}else{
				coordinatesSubset(params, response);
			}
			
			//Headers...
			setResponseHeaders(response, httpHeaders );
			IO.copyFileB(netcdfResult, response.getOutputStream(), 60000);			
			response.flushBuffer();
			response.getOutputStream().close();
			response.setStatus(HttpServletResponse.SC_OK);
		
			log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));			
		}		
		
	}
	
	private void spatialSubset(GridDataRequestParamsBean params, HttpServletResponse response )throws RequestTooLargeException, OutOfBoundariesException, InvalidRangeException, ParseException, IOException,VariableNotContainedInDatasetException, InvalidBBOXException{
		
		LatLonRect maxBB = getGridDataset().getBoundingBox();
		LatLonRect requestedBB = setBBForRequest(params,  gridDataset);			
        
		boolean hasBB = !ucar.nc2.util.Misc.closeEnough(requestedBB.getUpperRightPoint().getLatitude(), maxBB.getUpperRightPoint().getLatitude()) ||
				!ucar.nc2.util.Misc.closeEnough(requestedBB.getLowerLeftPoint().getLatitude(), maxBB.getLowerLeftPoint().getLatitude()) ||
                !ucar.nc2.util.Misc.closeEnough(requestedBB.getUpperRightPoint().getLongitude(), maxBB.getUpperRightPoint().getLongitude()) ||
                !ucar.nc2.util.Misc.closeEnough(requestedBB.getLowerLeftPoint().getLongitude(), maxBB.getLowerLeftPoint().getLongitude()); 

		if(checkBB(maxBB, requestedBB)){
        																			
			Range zRange = null;
			//Request with zRange --> adds a limitation: only variables with the same vertical level???
			if(params.getVertCoord()!=null)
				zRange = getZRange(getGridDataset(), params.getVertCoord(), params.getVar());
				
				List<CalendarDate> wantedDates = getRequestedDates(gridDataset, params);
				CalendarDateRange wantedDateRange = CalendarDateRange.of(wantedDates.get(0), wantedDates.get( wantedDates.size()-1 ));
				
				NetcdfCFWriter writer = new NetcdfCFWriter();
    		        	
				if(maxFileDownloadSize > 0){    		
					long estimatedSize = writer.makeGridFileSizeEstimate(getGridDataset(), params.getVar(), hasBB ? requestedBB : null, params.getHorizStride(), zRange, wantedDateRange, params.getTimeStride(), params.isAddLatLon() );
					if(estimatedSize > maxFileDownloadSize ){
						throw new RequestTooLargeException( "NCSS request too large = "+estimatedSize+" max = " + maxFileDownloadSize );
					}
				}
				makeGridFile(writer, getGridDataset(), params.getVar(), hasBB ? requestedBB : null, params.getHorizStride(), zRange, wantedDateRange, params.getTimeStride(), params.isAddLatLon() );        	        	        	
		}       
	}
	

	private void coordinatesSubset(GridDataRequestParamsBean params, HttpServletResponse response ) throws OutOfBoundariesException, ParseException, InvalidRangeException, RequestTooLargeException, IOException, InvalidBBOXException{
		
		//Check coordinate params: maxx, maxy, minx, miny
		Double minx = params.getMinx();
		Double maxx = params.getMaxx();
		Double miny = params.getMiny();
		Double maxy = params.getMaxx();
		
		int contValid =0;
		if(minx!= null) contValid++;
		if(maxx!= null) contValid++;
		if(miny!= null) contValid++;
		if(maxy!= null) contValid++;
		
		if(contValid == 4){			
			if(minx > maxx){
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have minx < maxx");
			}
			if(miny > maxy){
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have miny < maxy");
			}
			
		}else{
			throw new InvalidBBOXException("Invalid bbox. All params minx, maxx. miny, maxy must be provided");
		}
		
		
		ProjectionRect rect =new ProjectionRect( minx, miny , maxx, maxy );
				
		
		Range zRange = null;
		//Request with zRange --> adds a limitation: only variables with the same vertical level???
		if(params.getVertCoord()!=null)
			zRange = getZRange(getGridDataset(), params.getVertCoord(), params.getVar());
		
		List<CalendarDate> wantedDates = getRequestedDates(gridDataset, params);
		CalendarDateRange wantedDateRange = CalendarDateRange.of(wantedDates.get(0), wantedDates.get( wantedDates.size()-1 ));
		
		NetcdfCFWriter writer = new NetcdfCFWriter();
		
		if(maxFileDownloadSize > 0){    		
			long estimatedSize = writer.makeGridFileSizeEstimate(getGridDataset(), params.getVar(), rect, params.getHorizStride(), zRange, wantedDateRange, params.getTimeStride(), params.isAddLatLon() );
			if(estimatedSize > maxFileDownloadSize ){
				throw new RequestTooLargeException( "NCSS request too large = "+estimatedSize+" max = " + maxFileDownloadSize );
			}
		}		
		
	    //String filename = req.getRequestURI();
		String filename = gridDataset.getLocationURI();
	    int pos = filename.lastIndexOf("/");
	    filename = filename.substring(pos + 1);
	    if (!filename.endsWith(".nc"))
	      filename = filename + ".nc";

	    Random random = new Random(System.currentTimeMillis());
	    int randomInt = random.nextInt();

	    String pathname = Integer.toString(randomInt) + "/" + filename;
	    File ncFile = NcssDiskCache.getInstance().getDiskCache().getCacheFile(pathname);
	    String cacheFilename = ncFile.getPath();
	    
	    String url = buildCacheUrl(pathname);
	    
    	httpHeaders.set("Content-Location", url );
    	httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");		
		
		writer.makeFile(cacheFilename, gridDataset, params.getVar(), rect, params.getHorizStride(), zRange, wantedDateRange, 1, params.isAddLatLon());
		
		netcdfResult = new File(cacheFilename);
		

		
		
	}	
	
	private boolean isSpatialSubset(GridDataRequestParamsBean params) throws InvalidBBOXException{
		
		boolean spatialSubset = false;
		int contValid =0;
		if(params.getNorth()!= null) contValid++;
		if(params.getSouth()!= null) contValid++;
		if(params.getEast()!= null) contValid++;
		if(params.getWest()!= null) contValid++;
		
		if( contValid==4 ){
			if(params.getNorth() < params.getSouth()){
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have north > south");				
			}
			if(params.getEast() < params.getWest()){
				throw new InvalidBBOXException("Invalid bbox. Bounding Box must have east > west; if crossing 180 meridian, use east boundary > 180");				
			}
			spatialSubset=true;
		}else{
			if(contValid > 0)
				throw new InvalidBBOXException("Invalid bbox. All params north, south, east and west must be provided");			
			else{ //no bbox provided --> is spatialSubsetting
				if( params.getMaxx()==null && params.getMinx()==null && params.getMaxy() == null && params.getMiny()==null )
					spatialSubset =true;
			}
		}	
			
		return spatialSubset;
	}
	
	private boolean checkBB(LatLonRect maxBB, LatLonRect requestedBB) throws OutOfBoundariesException{	
	
		boolean isInBB = true;
		LatLonRect intersect = maxBB.intersect(requestedBB);
		
		if(intersect == null) 
			throw new OutOfBoundariesException("Request Bounding Box does not intersect the Data. Data Bounding Box = " + maxBB.toString2() );
		
		return isInBB;
		
	}
	
	private LatLonRect setBBForRequest(GridDataRequestParamsBean params, GridDataset gds ) throws InvalidBBOXException{
		
		if( params.getNorth()==null && params.getSouth()==null && params.getWest()==null && params.getEast()==null )
			return gds.getBoundingBox();
		
		
		return new LatLonRect( new LatLonPointImpl(params.getSouth(), params.getWest()),  new LatLonPointImpl(params.getNorth(), params.getEast()));
		
		
												
	}
	
	private Range getZRange(GridDataset gds, Double verticalCoord, List<String> vars) throws OutOfBoundariesException{
		
		boolean hasVerticalCoord = false;
		Range zRange = null;
		
		if(verticalCoord != null){
		  hasVerticalCoord = !Double.isNaN(verticalCoord);	
	      // allow a vert level to be specified - but only one, and look in first 3D var with nlevels > 1	      
	      if (hasVerticalCoord) {
	    	  try{	  
	    		  for (String varName : vars) {
	    			  GridDatatype grid = gds.findGridDatatype(varName);
	    			  GridCoordSystem gcs = grid.getCoordinateSystem();
	    			  CoordinateAxis1D vaxis = gcs.getVerticalAxis();
	    			  if (vaxis != null && vaxis.getSize() > 1) {
	    				  int bestIndex = -1;
	    				  double bestDiff = Double.MAX_VALUE;
	    				  for (int i = 0; i < vaxis.getSize(); i++) {
	    					  double diff = Math.abs(vaxis.getCoordValue(i) - verticalCoord);
	    					  if (diff < bestDiff) {
	    						  bestIndex = i;
	    						  bestDiff = diff;
	    					  }
	    				  }
	    				  if (bestIndex >= 0) zRange = new Range(bestIndex, bestIndex);
	    			  }
	    		  }
	    	  }catch(InvalidRangeException ire){
	    		  throw new OutOfBoundariesException( "Invalid vertical level: " + ire.getMessage());
	    	  }	  
	        // theres also a vertStride, but not needed since only 1D slice is allowed
	      }		
		}
	    return zRange;
	}
	
	private	void makeGridFile(NetcdfCFWriter writer, GridDataset gds, List<String> vars, LatLonRect bbox, Integer horizStride, Range zRange, CalendarDateRange dateRange, Integer timeStride, boolean addLatLon) throws RequestTooLargeException, InvalidRangeException, IOException{	
	    //String filename = req.getRequestURI();
		String filename = gds.getLocationURI();
	    int pos = filename.lastIndexOf("/");
	    filename = filename.substring(pos + 1);
	    if (!filename.endsWith(".nc"))
	      filename = filename + ".nc";

	    Random random = new Random(System.currentTimeMillis());
	    int randomInt = random.nextInt();

	    String pathname = Integer.toString(randomInt) + "/" + filename;
	    File ncFile = NcssDiskCache.getInstance().getDiskCache().getCacheFile(pathname);
	    String cacheFilename = ncFile.getPath();
	    
	    String url = buildCacheUrl(pathname);
	    
    	httpHeaders.set("Content-Location", url );
    	httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");	    

	    try {

	      writer.makeFile(cacheFilename, gds, vars,
	              bbox,
	              horizStride,
	              zRange,
	              dateRange,
	              timeStride,
	              addLatLon);

	    } catch (IllegalArgumentException e) { // file too big
	      	throw new RequestTooLargeException("Request too large", e);	
	    }

	    netcdfResult = new File(cacheFilename);
		
	}
	
	
	//Exception handlers
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.FORBIDDEN )
	public @ResponseBody String handle(RequestTooLargeException rtle ){
		log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, 0));
		return "NetCDF Subset Service exception handled : "+rtle.getMessage();
	}	  
	  
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody String handle(NcssException ncsse ){
		log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST , 0));
		return "NetCDF Subset Service exception handled : "+ncsse.getMessage();
	}
	
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR )
	public @ResponseBody String handle(IOException ioe ){
		log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
		return "I/O xception handled : "+ioe.getMessage();
	}
	
	@ExceptionHandler
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR )
	public @ResponseBody String handle(InvalidRangeException ire ){
		log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
		return "Invalid Range Exception handled (Invalid Lat/Lon or Time Range): "+ire.getMessage();
	}	
	
	
}
