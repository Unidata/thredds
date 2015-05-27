/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.util.Indent;
import java.util.*;

/**
 /*
 message CoordSys {
   required string name = 1;
   repeated string axes = 2;
   repeated CoordTransform transforms = 3;
   repeated CoordSys components = 4;        // ??
 }
 *
 * @author caron
 * @since 5/2/2015
 */
public class GridCoordSys {
  public enum Type {Coverage, Curvilinear, Grid, Swath, Fmrc}

  //////////////////////////////////////////////////
  String name;
  List<String> axisNames;
  List<String> transformNames;
  Type type;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getAxisNames() {
    return axisNames;
  }

  public void setAxisNames(List<String> axisNames) {
    this.axisNames = axisNames;
  }

  public void addAxisName(String p) {
    if (axisNames == null) axisNames = new ArrayList<>();
    axisNames.add(p);
  }

  public List<String> getTransformNames() {
    return (transformNames != null) ? transformNames : new ArrayList<String>();
  }

  public void setTransformNames(List<String> transformNames) {
    this.transformNames = transformNames;
  }

  public void addTransformName(String p) {
    if (transformNames == null) transformNames = new ArrayList<>();
    transformNames.add(p);
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
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
    f.format("%s CoordSys '%s'", indent, name);
    f.format(" has coordVars:");
    for (String v : axisNames)
      f.format("%s, ", v);
    f.format("; has transforms:");
    for (String t : transformNames)
      f.format("%s, ", t);
    f.format("%n");

    indent.decr();
  }

}
