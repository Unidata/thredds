/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.Attribute;
import ucar.nc2.util.Indent;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 message CoordTransform {
   required bool isHoriz = 1;
   required string name = 2;
   repeated Attribute params = 3;
 }
 * @author caron
 * @since 5/4/2015
 */
public class GridCoordTransform {

  boolean isHoriz;
  String name;
  List<Attribute> parameters;

  public boolean isHoriz() {
    return isHoriz;
  }

  public void setIsHoriz(boolean isHoriz) {
    this.isHoriz = isHoriz;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Attribute> getParameters() {
    return parameters;
  }

  public void setParameters(List<Attribute> parameters) {
    this.parameters = parameters;
  }

  public void addParameter(Attribute p) {
    if (parameters == null) parameters = new ArrayList<>();
    parameters.add(p);
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordTransform '%s'", indent, name);
    f.format(" isHoriz: %s%n", isHoriz);
    for (Attribute att : parameters)
      f.format("%s     %s%n", indent, att);
    f.format("%n");

    indent.decr();
  }
}
