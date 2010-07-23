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
package ucar.nc2;

import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.util.List;
import java.util.ArrayList;

/**
 * A Dimension is used to define the array shape of a Variable.
 * It may be shared among Variables, which provides a simple yet powerful way of associating Variables.
 * When a Dimension is shared, it has a unique name within its Group.
 * It may have a coordinate Variable, which gives each index a coordinate value.
 * A private Dimension cannot have a coordinate Variable, so use shared dimensions with coordinates when possible.
 * The Dimension length must be > 0, except for an unlimited dimension which may have length = 0, and a vlen
 * Dimension has length = -1.
 *
 *
 * <p> Immutable if setImmutable() was called, except for an Unlimited Dimension, whose size can change.
 *
 * @author caron
 */

public class Dimension implements Comparable {
  /** A variable-length dimension: the length is not known until the data is read. */
  static public Dimension VLEN = new Dimension( "*", -1, true, false, true).setImmutable(); // for Sequences, HDF5 VarLength

  static public String makeDimensionList(List<Dimension> dimList) {
    StringBuilder out = new StringBuilder();
    for (Dimension dim : dimList)
      out.append(dim.getName()).append(" ");
    return out.toString();
  }

  private boolean isUnlimited = false;
  private boolean isVariableLength = false;
  private boolean isShared = true; // shared means its in a group dimension list.
  private String name;
  private int length;
  private boolean immutable = false;
  private Group g; // null if !isShared

  /**
   * Returns the name of this Dimension; may be null.
   * A Dimension with a null name is called "anonymous" and must be private.
   * Dimension names are unique within a Group.
   * @return name of Dimension, may be null for anonymous dimension
   */
  public String getName() { return name; }

  /**
   * Get the length of the Dimension.
   * @return length of Dimension
   */
  public int getLength() { return length; }

  /**
   * If unlimited, then the length can increase; otherwise it is immutable.
   * @return if its an "unlimited" Dimension
   */
  public boolean isUnlimited() { return isUnlimited; }

  /**
   * If variable length, then the length is unknown until the data is read.
   * @return if its a "variable length" Dimension.
   */
  public boolean isVariableLength() { return isVariableLength; }

  /**
   * If this Dimension is shared, or is private to a Variable.
   * All Dimensions in NetcdfFile.getDimensions() or Group.getDimensions() are shared.
   * Dimensions in the Variable.getDimensions() may be shared or private.
   * @return if its a "shared" Dimension.
   */
  public boolean isShared() { return isShared; }


  /**
   * Get the Group that owns this Dimension.
   * @return owning group or null if !isShared
   */
  public Group getGroup() { return g; }

