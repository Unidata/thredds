// $Id:Dimension.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import java.util.ArrayList;
import java.util.List;

/**
 * A Dimension is used to define the array shape of a Variable.
 * It may be shared among Variables, which provides a simple yet powerful way of associating Variables.
 * When a Dimension is shared, it has a unique name within its Group.
 * It may have a coordinate Variable, which gives each index a coordinate value.
 * A private Dimension cannot have a coordinate Variable, so use shared dimensions with coordinates when possible.
 *
 * <p> Dimensions are considered immutable : do not change them after they have been constructed. Only the
 * Unlimited Dimension can grow.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class Dimension implements Comparable {
  static public Dimension UNLIMITED = new Dimension( "**", 0, true, true, false); // gets reset when file is read or written
  static public Dimension UNKNOWN = new Dimension( "*", -1, true, false, true); // for Sequences

  protected ArrayList coordVars = new ArrayList();
  protected boolean isUnlimited = false;
  protected boolean isVariableLength = false;
  protected boolean isShared = true; // shared means its in a group dimension list.
  protected String name;
  protected int length;

  /**
   * Returns the name of this Dimension; may be null.
   * A Dimension with a null name is called "anonymous" and must be private.
   * Dimension names are unique within a Group.
   */
  public String getName() { return name; }

  /**
   * Get the length of the Dimension.
   */
  public int getLength() { return length; }

  /**
   * If unlimited, then the length can increase; otherwise it is immutable.
   */
  public boolean isUnlimited() { return isUnlimited; }

  /**
   * If variable length, then the length is unknown until the data is read.
   */
  public boolean isVariableLength() { return isVariableLength; }

  /**
   * If this Dimension is shared, or is private to a Variable.
   * All Dimensions in NetcdfFile.getDimensions() or Group.getDimensions() are shared.
   * Dimensions in the Variable.getDimensions() may be shared or private.
   */
  public boolean isShared() { return isShared; }

  /**
   * Get the coordinate variables or coordinate variable aliases if the dimension has any, else return an empty list.
   * A coordinate variable has this as its single dimension, and names this Dimensions's the coordinates.
   * A coordinate variable alias is the same as a coordinate variable, but its name must match the dimension name.
   * If numeric, coordinate axis must be strictly monotonically increasing or decreasing.
   * @return List of Variable
   * @see Variable#getCoordinateDimension
   */
  public List getCoordinateVariables() { return coordVars; }

  /**
   * Instances which have same contents are equal.
   * Careful!! this is not object identity !!
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Dimension)) return false;
    Dimension other = (Dimension) oo;
    return (getLength() == other.getLength()) &&
           (getName().equals(other.getName())) &&
           (isUnlimited() == other.isUnlimited()) &&
           (isVariableLength() == other.isVariableLength()) &&
           (isShared() == other.isShared());
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
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

  /** String representation. */
  public String writeCDL(boolean strict) {
    StringBuffer buff = new StringBuffer();
    buff.append("   "+getName());
    if (isUnlimited())
      buff.append(" = UNLIMITED;   // ("+getLength()+" currently)");
    else if (isVariableLength())
      buff.append(" = UNKNOWN;" );
    else
      buff.append(" = "+getLength() +";");
    if (!strict && (getCoordinateVariables().size() > 0))
      buff.append("   // (has coord.var)");
    return buff.toString();
  }

  /**
   * Constructor
   * @param name name must be unique within group
   * @param length length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   */
  public Dimension(String name, int length, boolean isShared) {
    this.name = name;
    this.length = length;
    this.isShared = isShared;
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
    this.length = length;
    this.isShared = isShared;
    this.isUnlimited = isUnlimited;
    this.isVariableLength = isVariableLength;
  }

  /** Copy Constructor
   used to make global dimensions */
  public Dimension(String name, Dimension from) {
    this.name = name;
    this.length = from.length;
    this.isUnlimited = from.isUnlimited;
    this.isVariableLength = from.isVariableLength;
    this.isShared = from.isShared;
    this.coordVars = from.coordVars;
  }

  /** Add a coordinate variable or coordinate variable alias.
   * Remove previous if matches full name. */
  public void addCoordinateVariable( Variable v) {
    for (int i = 0; i < coordVars.size(); i++) {
      Variable cv = (Variable) coordVars.get(i);
      if (v.getName().equals(cv.getName())) {
        coordVars.remove(cv);
        break;
      }
    }
    coordVars.add(v);
  }
  /** Set whether this is unlimited, meaning length can increase. */
  public void setUnlimited( boolean b) {
    isUnlimited = b;
    hashCode = 0;
  }
  /** Set whether the length is variable. */
  public void setVariableLength( boolean b) {
    isVariableLength = b;
    hashCode = 0;
  }
  /** Set whether this is shared. */
  public void setShared( boolean b) {
    isShared = b;
    hashCode = 0;
  }
  /** Set the Dimension length. */
  public void setLength( int n) {
    this.length = n;
    hashCode = 0;
  }
  /** rename */
  public void setName( String name) {
    this.name = name;
    hashCode = 0;
  }

}