/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import ucar.ma2.DataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;

import java.util.List;


/**
 * Adapt a VariableSimpleIF into another VariableSimpleIF, so it can be subclassed.
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */

public class VariableSimpleSubclass implements VariableSimpleIF {
  protected VariableSimpleIF v;

  public VariableSimpleSubclass( VariableSimpleIF v) {
    this.v = v;
  }

  public String getFullName() { return v.getFullName(); }
  public String getName() { return v.getFullName(); }
  public String getShortName() { return v.getShortName(); }
  public DataType getDataType() { return v.getDataType(); }
  public String getDescription() { return v.getDescription(); }
  public String getInfo() { return v.toString(); }
  public String getUnitsString() { return v.getUnitsString(); }

  public int getRank() {  return v.getRank(); }
  public int[] getShape() { return v.getShape(); }
  public List<Dimension> getDimensions() { return v.getDimensions(); }
  public List<Attribute> getAttributes() { return v.getAttributes(); }
  public ucar.nc2.Attribute findAttributeIgnoreCase(String attName){
    return v.findAttributeIgnoreCase(attName);
  }

  public String toString() {
    return v.toString();
  }

  /**
   * Sort by name
   */
  public int compareTo(VariableSimpleIF o) {
    return getShortName().compareTo(o.getShortName());
  }
}