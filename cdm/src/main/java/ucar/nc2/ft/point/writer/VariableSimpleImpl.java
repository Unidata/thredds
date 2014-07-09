/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.ft.point.writer;

import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of VariableSimpleIF
 *
 * @author caron
 * @since 6/10/14
 */
public class VariableSimpleImpl implements VariableSimpleIF {

  static public VariableSimpleImpl makeFromMember(StructureMembers.Member m) {
    return new VariableSimpleImpl(m.getName(), m.getDescription(), m.getUnitsString(), m.getDataType(), null);

  }

  static public VariableSimpleImpl makeScalar(String name, String desc, String units, DataType dt) {
    return new VariableSimpleImpl(name, desc, units, dt, null);
  }

  static public VariableSimpleImpl makeString(String name, String desc, String units, int str_len) {
    Dimension d = new Dimension(name+"_strlen", str_len, false, false, false);
    // String dimString = Dimension.makeDimensionsString(new int[] {str_len});
    return new VariableSimpleImpl(name, desc, units, DataType.CHAR, Arrays.asList(d));
  }

  private final String name, desc, units;
  private final DataType dt;
  private final List<Attribute> atts = new ArrayList<>();
  private final List<Dimension> dims;
  private final int[] shape;

  VariableSimpleImpl(String name, String desc, String units, DataType dt, List<Dimension> dims) {
    this.name = name;
    this.desc = desc;
    this.units = units;
    this.dt = dt;

    if (dims == null) {
      this.dims = new ArrayList<>();
      this.shape = new int[0];
    } else {
      this.dims = dims;
      this.shape = new int[dims.size()];
      int count = 0;
      for (Dimension d : dims)
        this.shape[count++] = d.getLength();
    }

    if (units != null)
      atts.add(new Attribute(CDM.UNITS, units));
    if (desc != null)
      atts.add(new Attribute(CDM.LONG_NAME, desc));
  }

  public VariableSimpleImpl add(Attribute att) {
    atts.add(att);
    return this;
  }

  public String getName() {
    return name;
  }

  public String getFullName() {
    return name;
  }

  @Override
  public String getShortName() {
    return name;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public String getUnitsString() {
    return units;
  }

  @Override
  public int getRank() {
    return shape.length;
  }

  @Override
  public int[] getShape() {
    return shape;
  }

  @Override
  public List<Dimension> getDimensions() {
    return dims;
  }

  @Override
  public DataType getDataType() {
    return dt;
  }

  @Override
  public List<Attribute> getAttributes() {
    return atts;
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    for (Attribute att : atts) {
      if (att.getShortName().equalsIgnoreCase(name))
        return att;
    }
    return null;
  }

  @Override
  public int compareTo(VariableSimpleIF o) {
    return name.compareTo(o.getShortName()); // ??
  }
}