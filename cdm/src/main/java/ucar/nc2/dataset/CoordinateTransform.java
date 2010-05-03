/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dataset;

import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.ThreadSafe;
import ucar.unidata.util.Parameter;

/**
 * A CoordinateTransform is an abstraction of a function from a CoordinateSystem to a
 * "reference" CoordinateSystem, such as lat, lon.
 *
 * @author caron
 */

@ThreadSafe
public class CoordinateTransform implements Comparable {

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
