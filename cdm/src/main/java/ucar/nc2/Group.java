/*
 * Copyright 1998-2017 University Corporation for Atmospheric Research/Unidata
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

import ucar.ma2.DataType;
import ucar.nc2.util.Indent;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Collections;

/**
 * A Group is a logical collection of Variables.
 * The Groups in a Dataset form a hierarchical tree, like directories on a disk.
 * A Group has a name and optionally a set of Attributes.
 * There is always at least one Group in a dataset, the root Group, whose name is the empty string.
 * <p> Immutable if setImmutable() was called.
 *
 * @author caron
 */
public class Group extends CDMNode implements AttributeContainer {

  static List<Group> collectPath(Group g) {
    List<Group> list = new ArrayList<>();
    while (g != null) {
      list.add(0, g);
      g = g.getParentGroup();
    }
    return list;
  }

  protected NetcdfFile ncfile;
  protected List<Variable> variables = new ArrayList<>();
  protected List<Dimension> dimensions = new ArrayList<>();
  protected List<Group> groups = new ArrayList<>();
  protected List<Attribute> attributes = new ArrayList<>();
  protected List<Attribute> specials = new ArrayList<>();
  protected List<EnumTypedef> enumTypedefs = new ArrayList<>();
  private int hashCode = 0;

  /**
   * Get the full name, starting from the root Group.
   *
   * @return group full name
   */
/* see CDMNode.getFullName()
  public String getFullName() {
    String name = getShortName();
    Group parent = getParentGroup();
    if(parent == null) // we are the root group
	return name;
    else if(parent == ncfile.getRootGroup()) // we are just below root group
	return name; // this does not seem right; should it not return /name?
    else
	return parent.getFullName() + "/" + name;
  }
*/

  /**
   * Alias for getFullName
   * Deprecated because it is unclear
   * to the caller if it is short or fullname.
   *
   * @return group full name
   */
/*
   @Deprecated
   public String getName() {
    return getFullName();
   }
*/
 
  /**
   * Is this the root group?
   *
   * @return true if root group
   */
  public boolean isRoot() {
    return getParentGroup() == null;
  }

  /**
   * Get the "short" name, unique within its parent Group.
   *
   * @return group short name
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Get the Variables contained directly in this group.
   *
   * @return List of type Variable; may be empty, not null.
   */
  public java.util.List<Variable> getVariables() {
    return variables;
  }

  /**
   * Find the Variable with the specified (short) name in this group.
   *
   * @param varShortName short name of Variable within this group.
   * @return the Variable, or null if not found
   */
  public Variable findVariable(String varShortName) {
    if (varShortName == null) return null;

    for (Variable v : variables) {
      if (varShortName.equals(v.getShortName()))
        return v;
    }
    return null;
  }

  /*
   * Find the Variable with the specified escaped (short) name in this group.
   * @param varShortNameEscaped escaped short name of Variable within this group.
   * @return the Variable, or null if not found
   * @see NetcdfFile#escapeName
   * @see NetcdfFile#unescapeName
   *
  public Variable findVariableEscaped(String varShortNameEscaped) {
    if (varShortNameEscaped == null) return null;
    return findVariable( NetcdfFile.makeNameUnescaped(varShortNameEscaped));
  } */

  /**
   * Find the Variable with the specified (short) name in this group or a parent group.
   *
   * @param varShortName short name of Variable.
   * @return the Variable, or null if not found
   */
  public Variable findVariableOrInParent(String varShortName) {
    if (varShortName == null) return null;

    Variable v = findVariable(varShortName);
    Group parent = getParentGroup();
    if ((v == null) && (parent != null))
      v = parent.findVariableOrInParent(varShortName);
    return v;
  }

  /**
   * Get the Groups contained directly in this Group.
   *
   * @return List of type Group; may be empty, not null.
   */
  public java.util.List<Group> getGroups() {
    return groups;
  }

  /**
   * Get the owning NetcdfFile
   *
   * @return owning NetcdfFile.
   */
  public NetcdfFile getNetcdfFile() {
    return ncfile;
  }

  /**
   * Retrieve the Group with the specified (short) name.
   *
   * @param groupShortName short name of the nested group you are looking for.
   * @return the Group, or null if not found
   */
  public Group findGroup(String groupShortName) {
    if (groupShortName == null) return null;
    // groupShortName = NetcdfFile.makeNameUnescaped(groupShortName);

    for (Group group : groups) {
      if (groupShortName.equals(group.getShortName()))
        return group;
    }

    return null;
  }

  /**
   * Get the Dimensions contained directly in this group.
   *
   * @return List of type Dimension; may be empty, not null.
   */
  public java.util.List<Dimension> getDimensions() {
    return dimensions;
  }

