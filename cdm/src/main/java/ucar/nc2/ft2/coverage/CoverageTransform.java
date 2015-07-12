/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.dataset.CoordTransBuilder;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.ProjectionImpl;

import java.util.Formatter;

/**
 * Coverage CoordTransform,
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class CoverageTransform implements AttributeContainer {

  private final String name;
  private final AttributeContainerHelper attributes;
  private final boolean isHoriz;
  private ProjectionImpl projection;    // lazy instantiation

  public CoverageTransform(String name, AttributeContainerHelper attributes, boolean isHoriz) {
    this.name = name;
    this.attributes = attributes;
    this.isHoriz = isHoriz;
  }

  public boolean isHoriz() {
    return isHoriz;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean remove(Attribute a) {
    return false;
  }

  @Override
  public boolean removeAttribute(String attName) {
    return false;
  }

  @Override
  public boolean removeAttributeIgnoreCase(String attName) {
    return false;
  }

  public ProjectionImpl getProjection() {
    if (projection == null && isHoriz) {
      projection = CoordTransBuilder.makeProjection(this, new Formatter());
    }
    return projection;
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
    f.format(" isHoriz: %s%n", isHoriz());
    if (projection != null)
      f.format(" projection: %s%n", projection);
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


  @Override
  public void addAll(Iterable<Attribute> atts) {
    // NOOP
  }

  @Override
  public Attribute addAttribute(Attribute att) {
    return null;
  }

}
