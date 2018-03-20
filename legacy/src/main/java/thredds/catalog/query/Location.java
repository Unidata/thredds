/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: Location.java 48 2006-07-12 16:15:40Z caron $


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
