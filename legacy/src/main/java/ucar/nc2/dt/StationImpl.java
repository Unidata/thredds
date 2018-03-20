/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import ucar.unidata.geoloc.Station;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Implementation of Station
 *
 * @author caron
 * @deprecated use ucar.unidata.geoloc.StationImpl
 */

public class StationImpl extends ucar.unidata.geoloc.StationImpl {
  protected List<StationObsDatatype> obsList;
  protected int count = -1;

  public StationImpl() {
  }

  public StationImpl( String name, String desc, double lat, double lon, double alt) {
    super( name, desc, "", lat, lon, alt);
  }

  public StationImpl( String name, String desc, double lat, double lon, double alt, int count) {
    this( name, desc, lat, lon, alt);
    this.count = count;
  }

  public int getNumObservations() { return (obsList == null) ? count : obsList.size(); }

  /////

  public void incrCount() { count++; }

  public List getObservations() throws IOException {
    if (obsList == null)
        obsList = readObservations();
    return obsList;
  }

  // got to use this or subclass readObservations()
  public void addObs( StationObsDatatype sobs) {
    if (null == obsList) obsList = new ArrayList<StationObsDatatype>();
    obsList.add( sobs);
  }

  protected List<StationObsDatatype> readObservations()  throws IOException { return null; }

}