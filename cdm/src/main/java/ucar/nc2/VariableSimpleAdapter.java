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
package ucar.nc2;

import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;

import java.util.List;
import java.util.ArrayList;

/**
 * Adapt a StructureMembers.Member into a VariableSimpleIF.
 * @author caron
 * @since Apr 20, 2008
 */
public class VariableSimpleAdapter implements VariableSimpleIF {
  private StructureMembers.Member m;

  public VariableSimpleAdapter( StructureMembers.Member m) {
    this.m = m;
  }

  public String getName() { return m.getName(); }
  public String getShortName() { return m.getName(); }
  public DataType getDataType() { return m.getDataType(); }
  public String getDescription() { return m.getDescription(); }
  public String getUnitsString() { return m.getUnitsString(); }

  public int getRank() {  return m.getShape().length; }
  public int[] getShape() { return m.getShape(); }
  public List<Dimension> getDimensions() {
    List<Dimension> result = new ArrayList<Dimension>(getRank());
    for (int aShape : getShape())
      result.add(new Dimension(null, aShape, false));
    return result;
  }
  public List<Attribute> getAttributes() { return new ArrayList<Attribute>(1); }
  public ucar.nc2.Attribute findAttributeIgnoreCase(String attName){
    return null;
  }

  public String toString() {
    return m.toString();
  }

  /**
   * Sort by name
   */
  public int compareTo(VariableSimpleIF o) {
    return getName().compareTo(o.getName());
  }
}
