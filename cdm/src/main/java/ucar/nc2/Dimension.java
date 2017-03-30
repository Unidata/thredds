/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import ucar.nc2.util.Indent;

import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A Dimension is used to define the array shape of a Variable.
 * It may be shared among Variables, which provides a simple yet powerful way of associating Variables.
 * When a Dimension is shared, it has a unique name within its Group.
 * It may have a coordinate Variable, which gives each index a coordinate value.
 * A private Dimension cannot have a coordinate Variable, so use shared dimensions with coordinates when possible.
 * The Dimension length must be > 0, except for an unlimited dimension which may have length = 0, and a vlen
 * Dimension which has length = -1.
 * <p/>
 * <p> Immutable if setImmutable() was called, except for an Unlimited Dimension, whose size can change.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 *
 * @author caron
 */

public class Dimension extends CDMNode implements Comparable {
  /**
   * A variable-length dimension: the length is not known until the data is read.
   */
  static public Dimension VLEN = new Dimension("*", -1, false, false, true).setImmutable(); // for Sequences, HDF5 VarLength

  /* static public String makeDimensionList(List<Dimension> dimList) {
    StringBuilder out = new StringBuilder();
    for (Dimension dim : dimList)
      out.append(dim.getShortName()).append(" ");
    return out.toString();
  } */

