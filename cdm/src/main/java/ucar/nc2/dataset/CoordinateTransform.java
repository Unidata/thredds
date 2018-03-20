/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import ucar.unidata.util.Parameter;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;

/**
 * A CoordinateTransform is an abstraction of a function from a CoordinateSystem to a
 * "reference" CoordinateSystem, such as lat, lon.
 *
 * @author caron
 */

@ThreadSafe
public class CoordinateTransform implements Comparable<CoordinateTransform> {

  protected final String name, authority;
  protected final TransformType transformType;

  // immutable once these are done adding
  protected List<Parameter> params;

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
    this.params = new ArrayList<>();
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

  @Override
  public int compareTo(CoordinateTransform oct) {
    return name.compareTo(oct.getName());
  }

}
