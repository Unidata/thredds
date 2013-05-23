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
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

class PointCollectionNoTimeWriterWrapper implements CFPointWriterWrapper {

	static private Logger log = LoggerFactory.getLogger(PointCollectionNoTimeWriterWrapper.class);
	
	private WriterPointCollectionNoTime writerPointCollectionNoTime;
	
	private GridAsPointDataset gap;
	
	private PointCollectionNoTimeWriterWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts) throws IOException{

		writerPointCollectionNoTime = new WriterPointCollectionNoTime(version, filePath, atts);
	}
	
	@Override
	public boolean header(Map<String, List<String>> groupedVars,
			GridDataset gds, List<CalendarDate> wDates, List<Attribute> timeDimAtts,
			LatLonPoint point, Double vertCoord) {
		
		boolean headerDone = false;
		List<Attribute> atts = new ArrayList<Attribute>();
		atts.add(new Attribute( CDM.TITLE,  "Extract point data from Grid file "+ gds.getLocationURI()) );   		    		    	

    	NetcdfDataset ncfile = (NetcdfDataset) gds.getNetcdfFile(); // fake-arino
    	List<String> vars =  (new ArrayList<List<String>>(groupedVars.values())).get(0);    	
    	gap = NcssRequestUtils.buildGridAsPointDataset(gds, vars);
    	List<VariableSimpleIF> wantedVars = NcssRequestUtils.wantedVars2VariableSimple( vars , gds, ncfile);
    	CoordinateAxis1D zAxis =  gds.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
    	String zAxisUnitString =null;
		if(zAxis != null){
			zAxisUnitString = zAxis.getUnitsString();
		}
		
		//Create the list of stations (only one)
		String stnName="Grid Point";
		String desc = "Grid Point at lat/lon="+point.getLatitude()+","+point.getLongitude();
		Station s = new StationImpl( stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
		List<Station> stnList  = new ArrayList<Station>();
		stnList.add(s);		
		

		try {
			
			writerPointCollectionNoTime.writeHeader(stnList, wantedVars, zAxisUnitString);			
			headerDone = true;
			
		} catch (IOException ioe) {
			log.error("Error writing header", ioe);
		}

		return headerDone;
		
		
	}

	@Override
	public boolean write(Map<String, List<String>> groupedVars,
			GridDataset gridDataset, CalendarDate date, LatLonPoint point,
			Double targetLevel) {

		boolean allDone = false;	
		
		List<String> vars =  (new ArrayList<List<String>>(groupedVars.values())).get(0);
		//Create the structure with no time!!
		StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, vars, false);
		
		EarthLocation earthLocation =null;
		// Iterating vars		
		Iterator<String> itVars = vars.iterator();
		int cont =0;
		try{
			while (itVars.hasNext()) {
				String varName = itVars.next();
				GridDatatype grid = gridDataset.findGridDatatype(varName);
								
				//if (gap.hasTime(grid, date) ) {
					GridAsPointDataset.Point p = gap.readData(grid, null,	point.getLatitude(), point.getLongitude());
					//sdata.findMember("latitude").getDataArray().setDouble(0, p.lat );
					//sdata.findMember("longitude").getDataArray().setDouble(0, p.lon );		
					sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );
					earthLocation = new EarthLocationImpl(p.lat, p.lon, Double.NaN);
			
				//}else{ //Set missing value
					//sdata.findMember("latitude").getDataArray().setDouble(0, point.getLatitude() );
					//sdata.findMember("longitude").getDataArray().setDouble(0, point.getLongitude() );
				//	sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
				
				//}
				cont++;
			}
			
			//sobsWriter.writeRecord( (String)sdata.findMember("station").getDataArray().getObject(0), date.toDate() , sdata);			
			writerPointCollectionNoTime.writeRecord(earthLocation ,  sdata);
			allDone = true;
			
		}catch(IOException ioe){
			log.error("Error writing data", ioe);
		}
			
					  		
		return allDone;
		
	}

	@Override
	public boolean trailer() {

		boolean finished = false;
		try {
			writerPointCollectionNoTime.finish();
			finished = true;
			
		} catch (IOException ioe) {
			log.error("Error finishing  WriterCFPointCollection"+ioe); 
		}
		
		return finished;
	}

	
	static PointCollectionNoTimeWriterWrapper createWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts ) throws IOException{

		return new PointCollectionNoTimeWriterWrapper(version, filePath, atts);
		
	} 
	
	
}
