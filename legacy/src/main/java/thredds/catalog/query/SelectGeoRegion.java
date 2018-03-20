/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: SelectGeoRegion.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

/**
 * Implementation of a DQC select geo region.
 *
 * @author john caron
 */

public class SelectGeoRegion extends Selector {
  private Location lowerLeft, upperRight;

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectGeoRegion( Location lowerLeft, Location upperRight) {
    super();

    this.lowerLeft = lowerLeft;
    this.upperRight = upperRight;
  }

  public Location getLowerLeft() { return lowerLeft; }
  public Location getUpperRight() { return upperRight; }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectGeoRegion)) return false;
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

      if (getLowerLeft() != null)
        result = 37*result + getLowerLeft().hashCode();
      if (getUpperRight() != null)
        result = 37*result + getUpperRight().hashCode();

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}