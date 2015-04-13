/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncss.view.gridaspoint.netcdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thredds.server.ncss.dataservice.StructureDataFactory;
import thredds.server.ncss.util.NcssRequestUtils;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.point.writer.CFPointWriterUtils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

/**
 * 
 * @author mhermida
 *
 */
final class CFTimeSeriesProfileCollectionWriterWrapper implements CFPointWriterWrapper {

	static private Logger log = LoggerFactory.getLogger(CFTimeSeriesProfileCollectionWriterWrapper.class);

	private WriterCFTimeSeriesProfileCollection writerCFTimeSeriesProfileCollection;
	
	private CalendarDate timeOrigin;

	private CFTimeSeriesProfileCollectionWriterWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts ) throws IOException{

		writerCFTimeSeriesProfileCollection = new WriterCFTimeSeriesProfileCollection(version, filePath, atts); 
	}

	@Override
	public boolean header(Map<String, List<String>> groupedVars, GridDataset gds, List<CalendarDate> wDates, List<Attribute> timeDimAtts, LatLonPoint point, Double vertCoord){
	
		boolean headerDone = false;
		
		//timeOrigin = dateUnit.makeCalendarDate(0);		
		Attribute unitsAtt = CFPointWriterUtils.findCDMAtt(timeDimAtts, CDM.UNITS);
		
		DateUnit dateUnit;
		try {
			dateUnit = new DateUnit(unitsAtt.getStringValue());
			timeOrigin = dateUnit.makeCalendarDate(0);
		} catch (Exception e) {
			log.error("Error creating time units for: "+unitsAtt.getStringValue());
			return headerDone;
		}		
		
		List<Attribute> atts = new ArrayList<>();
		atts.add(new Attribute( CDM.TITLE,  "Extract time series profiles data from Grid file "+ gds.getLocation()) );

		//Create the list of stations (only one)
		String stnName="Grid Point";
		String desc = "Grid Point at lat/lon="+point.getLatitude()+","+point.getLongitude();
		Station s = new StationImpl( stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
		List<Station> stnList  = new ArrayList<>();
		stnList.add(s);		

		try {
			writerCFTimeSeriesProfileCollection.writeHeader(stnList, groupedVars, gds, timeDimAtts, wDates.size(), vertCoord );
			headerDone = true;
		} catch (IOException ioe) {
			log.error("Error writing header", ioe);
		}
		return headerDone;
	}

	@Override
	public boolean write(Map<String, List<String>> groupedVars,
			GridDataset gridDataset, CalendarDate date, LatLonPoint point,
			Double targetLevel) throws InvalidRangeException {

		boolean allDone = false;
		List<String> keys =new ArrayList<>(groupedVars.keySet());

		try{

			for(String key : keys){

				List<String> varsGroup = groupedVars.get(key);

				//Ensemble...
				CoordinateAxis1D ensAxis = gridDataset.findGridDatatype(varsGroup.get(0)).getCoordinateSystem().getEnsembleAxis();
				double[] ensCoords = new double[]{-1};
				if(ensAxis != null){
					ensCoords = ensAxis.getCoordValues();	
				}

				int ensIdx =0;
				for(double ensCoord : ensCoords){

					if(ensCoord >=0){
						writerCFTimeSeriesProfileCollection.writeEnsCoord(ensIdx, ensCoord);
						ensIdx++;
					}	
					
					CoordinateAxis1D zAxis = gridDataset.findGridDatatype(varsGroup.get(0)).getCoordinateSystem().getVerticalAxis();			
					//String profileName = NO_VERT_LEVEL;				
					EarthLocation earthLocation=null;	

					GridAsPointDataset gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset, varsGroup);
					GridDatatype timeGrid = NcssRequestUtils.getTimeGrid(groupedVars, gridDataset);
					if(timeGrid == null){			
						throw new IllegalArgumentException("Variables do not have time dimension");
					}
					
					Double timeCoordValue = NcssRequestUtils.getTimeCoordValue(timeGrid, date, timeOrigin);
					if(zAxis == null){ //Variables without vertical levels

						//Write no vert levels
						StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, varsGroup,true );		
						sdata.findMember("time").getDataArray().setDouble(0, timeCoordValue);				
						//sdata.findMember("time").getDataArray().setObject(0, date.toString());
						
						//gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset, varsGroup);
						
						Iterator<String> itVars = varsGroup.iterator();
						int cont =0;
						while (itVars.hasNext()) {
							String varName = itVars.next();
							GridDatatype grid = gridDataset.findGridDatatype(varName);

							if (gap.hasTime(grid, date) ) {
								GridAsPointDataset.Point p = gap.readData(grid, date, ensCoord, -1,  point.getLatitude(), point.getLongitude());
								sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );							
								if(cont ==0){
									earthLocation = new EarthLocationImpl(p.lat, p.lon, Double.NaN );
								}							


							}else{ //Set missing value
								sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
								earthLocation = new EarthLocationImpl(point.getLatitude(), point.getLongitude(), Double.NaN );
							}					
						}
						
						if(ensCoord >=0)
							writerCFTimeSeriesProfileCollection.writeRecord( timeCoordValue, ensCoord, date, earthLocation , sdata);
						else	
							writerCFTimeSeriesProfileCollection.writeRecord( timeCoordValue, date, earthLocation , sdata);
						


					}else{
						String profileName =zAxis.getShortName();
						//Loop over vertical levels
						double[] vertCoords= new double[]{0.0};
						int vertCoordsIndex = 0;
						if(targetLevel != null){
							vertCoords[0] = targetLevel;
							vertCoordsIndex = zAxis.findCoordElementBounded(targetLevel);
						}else{
							if(zAxis.getCoordValues().length > 1) vertCoords = zAxis.getCoordValues();
						}	
						
					 
						int vertCoordsIndexForFile =0;
						for(double vertLevel : vertCoords){
							//The zAxis was added to the variables and we need a structure data that contains z-levels  
							StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, varsGroup, zAxis, true);		
							//sdata.findMember("date").getDataArray().setObject(0, date.toString());						
							sdata.findMember("time").getDataArray().setDouble(0, timeCoordValue);
							sdata.findMember(zAxis.getShortName()).getDataArray().setDouble(0, zAxis.getCoordValues()[vertCoordsIndex]  );
							vertCoordsIndex++;
							vertCoordsIndexForFile++;
							int cont =0;
							// Iterating vars						
							Iterator<String> itVars = varsGroup.iterator();
							while (itVars.hasNext()) {
								String varName = itVars.next();
								GridDatatype grid = gridDataset.findGridDatatype(varName);
								
								if( grid.getCoordinateSystem().getVerticalTransform() != null ){
									double actualLevel = NcssRequestUtils.getActualVertLevel(grid, date, point, vertLevel);								
									sdata.findMember( grid.getCoordinateSystem().getVerticalCT().getName() ).getDataArray().setDouble(0, actualLevel );
								}
								
								if (gap.hasTime(grid, date) && gap.hasVert(grid, vertLevel)) {
									GridAsPointDataset.Point p = gap.readData(grid, date, ensCoord,	vertLevel, point.getLatitude(), point.getLongitude() );
									sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );

									if(cont ==0){
										earthLocation = new EarthLocationImpl(p.lat, p.lon, p.z);
									}

								}else{ //Set missing value
									sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						
									earthLocation = new EarthLocationImpl(point.getLatitude(), point.getLongitude() , vertLevel);
								}
								cont++;
							}
							if(ensCoord >=0){
								writerCFTimeSeriesProfileCollection.writeRecord(profileName, timeCoordValue, ensCoord, date, earthLocation , sdata, vertCoordsIndexForFile-1);
							}else{
								writerCFTimeSeriesProfileCollection.writeRecord(profileName, timeCoordValue, date, earthLocation , sdata, vertCoordsIndexForFile-1);
							}	
							allDone = true;
						}
					}

				}	
			}

		}catch(IOException ioe){
			log.error("Error writing data", ioe);
		}	


		return allDone;
	}

	@Override
	public boolean trailer(){

		boolean allDone =false;
		try{
			writerCFTimeSeriesProfileCollection.finish();
			allDone =true;
		}catch(IOException ioe){
			log.error("Error finishing  WriterCFTimeSeriesProfileCollection: "+ioe); 
		}
		return allDone;
	}


	public static CFTimeSeriesProfileCollectionWriterWrapper createWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts ) throws IOException{
		return new CFTimeSeriesProfileCollectionWriterWrapper(version, filePath, atts);
	}	

}
