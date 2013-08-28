package thredds.server.ncSubset.params;

import java.util.List;

import thredds.server.ncSubset.validation.SubsetTypeConstraint;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

@SubsetTypeConstraint
public class PointDataRequestParamsBean extends RequestParamsBean {
	
	//@NotNull(message="latitude param may not be null")	
	private Double latitude;
	
	//@NotNull(message="longitude param may not be null")
	private Double longitude;
			
	private Double north;
	
	private Double south;
	
	private Double east;
	
	private Double west;
	
	//private Boolean point;
	
	//Spatial subsetting type. Must be: point (default), all, bb, stns
	private String subset;
	
	//Must be present if stn=stns
	private List<String> stns;
	
	public Double getLatitude() {
		return latitude;
	}
		
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/*public Boolean getPoint() {
		return point;
	}
	public void setPoint(Boolean point) {
		this.point = point;
	}*/
	
	public String getSubset(){
		return this.subset;
	}
	
	public void setSubset(String subset){
		this.subset = subset;
	}
	
	public List<String> getStns(){
		return this.stns;
	}
	
	public void setStns(List<String> stns){
		this.stns = stns;
	}	
	
	/**
	 * @return the north
	 */
	public Double getNorth() {
		return north;
	}

	/**
	 * @param north the north to set
	 */
	public void setNorth(Double north) {
		this.north = north;
	}

	/**
	 * @return the south
	 */
	public Double getSouth() {
		return south;
	}

	/**
	 * @param south the south to set
	 */
	public void setSouth(Double south) {
		this.south = south;
	}

	/**
	 * @return the east
	 */
	public Double getEast() {
		return east;
	}

	/**
	 * @param east the east to set
	 */
	public void setEast(Double east) {
		this.east = east;
	}

	/**
	 * @return the west
	 */
	public Double getWest() {
		return west;
	}

	/**
	 * @param west the west to set
	 */
	public void setWest(Double west) {
		this.west = west;
	}

	//public LatLonPoint getLatLonPoint(){
	//	return new LatLonPointImpl(latitude, longitude);
	//}
	
}
