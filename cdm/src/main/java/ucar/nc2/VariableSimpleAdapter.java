/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Adapt a StructureMembers.Member into a VariableSimpleIF.
 * @author caron
 * @since Apr 20, 2008
 */
public class VariableSimpleAdapter implements VariableSimpleIF {
  private StructureMembers.Member m;

  public static List<VariableSimpleIF> convert(StructureMembers sm) {
    List<StructureMembers.Member> mlist = sm.getMembers();
    return mlist.stream().map(VariableSimpleAdapter::new).collect(Collectors.toList());
  }

  /**
   * Constructor
   * @param m adapt this Member
   */
  public VariableSimpleAdapter( StructureMembers.Member m) {
    this.m = m;
  }

  public String getFullName() {  return m.getFullName(); }
  public String getName() { return m.getName(); }
  public String getShortName() { return m.getName(); }
  public DataType getDataType() { return m.getDataType(); }
  public String getDescription() { return m.getDescription(); }
  public String getUnitsString() { return m.getUnitsString(); }

  public int getRank() {  return m.getShape().length; }
  public int[] getShape() { return m.getShape(); }
  public List<Dimension> getDimensions() {
    List<Dimension> result = new ArrayList<>(getRank());
    for (int aShape : getShape())
      result.add(new Dimension(null, aShape, false));
    return result;
  }
  public List<Attribute> getAttributes() { return new ArrayList<>(1); }
  public ucar.nc2.Attribute findAttributeIgnoreCase(String attName){
    return null;
  }

  public String toString() {
    return m.getName();
  }

  /**
   * Sort by name
   */
  public int compareTo(VariableSimpleIF o) {
    assert o != null;
    return getShortName().compareTo(o.getShortName());
  }
}
