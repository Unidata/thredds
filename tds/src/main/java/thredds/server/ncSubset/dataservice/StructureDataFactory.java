package thredds.server.ncSubset.dataservice;

import java.util.ArrayList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.vertical.VerticalTransform;

public final class StructureDataFactory {
	
	private static StructureDataFactory INSTANCE;
	
	private StructureDataFactory(){
		
	}
	
	public static StructureDataFactory getFactory(){
		
		if(INSTANCE == null){
			INSTANCE = new  StructureDataFactory();
		}
		
		return INSTANCE;
	}
	
	public StructureData createStructureDataWithVerticalLevels(GridDataset gds,	LatLonPoint point, List<String> vars, List<Double> vertLevels,	int arrLen, CoordinateAxis1D zAxis) {

		StructureMembers members = new StructureMembers("");
		int vertLevelsLen = vertLevels.size();
		int[] dimShape = new int[1];
		int[] dataShape = new int[2];
		int[] coordsShape = new int[3];

		StructureMembers.Member timeMember = members.addMember("date", null, null, DataType.STRING, dimShape);
		ArrayObject.D1 timeData = new ArrayObject.D1(String.class, arrLen);
		timeMember.setDataArray(timeData);

		StructureMembers.Member vertCoordMember = members.addMember("vertCoord", null, zAxis.getUnitsString(), DataType.DOUBLE, coordsShape);
		ArrayDouble.D3 vertData = new ArrayDouble.D3(arrLen,vertLevelsLen, vars.size());
		vertCoordMember.setDataArray(vertData);

		StructureMembers.Member latMember = members.addMember("lat", null, 	CDM.LAT_UNITS, DataType.DOUBLE, coordsShape);
		ArrayDouble.D3 latData = new ArrayDouble.D3(arrLen, vertLevelsLen, vars.size());
		latMember.setDataArray(latData);
		StructureMembers.Member lonMember = members.addMember("lon", null, CDM.LON_UNITS, DataType.DOUBLE, coordsShape);
		ArrayDouble.D3 lonData = new ArrayDouble.D3(arrLen, vertLevelsLen, vars.size());
		lonMember.setDataArray(lonData);

		StructureDataW sdata = new StructureDataW(members);

		sdata.setMemberData(timeMember, timeData);
		sdata.setMemberData(vertCoordMember, timeData);
		sdata.setMemberData(latMember, latData);
		sdata.setMemberData(lonMember, lonData);

		// add the grid members
		for (String varName : vars) {
			GridDatatype grid = gds.findGridDatatype(varName);
			StructureMembers.Member m = members.addMember(grid.getVariable().getShortName(), null, grid.getUnitsString(), grid.getDataType(), dataShape);
			ArrayDouble.D2 data = new ArrayDouble.D2(arrLen, vertLevelsLen);
			m.setDataArray(data);
			sdata.setMemberData(m, data);
		}

		return sdata;
	}
	
	public StructureData createStructureData(GridDataset gds, LatLonPoint point, List<String> vars, int arrLen) {

		StructureMembers members = new StructureMembers("");
		// int[] scalarShape = new int[0];
		int[] dataShape = new int[1];

		ArrayObject.D0 stnData = new ArrayObject.D0(String.class);

		StructureMembers.Member timeMember = members.addMember("date", null, null, DataType.STRING, dataShape);
		ArrayObject.D1 timeData = new ArrayObject.D1(String.class, arrLen);
		timeMember.setDataArray(timeData);

		StructureMembers.Member latMember = members.addMember("lat", null, CDM.LAT_UNITS, DataType.DOUBLE, dataShape);
		ArrayDouble.D1 latData = new ArrayDouble.D1(arrLen);
		latMember.setDataArray(latData);
		StructureMembers.Member lonMember = members.addMember("lon", null, CDM.LON_UNITS, DataType.DOUBLE, dataShape);
		ArrayDouble.D1 lonData = new ArrayDouble.D1(arrLen);
		lonMember.setDataArray(lonData);

		StructureDataW sdata = new StructureDataW(members);
		// sdata.setMemberData(stnMember, stnData);
		sdata.setMemberData(timeMember, timeData);
		sdata.setMemberData(latMember, latData);
		sdata.setMemberData(lonMember, lonData);

		// for now, we only have one point = one station ???
		String stnName = "GridPoint";
		String desc = "Grid Point at lat/lon=" + point.getLatitude() + ","
				+ point.getLongitude();
		ucar.unidata.geoloc.Station s = new ucar.unidata.geoloc.StationImpl(
				stnName, desc, "", point.getLatitude(), point.getLongitude(),
				Double.NaN);
		List<ucar.unidata.geoloc.Station> stnList = new ArrayList<ucar.unidata.geoloc.Station>();
		stnList.add(s);
		stnData.set(stnName);

		// add the grid members
		for (String varName : vars) {
			GridDatatype grid = gds.findGridDatatype(varName);
			StructureMembers.Member m = members.addMember(grid.getVariable()
					.getShortName(), null, grid.getUnitsString(), grid
					.getDataType(), dataShape);
			// Array data = Array.factory(grid.getDataType(), dataShape);
			ArrayDouble.D1 data = new ArrayDouble.D1(arrLen);
			m.setDataArray(data);
			sdata.setMemberData(m, data);
		}

		return sdata;
	}	
	
