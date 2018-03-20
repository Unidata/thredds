/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: SelectRange.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

/**
 * Implementation of a DQC select range.
 *
 * @author john caron
 */

public class SelectRange extends Selector {
  private String min, max, units, resolution, selectType;
  private boolean modulo;

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectRange( String min, String max, String units, String modulo,
                      String resolution, String selectType) {
    this.min = min;
    this.max = max;
    this.units = units;
    this.modulo = (modulo != null) && modulo.equals("true");
    this.resolution = resolution;
    this.selectType = selectType;
  }

  public String getMin() { return min; }
  public String getMax() { return max; }
  public String getUnits() { return units; }
  public boolean isModulo() { return modulo; }
  public String getResolution() { return resolution; }
  public String getSelectType() { return selectType; }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectRange)) return false;
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

      if (getMin() != null)
        result = 37*result + getMin().hashCode();
      if (getMax() != null)
        result = 37*result + getMax().hashCode();
      if (getUnits() != null)
        result = 37*result + getUnits().hashCode();
      if (getResolution() != null)
        result = 37*result + getResolution().hashCode();
      if (getSelectType() != null)
        result = 37*result + getSelectType().hashCode();
      if (isModulo()) result++;

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}
