/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.gempak;

import ucar.nc2.iosp.grid.GridParameter;
import ucar.unidata.util.StringUtil2;


/**
 * Class which represents a GEMPAK parameter.  Add on decimal scale
 * and numeric flag to the superclass.
 */

public class GempakParameter extends GridParameter {

  /**
   * decimal scale
   */
  private int decimalScale = 0;


  /**
   * numeric or char
   */
  private boolean isNumeric = true;


  /**
   * Create a new numeric GEMPAK parameter
   *
   * @param number
   * @param name
   * @param description
   * @param unit        of parameter
   * @param scale       decimal (10E*) scaling factor
   */
  public GempakParameter(int number, String name, String description, String unit, int scale) {
    this(number, name, description, unit, scale, true);
  }

  /**
   * Create a new GEMPAK parameter
   *
   * @param number
   * @param name
   * @param description
   * @param unit        of parameter
   * @param scale       decimal (10E*) scaling factor
   * @param isNumeric   flag for numeric
   */
  public GempakParameter(int number, String name, String description,
                         String unit, int scale, boolean isNumeric) {
    super(number, name, description, unit);
    decimalScale = scale;
    this.isNumeric = isNumeric;
  }

  /**
   * Get the decimal scale
   *
   * @return the decimal scale
   */
  public int getDecimalScale() {
    return decimalScale;
  }


  /**
   * Get whether this is numeric or not
   *
   * @return true if numeric
   */
  public boolean getIsNumeric() {
    return isNumeric;
  }


  /**
   * Set whether this is numeric or not
   *
   * @param yesorno true if numeric
   */
  public void setIsNumeric(boolean yesorno) {
    isNumeric = yesorno;
  }

  /**
   * Return a String representation of this object
   *
   * @return a String representation of this object
   * public String toString() {
   *   StringBuffer buf = new StringBuffer(super.toString());
   *   buf.append(" scale: ");
   *   buf.append(getDecimalScale());
   *   return buf.toString();
   * }
   */

  /**
   * Return a String representation of this object
   *
   * @return a String representation of this object
   */
  public String toString() {
    StringBuilder buf = new StringBuilder("GridParameter: ");
    buf.append(StringUtil2.padLeft(String.valueOf(getNumber()), 4));
    buf.append(" ");
    String param = getName() + " (" + getDescription() + ")";
    buf.append(StringUtil2.padRight(param, 40));
    buf.append(" [");
    buf.append(getUnit());
    buf.append("]");
    buf.append(" scale: ");
    buf.append(getDecimalScale());
    return buf.toString();
  }


  /**
   * Check for equality
   *
   * @param o the object in question
   * @return true if has the same parameters
   */
  public boolean equals(Object o) {
    if ((o == null) || !(o instanceof GempakParameter)) {
      return false;
    }
    GempakParameter that = (GempakParameter) o;
    return super.equals(that) && (decimalScale == that.decimalScale);
  }

  /**
   * Generate a hash code.
   *
   * @return the hash code
   */
  public int hashCode() {
    return super.hashCode() + 17 * decimalScale;
  }


}

