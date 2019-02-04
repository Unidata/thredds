package thredds.server.wfs;

/**
 * Enum representing WFS Request Types as specified by the OGC.
 * 
 * @author wchen@usgs.gov
 *
 */
public enum WFSRequestType {
	
	GetCapabilities("GetCapabilities", 0),
	GetFeature("GetFeature", 1),
	DescribeFeatureType("DescribeFeatureType", 2);
	
	String requestType;
	int number;
	
	WFSRequestType(String requestType, int identifier){
		this.requestType = requestType;
		this.number = identifier;
	}
	
	public String toString() {
		return requestType;
	}
	
	public int toID()
	{
		return this.number;
	}
	
	/**
	 * Converts a string to a WFSRequestType if possible.
	 * 
	 * @param request value
	 * @return the string as a WFSRequestType if possible.
	 */
	public static WFSRequestType getWFSRequestType(String request) {
		if(request == null) return null;
		if(request.equalsIgnoreCase(WFSRequestType.GetCapabilities.toString())) return WFSRequestType.GetCapabilities;
		if(request.equalsIgnoreCase(WFSRequestType.GetFeature.toString())) return WFSRequestType.GetFeature;
		if(request.equalsIgnoreCase(WFSRequestType.DescribeFeatureType.toString())) return WFSRequestType.DescribeFeatureType;
		else return null;
	}
}
