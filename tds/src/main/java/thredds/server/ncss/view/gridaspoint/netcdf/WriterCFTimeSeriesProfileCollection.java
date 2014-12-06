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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.nc2.ft.point.writer.CFPointWriterUtils;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * Write a CF "Discrete Sample" time series profile collection file (single station).
 * Example H.5.2 Multidimensional array representations of time series profiles
 *
 *
 * <p/>
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8414816" 
 * 
 * @author mhermida
 *
 */
class WriterCFTimeSeriesProfileCollection extends CFPointWriter {

	static private Logger log = LoggerFactory.getLogger(WriterCFTimeSeriesProfileCollection.class);

	private static final String PROFILE_DIM_NAME ="profile";
	private static final String STATION_DIM_NAME ="station";

	private double lastTimeCoordValue=-1; //Keep track of the last written time
	private int recno = -1; // Keeps track of the record number (profile)
	private int[] origin = new int[1];
	private Variable stnName, stnDesc, lat, lon, ensVar; 

	WriterCFTimeSeriesProfileCollection(NetcdfFileWriter.Version version, String fileOut,
			List<Attribute> atts) throws IOException {
		super( fileOut, atts, version);

		writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeriesProfile.name() ));

	}

	void writeHeader(List<Station> stns, Map<String, List<String>> groupedVars,
			GridDataset gds, List<Attribute> timeDimAtts, int timeDimLength, Double vertCoord) throws IOException{

		//--> Create dimensions and variables:
		//
		// Dimensions:
		//  - profile: 
		//  - vertical dimensions: one for each different vertical level: zn
		//  - station (we only have station but keep this dimension)

		// add the dimensions
		Dimension profile = writer.addDimension(null,PROFILE_DIM_NAME, timeDimLength);
		Dimension station = writer.addDimension(null, STATION_DIM_NAME, stns.size());

		List<Dimension> stationDims = new ArrayList<>();
		stationDims.add(station);

		List<Dimension> dims = new ArrayList<>();
		dims.add(station);	    	    
		dims.add(profile);
        
		List<Dimension> timeDims = new ArrayList<>();
		timeDims.addAll(dims);

		List<String> keys = new ArrayList<>(groupedVars.keySet());
		//Ensemble dimension (all variables should have it, if present)...
		CoordinateAxis1D ensAxis = gds.findGridDatatype( groupedVars.get(keys.get(0)).get(0)).getCoordinateSystem().getEnsembleAxis(); 
		if(ensAxis != null ){
			Dimension d = writer.addDimension(null, ensAxis.getShortName(), ensAxis.getCoordValues().length);
			dims.add(d);
			List<Dimension> ensDim = new ArrayList<>();
			ensDim.add(d);
			ensVar = writer.addVariable(null, ensAxis.getShortName() , ensAxis.getDataType() ,  ensDim );
			
		}		
		
		//Vertical dimensions and variables				 
		for(String key : keys){
			List<String> vars = groupedVars.get(key);    	
						
			List<Dimension> tempDims = new ArrayList<>();
			tempDims.addAll(dims);
			String coordinates ="time "+lonName+" "+latName;
						
			//Vertical
			CoordinateAxis1D zAxis = gds.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
			if(zAxis != null){
				int zAxisLength = zAxis.getCoordValues().length;
				if(vertCoord != null){
					zAxisLength = 1;
				}
				
				Dimension d = writer.addDimension(null, zAxis.getShortName(), zAxisLength);							
				tempDims.add(d);    		
				Variable zVar = writer.addVariable(null, zAxis.getShortName() , zAxis.getDataType() , tempDims);
				//Variable atts
				writer.addVariableAttribute(zVar, new Attribute(CF.STANDARD_NAME, zAxis.getShortName() ));
				writer.addVariableAttribute(zVar, new Attribute(CDM.LONG_NAME, zAxis.getFullName() ));
				writer.addVariableAttribute(zVar, new Attribute(CDM.UNITS , zAxis.getUnitsString() ));
                if (zAxis.getPositive() != null) {
                    writer.addVariableAttribute(zVar, new Attribute(CF.POSITIVE, zAxis.getPositive()));
                }
				writer.addVariableAttribute(zVar, new Attribute(CF.AXIS , "Z"  ));
				coordinates = coordinates +" "+d.getShortName();
			}

			for(String var : vars){
				GridDatatype grid = gds.findGridDatatype(var);
				Variable v = writer.addVariable(null, grid.getShortName() , grid.getDataType() , tempDims);
				//Variable atts
				writer.addVariableAttribute(v, new Attribute( CF.STANDARD_NAME, grid.getShortName() ));
				writer.addVariableAttribute(v, new Attribute( CDM.LONG_NAME, grid.getFullName() ));
				writer.addVariableAttribute(v, new Attribute( CDM.UNITS, grid.getUnitsString() ));
				writer.addVariableAttribute(v, new Attribute( CF.COORDINATES , coordinates ));
				
				//Vertical transformations -> if the grid has vertical transformations
				VerticalCT vct = grid.getCoordinateSystem().getVerticalCT();
				if(  vct != null  && writer.findVariable( vct.getName() ) == null ){
					
					Variable av = writer.addVariable(null, vct.getName(), DataType.DOUBLE, tempDims); 
					writer.addVariableAttribute(av, new Attribute( CF.STANDARD_NAME, vct.getName() )); //Check the names
					writer.addVariableAttribute(av, new Attribute( CDM.LONG_NAME,    vct.getName() ));    //Check the names
					writer.addVariableAttribute(av, new Attribute( CDM.UNITS, grid.getCoordinateSystem().getVerticalTransform().getUnitString() ));
					writer.addVariableAttribute(av, new Attribute( CF.COORDINATES , coordinates ));					
					
				}
			}

		}

		//Station names
		int name_strlen =1;
		int desc_strlen =1;
		for (Station stn : stns) {
			name_strlen = Math.max(name_strlen, stn.getName().length());
			desc_strlen = Math.max(name_strlen, stn.getDescription().length());
		}		

		stnName = writer.addStringVariable(null, "station_name", stationDims, name_strlen);
		writer.addVariableAttribute(stnName, new Attribute(CDM.LONG_NAME, "station name"));
		writer.addVariableAttribute(stnName, new Attribute(CF.CF_ROLE, CF.TIMESERIES_ID ));

		stnDesc = writer.addStringVariable(null, "station_description", stationDims, desc_strlen);
		writer.addVariableAttribute(stnDesc, new Attribute(CDM.LONG_NAME, "station description"));
		writer.addVariableAttribute(stnDesc, new Attribute(CF.STANDARD_NAME, CF.PLATFORM_NAME));		

		//Lon
		lat = writer.addVariable(null, latName, DataType.DOUBLE, STATION_DIM_NAME);
		writer.addVariableAttribute(lat, new Attribute(CDM.UNITS, CDM.LAT_UNITS));
		writer.addVariableAttribute(lat, new Attribute(CDM.LONG_NAME, "profile latitude"));

		//Lat
		lon = writer.addVariable(null, lonName, DataType.DOUBLE, STATION_DIM_NAME);
		writer.addVariableAttribute(lon, new Attribute(CDM.UNITS, CDM.LON_UNITS));
		writer.addVariableAttribute(lon, new Attribute(CDM.LONG_NAME, "profile longitude"));	    

		//TIME
		Variable time = writer.addVariable(null, timeName, DataType.DOUBLE, timeDims);
		
		for(Attribute att : timeDimAtts){
			writer.addVariableAttribute(time, att);
		}
		
		//writer.addVariableAttribute(time, new Attribute(CDM.UNITS, timeUnit.getUnitsString()));
		//writer.addVariableAttribute(time, new Attribute(CDM.LONG_NAME, "time of measurement"));	    

		writer.create();

		writeStations(stns);

	}

	private void writeStations(List<Station> stations){


		llbb = CFPointWriterUtils.getBoundingBox(stations); // gets written in super.finish();
		
		int nstns = stations.size();
		ArrayObject.D1 namesArray = new ArrayObject.D1(String.class, stations.size());
		ArrayObject.D1 descArray = new ArrayObject.D1(String.class, stations.size());
		ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
		ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);

		int i = 0;
		for (Station station : stations) {	      
			namesArray.set(i, station.getName() );
			descArray.set(i, station.getDescription() );
			latArray.set(i, station.getLatitude());
			lonArray.set(i, station.getLongitude());
			i++;
		}		

		try {

			writer.writeStringData(stnName , namesArray);
			writer.writeStringData(stnDesc , descArray);
			writer.write(lat, latArray);
			writer.write(lon, lonArray);

		} catch (IOException ioe) {
			log.error("Error writing station names:"+ioe );
		} catch (InvalidRangeException ire) {
			log.error("Invalid range exception error writing station names:"+ire );
		}


	}

	void writeRecord(double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata) throws IOException{
		trackBB(null, obsDate);
		
		try{
			updateRecno(timeCoordValue);
			
			//Variables without vert levels -> dimensions are (profile, station)
			int[] origin = new int[2];
			origin[0] = 0;
			origin[1] = recno;
						
			StructureMembers sm = sdata.getStructureMembers();

			for( Member m : sm.getMembers() ){

				Variable v = writer.findVariable(m.getName());
						
				if(v != null && !v.getShortName().equals("time")){
					
					if( writer.findVariable(m.getName()) != null){
					
						Array arr = CFPointWriterUtils.getArrayFromMember(writer.findVariable(m.getName()), m);
					    writer.write( writer.findVariable(m.getName()) , origin, arr );
					}										
				}	
			}
		}catch(InvalidRangeException ire){
			log.error("Error writing data: "+ire); 
		}

	}

	void writeRecord(String profileName, double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata, int vIndex)  throws IOException{
		trackBB(null, obsDate);

		try{

			updateRecno(timeCoordValue);
			// write the recno record
			origin[0] = recno;

			int[] tmp3D= new int[3];					
			tmp3D[0] = 0; //Station -> one single station!!
			tmp3D[1] = recno;
			tmp3D[2] = vIndex;			
			//writer.write(record, origin, sArray);
			StructureMembers sm = sdata.getStructureMembers();
			for( Member m : sm.getMembers() ){

				Variable v = writer.findVariable(m.getName());

				//Its a variable --> 3D (profile, station, z)
				if( v != null && !v.getShortName().equals(lonName) && !v.getShortName().equals(latName) && !v.getShortName().equals("time")){

					Array arr = CFPointWriterUtils.getArrayFromMember(writer.findVariable(m.getName()), m);
				    writer.write( writer.findVariable(m.getName()) , tmp3D, arr );					
					

				}

			}	    	

		}catch(InvalidRangeException ire){
			log.error("Error writing data: "+ire); 
		}	
	}
	
	void writeRecord(String profileName, double timeCoordValue, double ensCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata, int vIndex)  throws IOException{
		trackBB(null, obsDate);

		try{

			updateRecno(timeCoordValue);
			// write the recno record
			origin[0] = recno;

			int[] tmp4D= new int[4];					
			tmp4D[0] = 0; //Station -> one single station!!
			tmp4D[1] = recno;
			tmp4D[2] =  (int) ensCoordValue;
			tmp4D[3] = vIndex;			
					
			StructureMembers sm = sdata.getStructureMembers();
			for( Member m : sm.getMembers() ){

				Variable v = writer.findVariable(m.getName());

				//Its a variable --> 4D (profile, ensemble, station, z)
				if( v != null && !v.getShortName().equals(lonName) && !v.getShortName().equals(latName) && !v.getShortName().equals("time")){

					Array arr = CFPointWriterUtils.getArrayFromMember(writer.findVariable(m.getName()), m);
				    writer.write( writer.findVariable(m.getName()) , tmp4D, arr );					
										
				}

			}	    	

		}catch(InvalidRangeException ire){
			log.error("Error writing data: "+ire); 
		}	
	}
	
	
	void writeRecord( double timeCoordValue, double ensCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata)  throws IOException{
		trackBB(null, obsDate);

		try{

			updateRecno(timeCoordValue);
			// write the recno record
			origin[0] = recno;

			int[] tmp3D= new int[3];					
			tmp3D[0] = 0; //Station -> one single station!!
			tmp3D[1] = recno;
			tmp3D[2] =  (int) ensCoordValue;			
					
			StructureMembers sm = sdata.getStructureMembers();
			for( Member m : sm.getMembers() ){

				Variable v = writer.findVariable(m.getName());

				//Its a variable --> 3D (profile, ensemble, station)
				if( v != null && !v.getShortName().equals(lonName) && !v.getShortName().equals(latName) && !v.getShortName().equals("time")){
					
					Array arr = CFPointWriterUtils.getArrayFromMember(writer.findVariable(m.getName()), m);
				    writer.write( writer.findVariable(m.getName()) , tmp3D, arr );					

				}

			}	    	

		}catch(InvalidRangeException ire){
			log.error("Error writing data: "+ire); 
		}	
	}	
	

	void writeEnsCoord(int ensIdx, double ensCoord) throws IOException{
		
		ArrayDouble.D1 tmpArray = new ArrayDouble.D1(1);
		tmpArray.setDouble(0, ensCoord);
		int[] idx = new int[]{ensIdx};
		try {			
			writer.write( ensVar , idx, tmpArray );
		}catch(InvalidRangeException ire){
			log.error("Error writing data: "+ire); 
		}		
	}

	private void  updateRecno( double timeCoordValue ) throws IOException, InvalidRangeException{

		if(timeCoordValue != lastTimeCoordValue){
			//Updates recno = profile!!!
			recno++;
			int[] tmp= new int[2];			
			tmp[0] = 0; //Station -> one single station!!
			tmp[1] = recno;
			Double data = timeCoordValue;
			lastTimeCoordValue = timeCoordValue;			
			ArrayDouble.D2 tmpArray = new ArrayDouble.D2(1,1);
			tmpArray.setDouble(0, data);			
			//Writes time...
			writer.write( writer.findVariable( "time") , tmp, tmpArray );

		}		
	}

  @Override
  protected void makeFeatureVariables(StructureData featureData, boolean isExtended) throws IOException {
    // LOOK FAKE
  }
}
