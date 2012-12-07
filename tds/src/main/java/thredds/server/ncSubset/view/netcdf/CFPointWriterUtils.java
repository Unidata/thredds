/**
 * 
 */
package thredds.server.ncSubset.view.netcdf;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Variable;

/**
 * @author mhermida
 *
 */
final class CFPointWriterUtils {
	
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

}
