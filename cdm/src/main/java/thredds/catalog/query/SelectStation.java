// $Id: SelectStation.java 48 2006-07-12 16:15:40Z caron $
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

import java.util.*;

/**
 * Implementation of a DQC station selector element.
 * A SelectService contains a list of Objects of type Station.
 *
 * @author john caron
 */

public class SelectStation extends Selector {
  private ArrayList stations = new ArrayList();

  public SelectStation() {
    super();
  }

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectStation( String label, String id, String template, String selectType, String required) {
    super( label, id, template, selectType, required);
  }

  public void addStation( Station s) { stations.add(s); }
  public ArrayList getStations() { return stations; }
  public int getNumStations() { return stations.size(); }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectStation)) return false;
     return o.hashCode() == this.hashCode();
  }
  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (getTitle() != null)
        result = 37*result + getTitle().hashCode();
      if (getId() != null)
        result = 37*result + getId().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (isRequired()) result++;
      if (isMultiple()) result++;

      result = 37*result + getStations().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8


}
