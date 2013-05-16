/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.cdmremote.params;

import java.util.Formatter;

import org.springframework.http.MediaType;

import thredds.server.cdmremote.validation.CdmrfQueryConstraint;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Parses the query parameters for cdmRemote datasets.
 * This is the Model in Spring MVC
 *
 * @author caron
 * @since May 11, 2009
 */
@CdmrfQueryConstraint
public class CdmrfQueryBean {

  public enum RequestType {
    capabilities, cdl, data, dataForm, form, header, ncml, stations
  }

  public enum ResponseType {
    csv, netcdf, ncstream, html, xml
  }

  public enum SpatialSelection {
    all, bb, point, stns
  }

  public enum TemporalSelection {
    all, range, point
  }

  // type of request
  private String req = "";

  // type of response
  private String accept = "";

  // comma delimited list of variable names
  private String variables; // (forms) all some
  private String var;

  //// spatial selection
  private String spatial; // (forms) all, bb, point, stns
  private TemporalSelection temporalSelection;

  // comma delimited list of station ids
  private String stn;

  // spatial extent - one or none
  private String bbox;
  private String west, east, south, north;
  private String latitude, longitude;

  //// time selection
  private String temporal; // (forms) all, range, point
  private SpatialSelection spatialSelection;

  // time range
  private String time_start, time_end, time_duration;
  private String time;

  // parsed quantities
  private DateRange dateRange;
  private DateType timePoint;
  private LatLonRect llbb;
  private LatLonPoint latlonPoint;
  private RequestType reqType = null;
  private ResponseType resType = null;

  private boolean fatal = false;
  private Formatter errs = new Formatter();


  boolean hasFatalError() {
    return fatal;
  }

  String getErrorMessage() {
    return errs.toString();
  }

  public LatLonRect getLatLonRect() {
    return (spatialSelection == SpatialSelection.bb) ? llbb : null;
  }

  public void setDateRange(DateRange dateRange){
	  this.dateRange = dateRange;
  }
  
  public DateRange getDateRange() {
    return dateRange;
  }

  public LatLonPoint getLatlonPoint() {
    return latlonPoint;
  }

  public DateType getTimePoint() {
    return timePoint;
  }

  public RequestType getRequestType() {
    if (reqType == null) {
      if (req.equalsIgnoreCase("capabilities")) reqType = RequestType.capabilities;
      else if (req.equalsIgnoreCase("cdl")) reqType = RequestType.cdl;
      else if (req.equalsIgnoreCase("data")) reqType = RequestType.data;
      else if (req.equalsIgnoreCase("dataForm")) reqType = RequestType.dataForm;
      else if (req.equalsIgnoreCase("form")) reqType = RequestType.form;
      else if (req.equalsIgnoreCase("header")) reqType = RequestType.header;
      else if (req.equalsIgnoreCase("ncml")) reqType = RequestType.ncml;
      else if (req.equalsIgnoreCase("stations")) reqType = RequestType.stations;
      else reqType = RequestType.data; // default
    }
    return reqType;
  }

  public ResponseType getResponseType() {
    if (resType == null) {
      RequestType req = getRequestType();
      if (req == RequestType.capabilities) resType = ResponseType.xml;
      else if (req == RequestType.form) resType = ResponseType.html;
    }

    if (resType == null) {
      if (accept.equalsIgnoreCase("csv")) resType = ResponseType.csv;
      else if (accept.equalsIgnoreCase("ncstream")) resType = ResponseType.ncstream;
      else if (accept.equalsIgnoreCase("netcdf")) resType = ResponseType.netcdf;
      else if (accept.equalsIgnoreCase("xml")) resType = ResponseType.xml;
      else resType = ResponseType.ncstream; // default
    }

    return resType;
  }
  
  public MediaType getMediaType(){
	  
	  if(resType == ResponseType.xml || accept.equals("xml") || reqType == RequestType.capabilities)
		  return MediaType.TEXT_XML;
	  if(resType == ResponseType.html || reqType == RequestType.form)
		  return MediaType.TEXT_HTML;	  
	  if(resType == ResponseType.csv)
		  return MediaType.TEXT_PLAIN;	  	  	  
	  if(resType == ResponseType.netcdf )
		  return new MediaType("application", "x-netcdf"  );	  
	  if(resType == ResponseType.ncstream )//???
		  return new MediaType("application", "x-netcdf"  );	  
	  
	  
	  //Default...
	  return MediaType.TEXT_HTML;
  }

  public SpatialSelection getSpatialSelection() {
    return spatialSelection;
  }
  
  public void setSpatialSelection(SpatialSelection spatialSelection){
	  this.spatialSelection = spatialSelection;
  }


  public TemporalSelection getTemporalSelection() {
    return temporalSelection;
  }
  
