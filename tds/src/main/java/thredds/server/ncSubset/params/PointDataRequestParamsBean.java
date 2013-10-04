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
			

  private String req; // point or station

  public String getReq() {
    return req;
  }

  public void setReq(String req) {
    this.req = req;
  }

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
