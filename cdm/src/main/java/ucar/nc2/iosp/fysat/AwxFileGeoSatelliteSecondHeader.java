/**
 * 
 */
package ucar.nc2.iosp.fysat;

import ucar.nc2.iosp.fysat.util.EndianByteBuffer;

/**
 * @author Hurricane
 *
 */
public class AwxFileGeoSatelliteSecondHeader
extends AwxFileSecondHeader {

	String satelliteName; 
	short year; 
	short month; 
	short day; 
	short hour;
	short minute; 
	short channel;
	short flagOfProjection; 
	short widthOfImage; 
	short heightOfImage;
	short scanLineNumberOfImageTopLeft; 
	short pixelNumberOfImageTopLeft; 
	short sampleRatio; 
	float latitudeOfNorth; 
	float latitudeOfSouth; 
	float longitudeOfWest; 
	float longitudeOfEast; 
	float centerLatitudeOfProjection;
	float centerLongitudeOfProjection;
	float standardLatitude1;
	float standardLatitude2;
	short horizontalResolution;
	short verticalResolution; 
	short overlapFlagGeoGrid; 
	short overlapValueGeoGrid;
	short dataLengthOfColorTable; 
	short dataLengthOfCalibration;
	short dataLengthOfGeolocation;
	short reserved; 
	/**
	 * 
	 */
	public AwxFileGeoSatelliteSecondHeader() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void fillHeader(EndianByteBuffer byteBuffer){
		satelliteName = byteBuffer.getString(8).trim(); 
		year = byteBuffer.getShort();
		month = byteBuffer.getShort();
		day = byteBuffer.getShort();
		hour = byteBuffer.getShort();
		minute = byteBuffer.getShort();
		channel = byteBuffer.getShort();
		flagOfProjection = byteBuffer.getShort();
		widthOfImage = byteBuffer.getShort();
		heightOfImage = byteBuffer.getShort();
		scanLineNumberOfImageTopLeft = byteBuffer.getShort();
		pixelNumberOfImageTopLeft = byteBuffer.getShort();
		sampleRatio = byteBuffer.getShort();
		latitudeOfNorth = byteBuffer.getShort()/100.0f;
		latitudeOfSouth = byteBuffer.getShort()/100.0f;
		longitudeOfWest = byteBuffer.getShort()/100.0f;
		longitudeOfEast = byteBuffer.getShort()/100.0f;
		centerLatitudeOfProjection = byteBuffer.getShort()/100.0f;
		centerLongitudeOfProjection = byteBuffer.getShort()/100.0f;
		standardLatitude1 = byteBuffer.getShort()/100.0f;
		standardLatitude2 = byteBuffer.getShort()/100.0f;
		horizontalResolution = byteBuffer.getShort();
		verticalResolution = byteBuffer.getShort();
		overlapFlagGeoGrid = byteBuffer.getShort();
		overlapValueGeoGrid = byteBuffer.getShort();
		dataLengthOfColorTable = byteBuffer.getShort();
		dataLengthOfCalibration = byteBuffer.getShort();
		dataLengthOfGeolocation = byteBuffer.getShort();
		reserved = byteBuffer.getShort();; 
	}

}
