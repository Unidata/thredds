/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of VariableSimpleIF
 *
 * @author caron
 * @since 6/10/14
 */
public class VariableSimpleImpl implements VariableSimpleIF {

  static public VariableSimpleImpl makeScalar(String name, String desc, String units, DataType dt) {
    return new VariableSimpleImpl(name, desc, units, dt, null);
  }

  static public VariableSimpleImpl makeString(String name, String desc, String units, int str_len) {
    Dimension d = new Dimension(name+"_strlen", str_len, false, false, false);
    // String dimString = Dimension.makeDimensionsString(new int[] {str_len});
    return new VariableSimpleImpl(name, desc, units, DataType.CHAR, Collections.singletonList(d));
  }

  private final String name, desc, units;
  private final DataType dt;
  private final List<Attribute> atts = new ArrayList<>();
  private final List<Dimension> dims;
  private final int[] shape;

  public VariableSimpleImpl(String name, String desc, String units, DataType dt, List<Dimension> dims) {
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