// $Id:Structure.java 51 2006-07-12 17:13:13Z caron $
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * A Structure is a type of Variable that contains other Variables, like a struct in C.
 *  A Structure can be scalar or multidimensional.
 *<p>
 * A call to structure.read() will read all of the data in a Structure,
 *   including nested structures, and returns an Array of StructureData, with all of the data in memory.
 * <p>
 * Generally, the programmer can assume that the data in a Structure are stored together,
 *  so that it is efficient to read an entire Structure, and then access the Variable data through the
 *  Arrays in the StructureData.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class Structure extends Variable {
  private static int defaultBufferSize = 500 * 1000; // bytes

  protected HashMap memberHash = new HashMap(); // Variable
  protected ArrayList members = new ArrayList(); // Variable
  protected ArrayList memberNames = new ArrayList(); // String

  /** Constructor */
  public Structure( NetcdfFile ncfile, Group group, Structure parent, String shortName) {
    super (ncfile, group, parent, shortName);
    this.dataType = DataType.STRUCTURE;
    this.elementSize = -1; // gotta wait before calculating
  }


  /** copy constructor;
   * @param from  copy from this
   * @param reparent : if true, reparent the members. if so, cant use 'from' anymore
   */
  protected Structure( Structure from, boolean reparent) {
    super( from);

    members = new ArrayList(from.members);
    memberNames = new ArrayList(from.memberNames);

    if (reparent) {
      for (int i=0; i<members.size(); i++) {
        Variable v = (Variable) members.get(i);
        v.setParentStructure( this);
      }
    }
  }

  protected int calcStructureSize() {
    int structureSize = 0;
    for (int i = 0; i < members.size(); i++) {
      Variable member = (Variable) members.get(i);
      structureSize += member.getSize() * member.getElementSize();
    }
    return structureSize;
  }

  /** Caching is not allowed */
  public boolean isCaching() {
    return false;
  }

  /** Caching is not allowed */
  public void setCaching(boolean caching) {
    this.cache.isCaching = false;
    this.cache.cachingSet = true;
  }

  /** Override so it returns a Structure */
  public Variable section(List section) throws InvalidRangeException  {
    Variable vs = new Structure( this, false);  // LOOK ??
    makeSection( vs, section);
    return vs;
  }

  /** Add a member variable */
  public void addMemberVariable( Variable v) {
    members.add( v);
    memberNames.add( v.getShortName());
    memberHash.put(  v.getShortName(), v);
    v.setParentStructure( this);
  }

  /** Set the list of member variables. */
  public void setMemberVariables( ArrayList vars) {
    members = new ArrayList();
    memberNames = new ArrayList();
    memberHash = new HashMap(2*vars.size());
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      members.add( v);
      memberNames.add( v.getShortName());
      memberHash.put(  v.getShortName(), v);
    }
  }

  /** Remove a Variable : uses the variable hashCode to find it.
   * @return true if was found and removed */
  public boolean removeMemberVariable( Variable v) {
    if (v == null) return false;
    memberNames.remove( v.getShortName());
    return members.remove( v);
  }

  /** Replace a Variable with another that has the same name : uses the variable name to find it.
   * If old Var is not found, just add the new one
   * @return true if was found and replaced */
  public boolean replaceMemberVariable( Variable newVar) {
    boolean found = false;
    for (int i = 0; i < members.size(); i++) {
      Variable v = (Variable) members.get(i);
      if (v.getShortName().equals( newVar.getShortName())) {
        members.set( i, newVar);
        found = true;
      }
    }

    if (!found)
      members.add( newVar);
    return found;
  }

  /** Set the parent group of this Structure, and all member variables. */
  public void setParentGroup(Group group) {
    super.setParentGroup(group);
    for (int i=0; i<members.size(); i++) {
      Variable v = (Variable) members.get(i);
      v.setParentGroup( group);
    }
 }

  /** Get the variables contained directly in this Structure.
   * @return List of type Variable.
   */
  public java.util.List getVariables() { return members; }

  /** Get the (short) names of the variables contained directly in this Structure.
   * @return List of type String.
   */
  public java.util.List getVariableNames() { return memberNames; }

  /**
   * Find the Variable member with the specified (short) name, or null if not found.
   */
  public Variable findVariable(String shortName) {
    if (shortName == null) return null;
    return (Variable) memberHash.get( shortName);
  }

  /**
   * Make the StructureMembers object corresponding to this Structure.
   */
  public StructureMembers makeStructureMembers() {
    if (smembers == null) {
      smembers = new StructureMembers( getName());
      java.util.Iterator viter = getVariables().iterator();
      while (viter.hasNext()) {
        Variable v2 = (Variable) viter.next();
        StructureMembers.Member m = new StructureMembers.Member( v2.getShortName(), v2.getDescription(),
            v2.getUnitsString(), v2.getDataType(), v2.getShape());
        if (v2 instanceof Structure)
          m.setStructureMembers( ((Structure)v2).makeStructureMembers());
        smembers.addMember( m);
      }
    }
    return smembers;
  }
  private StructureMembers smembers;

  /**
   * Get the size of one element of the Structure.
   * @return size (in bytes)
   */
  public int getElementSize() {
    if (elementSize == -1) calcElementSize();
    return elementSize;
  }

  protected void calcElementSize() {
    int total = 0;
    for (int i=0; i<members.size(); i++) {
      Variable v = (Variable) members.get(i);
      total += v.getElementSize() * v.getSize();
    }
    elementSize = total;
  }

  /**
   * Use this when this is a scalar Structure. Its the same as read(), but it extracts the single
   * StructureData out of the Array.
   */
  public StructureData readStructure() throws IOException {
    if (getRank() != 0) throw new java.lang.UnsupportedOperationException("not a scalar structure");
    Array dataArray = read();
    ArrayStructure data = (ArrayStructure) dataArray;
    return data.getStructureData(0);
  }

  /**
   * Use this when this is a one dimensional array of Structures. This will read only the ith structure,
   * and return the data as a StructureData object.
   * @return ith StructureData
   */
  public StructureData readStructure(int index) throws IOException, ucar.ma2.InvalidRangeException {
    if (getRank() > 1) throw new java.lang.UnsupportedOperationException("not a vector structure");
    int[] origin = new int[] {index};
    int[] shape = new int[] {1};
    Array dataArray = read(origin, shape);
    ArrayStructure data = (ArrayStructure) dataArray;
    return data.getStructureData(0);
  }

  /**
   * Use this when this is a one dimensional array of Structures.
   * @return start - start + count-1 StructureData records in an ArrayStructure
   */
  public ArrayStructure readStructure(int start, int count) throws IOException, ucar.ma2.InvalidRangeException {
    if (getRank() != 1) throw new java.lang.UnsupportedOperationException("not a vector structure");
    int[] origin = new int[] {start};
    int[] shape = new int[] {count};
    if (NetcdfFile.debugStructureIterator)
      System.out.println("readStructure "+start+" "+count);
    return (ArrayStructure) read(origin, shape);
  }

  /** Iterator over all the data in a Structure. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * StructureData object.
   *  <pre>
   *  Structure.Iterator ii = structVariable.getStructureIterator();
   *  while (ii.hasNext()) {
        StructureData sdata = ii.next();
      }
      </pre>
   *  @return Iterator over type StructureData
   * @see #getStructureIterator(int bufferSize)
   */
  public Structure.Iterator getStructureIterator() {
    return new Structure.Iterator(defaultBufferSize);
  }

  /**
   * Get an efficient iterator over all the data in the Structure. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it. DO NOT KEEP ANY REFERENCES to the
   * StructureData object.
   *
   * This is the efficient way to get all the data, it can be much faster than reading one record at a time,
   *   and is optimized for large datasets.
   * This is accomplished by buffering bufferSize amount of data at once.
   *
   * <pre>Example:
   *
   *  Structure.Iterator ii = structVariable.getStructureIterator(100 * 1000);
   *  while (ii.hasNext()) {
   *    StructureData sdata = ii.next();
   *  }
   * </pre>
   *  @param bufferSize size in bytes to buffer
   *  @return Structure.Iterator over type StructureData
   */
  public Structure.Iterator getStructureIterator(int bufferSize) {
    return new Structure.Iterator(bufferSize);
  }

  /**
   * Iterator over type StructureData.
   */
  public class Iterator {
    private int count = 0;
    private int recnum = (int) getSize();
    private int readStart = 0;
    private int readAtaTime = 100;
    private int readCount = 0;
    private ArrayStructure as = null;

    protected Iterator(int bufferSize) {
      int structureSize = calcStructureSize();
      if (bufferSize <= 0)
        bufferSize = defaultBufferSize;
      readAtaTime = Math.max( 10, bufferSize / structureSize);
      if (NetcdfFile.debugStructureIterator)
        System.out.println("Iterator structureSize= "+structureSize+" readAtaTime= "+readAtaTime);
    }

    /** Return true if more records are available */
    public boolean hasNext() { return count < recnum; }

    /** Get next StructureData record. Do not keep references to it. */
    public StructureData next() throws IOException {
      if (count >= readStart)
        readNext();

      count++;
      return as.getStructureData( readCount++);
    }

    private void readNext() throws IOException {
      int left = Math.min(recnum, readStart+readAtaTime); // dont go over recnum
      int need = left - readStart; // how many to read this time
      try {
        as = readStructure( readStart, need); // LOOK 1D only
        if (NetcdfFile.debugStructureIterator)
          System.out.println("readNext "+count+" "+readStart);

      } catch (InvalidRangeException e) { } // cant happen
      readStart += need;
      readCount = 0;
    }
  }

  ////////////////////////////////////////////

  /** Get String with name and attributes. Used in short descriptions like tooltips. */
  public String getNameAndAttributes() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("Structure ");
    getNameAndDimensions(sbuff, false, false);
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

  /** String representation of Structure and nested variables. */
  public String toString() {
    return writeCDL("   ", false, false);
  }

  public String writeCDL(String space, boolean useFullName, boolean strict) {
    StringBuffer buf = new StringBuffer();
    buf.setLength(0);
    //buf.append("\n");
    buf.append(space);
    buf.append(dataType.toString());
    buf.append(" {\n");

    String nestedSpace = "  "+space;
    for (int i=0; i<members.size(); i++) {
      Variable v = (Variable) members.get(i);
      buf.append(v.writeCDL(nestedSpace, useFullName, strict));
    }

    buf.append(space);
    buf.append("} ");
    getNameAndDimensions(buf, useFullName, false);
    buf.append(";");
    buf.append(extraInfo());
    buf.append("\n");

    java.util.Iterator iter = getAttributes().iterator();
    while (iter.hasNext()) {
      buf.append(nestedSpace);
      if (strict) buf.append(getShortName());
      buf.append("  :");
      Attribute att = (Attribute) iter.next();
      buf.append(att.toString());
      buf.append(";");
      if (!strict && (att.getDataType() != DataType.STRING)) buf.append(" // "+att.getDataType());
      buf.append("\n");
    }

    return buf.toString();
  }

}
