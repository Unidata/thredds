/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dataset;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.util.Parameter;

/**
 * A CoordinateTransform is an abstraction of a function from a CoordinateSystem to a
 * "reference" CoordinateSystem, such as lat, lon.
 *
 * @author caron
 */

public class CoordinateTransform implements Comparable {

  protected String name, authority;
  protected TransformType transformType = null;

  protected List<Parameter> params;
  protected String id;

  /**
   * Create a Coordinate Transform.
   *
   * @param name          name of transform, must be unique within the NcML.
   * @param authority     naming authority
   * @param transformType type of transform.
   */
  public CoordinateTransform(String name, String authority, TransformType transformType) {
    this.name = name;
    this.authority = authority;
    this.transformType = transformType;
    this.params = new ArrayList<Parameter>();
  }

  /**
   * add a parameter
   * @param param add this Parameter
   */
  public void addParameter(Parameter param) {
    params.add(param);
  }

  /**
   * get the name
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * get the naming authority
   * @return the naming authority
   */
  public String getAuthority() {
    return authority;
  }

  /**
   * get the transform type
   * @return the transform type
   */
  public TransformType getTransformType() {
    return transformType;
  }

  /**
   * get list of ProjectionParameter objects.
   * @return list of ProjectionParameter objects.
   */
  public List<Parameter> getParameters() {
    return params;
  }


  /**
   * Convenience function; look up Parameter by name, ignoring case.
   *
   * @param name the name of the attribute
   * @return the Attribute, or null if not found
   */
  public Parameter findParameterIgnoreCase(String name) {
    for (Parameter a : params) {
      if (name.equalsIgnoreCase(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Instances which have same name, authority and parameters are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof CoordinateTransform)) return false;

    CoordinateTransform o = (CoordinateTransform) oo;
    if (!getName().equals(o.getName())) return false;
    if (!getAuthority().equals(o.getAuthority())) return false;
    if (!(getTransformType() == o.getTransformType())) return false;

    List<Parameter> oparams = o.getParameters();
    if (params.size() != oparams.size()) return false;

    for (int i = 0; i < params.size(); i++) {
      Parameter att =  params.get(i);
      Parameter oatt = oparams.get(i);
      if (!att.getName().equals(oatt.getName())) return false;
      //if (!att.getValue().equals(oatt.getValue())) return false;
    }
    return true;
  }

  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getAuthority().hashCode();
      result = 37 * result + getTransformType().hashCode();
      for (Parameter att : params) {
        result = 37 * result + att.getName().hashCode();
        //result = 37*result + att.getValue().hashCode();
      }
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0;

  public String toString() {
    return name;
  }

  public int compareTo(Object o) {
    CoordinateTransform oct = (CoordinateTransform) o;
    return name.compareTo(oct.getName());
  }

}
