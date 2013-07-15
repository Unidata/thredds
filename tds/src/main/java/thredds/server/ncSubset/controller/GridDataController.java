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
import java.net.SocketException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.ncSubset.exception.InvalidBBOXException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.RequestTooLargeException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.GridDataRequestParamsBean;
import thredds.server.ncSubset.params.RequestParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.ThreddsConfig;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFileWriter;
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
import ucar.unidata.geoloc.ProjectionRect;

@Controller
@Scope("request")
@RequestMapping(value = "/ncss/grid/")
class GridDataController extends AbstractNcssDataRequestController {

  static private final Logger log = LoggerFactory.getLogger(GridDataController.class);

  /*
   * Compression rate used to estimate the filesize of netcdf4 compressed files
   */
  static private final short ESTIMATED_C0MPRESION_RATE =5; 
  
  private HttpHeaders httpHeaders = new HttpHeaders();
  private File netcdfResult;
  private long maxFileDownloadSize = -1L;

  //@RequestMapping(value = "**", params = {"!latitude", "!longitude", "var"})
  @RequestMapping(value = "**", params = {"!latitude", "!longitude"})
  void getGridSubset(@Valid GridDataRequestParamsBean params, BindingResult validationResult, HttpServletResponse response)
          throws UnsupportedResponseFormatException, RequestTooLargeException, OutOfBoundariesException, VariableNotContainedInDatasetException, InvalidBBOXException, InvalidRangeException, ParseException, IOException, UnsupportedOperationException, TimeOutOfWindowException {

    if (validationResult.hasErrors()) {
      handleValidationErrorsResponse(response, HttpServletResponse.SC_BAD_REQUEST, validationResult);
    } else {
    	
      //Supported formats are netcdf3 (default) and netcdf4 (if available)
      SupportedFormat sf = getSupportedFormat(params, SupportedOperation.GRID_REQUEST);
      NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;
      
      if( sf.equals(SupportedFormat.NETCDF4) ){
    	  version = NetcdfFileWriter.Version.netcdf4;
      }
      
      checkRequestedVars(gridDataset, params);

      if (isSpatialSubset(params)) {
        spatialSubset(params, version);
      } else {
        coordinatesSubset(params, response, version);
      }

      //Headers...
      httpHeaders.set("Content-Type", sf.getResponseContentType() );
      setResponseHeaders(response, httpHeaders);
      IO.copyFileB(netcdfResult, response.getOutputStream(), 60000);
      response.flushBuffer();
      response.getOutputStream().close();
      response.setStatus(HttpServletResponse.SC_OK);
    }
  }

  private void spatialSubset(GridDataRequestParamsBean params, NetcdfFileWriter.Version version)
          throws RequestTooLargeException, OutOfBoundariesException, InvalidRangeException, ParseException, IOException, VariableNotContainedInDatasetException, InvalidBBOXException, TimeOutOfWindowException {

    LatLonRect maxBB = getGridDataset().getBoundingBox();
    LatLonRect requestedBB = setBBForRequest(params, gridDataset);

    boolean hasBB = !ucar.nc2.util.Misc.closeEnough(requestedBB.getUpperRightPoint().getLatitude(), maxBB.getUpperRightPoint().getLatitude()) ||
            !ucar.nc2.util.Misc.closeEnough(requestedBB.getLowerLeftPoint().getLatitude(), maxBB.getLowerLeftPoint().getLatitude()) ||
            !ucar.nc2.util.Misc.closeEnough(requestedBB.getUpperRightPoint().getLongitude(), maxBB.getUpperRightPoint().getLongitude()) ||
            !ucar.nc2.util.Misc.closeEnough(requestedBB.getLowerLeftPoint().getLongitude(), maxBB.getLowerLeftPoint().getLongitude());

    //Don't check this...
    //if (checkBB(maxBB, requestedBB)) {
    
      Range zRange = null;
      //Request with zRange --> adds a limitation: only variables with the same vertical level???
      if (params.getVertCoord() != null || params.getVertStride() > 1)
        zRange = getZRange(getGridDataset(), params.getVertCoord(), params.getVertStride(), params.getVar());

      List<CalendarDate> wantedDates = getRequestedDates(gridDataset, params);
      CalendarDateRange wantedDateRange=null;
      
      if(!wantedDates.isEmpty())
    	  wantedDateRange = CalendarDateRange.of(wantedDates.get(0), wantedDates.get(wantedDates.size() - 1));

      NetcdfCFWriter writer = new NetcdfCFWriter();
      maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
      if (maxFileDownloadSize > 0) {
        long estimatedSize = writer.makeGridFileSizeEstimate(getGridDataset(), params.getVar(), hasBB ? requestedBB : null, params.getHorizStride(), zRange, wantedDateRange, params.getTimeStride(), params.isAddLatLon());
        if(version == NetcdfFileWriter.Version.netcdf4){
        	estimatedSize /= ESTIMATED_C0MPRESION_RATE;
        }
        if (estimatedSize > maxFileDownloadSize) {
          throw new RequestTooLargeException("NCSS response too large = " + estimatedSize + " max = " + maxFileDownloadSize);
        }
      }
             
            
      makeGridFile(new NetcdfCFWriter(), getGridDataset(), params.getVar(), hasBB ? requestedBB : null, params.getHorizStride(), zRange, wantedDateRange, params.getTimeStride(), params.isAddLatLon(), version);
    //}
  }