  /**
   * Get the enumerations contained directly in this group.
   *
   * @return List of type EnumTypedef; may be empty, not null.
   */
  public java.util.List<EnumTypedef> getEnumTypedefs() {
    return enumTypedefs;
  }

  /**
   * Retrieve a Dimension using its (short) name. If it doesnt exist in this group,
   * recursively look in parent groups.
   *
   * @param name Dimension name.
   * @return the Dimension, or null if not found
   */
  public Dimension findDimension(String name) {
    if (name == null) return null;
    // name = NetcdfFile.makeNameUnescaped(name);
    Dimension d = findDimensionLocal(name);
    if (d != null) return d;
    Group parent = getParentGroup();
    if (parent != null)
      return parent.findDimension(name);

    return null;
  }

  /**
   * Retrieve a Dimension using its (short) name, in this group only
   *
   * @param name Dimension name.
   * @return the Dimension, or null if not found
   */
  public Dimension findDimensionLocal(String name) {
    if (name == null) return null;
    // name =  NetcdfFile.makeNameUnescaped(name);
    for (Dimension d : dimensions) {
      if (name.equals(d.getShortName()))
        return d;
    }

    return null;
  }

  /**
   * Get the set of attributes contained directly in this Group.
   *
   * @return immutable List of type Attribute; may be empty, not null.
   */
  public java.util.List<Attribute> getAttributes() {
    return immutable ? attributes : Collections.unmodifiableList(attributes);
  }

  /**
   * Add all; replace old if has same name
   */
  public void addAll(Iterable<Attribute> atts) {
    for (Attribute att : atts) addAttribute(att);
  }

  /**
   * Find an Attribute in this Group by its name.
   *
   * @param name the name of the attribute.
   * @return the attribute, or null if not found
   */
  public Attribute findAttribute(String name) {
    if (name == null) return null;
    // name = NetcdfFile.makeNameUnescaped(name);
    for (Attribute a : attributes) {
      if (name.equals(a.getShortName()))
        return a;
    }
    if(name.startsWith(Attribute.SPECIALPREFIX)) {
      for (Attribute a : specials) {
        if (name.equals(a.getShortName()))
          return a;
      }
    }
    return null;
  }

  /**
   * Find an Attribute in this Group by its name, ignore case.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    if (name == null) return null;
    //name =  NetcdfFile.makeNameUnescaped(name);
    for (Attribute a : attributes) {
      if (name.equalsIgnoreCase(a.getShortName()))
        return a;
    }
    return null;
  }

  /**
   * Find an Enumeration Typedef using its (short) name. If it doesnt exist in this group,
   * recursively look in parent groups.
   *
   * @param name Enumeration name.
   * @return the Enumeration, or null if not found
   */
  public EnumTypedef findEnumeration(String name) {
    if (name == null) return null;
    // name =  NetcdfFile.makeNameUnescaped(name);
    for (EnumTypedef d : enumTypedefs) {
      if (name.equals(d.getShortName()))
        return d;
    }
    Group parent = getParentGroup();
    if (parent != null)
      return parent.findEnumeration(name);

    return null;
  }

  /**
   * Get the common parent of this and the other group.
   * Cant fail, since the root group is always a parent of any 2 groups.
   *
   * @param other the other group
   * @return common parent of this and the other group
   */
  public Group commonParent(Group other) {
    if (isParent(other)) return this;
    if (other.isParent(this)) return other;
    while (!other.isParent(this))
      other = other.getParentGroup();
    return other;
  }

