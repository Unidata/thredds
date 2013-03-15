package thredds.server.ncSubset.params;

import javax.validation.constraints.NotNull;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

public class GridDataRequestParamsBean extends RequestParamsBean {
	
	//@NotNull(message="north param may not be null")	
	private Double north;
	
	//@NotNull(message="south param may not be null")	
	private Double south;
	
	//@NotNull(message="east param may not be null")	
	private Double east;
	
	//@NotNull(message="west param may not be null")	
	private Double west;
	
	
	private Double minx;
	
	private Double maxx;
	
	private Double miny;
	
	private Double maxy;
	
	private boolean addLatLon;
	
	//default 
	@NotNull(message="horizStride param may not be null")
	private Integer horizStride = 1;
	
	//default
	@NotNull(message="timeStride param may not be null")
	private Integer timeStride = 1;
	
	private Integer vertStride=1;

	public Double getNorth() {
		return north;
	}

	public void setNorth(Double north) {
		this.north = north;
	}

	public Double getSouth() {
		return south;
	}

	public void setSouth(Double south) {
		this.south = south;
	}

	public Double getEast() {
		return east;
	}

	public void setEast(Double east) {
		this.east = east;
	}

	public Double getWest() {
		return west;
	}

	public void setWest(Double west) {
		this.west = west;
	}

	public Double getMinx() {
		return minx;
	}

	public void setMinx(Double minx) {
		this.minx = minx;
	}

	public Double getMaxx() {
		return maxx;
	}

	public void setMaxx(Double maxx) {
		this.maxx = maxx;
	}

	public Double getMiny() {
		return miny;
	}

	public void setMiny(Double miny) {
		this.miny = miny;
	}

	public Double getMaxy() {
		return maxy;
	}

	public void setMaxy(Double maxy) {
		this.maxy = maxy;
	}

	public boolean isAddLatLon() {
		return addLatLon;
	}

	public void setAddLatLon(boolean addLatLon) {
		this.addLatLon = addLatLon;
	}

	public Integer getHorizStride() {
		return horizStride;
	}

	public void setHorizStride(Integer horizStride) {
		this.horizStride = horizStride;
	}

	public Integer getTimeStride() {
		return timeStride;
	}

	public void setTimeStride(Integer timeStride) {
		this.timeStride = timeStride;
	}
	
	public Integer getVertStride() {
		return vertStride;
	}

	public void setVertStride(Integer vertStride) {
		this.vertStride = vertStride;
	}	
	
	/*public LatLonRect getBB(){
		return new LatLonRect(new LatLonPointImpl(getSouth(), getWest()), new LatLonPointImpl(getNorth(), getEast()));
	}*/
	

}
