// $Id: Station.java 48 2006-07-12 16:15:40Z caron $
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

package thredds.catalog.query;

/**
 * Implementation of a DQC  station element. This extends Choice with a location.
 *
 * @author john caron
 */

public class Station extends ListChoice {
  private Location location;
  private String state = null, country = null;

   /**
    * Construct from fields in XML catalog.
    * @see Choice
    */
  public Station( Selector parent, String name, String value, String description) {
    super( parent, name, value, description);
  }

  public Station( Selector parent, String name, String value, String state, String country, String description) {
    super( parent, name, value, description);
    this.state = state;
    this.country = country;
  }

  public void setLocation(Location location) { this.location = location; }
  public Location getLocation() { return location; }

  public double getLatitude() { return location.getLatitude(); }

  public double getLongitude() { return location.getLongitude(); }

  public double getElevation() {
      if(location.hasElevation())
        return location.getElevation();
      else
        return Double.NaN;
  }
    
  public String getStationName() {
    return getName();
  }

  public String getStationID() {
      return getValue();
  }

  public String getState() {
      return state;
  }

  public String getCountry() {
      return country;
  }

  public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof Station )) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      result = 37*result + getValue().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (getDescription() != null)
        result = 37*result + getDescription().hashCode();

      result = 37*result + getLocation().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}