/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;

import java.util.List;
import java.util.ArrayList;

/**
 * Adapt a StructureMembers.Member into a VariableSimpleIF.
 *
 * @author caron
 * @since Apr 20, 2008
 */
public class VariableSimpleAdapter implements VariableSimpleIF {
  private StructureMembers.Member m;

  public static List<VariableSimpleIF> convert(StructureMembers sm) {
    List<StructureMembers.Member> mlist = sm.getMembers();
    List<VariableSimpleIF> result = new ArrayList<>(mlist.size());
    for (StructureMembers.Member m : mlist) {
      result.add(new VariableSimpleAdapter(m));
    }
    return result;
  }

  /**
   * Constructor
   *
   * @param m adapt this Member
   */
  public VariableSimpleAdapter(StructureMembers.Member m) {
    this.m = m;
  }

  @Override
  public String getFullName() { return m.getFullName(); }

  @Override
  public String getName() { return m.getName(); }

  @Override
  public String getShortName() { return m.getName(); }

  @Override
  public DataType getDataType() { return m.getDataType(); }

  @Override
  public String getDescription() { return m.getDescription(); }

  @Override
  public String getUnitsString() { return m.getUnitsString(); }

  @Override
  public int getRank() { return m.getShape().length; }

  @Override
  public int[] getShape() { return m.getShape(); }

  /**
   * Returns this variable's dimensions. Dimensions are named by prepending the string {@code "len"} to their length,
   * e.g. a dimension with length 17 would be named {@code "len17"}. The names needn't be unique because the dimensions
   * aren't shared.
   *
   * @return  this variable's dimensions.
   */
  @Override
  public List<Dimension> getDimensions() {
    List<Dimension> result = new ArrayList<>(getRank());
    for (int dimLen : getShape()) {
      String dimName = "len" + dimLen;
      result.add(new Dimension(dimName, dimLen, false));
    }
    return result;
  }

  @Override
  public List<Attribute> getAttributes() { return new ArrayList<>(1); }

  @Override
  public ucar.nc2.Attribute findAttributeIgnoreCase(String attName) {
    return null;
  }

  public String toString() {
    return m.getName();
  }

  /**
   * Sort by name
   */
  @Override
  public int compareTo(VariableSimpleIF o) {
    assert o != null;
    return getShortName().compareTo(o.getShortName());
  }
}
