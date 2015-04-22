// $Id: Location.java 48 2006-07-12 16:15:40Z caron $
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

package thredds.catalog.query;


/**
 * Implementation of a DQC location element.
 *
 * @author john caron
 */

public class Location {
  private double latitude, longitude, elevation;
  private String latitude_units, longitude_units, elevation_units;
  private boolean hasElevation = false;

   /**
    * Construct from fields in XML catalog.
    *
    * @param latitude : the latitude of the location.
    * @param longitude : the latitude of the location.
    * @param elevation : the elevation of the location (optional)
    * @param latitude_units : the units of latitude (optional, default degrees_north).
    * @param longitude_units : the units of longitude (optional, default degrees_east).
    * @param elevation_units : the units of elevation (optional, default meters).
    */
  public Location( String latitude, String longitude, String elevation,
    String latitude_units, String longitude_units, String elevation_units) {

    try {
      this.latitude = Double.parseDouble(latitude);
      this.longitude = Double.parseDouble(longitude);
      if (elevation != null) {
        this.elevation = Double.parseDouble(elevation);
        this.hasElevation = true;
      }
    } catch (NumberFormatException e) {
    }

    this.latitude_units = latitude_units; // (latitude_units == null) ? "degreesNorth" : latitude_units.intern();
    this.longitude_units = longitude_units; // (longitude_units == null) ? "degreesEast" : longitude_units.intern();
    this.elevation_units = elevation_units; // (elevation_units == null) ? "" : elevation_units.intern();
  }

  public double getLatitude() { return latitude; }
  public double getLongitude() { return longitude; }
  public boolean hasElevation() { return hasElevation; }
  public double getElevation() { return elevation; }
  public String getLatitudeUnits() { return latitude_units; }
  public boolean isDefaultLatitudeUnits() {
    return (latitude_units == null) || latitude_units.equals("degrees_north");
  }
  public String getLongitudeUnits() { return longitude_units; }
  public boolean isDefaultLongitudeUnits() {
    return (longitude_units == null) || longitude_units.equals("degrees_east");
  }
  public String getElevationUnits() { return elevation_units; }
  public boolean isDefaultElevationUnits() {
    return (elevation_units == null) || elevation_units.equals("msl");
  }

  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("lat=");
    sbuff.append(latitude); // should format nicer
    sbuff.append(" lon=");
    sbuff.append(longitude);
    if (hasElevation) {
      sbuff.append(" elev=");
      sbuff.append(elevation);
      sbuff.append(elevation_units);
    }
    return sbuff.toString();
  }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof Location)) return false;
     return o.hashCode() == this.hashCode();
  }
  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + (int) ( 1000.0 * getLatitude()) ;
      result = 37*result + (int) ( 1000.0 * getLongitude()) ;
      if (hasElevation())
        result = 37*result + (int) ( 1000.0 * getElevation()) ;
      if (getLatitudeUnits() != null)
        result = 37*result + getLatitudeUnits().hashCode();
      if (getLongitudeUnits() != null)
        result = 37*result + getLongitudeUnits().hashCode();
      if (getElevationUnits() != null)
        result = 37*result + getElevationUnits().hashCode();

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8


}
