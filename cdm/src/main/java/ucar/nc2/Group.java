// $Id$
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

import ucar.ma2.DataType;

import java.util.ArrayList;
import java.io.PrintStream;

/**
 * A Group is a logical collection of Variables.
 * The Groups in a Dataset form a hierarchical tree, like directories on a disk.
 * A Group has a name and optionally a set of Attributes.
 * There is always at least one Group in a dataset, the root Group, whose name is the empty string.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class Group {
  protected NetcdfFile ncfile;
  protected Group parent;
  protected String name;
  protected String shortName;
  protected ArrayList variables = new ArrayList();
  protected ArrayList dimensions = new ArrayList();
  protected ArrayList groups = new ArrayList();
  protected ArrayList attributes = new ArrayList();

   /** Get the full name, starting from the root Group. */
  public String getName() { return name; }

   /** Get the "short" name, unique within its parent Group. */
  public String getShortName() { return shortName; }

   /** Get its parent Group, or null if its the root group. */
  public Group getParentGroup() { return parent; }

  /** Get the Variables contained directly in this group.
   * @return List of type Variable; may be empty, not null.
   */
  public java.util.List getVariables() { return new ArrayList(variables); }

  /**
   * Find the Variable with the specified (short) name in this group.
   * @param shortName short name of Variable within this group.
   * @return the Variable, or null if not found
   */
  public Variable findVariable(String shortName) {
    if (shortName == null) return null;

    for (int i=0; i<variables.size(); i++) {
      Variable v = (Variable) variables.get(i);
      if (shortName.equals(v.getShortName()))
        return v;
    }
    return null;
  }

  /**
   * Find the Variable with the specified (short) name in this group or a parent group.
   * @param shortName short name of Variable.
   * @return the Variable, or null if not found
   */
  public Variable findVariableRecurse(String shortName) {
    if (shortName == null) return null;

    Variable v = findVariable( shortName);
    if ((v == null) && (parent != null))
      v = parent.findVariableRecurse(shortName);
    return v;
  }

  /** Get the Groups contained directly in this Group.
   * @return List of type Group; may be empty, not null.
   */
  public java.util.List getGroups() { return new ArrayList(groups); }

  /**
   * Retrieve the Group with the specified (short) name.
   * @param shortName short name of the nested group you are looking for.
   * @return the Group, or null if not found
   */
  public Group findGroup(String shortName) {
    if ( shortName == null) return null;

    for (int i=0; i<groups.size(); i++) {
      Group g = (Group) groups.get(i);
      if (shortName.equals(g.getShortName()))
        return g;
    }

    return null;
  }

  /**
   * Get the Dimensions contained directly in this group.
   * @return List of type Dimension; may be empty, not null.
   */
  public java.util.List getDimensions() { return new ArrayList( dimensions); }

  /**
   * Retrieve a Dimension using its (short) name. If it doesnt exist in this group,
   *  recursively look in parent groups.
   * @param name Dimension name.
   * @return the Dimension, or null if not found
   */
  public Dimension findDimension(String name) {
    Dimension d = findDimensionLocal( name);
    if (d != null) return d;

    if (parent != null)
      return parent.findDimension( name);

    return null;
  }

  /**
   * Retrieve a Dimension using its (short) name, in this group only
   * @param name Dimension name.
   * @return the Dimension, or null if not found
   */
  public Dimension findDimensionLocal(String name) {
    for (int i=0; i<dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
      if (name.equals(d.getName()))
        return d;
    }

    return null;
  }

  /**
  * remove a Dimension using its name, in this group only
  * @param dimName Dimension name.
  * @return true if dimension found and removed
  */
 public boolean removeDimension(String dimName) {
   for (int i=0; i<dimensions.size(); i++) {
     Dimension d = (Dimension) dimensions.get(i);
     if (dimName.equals(d.getName())) {
       dimensions.remove(d);
       return true;
     }
   }
   return false;
 }

 /**
  * remove a Variable using its (short) name, in this group only
  * @param varName Variable name.
  * @return true if Variable found and removed
  */
 public boolean removeVariable(String varName) {
   for (int i=0; i<variables.size(); i++) {
     Variable v = (Variable) variables.get(i);
     if (varName.equals(v.getShortName())) {
       variables.remove(v);
       return true;
     }
   }
   return false;
 }

  /**
   * Get the set of attributes contained directly in this Group.
   * @return List of type Attribute; may be empty, not null.
   */
  public java.util.List getAttributes() { return new ArrayList(attributes); }

  /**
   * Find an Attribute in this Group by its name.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttribute(String name) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (name.equals(a.getName()))
        return a;
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
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (name.equalsIgnoreCase(a.getName()))
        return a;
    }
    return null;
  }

  //////////////////////////////////////////////////////////////////////////////////////

  /** Get String with name and attributes. Used in short descriptions like tooltips. */
  public String getNameAndAttributes() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("Group ");
    sbuff.append(getShortName());
    sbuff.append("\n");
    for (int i=0; i<attributes.size(); i++) {
      Attribute att = (Attribute) attributes.get(i);
      sbuff.append("  "+getShortName()+":");
      sbuff.append(att.toString());
      sbuff.append(";");
      sbuff.append("\n");
    }
    return sbuff.toString();
  }

  protected void toString(PrintStream out, String indent) {
    writeCDL(out, indent, false);
  }

  protected void writeCDL(PrintStream out, String indent, boolean strict) {
    boolean hasD = (dimensions.size() > 0);
    boolean hasV = (variables.size() > 0);
    boolean hasG = (groups.size() > 0);
    boolean hasA = (attributes.size() > 0);

    if (hasD)
      out.print(indent+" dimensions:\n");
    for (int i=0; i<dimensions.size(); i++) {
      Dimension myd = (Dimension) dimensions.get(i);
      out.print(indent+myd.writeCDL(strict));
      out.print(indent+"\n");
    }

    if (hasV)
      out.print(indent+" variables:\n");
    for (int i=0; i<variables.size(); i++) {
      Variable v = (Variable) variables.get(i);
      out.print( v.writeCDL(indent+"   ", false, strict));
    }

    for (int i=0; i<groups.size(); i++) {
      Group g = (Group) groups.get(i);
      out.print("\n "+indent+"Group "+g.getShortName()+" {\n");
      g.toString( out, indent+"  ");
      out.print(indent+" }\n");
    }

    if (hasA && (hasD || hasV || hasG))
      out.print("\n");
    for (int i=0; i<attributes.size(); i++) {
      Attribute att = (Attribute) attributes.get(i);
      out.print(indent+" "+getShortName()+":");
      out.print(att.toString());
      out.print(";");
      if (!strict && (att.getDataType() != DataType.STRING)) out.print(" // "+att.getDataType());
      out.print("\n");
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////
  /** Constructor */
  public Group(NetcdfFile ncfile, Group parent, String shortName) {
    this.ncfile = ncfile;
    this.parent = parent == null ? ncfile.getRootGroup() : parent ;
    this.shortName = shortName;
    this.name = (parent == null) ? shortName : parent.getName() + "/" + shortName;
  }

  /** Set the Group name */
  public void setName( String name) {
    this.name = name;
  }

  /**  Add new Attribute; replace old if has same name. */
  public void addAttribute(Attribute att) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (att.getName().equals(a.getName())) {
        attributes.set(i, att); // replace
        return;
      }
    }
    attributes.add( att);
  }

  /** Add a shared Dimension */
  public void addDimension( Dimension d) {
    dimensions.add( d);
  }

  /** Add a nested Group */
  public void addGroup( Group g) {
    groups.add( g);
    g.parent = this;
  }

  /** Add a Variable */
  public void addVariable( Variable v) {
    if (v == null) return;
    variables.add( v);
    v.setParentGroup( this);
  }

  /** Remove an Attribute : uses the attribute hashCode to find it.
   * @return true if was found and removed */
  public boolean remove( Attribute a) {
    if (a == null) return false;
    return attributes.remove( a);
  }

  /** Remove an Dimension : uses the dimension hashCode to find it.
   * @return true if was found and removed */
  public boolean remove( Dimension d) {
    if (d == null) return false;
    return dimensions.remove( d);
  }

  /** Remove an Attribute : uses the attribute hashCode to find it.
   * @return true if was found and removed */
  public boolean remove( Group g) {
    if (g == null) return false;
    return groups.remove( g);
  }

  /** Remove a Variable : uses the variable hashCode to find it.
   * @return true if was found and removed */
  public boolean remove( Variable v) {
    if (v == null) return false;
    return variables.remove( v);
  }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Variable)) return false;
    return hashCode() == oo.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      if (getParentGroup() != null)
        result = 37*result + getParentGroup().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;
}
