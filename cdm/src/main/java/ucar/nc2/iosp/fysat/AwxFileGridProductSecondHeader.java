/**
 * 
 */
package ucar.nc2.iosp.fysat;

import ucar.nc2.iosp.fysat.util.EndianByteBuffer;

/**
 * @author Hurricane
 *
 */
public final class AwxFileGridProductSecondHeader extends AwxFileSecondHeader {
	
	/**
	 * 
	 */		
		String satelliteName; 
		short gridFeature;
		short byteAmountofData;
		short dataBaseValue;
		short dataScale;
		short timePeriodCode;

		short startYear; 
		short startMonth; 
		short startDay; 
		short startHour; 
		short startMinute; 

		short endYear; 
		short endMonth; 
		short endDay; 
		short endHour; 
		short endMinute; 

		float leftTopLat;
		float leftTopLon;
		float rightBottomLat;
		float rightBottomLon;
		

		short gridSpacingUnit;
		short horizontalSpacing;
		short verticalSpacing;
		short amountofHorizontalSpacing;
		short amountofVerticalSpacing;

		short hasLand;
		short landCode;

		short hasCloud;
		short cloudCode;

		short hasWater;
		short waterCode;

		short hasIce;
		short iceCode;

		short hasQualityControl;
		short qualityControlCeiling;
		short qualityControlFloor;
		short reserved;
		
		public static short GRID_FEATURE_OCEAN_TEMPERATURE = 1; // unit(K) 
		public static short GRID_FEATURE_SEA_ICE_DISTRIBUTION = 2; // no unit
		public static short GRID_FEATURE_SEA_ICE_DENSITY = 3;	//no unit 
		public static short GRID_FEATURE_OUTGOING_LONGWAVE_RADIATION = 4; // unit(W/m2) 
		public static short GRID_FEATURE_NORMALIZE_VEGETATIOIN_INDEX = 5; // no unit
		public static short GRID_FEATURE_RATIO_VEGETATIOIN_INDEX = 6; // no unit
		public static short GRID_FEATURE_SNOW_DISTRIBUTION = 7;  // no unit
		public static short GRID_FEATURE_SOIL_HUMIDITY = 8;  // unit(kg/m2)
		public static short GRID_FEATURE_SUNLIGHT_DURATION = 9;  // unit(hour)
		public static short GRID_FEATURE_CLOUD_TOP_HEIGHT = 10;  // unit(hPa)
		public static short GRID_FEATURE_CLOUD_TOP_TEMPERATURE = 11;  // unit(hPa)
		public static short GRID_FEATURE_LOW_CLOUD_AMOUNT = 12;  //  no unit
		public static short GRID_FEATURE_HIGH_CLOUD_AMOUNT = 13;  //  no unit
		public static short GRID_FEATURE_RAIN_INDEX_PER_HOUR = 14;  // unit(mm/hour)
		public static short GRID_FEATURE_RAIN_INDEX_PER_6HOUR = 15;  // unit(mm/6hour)
		public static short GRID_FEATURE_RAIN_INDEX_PER_12HOUR = 16;  // unit(mm/12hour)
		public static short GRID_FEATURE_RAIN_INDEX_PER_24HOUR = 17;  // unit(mm/24hour)
		// more ...
		// byte[] fillData = new byte[242];
		
		
		public static short GRID_SPACING_UNIT_POINT001DEGREE = 0; // 0.01degree
		public static short GRID_SPACING_UNIT_KILOMETER = 1; // kilometer
		public static short GRID_SPACING_UNIT_METER = 2; // meter
	
		public AwxFileGridProductSecondHeader() {
			super();
			// TODO Auto-generated constructor stub
		}
		
		
		
		public void fillHeader(EndianByteBuffer byteBuffer){
			satelliteName = byteBuffer.getString(8).trim(); 
			gridFeature = byteBuffer.getShort();
			byteAmountofData = byteBuffer.getShort();
			dataBaseValue = byteBuffer.getShort();
			dataScale = byteBuffer.getShort();
			timePeriodCode = byteBuffer.getShort();

			startYear = byteBuffer.getShort();
			startMonth = byteBuffer.getShort();
			startDay = byteBuffer.getShort();
			startHour = byteBuffer.getShort();
			startMinute = byteBuffer.getShort();

			endYear = byteBuffer.getShort();
			endMonth = byteBuffer.getShort();
			endDay = byteBuffer.getShort();
			endHour = byteBuffer.getShort();
			endMinute = byteBuffer.getShort();

			leftTopLat = byteBuffer.getShort() / 100.0f;
			leftTopLon = byteBuffer.getShort() / 100.0f;
			rightBottomLat = byteBuffer.getShort()/ 100.0f;
			rightBottomLon = byteBuffer.getShort() / 100.0f;
			

			// fix spacing according to unit.
			gridSpacingUnit = byteBuffer.getShort();
			horizontalSpacing = byteBuffer.getShort();
			verticalSpacing = byteBuffer.getShort();
			
			amountofHorizontalSpacing = byteBuffer.getShort();
			amountofVerticalSpacing = byteBuffer.getShort();

			hasLand = byteBuffer.getShort();
			landCode = byteBuffer.getShort();

			hasCloud = byteBuffer.getShort();
			cloudCode = byteBuffer.getShort();

			hasWater = byteBuffer.getShort();
			waterCode = byteBuffer.getShort();

			hasIce = byteBuffer.getShort();
			iceCode = byteBuffer.getShort();

			hasQualityControl = byteBuffer.getShort();
			qualityControlCeiling = byteBuffer.getShort();
			qualityControlFloor = byteBuffer.getShort();
			reserved = byteBuffer.getShort();
			
			// byte[] fillData = new byte[242];
		
		}
		
		public String getSpacingUnit()
		{
			switch(this.gridSpacingUnit)
			{
				case 0:
					return "0.01degree";
				case 1:
					return "kilometer";
				case 2:
					return "meter";				
			}
			return "";
		}

}
