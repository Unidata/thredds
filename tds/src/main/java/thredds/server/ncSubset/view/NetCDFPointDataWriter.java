package thredds.server.ncSubset.view;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.controller.AbstractNcssController;
import thredds.server.ncSubset.controller.NcssDiskCache;
import thredds.server.ncSubset.view.netcdf.CFPointWriterWrapper;
import thredds.server.ncSubset.view.netcdf.CFPointWriterWrapperFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.LatLonPoint;

public class NetCDFPointDataWriter implements PointDataWriter {

	static private Logger log = LoggerFactory.getLogger(NetCDFPointDataWriter.class);
	
	private OutputStream outputStream;
	
	private DiskCache2 diskCache;
	
	private File netcdfResult;
	
	//private boolean isProfile = false;
	
	private NetcdfFileWriter.Version version;  
	
	private CF.FeatureType featureType;
	
	private CFPointWriterWrapper pointWriterWrapper;
	
	private HttpHeaders httpHeaders = new HttpHeaders();
	
	//private List<VariableSimpleIF> wantedVars;
	
	private NetCDFPointDataWriter(NetcdfFileWriter.Version version, OutputStream outputStream){
		
		this.outputStream = outputStream;
		this.version = version;
		diskCache = NcssDiskCache.getInstance().getDiskCache();
		netcdfResult = diskCache.createUniqueFile("ncss", ".nc");		
	}
	
	public static NetCDFPointDataWriter createNetCDFPointDataWriter(NetcdfFileWriter.Version version, OutputStream outputStream){
		return new NetCDFPointDataWriter(version, outputStream);
	}
	
	//public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point, Double vertCoord) {
	public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, List<Attribute> timeDimAtts,LatLonPoint point, Double vertCoord) {
		
		boolean headerDone = false;
		if( groupedVars.size() > 1 && !wDates.isEmpty()){ //Variables with different vertical levels
			//featureType = CF.FeatureType.profile;
			featureType = CF.FeatureType.timeSeriesProfile;
			
		}else{
			List<String> keys = new ArrayList<String>(groupedVars.keySet());
			List<String> varsForRequest = groupedVars.get(keys.get(0));
			CoordinateAxis1D zAxis = gridDataset.findGridDatatype( varsForRequest.get(0)).getCoordinateSystem().getVerticalAxis();
			
			if( wDates.isEmpty()){// Point feature with no time axis!!!
				featureType = CF.FeatureType.point;
			
			}else if( zAxis == null ){//Station
				featureType = CF.FeatureType.timeSeries; 
			}else{//Time series profile with one variable
				featureType = CF.FeatureType.timeSeriesProfile;
			}
		}
		
		try{
			List<Attribute> atts = new ArrayList<Attribute>();
			atts.add(new Attribute( CDM.TITLE,  "Extract Points data from Grid file "+ gridDataset.getLocationURI()) );		
			pointWriterWrapper = CFPointWriterWrapperFactory.getWriterForFeatureType(version, featureType, netcdfResult.getAbsolutePath(), atts);			
			headerDone = pointWriterWrapper.header(groupedVars, gridDataset, wDates, timeDimAtts, point, vertCoord);			
		}catch(IOException ioe){
			log.error("Error writing header", ioe);
		}	
		
		return headerDone;
	}	

	
	public boolean write(Map<String, List<String>> groupedVars, GridDataset gds, List<CalendarDate> wDates, LatLonPoint point, Double vertCoord) throws InvalidRangeException{
		
		if(wDates.isEmpty()){
			return write( groupedVars, gds, CalendarDate.of(new Date()), point, vertCoord);
		}
		
		//loop over wDates
		CalendarDate date;
		Iterator<CalendarDate> it = wDates.iterator();
		boolean pointRead =true;		
		
		while( pointRead && it.hasNext() ){
			date = it.next();
			pointRead = write( groupedVars, gds, date, point, vertCoord);

		}		
		
		return pointRead;
		
	}
	

	private boolean write( Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double targetLevel) throws InvalidRangeException {
		
		boolean allWrite = pointWriterWrapper.write(groupedVars, gridDataset, date, point, targetLevel);		
				
		return allWrite;
	}	
	
	@Override
	public boolean trailer() {
		
		boolean allDone = false;
		
		pointWriterWrapper.trailer();
		
		try{
			IO.copyFileB(netcdfResult, outputStream, 60000);
			allDone = true;
		}catch(IOException ioe){
			log.error("Error copying result to the output stream", ioe);
		}
		
		return allDone;
	}
	
	@Override
	public HttpHeaders getResponseHeaders(){
		
		return httpHeaders;
	}	
	
	
	@Override
	public void setHTTPHeaders(GridDataset gridDataset, String pathInfo){

    	//Set the response headers...
//    	String filename = gridDataset.getLocationURI();
//        int pos = filename.lastIndexOf("/");
//        filename = filename.substring(pos + 1);
//        if (!filename.endsWith(".nc"))
//          filename = filename + ".nc";
        
       String fileName = getFileNameForResponse(version, pathInfo);
                
        String url = AbstractNcssController.buildCacheUrl(netcdfResult.getName());
    	httpHeaders.set("Content-Location", url );
    	httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		
	}
	
	private String getFileNameForResponse(NetcdfFileWriter.Version version, String pathInfo){

		String fileExtension = ".nc";
		
		if(version == NetcdfFileWriter.Version.netcdf4){
			fileExtension = ".nc4";
		}
		
        String[] tmp = pathInfo.split("/"); 
        StringBuilder sb = new StringBuilder();
        sb.append(tmp[tmp.length-2]).append("_").append(tmp[tmp.length-1]);
        String filename= sb.toString().split("\\.")[0]+fileExtension;
        
        return filename;
		
	}
	
}