	/**
	 * 
	 * Creates a StructureData that holds the variable values for a single time and vertical level
	 * 
	 * @param gds
	 * @param point
	 * @param vars
	 * @param hasTime
	 * @return  StructureData
	 */
	public StructureData createSingleStructureData(GridDataset gds, LatLonPoint point, List<String> vars, boolean hasTime) {
		
	    // construct the StructureData
	    StructureMembers members = new StructureMembers("");
	    int[] scalarShape = new int[0];

	    StructureMembers.Member stnMember = members.addMember("station", null, null, DataType.STRING, scalarShape);
	    //StructureMembers.Member stnMember = members.addMember("profileId", null, null, DataType.STRING, scalarShape);
	    ArrayObject.D0 stnData = new ArrayObject.D0(String.class);
	    stnMember.setDataArray(stnData);
	    
	    //StructureMembers.Member timeMember = members.addMember("date", null, null, DataType.STRING, scalarShape);
	    //ArrayObject.D0 timeData = new ArrayObject.D0(String.class);	    
	    //timeMember.setDataArray(timeData);
	    
	    
	    //StructureMembers.Member latMember = members.addMember("latitude", null, "degrees_north", DataType.DOUBLE, scalarShape);
	    //ArrayDouble.D0 latData = new ArrayDouble.D0();
	    //latMember.setDataArray(latData);
	    
	    //StructureMembers.Member lonMember = members.addMember("longitude", null, "degrees_east", DataType.DOUBLE, scalarShape);
	    //ArrayDouble.D0 lonData = new ArrayDouble.D0();
	    //lonMember.setDataArray(lonData);

	    StructureDataW sdata = new StructureDataW(members);
	    sdata.setMemberData(stnMember, stnData);
	    
	    if(hasTime){
	    	StructureMembers.Member timeMember = members.addMember("time", null, null, DataType.DOUBLE, scalarShape);
	    	ArrayDouble.D0 timeData = new ArrayDouble.D0();
	    	timeMember.setDataArray(timeData);
	    	sdata.setMemberData(timeMember, timeData);
	    }	    
	    
	    
	    //sdata.setMemberData(latMember, latData);
	    //sdata.setMemberData(lonMember, lonData);



	    // add the grid members
	    for (String varName : vars) {
	    	GridDatatype grid = gds.findGridDatatype(varName);	
	    	StructureMembers.Member m = members.addMember(grid.getVariable().getShortName(), null, grid.getUnitsString(), grid.getDataType(), scalarShape);
	    	Array data = Array.factory(grid.getDataType(), scalarShape);
	    	m.setDataArray(data);
	    	sdata.setMemberData(m, data);
	    }
    
	    String stnName = "GridPoint";
	    stnData.set(stnName);					    
	    
		return sdata;
	}
	
	public StructureData createSingleStructureData(GridDataset gds, LatLonPoint point, List<String> vars, CoordinateAxis1D zAxis, boolean hasTime){
		int[] scalarShape = new int[0];
		StructureDataW sd = (StructureDataW)createSingleStructureData(gds,  point,  vars, hasTime);
		
	    // add vertical
	    ArrayDouble.D0 zData = new ArrayDouble.D0();	 	    
	    StructureMembers.Member zMember = sd.getStructureMembers().addMember(zAxis.getShortName(), null, zAxis.getUnitsString(), DataType.DOUBLE, scalarShape);
	    zMember.setDataArray(zData);
	    sd.setMemberData(zMember, zData);
	    
	    for( String v : vars ){
	    	
	    	VerticalCT vct = gds.findGridByShortName(v).getCoordinateSystem().getVerticalCT();	    	
	    	if(vct != null){//Variables are grouped by vertical levels, so just one vertical transform is expected.
	    		VerticalTransform vt = gds.findGridByShortName(v).getCoordinateSystem().getVerticalTransform();
	    		ArrayDouble.D0 vData = new ArrayDouble.D0();
	    		StructureMembers.Member vMember = sd.getStructureMembers().addMember( vct.getName() , null, vt.getUnitString() , DataType.DOUBLE, scalarShape);
	    		vMember.setDataArray(vData);
	    		sd.setMemberData(vMember, vData);

	    	}
	    }
	    
	    
	    return sd;
	}
	
	public StructureData createSingleStructureData(GridDataset gds, LatLonPoint point, List<String> vars, String zUnits, boolean hasTime){
		int[] scalarShape = new int[0];
		StructureDataW sd = (StructureDataW)createSingleStructureData(gds,  point,  vars, hasTime);
		
	    // add vertical
	    ArrayDouble.D0 zData = new ArrayDouble.D0();	 	    
	    StructureMembers.Member zMember = sd.getStructureMembers().addMember("vertCoord", null, zUnits, DataType.DOUBLE, scalarShape);
	    zMember.setDataArray(zData);
	    sd.setMemberData(zMember, zData);
	    
	    return sd;
	}	

}