  private void coordinatesSubset(GridDataRequestParamsBean params, HttpServletResponse response, NetcdfFileWriter.Version version)
          throws OutOfBoundariesException, ParseException, InvalidRangeException, RequestTooLargeException, IOException, InvalidBBOXException, TimeOutOfWindowException {

    //Check coordinate params: maxx, maxy, minx, miny
    Double minx = params.getMinx();
    Double maxx = params.getMaxx();
    Double miny = params.getMiny();
    Double maxy = params.getMaxy();

    int contValid = 0;
    if (minx != null) contValid++;
    if (maxx != null) contValid++;
    if (miny != null) contValid++;
    if (maxy != null) contValid++;

    if (contValid == 4) {
      if (minx > maxx) {
        throw new InvalidBBOXException("Invalid bbox. Bounding Box must have minx < maxx");
      }
      if (miny > maxy) {
        throw new InvalidBBOXException("Invalid bbox. Bounding Box must have miny < maxy");
      }

    } else {
      throw new InvalidBBOXException("Invalid bbox. All params minx, maxx. miny, maxy must be provided");
    }

    ProjectionRect rect = new ProjectionRect(minx, miny, maxx, maxy);
    
    Range zRange = null;
    //Request with zRange --> adds a limitation: only variables with the same vertical level???
    if (params.getVertCoord() != null || params.getVertStride() > 1)
      zRange = getZRange(getGridDataset(), params.getVertCoord(), params.getVertStride(), params.getVar());

    List<CalendarDate> wantedDates = getRequestedDates(gridDataset, params);
    CalendarDateRange wantedDateRange = CalendarDateRange.of(wantedDates.get(0), wantedDates.get(wantedDates.size() - 1));

    NetcdfCFWriter writer = new NetcdfCFWriter();
    maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
    if (maxFileDownloadSize > 0) {
      long estimatedSize = writer.makeGridFileSizeEstimate(getGridDataset(), params.getVar(), rect, params.getHorizStride(), zRange, wantedDateRange, params.getTimeStride(), params.isAddLatLon());
      if(version == NetcdfFileWriter.Version.netcdf4){
      	estimatedSize /= ESTIMATED_C0MPRESION_RATE;
      }      
      if (estimatedSize > maxFileDownloadSize) {
    	  throw new RequestTooLargeException("NCSS response too large = " + estimatedSize + " max = " + maxFileDownloadSize);
      }
    }

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
    httpHeaders.set("Content-Location", url);
    httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    writer.makeFile(cacheFilename, gridDataset, params.getVar(), rect, params.getHorizStride(), zRange, wantedDateRange, 1, params.isAddLatLon(), version);
    netcdfResult = new File(cacheFilename);
  }