  /**
   * Instances which have same contents are equal.
   * Careful!! this is not object identity !!
   */
  @Override
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Dimension)) return false;
    Dimension other = (Dimension) oo;
    if ((g != null) && !g.equals(other.getGroup())) return false;
    if ((getName() == null) && (other.getName() != null)) return false;
    if ((getName() != null) && !getName().equals(other.getName())) return false;
    return (getLength() == other.getLength()) &&
           (isUnlimited() == other.isUnlimited()) &&
           (isVariableLength() == other.isVariableLength()) &&
           (isShared() == other.isShared());
  }

  /** Override Object.hashCode() to implement equals. */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (g != null) result += 37 * result + g.hashCode();
      if (null != getName()) result += 37 * result + getName().hashCode();
      result += 37 * result + getLength();
      result += 37 * result + (isUnlimited() ? 0 : 1);
      result += 37 * result + (isVariableLength() ? 0 : 1);
      result += 37 * result + (isShared() ? 0 : 1);
      hashCode = result;
    }
    return hashCode;
  }
  private int hashCode = 0;

  /** CDL representation, not strict. */
  @Override
  public String toString() {
    return writeCDL( false);
  }

  /**
   * Dimensions with the same name are equal.
   * @param o compare to this Dimension
   * @return 0, 1, or -1
   */
  public int compareTo(Object o) {
    Dimension odim = (Dimension) o;
    return name.compareTo(odim.getName());
  }

  /** CDL representation.
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    StringBuilder buff = new StringBuilder();
    String name = strict ? NetcdfFile.escapeName(getName()) : getName();
    buff.append("   ").append(name);
    if (isUnlimited())
      buff.append(" = UNLIMITED;   // (").append(getLength()).append(" currently)");
    else if (isVariableLength())
      buff.append(" = UNKNOWN;" );
    else
      buff.append(" = ").append(getLength()).append(";");
    return buff.toString();
  }

  /**
   * Constructor
   * @param name name must be unique within group
   * @param length length of Dimension
   */
  public Dimension(String name, int length) {
    this(name, length, true, false, false);
  }

  /**
   * Constructor
   * @param name name must be unique within group
   * @param length length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   */
  public Dimension(String name, int length, boolean isShared) {
    this(name, length, isShared, false, false);
  }

  /**
   * Constructor
   * @param name name must be unique within group. Can be null only if not shared.
   * @param length length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   * @param isUnlimited whether the length can grow.
   * @param isVariableLength whether the length is unknown until the data is read.
   */
  public Dimension(String name, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    this.name = name;
    this.isShared = isShared;
    this.isUnlimited = isUnlimited;
    this.isVariableLength = isVariableLength;
    setLength(length);
    assert (name != null) || !this.isShared;
    //if (!isShared)
    //  System.out.println("HEY");
  }

  /**
   * Copy Constructor. used to make global dimensions
   * @param name name must be unique within group. Can be null only if not shared.
   * @param from copy all other fields from here.
   */
  public Dimension(String name, Dimension from) {
    this.name = name;
    this.length = from.length;
    this.isUnlimited = from.isUnlimited;
    this.isVariableLength = from.isVariableLength;
    this.isShared = from.isShared;
  }

  ///////////////////////////////////////////////////////////
  // the following make this mutable

  /** Set whether this is unlimited, meaning length can increase.
   * @param b true if unlimited
   */
  public void setUnlimited( boolean b) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.isUnlimited = b;
    setLength(this.length); // check legal
  }
  /** Set whether the length is variable.
   * @param b true if variable length
   */
  public void setVariableLength( boolean b) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.isVariableLength = b;
    setLength(this.length); // check legal
  }
  /** Set whether this is shared.
   * @param b true if shared
   */
  public void setShared( boolean b) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.isShared = b;
    hashCode = 0;
  }
  /** Set the Dimension length.
   * @param n length of Dimension
   */
  public void setLength( int n) {
    if (immutable && !isUnlimited) throw new IllegalStateException("Cant modify");
    if (isVariableLength) {
      if (n != -1) throw new IllegalArgumentException("VariableLength Dimension length ="+n+" must be -1");
    } else if (isUnlimited) {
        if (n < 0) throw new IllegalArgumentException("Unlimited Dimension length ="+n+" must >= 0");
     } else {
      if (n < 1) throw new IllegalArgumentException("Dimension length ="+n+" must be > 0");
    }
    this.length = n;
    hashCode = 0;
  }

  /** rename
   * @param name new name of Dimension.
   */
  public void setName( String name) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.name = (name == null) ? null : AbstractIOServiceProvider.createValidNetcdfObjectName(name);
    hashCode = 0;
  }

  /**
   * Set the group
   * @param g parent group
   */
  public void setGroup( Group g) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.g = g;
    hashCode = 0;
  }


  /**
   * Make this immutable.
   * @return this
   */
  public Dimension setImmutable() {
    immutable = true;
    //coordVars = Collections.unmodifiableList(coordVars);
    return this;
  }

  //////////////////////////////////////////////////////////////////////
  // deprecated
  //private List<Variable> coordVars = null; // new ArrayList<Variable>();

  /**
   * @deprecated - do not use
   * @param v coord var
   */
  synchronized public void addCoordinateVariable( Variable v) {
  }

  /**
   * Use
   *  Variable cv = ncfile.findVariable(dim.getName());
   *  if ((cv != null) && cv.isCoordinateVariable()) ...
   * @deprecated - do not use
   * @return an empty list
   */
  public List<Variable> getCoordinateVariables() { return new ArrayList<Variable>(); }
}