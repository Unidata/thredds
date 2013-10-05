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

import javax.validation.Valid;

import thredds.server.ncSubset.validation.TimeParamsConstraint;
import thredds.server.ncSubset.validation.VarParamConstraint;

@TimeParamsConstraint
public class RequestParamsBean extends ParamsBean {
	
	@VarParamConstraint
	private List<String> var;

	@Valid
	private String time_start;

	@Valid
	private String time_end;
	
	@Valid
	private String time_duration;
	
	@Valid
	private String time_window;
	
	private String time;
	
	private String temporal;
	
	private Double vertCoord;
	
	private Double north;
	
	private Double south;
	
	private Double east;
	
	private Double west;
	
	
	public List<String> getVar() {
		return var;
	}
	public void setVar(List<String> var) {
		this.var = var;
	}
	public String getTime_start() {
		return time_start;
	}
	public void setTime_start(String time_start) {
		this.time_start = time_start;
	}
	public String getTime_end() {
		return time_end;
	}
	public void setTime_end(String time_end) {
		this.time_end = time_end;
	}
	public String getTime_duration() {
		return time_duration;
	}
	public void setTime_duration(String time_duration) {
		this.time_duration = time_duration;
	}
	
	public void setTime(String time){
		this.time = time;
	}
	
	public String getTime(){
		return this.time;
	}

	public void setTime_window(String time_window){
		this.time_window = time_window;
	}
	
	public String getTime_window(){
		return this.time_window;
	}	
	
	public void setTemporal(String temporal){
		this.temporal = temporal;
	}
	
	public String getTemporal(){
		return this.temporal;
	}
	
	public Double getVertCoord() {
		return vertCoord;
	}
	public void setVertCoord(Double vertCoord) {
		this.vertCoord = vertCoord;
	}
		
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

	/*public CalendarDateRange getCalendarDateRange() throws ParseException{
		
		DateRange dr; 
		if(time == null)
			dr = new DateRange( new DateType(time_start, null, null), new DateType(time_end, null, null), new TimeDuration(time_duration), null );
		else{
			DateType dtDate = new DateType(time, null, null);			
			dr = new DateRange( dtDate.getDate(), dtDate.getDate() );
		}	
		
		return CalendarDateRange.of(dr );
		
	}*/
}
