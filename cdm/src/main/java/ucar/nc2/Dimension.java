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
package ucar.nc2;

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
  static public Dimension VLEN = new Dimension( "*", -1, true, false, true).setImmutable(); // for Sequences, HDF5 VarLength

  private boolean isUnlimited = false;
  private boolean isVariableLength = false;
  private boolean isShared = true; // shared means its in a group dimension list.
  private String name;
  private int length;
  private boolean immutable = false;
  private Group g;

  /**
   * Returns the name of this Dimension; may be null.
   * A Dimension with a null name is called "anonymous" and must be private.
   * Dimension names are unique within a Group.
   * @return name of Dimension
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
   * @return owning group
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
    return (getLength() == other.getLength()) &&
           (getName().equals(other.getName())) &&
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
      result += 37 * result + getLength();
      result += 37 * result + getName().hashCode();
      result += 37 * result + (isUnlimited() ? 0 : 1);
      result += 37 * result + (isVariableLength() ? 0 : 1);
      result += 37 * result + (isShared() ? 0 : 1);
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;

  /** String representation. */
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

  /** String representation.
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    StringBuffer buff = new StringBuffer();
    buff.append("   ").append(getName());
    if (isUnlimited())
      buff.append(" = UNLIMITED;   // (").append(getLength()).append(" currently)");
    else if (isVariableLength())
      buff.append(" = UNKNOWN;" );
    else
      buff.append(" = ").append(getLength()).append(";");
    /* if (!strict && (getCoordinateVariables().size() > 0))
      buff.append("   // (has coord.var)"); */
    return buff.toString();
  }

  /**
   * Constructor
   * @param name name must be unique within group
   * @param length length of Dimension
   */
  public Dimension(String name, int length) {
    this.name = name;
    setLength(length);
  }

  /**
   * Constructor
   * @param name name must be unique within group
   * @param length length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   */
  public Dimension(String name, int length, boolean isShared) {
    this.name = name;
    this.isShared = isShared;
    setLength(length);
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
    //this.coordVars = new ArrayList<Variable>(from.coordVars);
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
    this.name = name;
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