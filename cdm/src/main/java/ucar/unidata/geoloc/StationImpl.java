/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import javax.annotation.Nonnull;

/**
 * Implementation of Station
 * @author caron
 */
public class StationImpl extends EarthLocationImpl implements Station {
  protected String name, desc, wmoId;
  protected int nobs = -1;

  protected StationImpl() {}

  public StationImpl( String name, String desc, String wmoId, double lat, double lon, double alt) {
    super(lat, lon, alt);
    setName(name);
    setDescription(desc);
    setWmoId(wmoId);
  }

  public StationImpl( String name, String desc, String wmoId, double lat, double lon, double alt, int nobs) {
    super(lat, lon, alt);
    setName(name);
    setDescription(desc);
    setWmoId(wmoId);
    setNobs( nobs);
  }

  public StationImpl( Station s, int nobs) {
    super(s.getLatitude(), s.getLongitude(), s.getAltitude());
    setName(s.getName());
    setDescription(s.getDescription());
    setWmoId(s.getWmoId());
    setNobs( nobs);
  }

  /**
   * Station name or id. Must be unique within the collection
   * @return station name or id. May not be null.
   */
  @Nonnull
  public String getName() { return name; }

  /**
   * Station description
   * @return station description
   */
  public String getDescription() { return desc; }

  /**
   * WMO station id
   * @return WMO station id, or null
   */
  public String getWmoId() { return wmoId; }

  public int getNobs() { return nobs; }

  /////

  protected void setName(String name) { this.name = name.trim(); }
  protected void setDescription(String desc) { this.desc = desc != null ? desc.trim() : null; }
  protected void setWmoId(String wmoId) { this.wmoId = wmoId != null ? wmoId.trim() : null; }
  protected void setNobs(int nobs) { this.nobs = nobs; }

  public void incrNobs() {
    this.nobs++;
  }

  public int compareTo(Station so) {
    return name.compareTo( so.getName());
  }

  public String toString() {
    return "name="+name+" desc="+desc+" "+super.toString();
  }

}
