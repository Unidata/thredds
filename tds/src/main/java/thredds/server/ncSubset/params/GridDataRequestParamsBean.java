/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncSubset.params;

import javax.validation.constraints.NotNull;

public class GridDataRequestParamsBean extends RequestParamsBean {
	
	
	private Double minx;
	
	private Double maxx;
	
	private Double miny;
	
	private Double maxy;
	
	private boolean addLatLon;
	
	//default 
	//@NotNull(message="horizStride param may not be null")
	private Integer horizStride = 1;
	
	//default
	//@NotNull(message="timeStride param may not be null")
	private Integer timeStride = 1;
	
	private Integer vertStride=1;

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
