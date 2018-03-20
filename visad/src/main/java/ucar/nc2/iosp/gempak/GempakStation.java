/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;


/**
 * Class to hold GEMPAK station information
 *
 * @author Don Murray
 */
public class GempakStation implements Station {

  /**
   * STID identifier
   */
  public static final String STID = "STID";

  /**
   * STNM identifier
   */
  public static final String STNM = "STNM";

  /**
   * SLAT identifier
   */
  public static final String SLAT = "SLAT";

  /**
   * SLON identifier
   */
  public static final String SLON = "SLON";

  /**
   * SELV identifier
   */
  public static final String SELV = "SELV";

  /**
   * STAT identifier
   */
  public static final String STAT = "STAT";

  /**
   * COUN identifier
   */
  public static final String COUN = "COUN";

  /**
   * STD2 identifier
   */
  public static final String STD2 = "STD2";

  /**
   * SPRI identifier
   */
  public static final String SPRI = "SPRI";

  /**
   * SWFO identifier
   */
  public static final String SWFO = "SWFO";

  /**
   * WFO2 identifier
   */
  public static final String WFO2 = "WFO2";

  /**
   * station id
   */
  private String stid = "";

  /**
   * station id2
   */
  private String std2 = "";

  /**
   * station number
   */
  private int stnm = GempakConstants.IMISSD;

  /**
   * state
   */
  private String stat = "";

  /**
   * country
   */
  private String coun = "";

  /**
   * wfo id
   */
  private String swfo = "";

  /**
   * second wfo id
   */
  private String wfo2 = "";

  /**
   * station latitude
   */
  private int slat = GempakConstants.IMISSD;

  /**
   * station longitude
   */
  private int slon = GempakConstants.IMISSD;

  /**
   * station elevation
   */
  private int selv = GempakConstants.IMISSD;

  /**
   * priority
   */
  private int spri;

  /**
   * row or column index
   */
  private int index;

  /**
   * Station description
   */
  private String sdesc = "";

  /**
   * Create a new GEMPAK station
   */
  public GempakStation() {
  }

