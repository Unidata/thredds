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

import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.RequestTooLargeException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
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
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.LatLonRect;

@Controller
@RequestMapping(value="/ncss/grid/")
class GridDataController extends AbstractNcssController{

	static private final Logger log = LoggerFactory.getLogger(GridDataController.class);
	
	private HttpHeaders httpHeaders = new HttpHeaders(); 
	private File netcdfResult;

	@RequestMapping(value = "**", params = { "!point", "var", "north", "south", "east", "west" })
	void getGridData(@Valid GridDataRequestParamsBean params, BindingResult  validationResult, HttpServletResponse response )throws RequestTooLargeException, OutOfBoundariesException, InvalidRangeException, ParseException, IOException, UnsupportedResponseFormatException{
		
		if( validationResult.hasErrors() ){
			
			handleValidationErrorsResponse(response, HttpServletResponse.SC_BAD_REQUEST, validationResult );
			
		}else{
			
			SupportedFormat sf = getSupportedFormat(params, SupportedOperation.GRID_REQUEST  );
			//Check the requested bb
			LatLonRect maxBB = getGridDataset().getBoundingBox();
        
			boolean hasBB = !ucar.nc2.util.Misc.closeEnough(params.getNorth(), maxBB.getUpperRightPoint().getLatitude()) ||
                !ucar.nc2.util.Misc.closeEnough(params.getSouth(), maxBB.getLowerLeftPoint().getLatitude()) ||
                !ucar.nc2.util.Misc.closeEnough(params.getEast(), maxBB.getUpperRightPoint().getLongitude()) ||
                !ucar.nc2.util.Misc.closeEnough(params.getWest(), maxBB.getLowerLeftPoint().getLongitude()); 

			if(checkBB(maxBB, params.getBB())){
        
				Range zRange = getZRange(getGridDataset(), params.getVertCoord(), params.getVar());
				NetcdfCFWriter writer = new NetcdfCFWriter();
    		        	
				long maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
				if(maxFileDownloadSize > 0){    		
					long estimatedSize = writer.makeGridFileSizeEstimate(getGridDataset(), params.getVar(), hasBB ? params.getBB() : null, params.getHorizStride(), zRange, params.getCalendarDateRange(), params.getTimeStride(), params.isAddLatLon() );
					if(estimatedSize > maxFileDownloadSize ){
						throw new RequestTooLargeException( "NCSS request too large = "+estimatedSize+" max = " + maxFileDownloadSize );
					}
				}
				makeGridFile(writer, getGridDataset(), params.getVar(), hasBB ? params.getBB() : null, params.getHorizStride(), zRange, params.getCalendarDateRange(), params.getTimeStride(), params.isAddLatLon() );
        	
				//Headers...
				setResponseHeaders(response, httpHeaders );
				IO.copyFileB(netcdfResult, response.getOutputStream(), 60000);			
				response.flushBuffer();
				response.getOutputStream().close();
				response.setStatus(HttpServletResponse.SC_OK);
			
				log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
        	        	
			}
		}
        
	}
	
	
	private boolean checkBB(LatLonRect maxBB, LatLonRect requestedBB) throws OutOfBoundariesException{	
	
		boolean isInBB = true;
		LatLonRect intersect = maxBB.intersect(requestedBB);
		
		if(intersect == null) 
			throw new OutOfBoundariesException("Request Bounding Box does not intersect the Data. Data Bounding Box = " + maxBB.toString2() );
		
		return isInBB;
		
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
	    		  throw new OutOfBoundariesException( "Invalid vertical level: " + ire.getMessage(),ire);
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


	    //response.addHeader("Content-Location", url);
	    //res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
	    //ServletUtil.returnFile(req, res, new File(cacheFilename), "application/x-netcdf");
	    
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
