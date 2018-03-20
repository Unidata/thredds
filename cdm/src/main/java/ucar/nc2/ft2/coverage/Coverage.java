/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.IsMissingEvaluator;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.util.Indent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Coverage - aka Grid or GeoGrid.
 * Immutable after setCoordSys() is called.
 *
 * @author caron
 * @since 7/11/2015
 */
// @Immutable
public class Coverage implements VariableSimpleIF, IsMissingEvaluator {
  private final String name;
  private final DataType dataType;
  private final AttributeContainerHelper atts;
  private final String units, description;
  private final String coordSysName;
  protected final CoverageReader reader;
  protected final Object user;

  private CoverageCoordSys coordSys; // almost immutable

  public Coverage(String name, DataType dataType, List<Attribute> atts, String coordSysName, String units, String description, CoverageReader reader, Object user) {
    this.name = name;
    this.dataType = dataType;
    this.atts = new AttributeContainerHelper(name, atts);
    this.coordSysName = coordSysName;
    this.units = units;
    this.description = description;
    this.reader = reader;
    this.user = user;
  }

  // copy constructor
  public Coverage(Coverage from, CoverageCoordSys coordSysSubset) {
    this.name = from.getName();
    this.dataType = from.getDataType();
    this.atts = from.atts;
    this.units = from.getUnitsString();
    this.description = from.getDescription();
    this.coordSysName = (coordSysSubset != null) ? coordSysSubset.getName() : from.coordSysName;
    this.reader = from.reader;
    this.user = from.user;
  }

  void setCoordSys (CoverageCoordSys coordSys) {
    if (this.coordSys != null) throw new RuntimeException("Cant change coordSys once set");
    this.coordSys = coordSys;
  }

  public String getName() {
    return name;
  }


  @Override
  public DataType getDataType() {
    return dataType;
  }

  @Override
  public List<Attribute> getAttributes() {
    return atts.getAttributes();
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    return atts.findAttributeIgnoreCase(name);
  }

  @Override
  public String getUnitsString() {
    return units;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public String getCoordSysName() {
    return coordSysName;
  }

  public Object getUserObject() {
    return user;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%n%s  %s %s(%s) desc='%s' units='%s'%n", indent, dataType, name, coordSysName, description, units);
    f.format("%s    attributes:%n", indent);
    for (Attribute att : atts.getAttributes())
      f.format("%s     %s%n", indent, att);
    indent.decr();
  }

  @Nonnull
  public CoverageCoordSys getCoordSys() {
    return coordSys;
  }

  ///////////////////////////////////////////////////////////////

  public long getSizeInBytes() {
    Section section = new Section(coordSys.getShape());
    return section.computeSize() * getDataType().getSize();
  }

  // LOOK must conform to whatever grid.readData() returns
  // LOOK need to deal with runtime(time), runtime(runtime, time)
  public String getIndependentAxisNamesOrdered() {
    StringBuilder sb = new StringBuilder();
    for (CoverageCoordAxis axis : coordSys.getAxes()) {
      if (!(axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)) continue;
      sb.append(axis.getName());
      sb.append(" ");
    }
    return sb.toString();
  }

  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////

  public GeoReferencedArray readData(SubsetParams subset) throws IOException, InvalidRangeException {
    return reader.readData(this, subset, false);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // implement VariableSimpleIF

  @Override
  public String getFullName() {
    return getName();
  }

  @Override
  public String getShortName() {
    return getName();
  }

  @Override
  public int getRank() {
    return getShape().length;
  }

  @Override
  public int[] getShape() {
    return coordSys.getShape();
  }

 // @Override
  public List<Dimension> getDimensions() {
    return null;
  }

  @Override
  public int compareTo(@Nonnull VariableSimpleIF o) {
    return getFullName().compareTo(o.getFullName());
  }
}
