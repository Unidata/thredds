package thredds.server.ncSubset.view.netcdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thredds.server.ncSubset.dataservice.StructureDataFactory;
import thredds.server.ncSubset.util.NcssRequestUtils;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonPoint;

public final class WriterCFPointCollectionWrapper implements CFPointWriterWrapper {
	
	static private Logger log = LoggerFactory.getLogger(WriterCFPointCollectionWrapper.class);
	
	private WriterCFPointCollection writerCFPointCollection;
	//private WriterCFStationCollection writerCFStationCollection;
	
	private GridAsPointDataset gap;
	
	private WriterCFPointCollectionWrapper(){}
	
	private WriterCFPointCollectionWrapper(String filePath, List<Attribute> atts) throws IOException{
		
		writerCFPointCollection = new WriterCFPointCollection(filePath, atts);
		//writerCFStationCollection = new WriterCFStationCollection(filePath, atts);
	}
	
	@Override
	public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit, LatLonPoint point) {
		
		boolean headerDone = false;
    	List<Attribute> atts = new ArrayList<Attribute>();
    	atts.add(new Attribute( CDM.TITLE,  "Extract Points data from Grid file "+ gridDataset.getLocationURI()) );   		    		    	

    	// for now, we only have one point = one station
    	String stnName = "GridPoint";
    	String desc = "Grid Point at lat/lon="+point.getLatitude()+","+point.getLongitude();
    	ucar.unidata.geoloc.Station s = new ucar.unidata.geoloc.StationImpl( stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
    	List<ucar.unidata.geoloc.Station> stnList  = new ArrayList<ucar.unidata.geoloc.Station>();
    	stnList.add(s);
    	
    	NetcdfDataset ncfile = (NetcdfDataset) gridDataset.getNetcdfFile(); // fake-arino
    	List<String> vars =  (new ArrayList<List<String>>(groupedVars.values())).get(0);    	
    	gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars);
    	List<VariableSimpleIF> wantedVars = NcssRequestUtils.wantedVars2VariableSimple( vars , gridDataset, ncfile);
    	CoordinateAxis1D zAxis =  gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
    	
    	try {
			writerCFPointCollection.writeHeader(wantedVars, dateUnit, zAxis.getUnitsString());
    		//writerCFStationCollection.writeHeader(stnList, wantedVars, dateUnit, zAxis.getUnitsString());
			headerDone= true;
		} catch (IOException ioe) {
			log.error("Error writing header", ioe);
		}
		
    	return headerDone;
		
	}

	@Override
	public boolean write(Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double requestLevel) {

		boolean allDone = false;
		List<String> vars =  (new ArrayList<List<String>>(groupedVars.values())).get(0);
		CoordinateAxis1D zAxis =  gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
		
		double[] targetLevels = zAxis.getCoordValues();
		
		if(zAxis.getCoordValues().length ==1 ){
			targetLevels[0]=0.0;
		}
						
		if(requestLevel !=null){
			targetLevels = new double[]{requestLevel};
		}
		 		
		try{
			for(double targetLevel : targetLevels){
				StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, vars, zAxis.getUnitsString());		
				sdata.findMember("date").getDataArray().setObject(0, date.toString());
				EarthLocation earthLocation=null;
				int cont =0;
				// Iterating vars
				Iterator<String> itVars = vars.iterator();
				while (itVars.hasNext()) {
					String varName = itVars.next();
					GridDatatype grid = gridDataset.findGridDatatype(varName);
				
					if (gap.hasTime(grid, date) && gap.hasVert(grid, targetLevel)) {
						GridAsPointDataset.Point p = gap.readData(grid, date,	targetLevel, point.getLatitude(), point.getLongitude() );
						sdata.findMember("lat").getDataArray().setDouble(0, p.lat );
						sdata.findMember("lon").getDataArray().setDouble(0, p.lon );
						sdata.findMember("vertCoord").getDataArray().setDouble(0, p.z );
						sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );
					
						if(cont ==0){
							earthLocation = new EarthLocationImpl(p.lat, p.lon, p.z);
						}
			
					}else{ //Set missing value
						sdata.findMember("lat").getDataArray().setDouble(0, point.getLatitude() );
						sdata.findMember("lon").getDataArray().setDouble(0, point.getLongitude() );
						sdata.findMember("vertCoord").getDataArray().setDouble(0,  targetLevel);
						sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
						earthLocation = new EarthLocationImpl(point.getLatitude(), point.getLongitude() , targetLevel);
					}
					cont++;
			}			

			Double timeCoordValue = NcssRequestUtils.getTimeCoordValue(gridDataset.findGridDatatype( vars.get(0) ), date);
			writerCFPointCollection.writeRecord(timeCoordValue, date, earthLocation , sdata);
			//writerCFStationCollection.writeRecord((String)sdata.findMember("station").getDataArray().getObject(0), timeCoordValue, date, sdata);
			allDone = true;
		}	
	}catch(IOException ioe){
		log.error("Error writing data", ioe);
	}
		
		return allDone;
	}
	
	public static WriterCFPointCollectionWrapper createWrapper(String filePath, List<Attribute> atts ) throws IOException{
		
		return new WriterCFPointCollectionWrapper(filePath, atts);
	}

	@Override
	public boolean trailer(){
		boolean finished = false;
		try {
			writerCFPointCollection.finish();
			//writerCFStationCollection.finish();
			finished = true;
			
		} catch (IOException ioe) {
			log.error("Error finishing  WriterCFPointCollection"+ioe); 
		}
		
		return finished;
		
	}	

}
