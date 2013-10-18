/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.point.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * Write a CF "Discrete Sample" station file.
 * Example H.7. Timeseries of station data in the indexed ragged array representation.
 *
 * <p/>
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8340320"
 * @author caron
 * @since Aug 19, 2009
 */
public class WriterCFStationCollection  extends CFPointWriter {
  private static final String stationDimName = "station";
  private static final String idName = "station_id";
  private static final String descName = "station_description";
  private static final String wmoName = "wmo_id";
  private static final String stationIndexName = "stationIndex";
  private static final boolean debug = false;

  //////////////////////////////////////////////////////////
  private int name_strlen = 1, desc_strlen = 1, wmo_strlen = 1;
  private Variable lat, lon, alt, time, id, wmoId, desc, stationIndex, record;

  private List<Dimension> stationDims = new ArrayList<Dimension>(1);
  private Dimension recordDim;

  private boolean useAlt = false;
  private boolean useWmoId = false;

  public WriterCFStationCollection(String fileOut, String title) throws IOException {
    this(null, fileOut, Arrays.asList(new Attribute[]{new Attribute(CDM.TITLE, title)}));
  }
  
  public WriterCFStationCollection(NetcdfFileWriter.Version version, String fileOut, List<Attribute> atts) throws IOException {
    super(fileOut, atts, version);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeries.name()));
  }

  public void writeHeader(List<ucar.unidata.geoloc.Station> stns, List<VariableSimpleIF> vars, DateUnit timeUnit, String altUnits) throws IOException {
    this.altUnits = altUnits;

    createStations(stns);
    createCoordVariables(timeUnit);
    createDataVariables(vars);

    writer.create(); // done with define mode
    record = writer.addRecordStructure();

    writeStationData(stns); // write out the station info
  }

  private void createStations(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();

    // see if there's altitude, wmoId for any stations
    for (Station stn : stnList) {
      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0))
        useWmoId = true;
    }

    /* if (useAlt)
      ncfile.addGlobalAttribute("altitude_coordinate", altName); */

    // find string lengths
    for (Station station : stnList) {
      name_strlen = Math.max(name_strlen, station.getName().length());
      desc_strlen = Math.max(desc_strlen, station.getDescription().length());
      if (useWmoId) wmo_strlen = Math.max(wmo_strlen, station.getWmoId().length());
    }

    llbb = getBoundingBox(stnList); // gets written in super.finish();

    // add the dimensions
    recordDim = writer.addUnlimitedDimension(recordDimName);
    Dimension stationDim = writer.addDimension(null, stationDimName, nstns);
    stationDims.add(stationDim);

    // add the station Variables using the station dimension
    lat = writer.addVariable(null, latName, DataType.DOUBLE, stationDimName);
    writer.addVariableAttribute(lat, new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    writer.addVariableAttribute(lat, new Attribute(CDM.LONG_NAME, "station latitude"));

    lon = writer.addVariable(null, lonName, DataType.DOUBLE, stationDimName);
    writer.addVariableAttribute(lon, new Attribute(CDM.UNITS, CDM.LON_UNITS));
    writer.addVariableAttribute(lon, new Attribute(CDM.LONG_NAME, "station longitude"));

    if (useAlt) {
      alt = writer.addVariable(null, altName, DataType.DOUBLE, stationDimName);
      writer.addVariableAttribute(alt, new Attribute(CDM.UNITS, "meters"));
      writer.addVariableAttribute(alt, new Attribute(CF.POSITIVE, CF.POSITIVE_UP));
      writer.addVariableAttribute(alt, new Attribute(CDM.LONG_NAME, "station altitude"));
      writer.addVariableAttribute(alt, new Attribute(CF.STANDARD_NAME, CF.SURFACE_ALTITUDE));
    }

    id = writer.addStringVariable(null, idName, stationDims, name_strlen);
    writer.addVariableAttribute(id, new Attribute(CDM.LONG_NAME, "station identifier"));
    writer.addVariableAttribute(id, new Attribute(CF.CF_ROLE, CF.TIMESERIES_ID));  // station_id:cf_role = "timeseries_id";
    
    desc = writer.addStringVariable(null, descName, stationDims, desc_strlen);
    writer.addVariableAttribute(desc, new Attribute(CDM.LONG_NAME, "station description"));
    writer.addVariableAttribute(desc, new Attribute(CF.STANDARD_NAME, CF.PLATFORM_NAME));

    if (useWmoId) {
      wmoId = writer.addStringVariable(null, wmoName, stationDims, wmo_strlen);
      writer.addVariableAttribute(wmoId, new Attribute(CDM.LONG_NAME, "station WMO id"));
      writer.addVariableAttribute(wmoId, new Attribute(CF.STANDARD_NAME, CF.PLATFORM_ID));
    }
  }

  private void createCoordVariables(DateUnit timeUnit) throws IOException {
    // time variable
	  
    time = writer.addVariable(null, timeName, DataType.DOUBLE, recordDimName);
    writer.addVariableAttribute(time, new Attribute(CDM.UNITS, timeUnit.getUnitsString()));
    writer.addVariableAttribute(time, new Attribute(CDM.LONG_NAME, "time of measurement"));

    stationIndex = writer.addVariable(null, stationIndexName, DataType.INT, recordDimName);
    writer.addVariableAttribute(stationIndex, new Attribute(CDM.LONG_NAME, "station index for this observation record"));
    writer.addVariableAttribute(stationIndex, new Attribute(CF.INSTANCE_DIMENSION, stationDimName));
  }

  private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {
    String coordNames = latName + " " + lonName + " " + altName + " " + timeName;
    if (!useAlt){
    	coordNames = latName + " " + lonName + " " + timeName;
    }

    /* find all dimensions needed by the data variables
    for (VariableSimpleIF var : dataVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (d.isUnlimited()) continue;
      if (d.isShared())
        writer.addDimension(null, d.getShortName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    }  */
    
    // eliminate coordinate variables
    List<VariableSimpleIF> useDataVars = new ArrayList<VariableSimpleIF>(dataVars.size());
    for (VariableSimpleIF var : dataVars) {
      if (writer.findVariable(var.getShortName()) == null) useDataVars.add(var);
    }

    // add the data variables, all using the record dimension
    for (VariableSimpleIF oldVar : useDataVars) {
      List<Dimension> dims = getNewDimensions(oldVar);
      dims.add(0, recordDim);

      /* StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : dims) {
        if (d.isUnlimited()) continue;
        if (d.isShared())
          dimNames.append(" ").append(d.getShortName());
        else { // anon dimensions
          String dimName = oldVar.getShortName() + "_strlen";
          writer.addDimension(null, dimName, d.getLength());
          dimNames.append(" ").append(dimName);
        }
      }  */

      Variable newVar;
      if ((oldVar.getDataType().equals(DataType.STRING))) {
        newVar = writer.addStringVariable(null, oldVar.getShortName(), dims, 10);

      /* } else if ((oldVar.getDataType().equals(DataType.CHAR)) && dims.size() > 1) {
        int n = dims.size();
        List<Dimension> cdims = dims.subList(0, n-1);
        Dimension lenDim = dims.get(n-1);
        newVar = writer.addStringVariable(null, oldVar.getShortName(), cdims, lenDim.getLength());  */

      } else {
        newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dims);
      }

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        if (!reservedVariableAtts.contains(att.getShortName()))
          newVar.addAttribute(att);
      }
      newVar.addAttribute(new Attribute(CF.COORDINATES, coordNames));
    }
  }

  int countDim = 0;
  private final Map<String, Dimension> gdimHash = new HashMap<String, Dimension>(); // name, newDim : global dimensions (classic mode)
  private List<Dimension> getNewDimensions(VariableSimpleIF oldVar) {
    List<Dimension> result = new ArrayList<Dimension>(oldVar.getRank());

    // dimensions
    for (Dimension oldD : oldVar.getDimensions()) {
      Dimension newD = gdimHash.get(oldD.getShortName());
      if (newD != null) continue;
      if (oldD.isShared()) {
        newD = writer.addDimension(null, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
                oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
        gdimHash.put(oldD.getShortName(), newD);
        if (debug) System.out.println("add dim= " + newD);
        result.add(newD);
      } else {
        String dimName = (oldVar.getDataType() == DataType.CHAR) ? oldVar.getShortName()+"_strlen" : "dim"+countDim++;
        newD = writer.addDimension(null, dimName, oldD.isUnlimited() ? 0 : oldD.getLength());
        gdimHash.put(dimName, newD);
        result.add(newD);
      }
    }
    return result;
  }

  private HashMap<String, Integer> stationMap;

  private void writeStationData(List<ucar.unidata.geoloc.Station> stnList) throws IOException {
    int nstns = stnList.size();
    stationMap = new HashMap<String, Integer>(2 * nstns);
    if (debug) System.out.println("stationMap created");

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 descArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 wmoArray = new ArrayObject.D1(String.class, nstns);

    for (int i = 0; i < stnList.size(); i++) {
      ucar.unidata.geoloc.Station stn = stnList.get(i);
      stationMap.put(stn.getName(), i);

      latArray.set(i, stn.getLatitude());
      lonArray.set(i, stn.getLongitude());
      if (useAlt) altArray.set(i, stn.getAltitude());

      idArray.set(i, stn.getName());
      descArray.set(i, stn.getDescription());
      if (useWmoId) wmoArray.set(i, stn.getWmoId());
    }

    try {
      writer.write(lat, latArray);
      writer.write(lon, lonArray);
      if (useAlt) writer.write(alt, altArray);
      writer.writeStringData(id, idArray);
      writer.writeStringData(desc, descArray);
      if (useWmoId) writer.writeStringData(wmoId, wmoArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private int recno = 0;
  private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  //private ArrayInt.D1 prevArray = new ArrayInt.D1(1);
  private ArrayInt.D1 parentArray = new ArrayInt.D1(1);
  private int[] origin = new int[1];

  public void writeRecord(Station s, PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(s.getName(), sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sdata);
  }

  public void writeRecord(String stnName, double timeCoordValue, CalendarDate obsDate, StructureData sdata) throws IOException {
    trackBB(null, obsDate);

    Integer parentIndex = stationMap.get(stnName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find station " + stnName);

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    timeArray.set(0, timeCoordValue);
    parentArray.set(0, parentIndex);

    // write the recno record
    origin[0] = recno;
    try {
      writer.write(record, origin, sArray);
      writer.write(time, origin, timeArray);
      writer.write(stationIndex, origin, parentArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }
  
  /**
   * Use this when record structure is not available (netcdf4)
 * @throws IOException 
   */
  public void writeStructure(Station s, PointFeature sobs, StructureData sdata) throws IOException{
	  String stnName = s.getName();
	  double timeCoordValue = sobs.getObservationTime();
	  CalendarDate obsDate = sobs.getNominalTimeAsCalendarDate();
	  trackBB(null,obsDate);
	  
	  Integer parentIndex = stationMap.get(stnName);
	  if (parentIndex == null)
		throw new RuntimeException("Cant find station " + stnName);

	  // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
	  ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
	  sArray.setStructureData(sdata, 0);

	  timeArray.set(0, timeCoordValue);
	  parentArray.set(0, parentIndex);	  
	  
	  //update the recno record, even if we are not using record structure. It keeps track of the origin for writing vars.
	  origin[0] = recno;
	  
	  try{
		  
		  writer.write(time, origin, timeArray);
		  writer.write(stationIndex, origin, parentArray);
		  
			StructureMembers sm = sdata.getStructureMembers();
			for( Member m : sm.getMembers() ){
				Variable v = writer.findVariable(m.getName());

				//if( v != null && !v.getShortName().equals(lonName) && !v.getShortName().equals(latName) && !v.getShortName().equals("time") ){
				//if( v != null && v.getDataType() != DataType.CHAR && !v.getShortName().equals(lonName) && !v.getShortName().equals(latName)){
				if( v != null && v.getDataType() != DataType.CHAR && v.getDimensionsString().equals("obs") ){					
					
					Array arr = CFPointWriterUtils.getArrayFromMember(v, m);
					writer.write( v , origin, arr );																	

				}																					
			}		  
		  
		  
		  
	  }catch(InvalidRangeException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}	      

		recno++;	  
  }
  
  
  
  /*private Array getArrayFromMember(Variable var, Member m){
		
		//DataType m_dt = writer.findVariable(m.getName()).getDataType();
		DataType v_dt = var.getDataType();
		//DataType m_dt = m.getDataType();
		
		//Writes one single data 
		//int[] shape = writer.findVariable(m.getName()).getShape();
		int[] shape = var.getShape();
		
		//Set the shape we really want
		for(int i=0; i< shape.length; i++ ){
			shape[i] = 1;
		}					
									
		Array arr = Array.factory(v_dt, shape );
		setDataArray( v_dt, arr, m );						
		
		return arr;
				
	}
  
	private void setDataArray(DataType dt, Array arr, Member m){

		//Set the value (int, short, float, double...)
		if( dt  == DataType.SHORT){
			arr.setShort(0, m.getDataArray().getShort(0) );
		}
		
		if( dt  == DataType.INT ){
			arr.setInt(0, m.getDataArray().getInt(0) );
		}		
		
		if( dt  == DataType.DOUBLE){
			arr.setDouble(0, m.getDataArray().getDouble(0) );
		}
		
		if( dt  == DataType.FLOAT){
			arr.setFloat(0, m.getDataArray().getFloat(0) );
		}		
		
	}*/  
  
  

  private LatLonRect getBoundingBox(List stnList) {
    ucar.unidata.geoloc.Station s = (ucar.unidata.geoloc.Station) stnList.get(0);
    LatLonPointImpl llpt = new LatLonPointImpl();
    llpt.set(s.getLatitude(), s.getLongitude());
    LatLonRect rect = new LatLonRect(llpt, .001, .001);

    for (int i = 1; i < stnList.size(); i++) {
      s = (ucar.unidata.geoloc.Station) stnList.get(i);
      llpt.set(s.getLatitude(), s.getLongitude());
      rect.extend(llpt);
    }

    return rect;
  }

  ////////////////////////////

  public static void main(String args[]) throws IOException {
    long start = System.currentTimeMillis();
    String outputFile = "G:/work/manross/split/872d794d.bufr.nc";
    String fDataset = "G:/work/manross/split/872d794d.bufr";

    System.out.println("WriterCFStationCollection from "+fDataset+" to "+outputFile);

        // open point dataset
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, fDataset, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", fDataset, out);
      assert false;
    }
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;


    int count = CFPointWriter.writeFeatureCollection(fdpoint, outputFile, null);
    System.out.printf(" nrecords written = %d%n%n", count);


    long took = System.currentTimeMillis() - start;
    System.out.println("That took = " + took);

  }

}