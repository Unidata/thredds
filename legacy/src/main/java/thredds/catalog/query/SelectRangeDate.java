/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: SelectRangeDate.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

/**
 * Implementation of a DQC select range date.
 *
 * @author john caron
 */

public class SelectRangeDate extends Selector {
  private String start, end, duration, resolution, selectType;

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectRangeDate( String start, String end, String duration,
                      String resolution, String selectType) {
    this.start = start;
    this.end = end;
    this.duration = duration;
    this.resolution = resolution;
    this.selectType = selectType;
  }

  public String getStart() { return start; }
  public String getEnd() { return end; }
  public String getDuration() { return duration; }
  public String getResolution() { return resolution; }
  public String getSelectType() { return selectType; }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectRangeDate)) return false;
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

      if (getStart() != null)
        result = 37*result + getStart().hashCode();
      if (getEnd() != null)
        result = 37*result + getEnd().hashCode();
      if (getDuration() != null)
        result = 37*result + getDuration().hashCode();
      if (getResolution() != null)
        result = 37*result + getResolution().hashCode();
      if (getSelectType() != null)
        result = 37*result + getSelectType().hashCode();

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}
