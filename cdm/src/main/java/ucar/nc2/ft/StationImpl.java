/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.ft;

/**
 * Implementation of Station
 * @author caron
 */
public class StationImpl extends EarthLocationImpl implements Station {
  protected String name, desc, wmoId;

  public StationImpl( String name, String desc, double lat, double lon, double alt) {
    super( lat, lon, alt);
    this.name = name;
    this.desc = desc;
  }

  /**
   * Station name or id. Must be unique within the collection
   * @return station name or id. May not be null.
   */
  public String getName() { return name; }

  /**
   * Station description
   * @return station description
   */
  public String getDescription() { return desc; }

  public String getWmoId() { return wmoId; }

  /////

  protected void setName(String name) { this.name = name; }
  protected void setDescription(String desc) { this.desc = desc; }
  protected void setWmoId(String wmoId) { this.wmoId = wmoId; }

  public int compareTo(Object o) {
    StationImpl so = (StationImpl) o;
    return name.compareTo( so.getName());
  }

  public String toString() {
    return "name="+name+" desc="+desc+" "+super.toString();
  }

}
