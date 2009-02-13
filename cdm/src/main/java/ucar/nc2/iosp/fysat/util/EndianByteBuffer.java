package ucar.nc2.iosp.fysat.util;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteOrder;

import java.nio.ByteBuffer;

public class EndianByteBuffer {
	
	public static short LITTLE_ENDIAN = 0; // non_zero for little endian , suit for intel model
	public static short BIG_ENDIAN = -1;// non_zero for big endian , suit for motoroal model
	private short awxEndian = EndianByteBuffer.LITTLE_ENDIAN;
	
	private ByteBuffer byteBuffer;
	
	public EndianByteBuffer(byte[] byteArray,short endian)	{

		if(endian == 0){
			this.awxEndian = EndianByteBuffer.LITTLE_ENDIAN;
		}
		else {
			this.awxEndian = EndianByteBuffer.BIG_ENDIAN;
		}		
		this.byteBuffer = ByteBuffer.allocate(byteArray.length);
		this.byteBuffer.put(byteArray);
		this.byteBuffer.position(0);
	}
	
	public EndianByteBuffer(byte[] byteArray){	
		this(byteArray, EndianByteBuffer.LITTLE_ENDIAN);
		
	}	
	
	
	public void setEndian(short endian){
		if(endian == 0){
			this.awxEndian = EndianByteBuffer.LITTLE_ENDIAN;
		}
		else {
			this.awxEndian = EndianByteBuffer.BIG_ENDIAN;
		}	
	}
	
	public int getInt(){
		int v;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			v= byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getInt();
		}
		else{
			v= byteBuffer.getInt();
		}
		return v;
	}
	
	public short getShort(){
		short v;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			v= byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getShort();
		}
		else{
			v= byteBuffer.getShort();
		}
		return v;
	}
	
	public  long getLong(){
		long v;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			v= byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getLong();
		}
		else{
			v= byteBuffer.getLong();
		}
		return v;
	}
	
	public float getFloat(){
		float v;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			v= byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getFloat();
		}
		else{
			v= byteBuffer.getFloat();
		}
		return v;
	}
	
	public double getDouble(){
		double v;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			v= byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getDouble();
		}
		else{
			v= byteBuffer.getDouble();
		}
		return v;
	}
	
	
	public String getString(int byteCount){
		byte[] buf = new byte[byteCount];
		
		byteBuffer.get(buf);
		String v = null ;
		try {
			v = new String(buf, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return v; 
	}
	
	public short[] getShortArray(){
		int count = (byteBuffer.array().length - byteBuffer.position())/2;
		short[] va = new short[count] ;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			for(int i=0; i<count; i++){
				va[i] = byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getShort();
			}
		}
		else{
			for(int i=0; i<count; i++){
				va[i] = byteBuffer.getShort();
			}
		}
		return va;
	}
	
	public  long[] getLongArray(){
		int count = (byteBuffer.array().length - byteBuffer.position())/8;
		long[] va = new long[count] ;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			for(int i=0; i<count; i++){
				va[i] = byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getLong();
			}
		}
		else{
			for(int i=0; i<count; i++){
				va[i] = byteBuffer.getLong();
			}
		}
		return va;
	}
	public int[] getIntArray(){
		int count = (byteBuffer.array().length - byteBuffer.position())/4;
		int[] va = new int[count] ;
		if(this.awxEndian == EndianByteBuffer.LITTLE_ENDIAN){			
			for(int i=0; i<count; i++){
				va[i] = byteBuffer.order( ByteOrder.LITTLE_ENDIAN ).getInt();
			}
		}
		else{
			for(int i=0; i<count; i++){
				va[i] = byteBuffer.getInt();
			}
		}
		return va;
	}
	
	public final void position(int newPosition) {
		this.byteBuffer.position(newPosition);
	}
}
