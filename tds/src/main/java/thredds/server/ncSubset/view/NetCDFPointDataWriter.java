package thredds.server.ncSubset.view;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncSubset.controller.AbstractNcssController;
import thredds.server.ncSubset.controller.NcssDiskCache;
import thredds.server.ncSubset.dataservice.StructureDataFactory;
import thredds.server.ncSubset.util.NcssRequestUtils;
import ucar.ma2.StructureData;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.dt.point.WriterProfileObsDataset;
import ucar.nc2.dt.point.WriterStationObsDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.unidata.geoloc.LatLonPoint;

public class NetCDFPointDataWriter implements PointDataWriter {

	static private Logger log = LoggerFactory.getLogger(NetCDFPointDataWriter.class);
	
	private OutputStream outputStream;
	
	private DiskCache2 diskCache;
	
	private File netcdfResult;
	
	private boolean isProfile = false;
	
	private HttpHeaders httpHeaders = new HttpHeaders();
	
	private WriterHolder writerHolder;
	private List<VariableSimpleIF> wantedVars;
	
	private NetCDFPointDataWriter(OutputStream outputStream){
		
		this.outputStream = outputStream;		
		diskCache = NcssDiskCache.getInstance().getDiskCache();
		netcdfResult = diskCache.createUniqueFile("ncss", ".nc");		
	}
	
	public static NetCDFPointDataWriter createNetCDFPointDataWriter(OutputStream outputStream){
		return new NetCDFPointDataWriter(outputStream);
	}
	
