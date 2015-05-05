/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.ProjectionImpl;

import java.util.Formatter;

/**
 message CoordTransform {
   required bool isHoriz = 1;
   required string name = 2;
   repeated Attribute params = 3;
 }
 * @author caron
 * @since 5/4/2015
 */
public class GridCoordTransform implements AttributeContainer {

  boolean isHoriz;
  String name;
  AttributeContainerHelper attributes;
  ProjectionImpl projection;

  public GridCoordTransform(String name) {
    this.name = name;
    attributes = new AttributeContainerHelper(name);
  }

  public boolean isHoriz() {
    return isHoriz;
  }

  public void setIsHoriz(boolean isHoriz) {
    this.isHoriz = isHoriz;
  }

  public String getName() {
    return name;
  }

  public ProjectionImpl getProjection() {
    return projection;
  }

  public void setProjection(ProjectionImpl projection) {
    this.projection = projection;
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
    f.format("%s CoordTransform '%s'", indent, name);
    f.format(" isHoriz: %s%n", isHoriz);
    for (Attribute att : attributes.getAttributes())
      f.format("%s     %s%n", indent, att);
    f.format("%n");

    indent.decr();
  }

    //////////////////////////////////////////////////////////////////////////////////////////////////
  // AttributeHelper

  public java.util.List<Attribute> getAttributes() {
    return attributes.getAttributes();
  }

  public Attribute findAttribute(String name) {
    return attributes.findAttribute(name);
  }

  public Attribute findAttributeIgnoreCase(String name) {
    return attributes.findAttributeIgnoreCase(name);
  }

  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    return attributes.findAttValueIgnoreCase(attName, defaultValue);
  }

  public Attribute addAttribute(Attribute att) {
    return attributes.addAttribute(att);
  }

  public void addAll(Iterable<Attribute> atts) {
    attributes.addAll(atts);
  }

  public boolean remove(Attribute a) {
    return attributes.remove(a);
  }

  public boolean removeAttribute(String attName) {
    return attributes.removeAttribute(attName);
  }

  public boolean removeAttributeIgnoreCase(String attName) {
    return attributes.removeAttributeIgnoreCase(attName);
  }
}
