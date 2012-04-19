package thredds.server.ncSubset.params;

import javax.validation.constraints.NotNull;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

public class PointDataRequestParamsBean extends RequestParamsBean {
	
	@NotNull(message="latitude param may not be null")	
	private Double latitude;
	
	@NotNull(message="longitude param may not be null")
	private Double longitude;
			
	//@NotNull(message="point param may not be null")
	private Boolean point;
		
	
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

	public Boolean getPoint() {
		return point;
	}
	public void setPoint(Boolean point) {
		this.point = point;
	}
	
	public LatLonPoint getLatLonPoint(){
		return new LatLonPointImpl(latitude, longitude);
	}

}
