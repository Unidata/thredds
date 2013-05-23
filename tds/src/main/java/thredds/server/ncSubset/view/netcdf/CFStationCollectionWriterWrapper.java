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
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

/**
 * @author mhermida
 *
 */
public final class CFStationCollectionWriterWrapper implements CFPointWriterWrapper {

	static private Logger log = LoggerFactory.getLogger(CFStationCollectionWriterWrapper.class);

	private WriterCFStationCollection writerCFStationCollection;

	private GridAsPointDataset gap;
	
	private CalendarDate timeOrigin;

	private CFStationCollectionWriterWrapper(){}

	private CFStationCollectionWriterWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts) throws IOException{

		writerCFStationCollection = new WriterCFStationCollection(version, filePath, atts);

	}

	@Override
	public boolean header(Map<String, List<String>> groupedVars, GridDataset gridDataset, List<CalendarDate> wDates, List<Attribute> timeDimAtts, LatLonPoint point, Double vertCoord) {

		//timeOrigin = dateUnit.makeCalendarDate(0); 
		boolean headerDone = false;
		Attribute unitsAtt = CFPointWriterUtils.findCDMAtt(timeDimAtts, CDM.UNITS);
		
		DateUnit dateUnit;
		try {
			dateUnit = new DateUnit(unitsAtt.getStringValue());
			timeOrigin = dateUnit.makeCalendarDate(0);
		} catch (Exception e) {
			log.error("Error creating time units for: "+unitsAtt.getStringValue());
			return headerDone;
		}
		
		
		String stnName = "GridPoint";
		String desc = "Grid Point at lat/lon="+point.getLatitude()+","+point.getLongitude();
		Station s = new StationImpl( stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
		List<ucar.unidata.geoloc.Station> stnList  = new ArrayList<ucar.unidata.geoloc.Station>();
		stnList.add(s);

		NetcdfDataset ncfile = (NetcdfDataset) gridDataset.getNetcdfFile(); // fake-arino
		List<String> vars =  (new ArrayList<List<String>>(groupedVars.values())).get(0);    	
		gap = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars);

		List<VariableSimpleIF> wantedVars = NcssRequestUtils.wantedVars2VariableSimple( vars , gridDataset, ncfile);
		try {
			writerCFStationCollection.writeHeader(stnList, wantedVars, gridDataset, timeDimAtts, "");
			headerDone= true;
		} catch (IOException ioe) {
			log.error("Error writing header", ioe);
		}


		return headerDone;
	}

	@Override
	public boolean write(Map<String, List<String>> groupedVars,	GridDataset gridDataset, CalendarDate date, LatLonPoint point, Double targetLevel){

		boolean allDone = false;	

		List<String> vars =  (new ArrayList<List<String>>(groupedVars.values())).get(0);
		StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, vars, true);
		GridDatatype timeGrid = NcssRequestUtils.getTimeGrid(groupedVars, gridDataset);
		if(timeGrid == null){			
			throw new IllegalArgumentException("Variables do not have time dimension");
		}
					
		Double timeCoordValue = NcssRequestUtils.getTimeCoordValue(timeGrid, date, timeOrigin);
		sdata.findMember("time").getDataArray().setDouble(0, timeCoordValue);

		//Ensemble...
		CoordinateAxis1D ensAxis =  gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getEnsembleAxis();
		
		double[] ensCoords = new double[]{-1}; 
		if( ensAxis != null ){
			ensCoords = ensAxis.getCoordValues();
		}		


		
		try{
			
			int ensIdx =0;
			for(double ensCoord : ensCoords){
				
				if(ensCoord >=0){
					writerCFStationCollection.writeEnsCoord(ensIdx, ensCoord);
					ensIdx++;
				}				
				
				//int cont =0;
				// Iterating vars											
				Iterator<String> itVars = vars.iterator();				
				while (itVars.hasNext()) {
					String varName = itVars.next();
					GridDatatype grid = gridDataset.findGridDatatype(varName);

					if (gap.hasTime(grid, date) ) {
						GridAsPointDataset.Point p = gap.readData(grid, date, ensCoord, -1, point.getLatitude(), point.getLongitude());
						sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue );						

					}else{ //Set missing value
						sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );						

					}
					//cont++;
				}
				
				if(ensCoord < 0)
					writerCFStationCollection.writeRecord((String)sdata.findMember("station").getDataArray().getObject(0), timeCoordValue, date, sdata);
				else
					writerCFStationCollection.writeRecord((String)sdata.findMember("station").getDataArray().getObject(0), timeCoordValue, date, ensCoord, sdata);
				
				allDone = true;

			}

		}catch(IOException ioe){
			log.error("Error writing data", ioe);
		}


		return allDone;	

	}

	public boolean trailer(){
		boolean finished = false;
		try {
			writerCFStationCollection.finish();
			finished = true;

		} catch (IOException ioe) {
			log.error("Error finishing  WriterCFStationCollection"+ioe); 
		}

		return finished;

	}

	public static CFStationCollectionWriterWrapper createWrapper( NetcdfFileWriter.Version version, String filePath, List<Attribute> atts ) throws IOException{

		return new CFStationCollectionWriterWrapper(version, filePath, atts);
	} 	


}
