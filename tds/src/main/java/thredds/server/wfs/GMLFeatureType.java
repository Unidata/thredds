package thredds.server.wfs;

/**
 * An enum for WFS Feature Types as related to GML Types and
 * simple geometry types.
 * 
 * @author wchen@usgs.gov
 *
 */
public enum GMLFeatureType {
	
	Polygon("Polygon");
	

	
	
	
	
	private String stringRepresent;
	
	public String toString() {
		return this.stringRepresent;
	}
	
	private GMLFeatureType(String stringRepresent) {
		this.stringRepresent = stringRepresent;
	}

}
