/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.util.Indent;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Coverage
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class Coverage implements IsMissingEvaluator {
  private final String name;
  private final DataType dataType;
  private final List<Attribute> atts;
  private final String units, description;
  private final String coordSysName;
  private CoverageCoordSys coordSys; // almost immutable

  public Coverage(String name, DataType dataType, List<Attribute> atts, String coordSysName, String units, String description) {
    this.name = name;
    this.dataType = dataType;
    this.atts = atts;
    this.coordSysName = coordSysName;
    this.units = units;
    this.description = description;
  }

  // copy constructor
  public Coverage(Coverage from) {
    this.name = from.getName();
    this.dataType = from.getDataType();
    this.atts = from.getAttributes();
    this.units = from.getUnits();
    this.description = from.getDescription();
    this.coordSysName = from.getCoordSysName();
  }

  void setCoordSys (CoverageCoordSys coordSys) {
    if (this.coordSys != null) throw new RuntimeException("Cant change coordSys once set");
    this.coordSys = coordSys;
  }

  public String getName() {
    return name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public List<Attribute> getAttributes() {
    return atts;
  }

  public String getCoordSysName() {
    return coordSysName;
  }

  public String getUnits() {
    return units;
  }

  public String getDescription() {
    return description;
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

  public CoverageCoordSys getCoordSys() {
    return coordSys;
  }

  ///////////////////////////////////////////////////////////////

  public long getSizeInBytes() {
    long total = 1;
    for (String axisName : coordSys.getAxisNames()) {  // LOOK this assumes a certain order
      CoverageCoordAxis axis = coordSys.getAxis(axisName);
      total *= axis.getNcoords();
    }
    total *= getDataType().getSize();
    return total;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////

  public GeoReferencedArray readData(SubsetParams subset) throws IOException, InvalidRangeException {
    CoverageReader reader = coordSys.dataset.reader;
    return reader.readData(this, subset);
  }

  /* LOOK problem this violates coordinate-only data access. NA with cdmr
  // used by CFGridCoverageWriter. this is asking that the coordinates dont change, eg only allow a rectangular subset of the (possibly) bigger grid
  public abstract Array readSubset(List<Range> subset) throws IOException, InvalidRangeException;   */

  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }
}