	@Override
	public boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, LatLonPoint point, CoordinateAxis1D zAxis) {
	
		isProfile = true;
		
		boolean headerDone = false;
	    try{		
	    	WriterProfileObsDataset pobsWriter = new WriterProfileObsDataset(netcdfResult.getAbsolutePath(), "Extract Points data from Grid file "+ gridDataset.getLocationURI());
	    	writerHolder = new WriterHolder(pobsWriter); 
	    	NetcdfDataset ncfile = (NetcdfDataset) gridDataset.getNetcdfFile(); // fake-arino
	    	wantedVars = wantedVars2VariableSimple(vars,gridDataset ,ncfile );
	    	// for now, we only have one point = one station
	    	String stnName = "GridPoint";
	    	String desc = "Grid Point at lat/lon="+point.getLatitude()+","+point.getLongitude();
	    	ucar.unidata.geoloc.Station s = new ucar.unidata.geoloc.StationImpl( stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
	    	List<ucar.unidata.geoloc.Station> stnList  = new ArrayList<ucar.unidata.geoloc.Station>();
	    	stnList.add(s);
	    	//stnData.set(stnName);	
	    	pobsWriter.writeHeader(stnList, wantedVars, wDates.size(), zAxis.getFullName() );
	    	setHeaders(gridDataset);	    	
	    	headerDone = true;
	    }catch(IOException ioe){
	    	log.error("Error writing header", ioe);
	    }
	    
	    return headerDone;
	}

	@Override
	public boolean header(List<String> vars, GridDataset gridDataset, List<CalendarDate> wDates, LatLonPoint point) {
		boolean headerDone = false;
	    try{		
	    	WriterStationObsDataset sobsWriter = new WriterStationObsDataset(netcdfResult.getAbsolutePath(), "Extract Points data from Grid file "+ gridDataset.getLocationURI());
	    	writerHolder = new WriterHolder(sobsWriter); 
	    	NetcdfDataset ncfile = (NetcdfDataset) gridDataset.getNetcdfFile(); // fake-arino
	    	wantedVars = wantedVars2VariableSimple(vars,gridDataset ,ncfile );
	    	// for now, we only have one point = one station
	    	String stnName = "GridPoint";
	    	String desc = "Grid Point at lat/lon="+point.getLatitude()+","+point.getLongitude();
	    	ucar.unidata.geoloc.Station s = new ucar.unidata.geoloc.StationImpl( stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
	    	List<ucar.unidata.geoloc.Station> stnList  = new ArrayList<ucar.unidata.geoloc.Station>();
	    	stnList.add(s);
	    	//stnData.set(stnName);	
	    	sobsWriter.writeHeader(stnList, wantedVars);
	    	setHeaders(gridDataset);
	    	headerDone = true;
	    }catch(IOException ioe){
	    	log.error("Error writing header", ioe);
	    }
	    
	    return headerDone;
	}

	@Override
	public boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point, 	Double targetLevel, String zUnits) {

		boolean allDone = false;
		WriterProfileObsDataset pobsWriter = (WriterProfileObsDataset)writerHolder.getWriter();
		StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, vars, zUnits);
		
		sdata.findMember("date").getDataArray().setObject(0, date.toString());
		// Iterating vars 
		Iterator<String> itVars = vars.iterator();
		int cont =0;
		try{
			while (itVars.hasNext()) {
				String varName = itVars.next();
				GridDatatype grid = gridDataset.findGridDatatype(varName);
								
				if (gap.hasTime(grid, date) && gap.hasVert(grid, targetLevel)) {
					GridAsPointDataset.Point p = gap.readData(grid, date,	targetLevel, point.getLatitude(), point.getLongitude() );
					sdata.findMember("lat").getDataArray().setDouble(0, p.lat );
					sdata.findMember("lon").getDataArray().setDouble(0, p.lon );
					sdata.findMember("vertCoord").getDataArray().setDouble(0, p.z );
					sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );						
			
				}else{ //Set missing value
					sdata.findMember("lat").getDataArray().setDouble(0, point.getLatitude() );
					sdata.findMember("lon").getDataArray().setDouble(0, point.getLongitude() );
					sdata.findMember("vertCoord").getDataArray().setDouble(0,  targetLevel);
					sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
				
				}
				cont++;
			}
			
			pobsWriter.writeRecord( (String)sdata.findMember("station").getDataArray().getObject(0), date.toDate() , sdata);
			allDone = true;
			
		}catch(IOException ioe){
			log.error("Error writing data", ioe);
		}
		
		return allDone;
	}

	@Override
	public boolean write(List<String> vars, GridDataset gridDataset, GridAsPointDataset gap, CalendarDate date, LatLonPoint point) {
		boolean allDone = false;
		WriterStationObsDataset sobsWriter = (WriterStationObsDataset)writerHolder.getWriter();
		StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, vars);
		
		sdata.findMember("date").getDataArray().setObject(0, date.toString());
		// Iterating vars 
		Iterator<String> itVars = vars.iterator();
		int cont =0;
		try{
			while (itVars.hasNext()) {
				String varName = itVars.next();
				GridDatatype grid = gridDataset.findGridDatatype(varName);
								
				if (gap.hasTime(grid, date) ) {
					GridAsPointDataset.Point p = gap.readData(grid, date,	point.getLatitude(), point.getLongitude());
					sdata.findMember("lat").getDataArray().setDouble(0, p.lat );
					sdata.findMember("lon").getDataArray().setDouble(0, p.lon );		
					sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );						
			
				}else{ //Set missing value
					sdata.findMember("lat").getDataArray().setDouble(0, point.getLatitude() );
					sdata.findMember("lon").getDataArray().setDouble(0, point.getLongitude() );
					sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
				
				}
				cont++;
			}
			
			sobsWriter.writeRecord( (String)sdata.findMember("station").getDataArray().getObject(0), date.toDate() , sdata);
			allDone = true;
			
		}catch(IOException ioe){
			log.error("Error writing data", ioe);
		}
		
		return allDone;
	}

	@Override
	public boolean trailer() {
		
		boolean allDone = false;
		if(isProfile){
			WriterProfileObsDataset pobsWriter = (WriterProfileObsDataset)writerHolder.getWriter();
			try{
				pobsWriter.finish();
				
			}catch(IOException ioe){
				log.error("Error writing WriterProfileObsDataset trailer", ioe);
			}
			
		}else{
			WriterStationObsDataset sobsWriter = (WriterStationObsDataset)writerHolder.getWriter();
			try{
				sobsWriter.finish();
				
			}catch(IOException ioe){
				log.error("Error writing WriterStationObsDataset trailer", ioe);
			}		
		}
		
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
	
	private List<VariableSimpleIF> wantedVars2VariableSimple(List<String> wantedVars, GridDataset gds, NetcdfDataset ncfile ){

        // need VariableSimpleIF for each variable
        List<VariableSimpleIF> varList = new ArrayList<VariableSimpleIF>(wantedVars.size());
        
        //And wantedVars are in the dataset (controller checked that)
        for(String var : wantedVars){        	
        	VariableEnhanced ve = gds.findGridDatatype(var).getVariable();
            String dims = ""; // always scalar ????
            VariableSimpleIF want = new VariableDS( ncfile, null, null, ve.getShortName(), ve.getDataType(), dims, ve.getUnitsString(), ve.getDescription());
            varList.add( want);        	
        }
        
        /*for (GridDatatype grid : grids ) {
          if (wantedVars.contains(grid.getFullName())) {
            VariableEnhanced ve = grid.getVariable();
            String dims = ""; // always scalar ????
            VariableSimpleIF want = new VariableDS( ncfile, null, null, ve.getShortName(), ve.getDataType(), dims, ve.getUnitsString(), ve.getDescription());
            varList.add( want);
          }
        }*/
		
		return varList;
	}	

	
			
	private class WriterHolder{
		
		private final Object writer;
		boolean isEmpty = true;
		
		WriterHolder(Object writer){
			this.writer = writer;
			isEmpty = false;
		}
		
		Object getWriter(){
			return writer;
		}
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

