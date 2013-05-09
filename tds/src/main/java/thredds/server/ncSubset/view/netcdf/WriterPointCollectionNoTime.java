package thredds.server.ncSubset.view.netcdf;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * 
 * Writes point feature collection with no time axis. It does not actually match any CF point feature type but we'll use type.
 *  
 * 
 * @author mhermida
 *
 */
class WriterPointCollectionNoTime extends CFPointWriter {

	private Variable lat, lon, alt, record;

	private int recno = 0;
	private ArrayDouble.D1 latArray = new ArrayDouble.D1(1);
	private ArrayDouble.D1 lonArray = new ArrayDouble.D1(1);
	private ArrayDouble.D1 altArray = new ArrayDouble.D1(1);
	private int[] origin = new int[1];	

	static private Logger log = LoggerFactory.getLogger(WriterPointCollectionNoTime.class);

	protected WriterPointCollectionNoTime(NetcdfFileWriter.Version version, String fileOut, List<Attribute> atts)
			throws IOException {
		super(fileOut, atts, version, false);

	}


	public void writeHeader(List<Station> stns, List<VariableSimpleIF> vars, String altUnits) throws IOException {
		this.altUnits = altUnits;

		createCoordinates();
		createDataVariables(vars);
		llbb = CFPointWriterUtils.getBoundingBox(stns); // gets written in super.finish();
		
		writer.create(); // done with define mode
		record = writer.addRecordStructure();
	}

	public void writeRecord( EarthLocation loc, StructureData sdata) throws IOException {


		// needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
		ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
		sArray.setStructureData(sdata, 0);

		//timeArray.set(0, timeCoordValue);
		latArray.set(0, loc.getLatitude());
		lonArray.set(0, loc.getLongitude());
		if (altUnits != null)
			altArray.set(0, loc.getAltitude());

		// write the recno record
		origin[0] = recno;
		try {
			//Cannot use record -> NetcdfFileWriter does not use 
			//records if the format is netcdf4
			//writer.write(record, origin, sArray);			
			for( Member m : sdata.getMembers() ){
				
				if( writer.findVariable(m.getName()) != null){
										
					Array arr = CFPointWriterUtils.getArrayFromMember(writer.findVariable(m.getName()), m);						 
					writer.write( writer.findVariable(m.getName()) , origin, arr );

				}								
			}


			writer.write(lat, origin, latArray);
			writer.write(lon, origin, lonArray);
			if (altUnits != null)
				writer.write(alt, origin, altArray);

			//trackBB(loc);

		} catch (InvalidRangeException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}

		recno++;
	}

	//	public void finish() throws IOException{
	//		
	//		writer.close();
	//	}	  

	private void createCoordinates(){

		writer.addUnlimitedDimension(recordDimName);

		// add the station Variables using the station dimension
		lat = writer.addVariable(null, latName, DataType.DOUBLE, recordDimName);
		writer.addVariableAttribute(lat, new Attribute(CDM.UNITS, CDM.LAT_UNITS));
		writer.addVariableAttribute(lat, new Attribute(CDM.LONG_NAME, "station latitude"));

		lon = writer.addVariable(null, lonName, DataType.DOUBLE, recordDimName);
		writer.addVariableAttribute(lon, new Attribute(CDM.UNITS, CDM.LON_UNITS));
		writer.addVariableAttribute(lon, new Attribute(CDM.LONG_NAME, "station longitude"));

		if (altUnits != null) {
			alt = writer.addVariable(null, altName, DataType.DOUBLE, recordDimName);
			writer.addVariableAttribute(alt, new Attribute(CDM.UNITS, altUnits));
			// ncfile.addVariableAttribute(v, new Attribute("positive", "up"));
			writer.addVariableAttribute(alt, new Attribute(CDM.LONG_NAME, "altitude"));
		}		
	}

	private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {
		String coordNames = latName +" "+ lonName;
		if (altUnits != null)
			coordNames = coordNames +" " + altName;

		// find all dimensions needed by the data variables
		for (VariableSimpleIF var : dataVars) {
			List<Dimension> dims = var.getDimensions();
			dimSet.addAll(dims);
		}

		// add them
		for (Dimension d : dimSet) {
			if (!d.isUnlimited())
				writer.addDimension(null, d.getShortName(), d.getLength(), d.isShared(), false, d.isVariableLength());
		}

		// add the data variables all using the record dimension
		for (VariableSimpleIF oldVar : dataVars) {
			List<Dimension> dims = oldVar.getDimensions();
			StringBuilder dimNames = new StringBuilder(recordDimName);
			for (Dimension d : dims) {
				if (!d.isUnlimited())
					dimNames.append(" ").append(d.getShortName());
			}
			Variable newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());

			List<Attribute> atts = oldVar.getAttributes();
			for (Attribute att : atts) {
				newVar.addAttribute( att);
			}

			newVar.addAttribute( new Attribute(CF.COORDINATES, coordNames));
		}

	}

/*	protected void trackBB(EarthLocation loc) {
		if (loc != null) {
			if (llbb == null) {
				llbb = new LatLonRect(loc.getLatLon(), .001, .001);
				return;
			}
			llbb.extend(loc.getLatLon());
		}

	}*/	

}
