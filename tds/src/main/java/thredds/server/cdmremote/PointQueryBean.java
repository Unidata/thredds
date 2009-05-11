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
package thredds.server.cdmremote;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Formatter;

/**
 * Class Description
 *
 * @author caron
 * @since May 11, 2009
 */
public class PointQueryBean {

  // comma delimited list of variable names
  private String vars;

  // spatial extent - one or none
  private String bbox;
  private String west, east, south, north;
  private String latitude, longitude;

  // time range
  private String timeStart, timeEnd, timeDuration;
  private String time;

  // type of request
  private String header; // ??
  private String iterator;

  private LatLonRect llbb;
  private boolean fatal = false;
  private Formatter errs = new Formatter();


  boolean hasFatalError() {
    return fatal;
  }

  String getErrorMessage() {
    return errs.toString();
  }

  LatLonRect getLatLonRect() {
    return llbb;
  }

  boolean parse() {
    parseSpatialExtent();
    return !fatal;
  }

  boolean wantHeader() {
    return (header != null);
  }

  private void parseSpatialExtent() {
    if (bbox != null) {
      String[] s = bbox.split(",");
      if (s.length != 4) {
        errs.format("bbox must have form 'bbox=west,east,south,north'; found 'bbox=%s'%n", bbox);
        fatal = true;
        return;
      }
      west = s[0];
      east = s[1];
      south = s[2];
      north = s[3];
    }

    if ((west != null) || (east != null) || (south != null) || (north != null)) {
      if ((west == null) || (east == null) || (south == null) || (north == null)) {
        errs.format("All edges (west,east,south,north) must be specified; found west=%s east=%s south=%s north=%s %n", west, east, south, north);
        fatal = true;
        return;
      }
      double westd = parseLon("west", west);
      double eastd = parseLon("east", east);
      double southd = parseLat("south", south);
      double northd = parseLat("north", north);

      if (!fatal) {
        llbb = new LatLonRect(new LatLonPointImpl(southd, westd), new LatLonPointImpl(northd, eastd));
      }
    }
  }

  private double parseLat(String key, String value) {
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

  private double parseLon(String key, String value) {
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

  /////////////////////////////////////

  public void setVars(String vars) {
    this.vars = vars;
  }

  public void setBbox(String bbox) {
    this.bbox = bbox;
  }

  public void setWest(String west) {
    this.west = west;
  }

  public void setEast(String east) {
    this.east = east;
  }

  public void setSouth(String south) {
    this.south = south;
  }

  public void setNorth(String north) {
    this.north = north;
  }

  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  public void setTimeStart(String timeStart) {
    this.timeStart = timeStart;
  }

  public void setTimeEnd(String timeEnd) {
    this.timeEnd = timeEnd;
  }

  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public void setIterator(String iterator) {
    this.iterator = iterator;
  }

  @Override
  public String toString() {
    return "PointQueryBean{" +
        "vars='" + vars + '\'' +
        ", bbox='" + bbox + '\'' +
        ", west='" + west + '\'' +
        ", east='" + east + '\'' +
        ", south='" + south + '\'' +
        ", north='" + north + '\'' +
        ", latitude='" + latitude + '\'' +
        ", longitude='" + longitude + '\'' +
        ", timeStart='" + timeStart + '\'' +
        ", timeEnd='" + timeEnd + '\'' +
        ", timeDuration='" + timeDuration + '\'' +
        ", time='" + time + '\'' +
        ", header='" + header + '\'' +
        ", iterator='" + iterator + '\'' +
        '}';
  }
}
