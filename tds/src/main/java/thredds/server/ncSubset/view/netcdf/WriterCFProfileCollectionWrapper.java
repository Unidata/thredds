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
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.point.writer.WriterCFProfileCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonPoint;

public final class WriterCFProfileCollectionWrapper implements CFPointWriterWrapper {
	
	static private Logger log = LoggerFactory.getLogger(WriterCFProfileCollectionWrapper.class);
	
	private static final String NO_VERT_LEVEL ="no_vert_level";
	
	private WriterCFProfileCollection writerCFProfileCollection;

	private List<String> profilesNames;
	private GridAsPointDataset gap;
	
	private WriterCFProfileCollectionWrapper(){}
	
	private WriterCFProfileCollectionWrapper(String filePath, List<Attribute> atts) throws IOException{
		
		writerCFProfileCollection = new WriterCFProfileCollection(filePath, atts);
	}

	@Override
	public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, DateUnit dateUnit, LatLonPoint point) {

		boolean headerDone = false;
		profilesNames = new ArrayList<String>();
		
		List<VariableSimpleIF> wantedVars = new ArrayList<VariableSimpleIF>();
		NetcdfDataset ncfile = (NetcdfDataset) gridDataset.getNetcdfFile(); // fake-arino
		
		List<String> keys =new ArrayList<String>(groupedVars.keySet());
				
		for(String key : keys){
			List<String> varsGroup = groupedVars.get(key);
			
			wantedVars.addAll( NcssRequestUtils.wantedVars2VariableSimple( varsGroup , gridDataset, ncfile) );
			
			CoordinateAxis1D zAxis = gridDataset.findGridDatatype(varsGroup.get(0)).getCoordinateSystem().getVerticalAxis();
			if(zAxis == null){
				profilesNames.add(NO_VERT_LEVEL);
			}else{
				profilesNames.add( zAxis.getShortName());				
				//Add the zAxis as variable to keep the record in the new file (same as lat, lon. However lat and lon shouldn't be lat(profile) and lon(profile)? --> check!!  ) 
				wantedVars.add(zAxis);
			}
		}
		
		try {
			writerCFProfileCollection.writeHeader(profilesNames, wantedVars, dateUnit, "");
			headerDone = true;
		} catch (IOException ioe) {
			log.error("Error writing header", ioe);
		}
		
		
		return headerDone;
	}

	@Override
	public boolean write(Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double targetLevel){
		
		boolean allDone = false;
		List<String> keys =new ArrayList<String>(groupedVars.keySet());
		
		try{
		
			for(String key : keys){
			
				List<String> varsGroup = groupedVars.get(key);
				CoordinateAxis1D zAxis = gridDataset.findGridDatatype(varsGroup.get(0)).getCoordinateSystem().getVerticalAxis();			
				String profileName = NO_VERT_LEVEL;
				EarthLocation earthLocation=null;			
				Double timeCoordValue = NcssRequestUtils.getTimeCoordValue(gridDataset.findGridDatatype( varsGroup.get(0) ), date);
				
				if(zAxis == null){
					//Write no vert levels
					StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, varsGroup );		
					sdata.findMember("date").getDataArray().setObject(0, date.toString());				
					//sdata.findMember("time").getDataArray().setObject(0, date.toString());
					gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset, varsGroup);
					Iterator<String> itVars = varsGroup.iterator();
					int cont =0;
					while (itVars.hasNext()) {
						String varName = itVars.next();
						GridDatatype grid = gridDataset.findGridDatatype(varName);
										
						if (gap.hasTime(grid, date) ) {
							GridAsPointDataset.Point p = gap.readData(grid, date,	point.getLatitude(), point.getLongitude());
							//sdata.findMember("latitude").getDataArray().setDouble(0, p.lat );
							//sdata.findMember("longitude").getDataArray().setDouble(0, p.lon );		
							sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );							
							if(cont ==0){
								earthLocation = new EarthLocationImpl(p.lat, p.lon, Double.NaN );
							}							
							
					
						}else{ //Set missing value
							//sdata.findMember("latitude").getDataArray().setDouble(0, point.getLatitude() );
							//sdata.findMember("longitude").getDataArray().setDouble(0, point.getLongitude() );
							sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
							earthLocation = new EarthLocationImpl(point.getLatitude(), point.getLongitude(), Double.NaN );
						}					
					}
					
					writerCFProfileCollection.writeRecord(profileName, timeCoordValue, date, earthLocation , sdata);					
								
				}else{
					
					profileName =zAxis.getShortName();
					//Loop over vertical levels
					double[] vertCoords= new double[]{0.0};
					if(zAxis.getCoordValues().length > 1) vertCoords = zAxis.getCoordValues();
					
					 
					gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset, varsGroup);					
				    int vertCoordsIndex = 0;
					for(double vertLevel : vertCoords){
						
						//The zAxis was added to the variables and we need a structure data that contains z-levels  
						StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, varsGroup, zAxis);		
						//sdata.findMember("date").getDataArray().setObject(0, date.toString());
						
						sdata.findMember("time").getDataArray().setDouble(0, timeCoordValue);
						sdata.findMember(zAxis.getShortName()).getDataArray().setDouble(0, zAxis.getCoordValues()[vertCoordsIndex]  );
						vertCoordsIndex++;
						
						int cont =0;
						// Iterating vars						
						Iterator<String> itVars = varsGroup.iterator();
						while (itVars.hasNext()) {
							String varName = itVars.next();
							GridDatatype grid = gridDataset.findGridDatatype(varName);
						
							if (gap.hasTime(grid, date) && gap.hasVert(grid, vertLevel)) {
								GridAsPointDataset.Point p = gap.readData(grid, date,	vertLevel, point.getLatitude(), point.getLongitude() );
								//sdata.findMember("latitude").getDataArray().setDouble(0, p.lat );
								//sdata.findMember("longitude").getDataArray().setDouble(0, p.lon );
								//sdata.findMember("vertCoord").getDataArray().setDouble(0, p.z );
								sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );
						
								if(cont ==0){
									earthLocation = new EarthLocationImpl(p.lat, p.lon, p.z);
								}
				
							}else{ //Set missing value
								//sdata.findMember("latitude").getDataArray().setDouble(0, point.getLatitude() );
								//sdata.findMember("longitude").getDataArray().setDouble(0, point.getLongitude() );
								//sdata.findMember("vertCoord").getDataArray().setDouble(0,  vertLevel);
								sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
								earthLocation = new EarthLocationImpl(point.getLatitude(), point.getLongitude() , vertLevel);
							}
							cont++;
						}			
						//Double timeCoordValue = NcssRequestUtils.getTimeCoordValue(gridDataset.findGridDatatype( varsGroup.get(0) ), date);
						writerCFProfileCollection.writeRecord(profileName, timeCoordValue, date, earthLocation , sdata);									
					}								
				}			
			}					
		}catch(IOException ioe){
			log.error("Error writing data", ioe);
		}
		allDone=true;
		
		return allDone;
	}
	

	@Override
	public boolean trailer(){
		boolean finished = false;
		try {
			writerCFProfileCollection.finish();
			finished = true;
			
		} catch (IOException ioe) {
			log.error("Error finishing  WriterCFStationCollection"+ioe); 
		}
		
		return finished;
	}
	
	public static WriterCFProfileCollectionWrapper createWrapper(String filePath, List<Attribute> atts ) throws IOException{
		
		return new WriterCFProfileCollectionWrapper(filePath, atts);
	}	

}