  protected void checkRequestedVars(GridDataset gds, RequestParamsBean params) throws VariableNotContainedInDatasetException, UnsupportedOperationException {
    //Check vars
    //if var = all--> all variables requested
    if (params.getVar().get(0).equals("all")) {
      params.setVar(NcssRequestUtils.getAllVarsAsList(getGridDataset()));
    }

    //Only check vertical levels if we the request has vertCoord --> we allow request for variables with different vertical levels if vertCoord!=null for grid requests
    boolean checkVertLevels = (params.getVertCoord() != null);
    //Check not only all vars are contained in the grid, also they have the same vertical coords
    Iterator<String> it = params.getVar().iterator();
    String varName = it.next();
    //GridDatatype grid = gds.findGridByShortName(varName);
    GridDatatype grid = gds.findGridDatatype(varName);
    if (grid == null)
      throw new VariableNotContainedInDatasetException("Variable: " + varName + " is not contained in the requested dataset");

    CoordinateAxis1D vertAxis = grid.getCoordinateSystem().getVerticalAxis();
    CoordinateAxis1D newVertAxis = null;
    boolean sameVertCoord = true;

    while (sameVertCoord && it.hasNext()) {
      varName = it.next();
      //grid = gds.findGridByShortName(varName);
      grid = gds.findGridDatatype(varName);
      if (grid == null)
        throw new VariableNotContainedInDatasetException("Variable: " + varName + " is not contained in the requested dataset");

      if (checkVertLevels) {
        newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
        if (vertAxis != null) {
          if (vertAxis.equals(newVertAxis)) {
            vertAxis = newVertAxis;
          } else {
            sameVertCoord = false;
          }
        } else {
          if (newVertAxis != null) sameVertCoord = false;
        }
      }
    }

    if (!sameVertCoord)
      throw new UnsupportedOperationException("The variables requested: " + params.getVar() + " have different vertical levels. Grid requests with vertCoord must have variables with same vertical levels.");
  }

  private boolean isSpatialSubset(GridDataRequestParamsBean params) throws InvalidBBOXException {

    boolean spatialSubset = false;
    int contValid = 0;
    if (params.getNorth() != null) contValid++;
    if (params.getSouth() != null) contValid++;
    if (params.getEast() != null) contValid++;
    if (params.getWest() != null) contValid++;

    if (contValid == 4) {
      if (params.getNorth() < params.getSouth()) {
        throw new InvalidBBOXException("Invalid bbox. Bounding Box must have north > south");
      }
      if (params.getEast() < params.getWest()) {
        throw new InvalidBBOXException("Invalid bbox. Bounding Box must have east > west; if crossing 180 meridian, use east boundary > 180");
      }
      spatialSubset = true;
    } else {
      if (contValid > 0)
        throw new InvalidBBOXException("Invalid bbox. All params north, south, east and west must be provided");
      else { //no bbox provided --> is spatialSubsetting
        if (params.getMaxx() == null && params.getMinx() == null && params.getMaxy() == null && params.getMiny() == null)
          spatialSubset = true;
      }
    }
    return spatialSubset;
  }
 
  
  
  private boolean checkBB(LatLonRect maxBB, LatLonRect requestedBB) throws OutOfBoundariesException {

    boolean isInBB = true;
    LatLonRect intersect = maxBB.intersect(requestedBB);

    if (intersect == null)
      throw new OutOfBoundariesException("Request Bounding Box does not intersect the Data. Data Bounding Box = " + maxBB.toString2());

    return isInBB;
  }

  private LatLonRect setBBForRequest(GridDataRequestParamsBean params, GridDataset gds) throws InvalidBBOXException {

    if (params.getNorth() == null && params.getSouth() == null && params.getWest() == null && params.getEast() == null)
      return gds.getBoundingBox();

    //return new LatLonRect(new LatLonPointImpl(params.getSouth(), params.getWest()), new LatLonPointImpl(params.getNorth(), params.getEast()));
    return new LatLonRect(new LatLonPointImpl(params.getSouth(), params.getWest()), params.getNorth() - params.getSouth(), params.getEast() -params.getWest() );
  }