  /**
   * Is this a parent of the other Group?
   *
   * @param other another Group
   * @return true is it is equal or a parent
   */
  public boolean isParent(Group other) {
    while ((other != this) && (other.getParentGroup() != null))
      other = other.getParentGroup();
    return (other == this);
  }

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get String with name and attributes. Used in short descriptions like tooltips.
   *
   * @return name and attributes String.
   */
  public String getNameAndAttributes() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Group ");
    sbuff.append(getShortName());
    sbuff.append("\n");
    for (Attribute att : attributes) {
      sbuff.append("  ").append(getShortName()).append(":");
      sbuff.append(att.toString());
      sbuff.append(";");
      sbuff.append("\n");
    }
    return sbuff.toString();
  }

  /**
   * CDL representation.
   *
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    Formatter buf = new Formatter();
    writeCDL(buf, new Indent(2), strict);
    return buf.toString();
  }

  protected void writeCDL(Formatter out, Indent indent, boolean strict) {
    boolean hasE = (enumTypedefs.size() > 0);
    boolean hasD = (dimensions.size() > 0);
    boolean hasV = (variables.size() > 0);
    // boolean hasG = (groups.size() > 0);
    boolean hasA = (attributes.size() > 0);

    if (hasE) {
      out.format("%stypes:%n", indent);
      indent.incr();
      for (EnumTypedef e : enumTypedefs) {
        e.writeCDL(out, indent, strict);
        out.format("%n");
      }
      indent.decr();
      out.format("%n");
    }

    if (hasD) {
      out.format("%sdimensions:%n", indent);
      indent.incr();
      for (Dimension myd : dimensions) {
        myd.writeCDL(out, indent, strict);
        out.format("%n");
      }
      indent.decr();
    }

    if (hasV) {
      out.format("%svariables:%n", indent);
      indent.incr();
      for (Variable v : variables) {
        v.writeCDL(out, indent, false, strict);
        out.format("%n");
      }
      indent.decr();
    }

    for (Group g : groups) {
      String gname = strict ? NetcdfFile.makeValidCDLName(g.getShortName()) : g.getShortName();
      out.format("%n%sgroup: %s {%n", indent, gname);
      indent.incr();
      g.writeCDL(out, indent, strict);
      indent.decr();
      out.format("%s}%n", indent);
    }

    //if (hasA && (hasE || hasD || hasV || hasG))
    //  out.format("%n");

    if (hasA) {
      if (isRoot())
        out.format("%s// global attributes:%n", indent);
      else
        out.format("%s// group attributes:%n", indent);
      //indent.incr();
      for (Attribute att : attributes) {
        //String name = strict ? NetcdfFile.escapeNameCDL(getShortName()) : getShortName();
        out.format("%s:", indent);
        att.writeCDL(out, strict);
        out.format(";");
        if (!strict && (att.getDataType() != DataType.STRING)) out.format(" // %s", att.getDataType());
        out.format("%n");
      }
      //indent.decr();
    }

  }


  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   *
   * @param ncfile    NetcdfFile owns this Group
   * @param parent    parent of Group. If null, this is the root Group.
   * @param shortName short name of Group.
   */
  public Group(NetcdfFile ncfile, Group parent, String shortName) {
    super(shortName);    
    this.ncfile = ncfile;
    setParentGroup(parent == null ? ncfile.getRootGroup() : parent);
  }

  /**
   * Set the Group's parent Group
   *
   * @param parent parent group.
   */
  public void setParentGroup(Group parent) {
    if (immutable) throw new IllegalStateException("Cant modify");
    super.setParentGroup(parent == null ? ncfile.getRootGroup() : parent);
  }


  /**
   * Set the short name, converting to valid CDM object name if needed.
   *
   * @param shortName set to this value
   * @return valid CDM object name
   */
  public String setName(String shortName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    setShortName(shortName);
    return getShortName();
  }

  /**
   * Add new Attribute; replace old if has same name.
   *
   * @param att add this Attribute.
   */
  public Attribute addAttribute(Attribute att) {
    if (immutable) throw new IllegalStateException("Cant modify");
    List<Attribute> container = this.attributes;
    if(Attribute.isspecial(att))
	container = this.specials;
    for (int i = 0; i < container.size(); i++) {
      Attribute a = container.get(i);
      if (att.getShortName().equals(a.getShortName())) {
        container.set(i, att); // replace
        return att;
      }
    }
    container.add(att);
    return att;
  }

  /**
   * Add a shared Dimension
   *
   * @param d add this Dimension
   */
  public void addDimension(Dimension d) {
    if (immutable) throw new IllegalStateException("Cant modify");

    if (findDimensionLocal(d.getShortName()) != null)
      throw new IllegalArgumentException("Dimension name (" + d.getShortName() + ") must be unique within Group " + getShortName());

    dimensions.add(d);
    d.setGroup(this);
  }

  public boolean addDimensionIfNotExists(Dimension d) {
    if (immutable) throw new IllegalStateException("Cant modify");

    if (findDimensionLocal(d.getShortName()) != null)
      return false;

    dimensions.add(d);
    d.setGroup(this);
    return true;
  }

  /**
   * Add a nested Group
   *
   * @param g add this Group.
   */
  public void addGroup(Group g) {
    if (immutable) throw new IllegalStateException("Cant modify");

    if (findGroup(g.getShortName()) != null)
      throw new IllegalArgumentException("Group name (" + g.getShortName() + ") must be unique within Group " + getShortName());

    groups.add(g);
    g.setParentGroup(this); // groups are a tree - only one parent
  }

  /**
   * Add an Enumeration
   *
   * @param e add this Enumeration.
   */
  public void addEnumeration(EnumTypedef e) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (e == null) return;
    e.setParentGroup(this);
    enumTypedefs.add(e);
  }

  /**
   * Add a Variable
   *
   * @param v add this Variable.
   */
  public void addVariable(Variable v) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (v == null) return;

    if (findVariable(v.getShortName()) != null) {
      //Variable other = findVariable(v.getShortName()); // debug
      throw new IllegalArgumentException("Variable name (" + v.getShortName() + ") must be unique within Group " + getShortName());
    }

    variables.add(v);
    v.setParentGroup(this); // variable can only be in one group
  }

  /**
   * Remove an Attribute : uses the attribute hashCode to find it.
   *
   * @param a remove this Attribute.
   * @return true if was found and removed
   */
  public boolean remove(Attribute a) {
    if (immutable) throw new IllegalStateException("Cant modify");
    return a != null && attributes.remove(a);
  }

  /**
   * Remove an Dimension : uses the dimension hashCode to find it.
   *
   * @param d remove this Dimension.
   * @return true if was found and removed
   */
  public boolean remove(Dimension d) {
    if (immutable) throw new IllegalStateException("Cant modify");
    return d != null && dimensions.remove(d);
  }

  /**
   * Remove an Attribute : uses the Group hashCode to find it.
   *
   * @param g remove this Group.
   * @return true if was found and removed
   */
  public boolean remove(Group g) {
    if (immutable) throw new IllegalStateException("Cant modify");
    return g != null && groups.remove(g);
  }

  /**
   * Remove a Variable : uses the variable hashCode to find it.
   *
   * @param v remove this Variable.
   * @return true if was found and removed
   */
  public boolean remove(Variable v) {
    if (immutable) throw new IllegalStateException("Cant modify");
    return v != null && variables.remove(v);
  }

  /**
   * remove a Dimension using its name, in this group only
   *
   * @param dimName Dimension name.
   * @return true if dimension found and removed
   */
  public boolean removeDimension(String dimName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension d = dimensions.get(i);
      if (dimName.equals(d.getShortName())) {
        dimensions.remove(d);
        return true;
      }
    }
    return false;
  }

  /**
   * remove a Variable using its (short) name, in this group only
   *
   * @param shortName Variable name.
   * @return true if Variable found and removed
   */
  public boolean removeVariable(String shortName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    for (int i = 0; i < variables.size(); i++) {
      Variable v = variables.get(i);
      if (shortName.equals(v.getShortName())) {
        variables.remove(v);
        return true;
      }
    }
    return false;
  }

  /**
   * Make this immutable.
   *
   * @return this
   */
  public Group setImmutable() {
    super.setImmutable(true);
    variables = Collections.unmodifiableList(variables);
    dimensions = Collections.unmodifiableList(dimensions);
    groups = Collections.unmodifiableList(groups);
    attributes = Collections.unmodifiableList(attributes);
    return this;
  }

  @Override
  public String toString() {
    return writeCDL(false);
  }

  /**
   * Instances which have same name and parent are equal.
   */
  @Override
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof Group)) return false;
    Group og = (Group) oo;
    if (!getShortName().equals(og.getShortName()))
      return false;
    if ((getParentGroup() != null) && !getParentGroup().equals(og.getParentGroup()))
      return false;
    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getShortName().hashCode();
      if (getParentGroup() != null)
        result = 37 * result + getParentGroup().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  public void hashCodeShow(Indent indent) {
    System.out.printf("%sGroup hash = %d%n", indent, hashCode());
    System.out.printf("%s shortName %s = %d%n", indent, getShortName(), getShortName().hashCode());
    System.out.printf("%s parentGroup %s = %d%n", indent, getParentGroup(), getParentGroup().hashCode());
  }

  /**
   * Create groups to ensure path is defined
   *
   * @param ncf        the containing netcdf file object
   * @param path       the path to the desired group
   * @param ignorelast true => ignore last element in the path
   * @return the Group, or null if not found
   */
  public Group makeRelativeGroup(NetcdfFile ncf, String path, boolean ignorelast) {
    path = path.trim();
    path = path.replace("//", "/");
    boolean isabsolute = (path.charAt(0) == '/');
    if (isabsolute)
      path = path.substring(1);

    // iteratively create path
    String pieces[] = path.split("/");
    if (ignorelast) pieces[pieces.length - 1] = null;

    Group current = (isabsolute ? ncfile.getRootGroup() : this);
    for (String name : pieces) {
      if (name == null) continue;
      String clearname = NetcdfFile.makeNameUnescaped(name);  //??
      Group next = current.findGroup(clearname);
      if (next == null) {
        next = new Group(ncf, current, clearname);
        current.addGroup(next);
      }
      current = next;
    }
    return current;
  }
}
