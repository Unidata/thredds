/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: SelectStation.java 48 2006-07-12 16:15:40Z caron $


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