  private Range getZRange(GridDataset gds, Double verticalCoord, Integer vertStride, List<String> vars) throws OutOfBoundariesException {

    boolean hasVerticalCoord = false;
    Range zRange = null;

    if (verticalCoord != null) {
      hasVerticalCoord = !Double.isNaN(verticalCoord);
      // allow a vert level to be specified - but only one, and look in first 3D var with nlevels > 1
      if (hasVerticalCoord) {
        try {
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
        } catch (InvalidRangeException ire) {
          throw new OutOfBoundariesException("Invalid vertical level: " + ire.getMessage());
        }
        // there's also a vertStride, but not needed since only 1D slice is allowed
      }
    } else {//No vertical range was provided, we get the zRange with the zStride (1 by default)

      if (vertStride > 1) {
        try {
          zRange = new Range(0, 0, vertStride);
          for (String varName : vars) {
            GridDatatype grid = gds.findGridDatatype(varName);
            GridCoordSystem gcs = grid.getCoordinateSystem();
            CoordinateAxis1D vaxis = gcs.getVerticalAxis();
            if (vaxis != null) {
              //Range vRange = new Range(0,  (int)vaxis.getSize()-1, 1);
              zRange = new Range(zRange.first(), zRange.last() > vaxis.getSize() ? zRange.last() : (int) vaxis.getSize() - 1, vertStride);
            }
          }
        } catch (InvalidRangeException ire) {
          throw new OutOfBoundariesException("Invalid vertical stride: " + ire.getMessage());
        }


      }
    }

    return zRange;
  }

  private void makeGridFile(NetcdfCFWriter writer, GridDataset gds, List<String> vars, LatLonRect bbox, Integer horizStride,
                            Range zRange, CalendarDateRange dateRange, Integer timeStride, boolean addLatLon, NetcdfFileWriter.Version version)
          throws RequestTooLargeException, InvalidRangeException, IOException {
	  		 
    Random random = new Random(System.currentTimeMillis());
    int randomInt = random.nextInt();
    
    String filename = getFileNameForResponse(version);
    String pathname = Integer.toString(randomInt) + "/" + filename;
    File ncFile = NcssDiskCache.getInstance().getDiskCache().getCacheFile(pathname);
       
    String cacheFilename = ncFile.getPath();    

    String url = buildCacheUrl(pathname);

    httpHeaders.set("Content-Location", url);
    httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    
    writer.makeFile(cacheFilename, gds, vars,
              bbox,
              horizStride,
              zRange,
              dateRange,
              timeStride,
              addLatLon,
              version);

    netcdfResult = new File(cacheFilename);

  }
  
  private String getFileNameForResponse(NetcdfFileWriter.Version version){
	  	String fileExtension =".nc";
	  	
	  	if(version == NetcdfFileWriter.Version.netcdf4 ){
	  		fileExtension =".nc4";
	  	}
	  
	    String[] tmp = requestPathInfo.split("/"); 
	    StringBuilder sb = new StringBuilder();
	    sb.append(tmp[tmp.length-2]).append("_").append(tmp[tmp.length-1]);
	    String filename= sb.toString().split("\\.")[0]+fileExtension;
	    return filename; 
  }

  //Exception handlers
  @ExceptionHandler(RequestTooLargeException.class)
  public ResponseEntity<String> handle(RequestTooLargeException rtle) {
	HttpHeaders responseHeaders = new HttpHeaders();
	responseHeaders.setContentType(MediaType.TEXT_PLAIN);	  	  
    return new ResponseEntity<String>( "NetCDF Subset Service exception handled : " + rtle.getMessage(), responseHeaders, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(SocketException.class)
  public ResponseEntity<String> handle(SocketException ioe) {
	HttpHeaders responseHeaders = new HttpHeaders();
	responseHeaders.setContentType(MediaType.TEXT_PLAIN);	  
    return new ResponseEntity<String>( "SocketException handled : " + ioe.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<String> handle(IOException ioe,  HttpServletResponse response, HttpServletRequest request ) {
	if( !response.isCommitted()){
		log.error("I/O Exception handled in GridDataController", ioe);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		return new ResponseEntity<String>("I/O Exception handled : " + ioe.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	return null;
  }

  @ExceptionHandler(InvalidRangeException.class)
  public ResponseEntity<String> handle(InvalidRangeException ire) {
	HttpHeaders responseHeaders = new HttpHeaders();
	responseHeaders.setContentType(MediaType.TEXT_PLAIN);
	return new ResponseEntity<String>("Invalid Range Exception handled (Invalid Lat/Lon or Time Range): " + ire.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST );    
  }

}


