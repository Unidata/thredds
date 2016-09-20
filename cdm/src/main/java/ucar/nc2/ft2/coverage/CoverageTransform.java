/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft2.coverage;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.dataset.CoordTransBuilder;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.ProjectionImpl;

import javax.annotation.concurrent.Immutable;
import java.util.Formatter;

/**
 * Coverage Coordinate Transform.
 * Immutable with lazy instantiation of projection
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

  public ProjectionImpl getProjection() {
    if (projection == null && isHoriz) {
      synchronized (this) {
        projection = CoordTransBuilder.makeProjection(this, new Formatter());
      }
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

  @Override
  public java.util.List<Attribute> getAttributes() {
    return attributes.getAttributes();
  }

  @Override
  public Attribute findAttribute(String name) {
    return attributes.findAttribute(name);
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    return attributes.findAttributeIgnoreCase(name);
  }

  @Override
  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    return attributes.findAttValueIgnoreCase(attName, defaultValue);
  }


  @Override
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


  @Override
  public void addAll(Iterable<Attribute> atts) {
    // NOOP
  }

  @Override
  public Attribute addAttribute(Attribute att) {
    return null;
  }

}
