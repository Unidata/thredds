package thredds.server.ncSubset.view;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.controller.AbstractNcssController;
import thredds.server.ncSubset.controller.NcssDiskCache;
import thredds.server.ncSubset.dataservice.StructureDataFactory;
import thredds.server.ncSubset.view.netcdf.CFPointWriterWrapper;
import thredds.server.ncSubset.view.netcdf.CFPointWriterWrapperFactory;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.ft.point.writer.WriterCFStationCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonPoint;

public class NetCDFPointDataWriter implements PointDataWriter {

	static private Logger log = LoggerFactory.getLogger(NetCDFPointDataWriter.class);
	
	private OutputStream outputStream;
	
	private DiskCache2 diskCache;
	
	private File netcdfResult;
	
	//private boolean isProfile = false;
	
	private CF.FeatureType featureType;
	
	private CFPointWriterWrapper pointWriterWrapper;
	
	private HttpHeaders httpHeaders = new HttpHeaders();
	
	//private List<VariableSimpleIF> wantedVars;
	
	private NetCDFPointDataWriter(OutputStream outputStream){
		
		this.outputStream = outputStream;		
		diskCache = NcssDiskCache.getInstance().getDiskCache();
		netcdfResult = diskCache.createUniqueFile("ncss", ".nc");		
	}
	
	public static NetCDFPointDataWriter createNetCDFPointDataWriter(OutputStream outputStream){
		return new NetCDFPointDataWriter(outputStream);
	}
	
	public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit,LatLonPoint point) {
		
		boolean headerDone = false;
		if( groupedVars.size() > 1 ){ //Variables with different vertical levels
			featureType = CF.FeatureType.profile;
			
		}else{
			List<String> keys = new ArrayList<String>(groupedVars.keySet());
			List<String> varsForRequest = groupedVars.get(keys.get(0));
			CoordinateAxis1D zAxis = gridDataset.findGridDatatype( varsForRequest.get(0)).getCoordinateSystem().getVerticalAxis();
			if( zAxis == null ){//Station
				featureType = CF.FeatureType.timeSeries; 
			}else{//Point collection
				featureType = CF.FeatureType.point;
			}
		}
		
		try{
			List<Attribute> atts = new ArrayList<Attribute>();
			atts.add(new Attribute( CDM.TITLE,  "Extract Points data from Grid file "+ gridDataset.getLocationURI()) );		
			pointWriterWrapper = CFPointWriterWrapperFactory.getWriterForFeatureType(featureType, netcdfResult.getAbsolutePath(), atts);			
			headerDone = pointWriterWrapper.header(groupedVars, gridDataset, wDates, dateUnit, point);			
		}catch(IOException ioe){
			log.error("Error writing header", ioe);
		}	
		
		return headerDone;
	}	

	@Override
	public boolean write(Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double targetLevel) {
		
		boolean allWrite = pointWriterWrapper.write(groupedVars, gridDataset, date, point, targetLevel);
		
		if(allWrite) setHeaders(gridDataset);
		
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
	
	
	private void setHeaders(GridDataset gridDataset){

    	//Set the response headers...
    	String filename = gridDataset.getLocationURI();
        int pos = filename.lastIndexOf("/");
        filename = filename.substring(pos + 1);
        if (!filename.endsWith(".nc"))
          filename = filename + ".nc";
                
        String url = AbstractNcssController.buildCacheUrl(netcdfResult.getName());
    	httpHeaders.set("Content-Location", url );
    	httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		
	}
	
}

