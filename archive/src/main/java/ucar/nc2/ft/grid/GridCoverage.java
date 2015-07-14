/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.ArrayWithCoordinates;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.util.Indent;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 message GridCoverage {
   required string name = 1; // short name
   required DataType dataType = 2;
   optional bool unsigned = 3 [default = false];
   repeated Attribute atts = 4;
   required string coordSys = 5;
 }
 * @author caron
 * @since 5/2/2015
 */
public abstract class GridCoverage implements IsMissingEvaluator {
  String name;
  DataType dataType;
  List<Attribute> atts;
  String coordSysName;
  String units, description;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  public List<Attribute> getAttributes() {
    return atts;
  }

  public void setAtts(List<Attribute> atts) {
    this.atts = atts;
  }

  public String getCoordSysName() {
    return coordSysName;
  }

  public void setCoordSysName(String coordSysName) {
    this.coordSysName = coordSysName;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
    for (Attribute att : atts)
      f.format("%s     %s%n", indent, att);
    indent.decr();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  // LOOK what is the contract? what order are the values ?? ie what are the coordinates of each point ??
  // think about "crossing the seam", the coordsys has to shift
  // public abstract ReferencedArray readData(GridSubset subset) throws IOException;

  // LOOK what is the contract? what order are the returned values ?? ie what are the coordinates of each point ??
  public abstract ArrayWithCoordinates readData(SubsetParams subset) throws IOException;

  // LOOK problem this violates coordinate-only data access. NA with cdmr
  // used by CFGridCoverageWriter. this is asking that the coordinates dont change, eg only allow a rectangular subset of the (possibly) bigger grid
  public abstract Array readSubset(List<Range> subset) throws IOException, InvalidRangeException;

  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }
}
