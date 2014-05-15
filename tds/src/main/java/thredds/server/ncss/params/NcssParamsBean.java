package thredds.server.ncss.params;

import java.text.ParseException;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.validation.NcssRequestConstraint;
import thredds.server.ncss.validation.TimeParamsConstraint;
import thredds.server.ncss.validation.VarParamConstraint;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Ncss Parameters
 *
 * @author caron
 * @since 10/5/13
 */

@TimeParamsConstraint
@NcssRequestConstraint
public class NcssParamsBean {

  private String accept;

  @VarParamConstraint
 	private List<String> var;

 	@DateTimeFormat
 	private String time_start;

  @DateTimeFormat
 	private String time_end;

  @DateTimeFormat
 	private String time_duration;

  @DateTimeFormat
 	private String time_window;  // time_window is meant to be used with time=present. When time=present it returns the closest time to current in the dataset  
  								 // but if the dataset does not have up to date data that could be really far from the current time and most
  								 // likely useless (in particular for observation data).
  								 // time_window tells the server give me the data if it's within this period otherwise don't bother.  								 
  							     // time_window must be a valid W3C time duration.			
  @DateTimeFormat
 	private String time;

  private String temporal;  // == all

 	private Double north;

 	private Double south;

 	private Double east;

 	private Double west;

  private Double latitude;

 	private Double longitude;


  //// grid only

  private Double minx;

 	private Double maxx;

 	private Double miny;

 	private Double maxy;

 	private boolean addLatLon;

 	private Integer horizStride = 1;

 	private Integer timeStride = 1;

  private Double vertCoord;

 	private Integer vertStride=1;

  //// station only
	private List<String> stns;

  /////////////////////////////////////////////////////

  public String getAccept() {
    return accept;
  }

  public void setAccept(String accept) {
    this.accept = accept;
  }

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

  public String getTime_window() {
    return time_window;
  }

  public void setTime_window(String time_window) {
    this.time_window = time_window;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
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

  public List<String> getStns() {
    return stns;
  }

  public void setStns(List<String> stns) {
    this.stns = stns;
  }

  public boolean hasLatLonPoint() {
    return latitude != null && longitude != null;
  }

  public boolean hasLatLonBB() { // need to validate
    return east != null && west != null && north != null && south != null;
  }

  public boolean hasProjectionBB() { // need to validate
    return minx != null && miny != null && maxx != null && maxy != null;
  }

  public boolean hasStations() {
    return stns != null && !stns.isEmpty();
  }

  public String getTemporal() {
    return temporal;
  }

  public void setTemporal(String temporal) {
    this.temporal = temporal;
  }

  public boolean isAllTimes() {
    return temporal != null && temporal.equalsIgnoreCase("all");
  }

  public TimeDuration parseTimeDuration() throws NcssException {
    if (getTime_duration() == null) return null;
    try {
      return TimeDuration.parseW3CDuration(getTime_duration());
    } catch (ParseException e) {
      throw new NcssException("invalid time duration");
    }
  }

  public LatLonRect getBB(){
    double width = getEast() - getWest();
    double height = getNorth() - getSouth();
		return new LatLonRect(new LatLonPointImpl(getSouth(), getWest()), height, width);
	}

  private boolean hasValidTime;
  private boolean hasValidDateRange;

  public void setHasValidTime(boolean hasValidTime) {
    this.hasValidTime = hasValidTime;
  }

  public void setHasValidDateRange(boolean hasValidDateRange) {
    this.hasValidDateRange = hasValidDateRange;
  }

  public CalendarDateRange getCalendarDateRange(Calendar cal) throws ParseException {
    if (!hasValidDateRange) return null;

		DateRange dr;
		if (time == null)
			dr = new DateRange( new DateType(time_start, null, null, cal), new DateType(time_end, null, null, cal), new TimeDuration(time_duration), null );
		else{
			//DateType dtDate = new DateType(time, null, null, cal);
			dr = new DateRange( new DateType(time, null, null, cal), new DateType(time, null, null, cal), new TimeDuration(time_duration), null );
		}

		//return CalendarDateRange.of(dr );
		return CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate() ); 
	}

  public CalendarDate getRequestedTime( Calendar cal ) throws ParseException {
    if (!hasValidTime) return null;

 			CalendarDate date=null;
 			if( getTime().equalsIgnoreCase("present") ){
 				 java.util.Calendar c = java.util.Calendar.getInstance();
 				 c.setTime( new Date()  );
 				 return CalendarDate.of( cal, c.getTimeInMillis()  );
 			}

    // default calendar (!)
    return CalendarDateFormatter.isoStringToCalendarDate(cal, getTime());
 	}

  public boolean isValidGridRequest() {
    return true;
  }

  public boolean intersectsTime(FeatureDataset fd, Formatter errs) throws ParseException {
    CalendarDateRange have =  fd.getCalendarDateRange();
    if (have == null) return true;
    Calendar dataCal = have.getStart().getCalendar(); // use the same calendar as the dataset

    CalendarDateRange want = getCalendarDateRange(dataCal);
    if (want != null) {
      if (have.intersects(want)) {
        return true;
      } else {
        errs.format("Requested time range %s does not intersect actual time range %s", want, have);
        return false;
      }
    }

    CalendarDate wantTime = getRequestedTime(dataCal);
    if (wantTime == null) return true;
    if (!have.includes(wantTime)) {
      errs.format("Requested time %s does not intersect actual time range %s", wantTime, have);
      return false;
    }
    return true;
  }


}