  /**
   * Make a space-delineated String from a list of Dimension names.
   * Inverse of makeDimensionsList().
   *
   * @return space-delineated String of Dimension names.
   */
  static public String makeDimensionsString(List<Dimension> dimensions) {
    if (dimensions == null) return "";

    Formatter buf = new Formatter();
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension myd = dimensions.get(i);
      String dimName = myd.getShortName();

      if (i != 0) buf.format(" ");

      if (myd.isVariableLength()) {
        buf.format("*");
      } else if (myd.isShared()) {
        buf.format("%s", dimName);
      } else {
        //if (dimName != null)          // LOOK losing anon dim name
        //  buf.format("%s=", dimName);
        buf.format("%d", myd.getLength());
      }
    }
    return buf.toString();
  }

  /**
   * Create a dimension list using the dimensions names. The dimension is searched for recursively in the parent groups, so it must already have been added.
   * Inverse of makeDimensionsString().
   *
   * @param parentGroup containing group, may not be null
   * @param dimString   : whitespace separated list of dimension names, or '*' for Dimension.UNKNOWN, or number for anon dimension. null or empty String is a scalar.
   * @return list of dimensions
   * @throws IllegalArgumentException if cant find dimension or parse error.
   */
  static public List<Dimension> makeDimensionsList(Group parentGroup, String dimString) throws IllegalArgumentException {

    List<Dimension> newDimensions = new ArrayList<>();

    if ((dimString == null) || (dimString.length() == 0)) { // scalar
      return newDimensions; // empty list
    }

    StringTokenizer stoke = new StringTokenizer(dimString);
    while (stoke.hasMoreTokens()) {
      String dimName = stoke.nextToken();
      Dimension d = dimName.equals("*") ? Dimension.VLEN : parentGroup.findDimension(dimName);
      if (d == null) {
        // if numeric - then its anonymous dimension
        try {
          int len = Integer.parseInt(dimName);
          d = new Dimension(null, len, false, false, false);
        } catch (Exception e) {
          throw new IllegalArgumentException("Dimension " + dimName + " does not exist");
        }
      }
      newDimensions.add(d);
    }

    return newDimensions;
  }

  static public List<Dimension> makeDimensionsAnon(int[] shape) {

    List<Dimension> newDimensions = new ArrayList<>();

    if ((shape == null) || (shape.length == 0)) { // scalar
      return newDimensions; // empty list
    }

    for (int s : shape)
      newDimensions.add(new Dimension(null, s, false, false, false));

    return newDimensions;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private boolean isUnlimited = false;
  private boolean isVariableLength = false;
  private boolean isShared = true; // shared means its in a group dimension list.
  private int length;

  /**
   * Returns the name of this Dimension; may be null.
   * A Dimension with a null name is called "anonymous" and must be private.
   * Dimension names are unique within a Group.
   * @return name of Dimension, may be null for anonymous dimension
   */
  /* @Obsolete
  public String getName() { return getShortName(); }
  */

  /**
   * Get the length of the Dimension.
   *
   * @return length of Dimension
   */
  public int getLength() {
    return length;
  }

  /**
   * If unlimited, then the length can increase; otherwise it is immutable.
   *
   * @return if its an "unlimited" Dimension
   */
  public boolean isUnlimited() {
    return isUnlimited;
  }

  /**
   * If variable length, then the length is unknown until the data is read.
   *
   * @return if its a "variable length" Dimension.
   */
  public boolean isVariableLength() {
    return isVariableLength;
  }

  /**
   * If this Dimension is shared, or is private to a Variable.
   * All Dimensions in NetcdfFile.getDimensions() or Group.getDimensions() are shared.
   * Dimensions in the Variable.getDimensions() may be shared or private.
   *
   * @return if its a "shared" Dimension.
   */
  public boolean isShared() {
    return isShared;
  }


  /**
   * Get the Group that owns this Dimension.
   *
   * @return owning group or null if !isShared
   */
  public Group getGroup() {
    return getParentGroup();
  }

  public String makeFullName() {
    return super.getFullName();
/*    Group g = getGroup();
    if (((g == null) || g.isRoot())) return getShortName();
    return g.getFullName() +"/" + this.getShortName();
*/
  }

  /**
   * Instances which have same contents are equal.
   * Careful!! this is not object identity !!
   */
  @Override
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof Dimension)) return false;
    Dimension other = (Dimension) oo;
    Group g = getGroup();
    if ((g != null) && !g.equals(other.getGroup())) return false;
    if ((getShortName() == null) && (other.getShortName() != null)) return false;
    if ((getShortName() != null) && !getShortName().equals(other.getShortName())) return false;
    return (getLength() == other.getLength()) &&
            (isUnlimited() == other.isUnlimited()) &&
            (isVariableLength() == other.isVariableLength()) &&
            (isShared() == other.isShared());
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      Group g = getGroup();
      if (g != null) result += 37 * result + g.hashCode();
      if (null != getShortName()) result += 37 * result + getShortName().hashCode();
      result += 37 * result + getLength();
      result += 37 * result + (isUnlimited() ? 0 : 1);
      result += 37 * result + (isVariableLength() ? 0 : 1);
      result += 37 * result + (isShared() ? 0 : 1);
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode = 0;

  @Override
  public void hashCodeShow(Indent indent) {
    System.out.printf("%sDim hash = %d%n", indent, hashCode());
    System.out.printf("%s shortName '%s' = %d%n", indent, getShortName(), getShortName() == null ? -1 : getShortName().hashCode());
    System.out.printf("%s getLength %s%n", indent, getLength());
    System.out.printf("%s isUnlimited %s%n", indent, isUnlimited());
    System.out.printf("%s isVariableLength %s%n", indent, isVariableLength());
    System.out.printf("%s isShared %s%n", indent, isShared());
    if (getGroup() != null)
      System.out.printf("%s parentGroup %s = %d%n", indent, getGroup(), getGroup().hashCode());
  }


  /**
   * CDL representation, not strict.
   */
  @Override
  public String toString() {
    return writeCDL(false);
  }

  /**
   * Dimensions with the same name are equal. This method is inconsistent with equals()!
   *
   * @param o compare to this Dimension
   * @return 0, 1, or -1
   */
  public int compareTo(Object o) {
    Dimension odim = (Dimension) o;
    String name = getShortName();
    return name.compareTo(odim.getShortName());
  }

  /**
   * CDL representation.
   *
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(2), strict);
    return f.toString();
  }

  protected void writeCDL(Formatter out, Indent indent, boolean strict) {
    String name = strict ? NetcdfFile.makeValidCDLName(getShortName()) : getShortName();
    out.format("%s%s", indent, name);
    if (isUnlimited())
      out.format(" = UNLIMITED;   // (%d currently)", getLength());
    else if (isVariableLength())
      out.format(" = UNKNOWN;");
    else
      out.format(" = %d;", getLength());

      /* { if (strict) {
        if (name.length() == 0) buff.append(getLength()); // CDL doesnt allow anon dimensions?
      } else {
        if (name.length() > 0) buff.append(" = "); // skip for anon dimensions
        buff.append(getLength());
      }
      buff.append(";");
    } */
  }

  /**
   * Constructor
   *
   * @param name   name must be unique within group
   * @param length length of Dimension
   */
  public Dimension(String name, int length) {
    this(name, length, true, false, false);
  }

  /**
   * Constructor
   *
   * @param name     name must be unique within group
   * @param length   length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared whether its shared or local to Variable.
   */
  public Dimension(String name, int length, boolean isShared) {
    this(name, length, isShared, false, false);
  }

  /**
   * Constructor
   *
   * @param name             name must be unique within group. Can be null only if not shared.
   * @param length           length, or UNLIMITED.length or UNKNOWN.length
   * @param isShared         whether its shared or local to Variable.
   * @param isUnlimited      whether the length can grow.
   * @param isVariableLength whether the length is unknown until the data is read.
   */
  public Dimension(String name, int length, boolean isShared, boolean isUnlimited, boolean isVariableLength) {
    super(name);
    this.isShared = isShared;
    this.isUnlimited = isUnlimited;
    this.isVariableLength = isVariableLength;
    if (isVariableLength && (isUnlimited || isShared))
      throw new IllegalArgumentException("variable length dimension cannot be shared or unlimited");
    setLength(length);
    assert (name != null) || !this.isShared;
  }

  /**
   * Copy Constructor. used to make global dimensions
   *
   * @param name name must be unique within group. Can be null only if not shared.
   * @param from copy all other fields from here.
   */
  public Dimension(String name, Dimension from) {
    super(name);
    this.length = from.length;
    this.isUnlimited = from.isUnlimited;
    this.isVariableLength = from.isVariableLength;
    this.isShared = from.isShared;
  }

  ///////////////////////////////////////////////////////////
  // the following make this mutable

  /**
   * Set whether this is unlimited, meaning length can increase.
   *
   * @param b true if unlimited
   */
  public void setUnlimited(boolean b) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.isUnlimited = b;
    setLength(this.length); // check legal
  }

  /**
   * Set whether the length is variable.
   *
   * @param b true if variable length
   */
  public void setVariableLength(boolean b) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.isVariableLength = b;
    if (b) {
      this.isShared = false;
      this.isUnlimited = false;
    }
    setLength(this.length); // check legal
  }

  /**
   * Set whether this is shared.
   *
   * @param b true if shared
   */
  public void setShared(boolean b) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.isShared = b;
    hashCode = 0;
  }

  /**
   * Set the Dimension length.
   *
   * @param n length of Dimension
   */
  public void setLength(int n) {
    if (immutable && !isUnlimited) throw new IllegalStateException("Cant modify");
    if (isVariableLength) {
      if (n != -1)
        throw new IllegalArgumentException("VariableLength Dimension length =" + n + " must be -1");
    } else if (isUnlimited) {
      if (n < 0)
        throw new IllegalArgumentException("Unlimited Dimension length =" + n + " must >= 0");
    } else {
      if (n < 1)
        throw new IllegalArgumentException("Dimension length =" + n + " must be > 0");
    }
    this.length = n;
    hashCode = 0;
  }


  /**
   * Set the name, converting to valid CDM object name if needed.
   *
   * @param name set to this value
   * @return valid CDM object name
   */
  public String setName(String name) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (name.length() == 0) name = null;
    setShortName(name);
    hashCode = 0;
    return getShortName();
  }

  /**
   * Set the group
   *
   * @param g parent group
   */
  public void setGroup(Group g) {
    if (immutable) throw new IllegalStateException("Cant modify");
    setParentGroup(g);
    hashCode = 0;
  }


  /**
   * Make this immutable.
   *
   * @return this
   */
  public Dimension setImmutable() {
    immutable = true;
    //coordVars = Collections.unmodifiableList(coordVars);
    return this;
  }

}