  /**
   * Return a String representation of this
   *
   * @return a String representation of this
   */
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(StringUtil2.padRight((stid.trim() + std2.trim()), 8));
    builder.append(" ");
    builder.append(Format.i(stnm, 6));
    builder.append(" ");
    builder.append(StringUtil2.padRight(sdesc, 32));
    builder.append(" ");
    builder.append(StringUtil2.padLeft(stat.trim(), 2));
    builder.append(" ");
    builder.append(StringUtil2.padLeft(coun.trim(), 2));
    builder.append(" ");
    builder.append(Format.i(slat, 5));
    builder.append(" ");
    builder.append(Format.i(slon, 6));
    builder.append(" ");
    builder.append(Format.i(selv, 5));
    builder.append(" ");
    builder.append(Format.i(spri, 2));
    builder.append(" ");
    builder.append(StringUtil2.padLeft(swfo.trim(), 3));
    return builder.toString();
  }

  public int getNobs() {
    return -1;
  }

  /**
   * Set the STID
   *
   * @param value new value
   */
  public void setSTID(String value) {
    stid = value;
  }

  /**
   * Get the STID
   *
   * @return the STID value
   */
  public String getSTID() {
    return stid;
  }

  /**
   * Set the STNM
   *
   * @param value new value
   */
  public void setSTNM(int value) {
    stnm = value;
  }

  /**
   * Get the STNM
   *
   * @return the STNM value
   */
  public int getSTNM() {
    return stnm;
  }

  /**
   * Set the STAT
   *
   * @param value new value
   */
  public void setSTAT(String value) {
    stat = value;
  }

  /**
   * Get the STAT
   *
   * @return the STAT value
   */
  public String getSTAT() {
    return stat;
  }

  /**
   * Set the COUN
   *
   * @param value new value
   */
  public void setCOUN(String value) {
    coun = value;
  }

  /**
   * Get the COUN
   *
   * @return the COUN value
   */
  public String getCOUN() {
    return coun;
  }

  /**
   * Set the STD2
   *
   * @param value new value
   */
  public void setSTD2(String value) {
    std2 = value;
  }

  /**
   * Get the STD2
   *
   * @return the STD2 value
   */
  public String getSTD2() {
    return std2;
  }

  /**
   * Set the SWFO
   *
   * @param value new value
   */
  public void setSWFO(String value) {
    swfo = value;
  }

  /**
   * Get the SWFO
   *
   * @return the SWFO value
   */
  public String getSWFO() {
    return swfo;
  }

  /**
   * Set the WFO2
   *
   * @param value new value
   */
  public void setWFO2(String value) {
    wfo2 = value;
  }

  /**
   * Get the WFO2
   *
   * @return the WFO2 value
   */
  public String getWFO2() {
    return wfo2;
  }

  /**
   * Set the SLAT
   *
   * @param value new value
   */
  public void setSLAT(int value) {
    slat = value;
  }

  /**
   * Get the SLAT
   *
   * @return the SLAT value
   */
  public int getSLAT() {
    return slat;
  }

  /**
   * Set the SLON
   *
   * @param value new value
   */
  public void setSLON(int value) {
    slon = value;
  }

  /**
   * Get the SLON
   *
   * @return the SLON value
   */
  public int getSLON() {
    return slon;
  }

  /**
   * Set the SELV
   *
   * @param value new value
   */
  public void setSELV(int value) {
    selv = value;
  }

  /**
   * Get the SELV
   *
   * @return the SELV value
   */
  public int getSELV() {
    return selv;
  }

  /**
   * Set the SPRI
   *
   * @param value new value
   */
  public void setSPRI(int value) {
    spri = value;
  }

  /**
   * Get the SPRI
   *
   * @return the SPRI value
   */
  public int getSPRI() {
    return spri;
  }

  // Station interface stuff  have this class extend when we
  // decide where the interface lives

  /**
   * Get the latitude in decimal degrees north
   *
   * @return the latitude
   */
  public double getLatitude() {
    if (slat == GempakConstants.IMISSD) {
      return slat;
    }
    return slat / 100.;
  }

  /**
   * Get the longitude in decimal degrees east
   *
   * @return the longitude
   */
  public double getLongitude() {
    if (slon == GempakConstants.IMISSD) {
      return slon;
    }
    return slon / 100.;
  }

  /**
   * Get the altitude in meters;  missing = NaN.
   *
   * @return the altitude
   */
  public double getAltitude() {
    return selv;
  }

  /**
   * Get the Station name. Must be unique within the collection
   *
   * @return the station id
   */
  public String getName() {
    return stid.trim() + std2.trim();
  }

  /**
   * Get the Station description
   *
   * @return station description
   */
  public String getDescription() {
    return sdesc;
  }

  /**
   * Set the station description
   *
   * @param desc the description
   */
  public void setDescription(String desc) {
    sdesc = desc;
  }

  /**
   * Get the WMO Station ID as a string
   *
   * @return the WMO id
   */
  public String getWmoId() {
    String wmoID = "";
    if (!(stnm == GempakConstants.IMISSD)) {
      wmoID = String.valueOf((int) (stnm / 10));
    }
    return wmoID;
  }

  /**
   * Compare this to another
   *
   * @param o other object
   * @return comparison on nam
   */
  public int compareTo(Station o) {
    return getName().compareTo(o.getName());
  }

  /**
   * Are either lat or lon missing?
   *
   * @return true if lat or lon is missing
   */
  public boolean isMissing() {
    return ((slat == GempakConstants.IMISSD)
            || (slon == GempakConstants.IMISSD));
  }

  /**
   * Get the lat/lon location
   *
   * @return lat/lon location
   */
  public LatLonPoint getLatLon() {
    return new LatLonPointImpl(getLatitude(), getLongitude());
  }

  /**
   * Set the row or column index for this station
   *
   * @param rowOrCol the index
   */
  public void setIndex(int rowOrCol) {
    index = rowOrCol;
  }

  /**
   * Get the row or column index for this station
   *
   * @return the index
   */
  public int getIndex() {
    return index;
  }
}