  public void setTemporalSelection(TemporalSelection temporalSelecion){
	  this.temporalSelection = temporalSelecion;
  }



  public double parseLat(String key, String value) {
    double lat = parseDouble(key, value);
    if (!Double.isNaN(lat)) {
      if ((lat > 90.0) || (lat < -90.0)) {
        errs.format("Illegal param= param='%s=%s' must be between +/- 90.0 %n", key, value);
        lat = Double.NaN;
        fatal = true;
      }
    }
    return lat;
  }

  public double parseLon(String key, String value) {
    return parseDouble(key, value);
  }

  private double parseDouble(String key, String value) {
    value = value.trim();
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      errs.format("Illegal param='%s=%s' must be valid floating point number%n", key, value);
      fatal = true;
    }
    return Double.NaN;
  }

  ////////////////////////////////////////


  public DateType parseDate(String key, String value) {
    if (value != null) {
      try {
        return new DateType(value, null, null);
      } catch (java.text.ParseException e) {
        errs.format("Illegal param='%s=%s'  must be valid ISO Date%n", key, value);
        fatal = true;
      }
    }
    return null;
  }

  public TimeDuration parseW3CDuration(String key, String value) {
    if (value != null) {
      try {
        return new TimeDuration(value);
      } catch (java.text.ParseException e) {
        errs.format("Illegal param='%s=%s'  must be valid ISO Duration%n", key, value);
        fatal = true;
      }
    }
    return null;
  }

  /////////////////////////////////////

  public void setAccept(String accept) {
    this.accept = accept;
  }

  public void setReq(String req) {
    this.req = req;
  }

  // variable names
  public void setVariables(String variables) {
    this.variables = variables;
  }
  
  public String getVariables(){
	  return variables;
  }

  public void setVar(String var) {
    this.var = var;
  }

  public String getVar() {
    return var;
  }

  public String[] getVarNames() {
    return (var == null) ? null : var.split(",");
  }

  //////// spatial

  public void setSpatial(String spatial) {
    this.spatial = spatial;
  }
  
  public String getSpatial(){
	  return spatial;
  }
  
  

  public void setStn(String stn) {
    this.stn = stn;
  }

  public String getStn() {
    return stn;
  }

  public String[] getStnNames() {
    if (!(spatialSelection == SpatialSelection.stns)) return null;
    return (stn == null) ? null : stn.split(",");
  }

  public void setBbox(String bbox) {
    this.bbox = bbox;
  }
  
  public String getBbox(){
	  return bbox;
  }

  public void setWest(String west) {
    this.west = west;
  }
  
  public String getWest(){
	  return west;
  }

  public void setEast(String east) {
    this.east = east;
  }
  
  public String getEast(){
	  return east;
  }

  public void setSouth(String south) {
    this.south = south;
  }
  
  public String getSouth(){
	  return south;
  } 

  public void setNorth(String north) {
    this.north = north;
  }
  
  public String getNorth(){
	  return north;
  }

  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }
  
  public String getLatitude(){
	  return latitude;
  }

  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }
  
  public String getLongitude(){
	  return longitude;
  }

  //////// temporal
  public void setTemporal(String temporal) {
    this.temporal = temporal;
  }
  
  public String getTemporal(){
	  return temporal;
  }

  public void setTime_start(String timeStart) {
    this.time_start = timeStart;
  }
  
  public String getTime_start(){
	  return time_start;
  }

  public void setTime_end(String timeEnd) {
    this.time_end = timeEnd;
  }
  
  public String getTime_end(){
	  return time_end;
  }

  public void setTime_duration(String timeDuration) {
    this.time_duration = timeDuration;
  }

  public String getTime_duration(){
	  return time_duration;
  }
  
  public void setTime(String time) {
    this.time = time;
  }
  
  public String getTime(){
	  return time;
  }
  
  public void setTimepoint(DateType type){
	  this.timePoint= type;
  }
  
  public void setLLBB(LatLonRect llbb){
	  this.llbb = llbb;
  }
  
  public void setLatLonPoint(LatLonPoint point){
	  this.latlonPoint = point;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("QueryBean: reqType=%s resType=%s", getRequestType(), getResponseType());

    if (spatialSelection == SpatialSelection.all)
      f.format(" spatialSelection=all;");
    else if (spatialSelection == SpatialSelection.bb)
      f.format(" bb=%s;", getLatLonRect());
    else if (spatialSelection == SpatialSelection.stns)
      f.format(" stns=%s;", getStn());

    if (temporalSelection == TemporalSelection.all)
      f.format(" temporalSelection=all;");
    else if (temporalSelection == TemporalSelection.range)
      f.format(" range=%s;", getDateRange());

    if (var != null)
      f.format(" vars=%s", var);

    return f.toString();
  }
}
