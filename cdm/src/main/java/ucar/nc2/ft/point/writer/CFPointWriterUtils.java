/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * 
 */
package ucar.nc2.ft.point.writer;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.util.Iterator;
import java.util.List;

/**
 * @author mhermida
 *
 */
public final class CFPointWriterUtils {
	
	private CFPointWriterUtils(){}
	
	public static Array getArrayFromMember(Variable var, Member m){
	
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
	
	private static void setDataArray(DataType dt, Array arr, Member m){

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
		
	}

	public static LatLonRect getBoundingBox(List<? extends Station> stnList) {
		Station s =  stnList.get(0);
		LatLonPointImpl llpt = new LatLonPointImpl();
		llpt.set(s.getLatitude(), s.getLongitude());
		LatLonRect rect = new LatLonRect(llpt, 0, 0);

		for (int i = 1; i < stnList.size(); i++) {
			s = stnList.get(i);
			llpt.set(s.getLatitude(), s.getLongitude());
			rect.extend(llpt);
		}

    // To give a little "wiggle room", we're going to slightly expand the bounding box.
    double newLowerLeftLat = rect.getLowerLeftPoint().getLatitude() - .0005;
    double newLowerLeftLon = rect.getLowerLeftPoint().getLongitude() - .0005;
    LatLonPointImpl newLowerLeftPoint = new LatLonPointImpl(newLowerLeftLat, newLowerLeftLon);

    double newUpperRightLat = rect.getUpperRightPoint().getLatitude() + .0005;
    double newUpperRightLon = rect.getUpperRightPoint().getLongitude() + .0005;
    LatLonPointImpl newUpperRightPoint = new LatLonPointImpl(newUpperRightLat, newUpperRightLon);

    rect.extend(newLowerLeftPoint);
    rect.extend(newUpperRightPoint);

		return rect;
	}
	
	public static Attribute findCDMAtt(List<Attribute> atts, String attName){
		
		Iterator<Attribute> it = atts.iterator();
	
		Attribute target = null;
		while( it.hasNext() && target == null ){
			Attribute att = it.next();
			if( att.getFullName().equals(attName) ||  att.getShortName().equals(attName)){
					target = att;				
			}
		}
		
		return target;
	}
}
