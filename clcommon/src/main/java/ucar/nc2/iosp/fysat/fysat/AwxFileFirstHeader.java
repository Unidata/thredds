/**
 * 
 */
package ucar.nc2.iosp.fysat;

import ucar.nc2.iosp.fysat.util.EndianByteBuffer;


/**
 * @author Hurricane
 *
 */
public class AwxFileFirstHeader {

	static final int AWX_PRODUCT_TYPE_UNDEFINED = 0;
	static final int AWX_PRODUCT_TYPE_GEOSAT_IMAGE = 1;
	static final int AWX_PRODUCT_TYPE_POLARSAT_IMAGE = 2; //Polar_orbiting_satellites 
	static final int AWX_PRODUCT_TYPE_GRID = 3;
	static final int AWX_PRODUCT_TYPE_DISCREET = 4;
	static final int AWX_PRODUCT_TYPE_GRAPH_ANALIYSIS = 5;
	  
//	char[] fileName = new char[12]; 
//	short  byteOrder; 
//	short  firstHeaderLength; 
//	short  secondHeaderLength; 
//	short  fillSectionLength; 
//	short  recoderLength; 
//	short  recordsOfHeader; 
//	short  recordsOfData; 
//	short  typeOfProduct; 
//	short  typeOfCompress; 
//	char[] version = new char[8]; 
//	short  flagOfQuality; 
	
	String fileName ; 
	short  byteOrder; 
	short  firstHeaderLength; 
	short  secondHeaderLength; 
	short  fillSectionLength; 
	short  recoderLength; 
	short  recordsOfHeader; 
	short  recordsOfData; 
	short  typeOfProduct; 
	short  typeOfCompress; 
	String version ; 
	short  flagOfQuality; 
	
	/**
	 * 
	 */
	public AwxFileFirstHeader() {
		// TODO Auto-generated constructor stub
	}
	
	
	public void fillHeader(EndianByteBuffer ebb){
//		char[] fileName = new char[12]; 
//		short  byteOrder; 
//		short  firstHeaderLength; 
//		short  secondHeaderLength; 
//		short  fillSectionLength; 
//		short  recoderLength; 
//		short  recordsOfHeader; 
//		short  recordsOfData; 
//		short  typeOfProduct; 
//		short  typeOfCompress; 
//		char[] version = new char[8]; 
//		short  flagOfQuality; 
		this.fileName = ebb.getString(12);
		this.byteOrder = ebb.getShort();
		this.firstHeaderLength = ebb.getShort();
		this.secondHeaderLength = ebb.getShort();
		this.fillSectionLength = ebb.getShort();
		this.recoderLength = ebb.getShort();
		this.recordsOfHeader = ebb.getShort();
		this.recordsOfData = ebb.getShort();
		this.typeOfProduct = ebb.getShort();
		this.typeOfCompress = ebb.getShort();
		this.version = ebb.getString(8).trim();
		this.flagOfQuality = ebb.getShort();
		
	}

}
