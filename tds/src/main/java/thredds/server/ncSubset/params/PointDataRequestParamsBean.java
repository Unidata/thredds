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
	
	//public LatLonPoint getLatLonPoint(){
	//	return new LatLonPointImpl(latitude, longitude);
	//}
	
}
