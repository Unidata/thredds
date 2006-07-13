// $Id: StationImpl.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Implementation of Station
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */

public class StationImpl extends EarthLocationImpl implements Station, Comparable {
  protected String name, desc, wmoId;
  protected ArrayList obsList;
  protected int count = -1;

  public StationImpl() {
  }

  public StationImpl( String name, String desc, double lat, double lon, double alt) {
    super( lat, lon, alt);
    this.name = name;
    this.desc = desc;
  }

  public StationImpl( String name, String desc, double lat, double lon, double alt, int count) {
    this( name, desc, lat, lon, alt);
    this.count = count;
  }

  public String getName() { return name; }
  public String getDescription() { return desc; }
  public String getWmoId() { return wmoId; }
  public int getNumObservations() { return (obsList == null) ? count : obsList.size(); }

  /////

  public void setName(String name) { this.name = name; }
  public void setDescription(String desc) { this.desc = desc; }
  public void setWmoId(String wmoId) { this.wmoId = wmoId; }
  public void incrCount() { count++; }

  public List getObservations() throws IOException {
    if (obsList == null)
        obsList = readObservations();
    return obsList;
  }

  // got to use this or subclass readObservations()
  public void addObs( StationObsDatatype sobs) {
    if (null == obsList) obsList = new ArrayList();
    obsList.add( sobs);
  }

  protected ArrayList readObservations()  throws IOException { return null; }

  public int compareTo(Object o) {
    StationImpl so = (StationImpl) o;
    return name.compareTo( so.getName());
  }
}