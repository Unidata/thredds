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
 */

public class Structure extends Variable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Structure.class);
  static private int defaultBufferSize = 500 * 1000; // 500K bytes

  protected List<Variable> members = new ArrayList<Variable>();
  protected HashMap<String, Variable> memberHash = new HashMap<String, Variable>();
  protected boolean isSubset;

  /** Constructor
   *
   * @param ncfile    the containing NetcdfFile.
   * @param group     the containing group; if null, use rootGroup
   * @param parent    parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   */
  public Structure( NetcdfFile ncfile, Group group, Structure parent, String shortName) {
    super (ncfile, group, parent, shortName);
    this.dataType = DataType.STRUCTURE;
    this.elementSize = -1; // gotta wait before calculating
  }


  /** Copy constructor.
   * @param from  copy from this
   * @param reparent : if true, reparent the members. if so, cant use 'from' anymore
   */
  protected Structure( Structure from, boolean reparent) {
    super( from);

    members = new ArrayList<Variable>(from.members);
    memberHash = new HashMap<String, Variable>(from.memberHash);

    if (reparent) {
      for (Variable v : members) {
        v.setParentStructure(this);
      }
    }
  }

  /**
   * Create a subset of the Structure consisting only of the given member variables
   * @param members list of Variable
   * @return subsetted Structure
   */
  public Structure subset( List<Variable> members) {
    Structure result = new Structure(this.ncfile, this.group, this.parent, this.shortName);
    result.setDimensions( this.getDimensions());
    for (Variable m : members)
      result.addMemberVariable(m);
    result.isSubset = true;
    return result;
  }

  public boolean isSubset() { return isSubset; }

  // for section and slice
  @Override
  protected Variable copy() {
    return new Structure(this, false); // dont need to reparent
  }

  protected int calcStructureSize() {
    int structureSize = 0;
    for (Variable member : members) {
      structureSize += member.getSize() * member.getElementSize();
    }
    return structureSize;
  }

  /** Caching is not allowed */
  @Override
  public boolean isCaching() {
    return false;
  }

  /** Caching is not allowed */
  @Override
  public void setCaching(boolean caching) {
    this.cache.isCaching = false;
    this.cache.cachingSet = true;
  }

  /** Add a member variable
   * @param v add this variable as a member of this structure
   */
  public void addMemberVariable( Variable v) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    members.add( v);
    memberHash.put( v.getShortName(), v);
    //smembers = null;
    v.setParentStructure( this);
  }

  /** Set the list of member variables.
   * @param vars this is the list of member variables
   */
  public void setMemberVariables( List<Variable> vars) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    members = new ArrayList<Variable>();
    memberHash = new HashMap<String, Variable>(2*vars.size());
    //smembers = null;
    for (Variable v : vars) {
      members.add(v);
      memberHash.put( v.getShortName(), v);
    }
  }

  /** Remove a Variable : uses the Variable name to find it.
   * @param v remove this variable as a member of this structure
   * @return true if was found and removed
   */
  public boolean removeMemberVariable( Variable v) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    if (v == null) return false;
    //smembers = null;
    java.util.Iterator<Variable> iter = members.iterator();
    while (iter.hasNext()) {
      Variable mv =  iter.next();
      if (mv.getShortName().equals(v.getShortName())) {
        iter.remove();
        memberHash.remove( v.getShortName());
        return true;
      }
    }
    return false;
  }

  /** Replace a Variable with another that has the same name : uses the variable name to find it.
   * If old Var is not found, just add the new one
   * @param newVar add this variable as a member of this structure
   * @return true if was found and replaced */
  public boolean replaceMemberVariable( Variable newVar) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    //smembers = null;
    boolean found = false;
    for (int i = 0; i < members.size(); i++) {
      Variable v =  members.get(i);
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
  @Override
  public void setParentGroup(Group group) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    super.setParentGroup(group);
    for (Variable v : members) {
      v.setParentGroup(group);
    }
  }

  @Override
  public Variable setImmutable() {
    members = Collections.unmodifiableList(members);
    for (Variable m : members)
      m.setImmutable();

    super.setImmutable();
    return this;
  }

  /** Get the variables contained directly in this Structure.
   * @return List of type Variable.
   */
  public java.util.List<Variable> getVariables() { return isImmutable() ?  members : new ArrayList<Variable>(members); }

  /** Get the (short) names of the variables contained directly in this Structure.
   * @return List of type String.
   */
  public java.util.List<String> getVariableNames() {
    return new ArrayList<String>(memberHash.keySet());
  }

  /**
   * Find the Variable member with the specified (short) name.
   * @param shortName name of the member variable.
   * @return the Variable member with the specified (short) name, or null if not found.
   */
  public Variable findVariable(String shortName) {
    if (shortName == null) return null;
    return memberHash.get(shortName);
  }

  /**
   * Create a StructureMembers object that describes this Structure.
   * CAUTION: Do not use for iterating over a StructureData or ArrayStructure - get the StructureMembers object
   * directly from the StructureData or ArrayStructure.
   *
   * @return a StructureMembers object that describes this Structure.
   */
  public StructureMembers makeStructureMembers() {
    //if (smembers == null) {
      StructureMembers smembers = new StructureMembers( getName());
      for (Variable v2 : getVariables()) {
        StructureMembers.Member m = new StructureMembers.Member( v2.getShortName(), v2.getDescription(),
            v2.getUnitsString(), v2.getDataType(), v2.getShape());
        if (v2 instanceof Structure)
          m.setStructureMembers( ((Structure)v2).makeStructureMembers());
        smembers.addMember( m);
      }
    //}
    return smembers;
  }
  //protected StructureMembers smembers = null;

  /**
   * Get the size of one element of the Structure.
   * @return size (in bytes)
   */
  @Override
  public int getElementSize() {
    if (elementSize == -1) calcElementSize();
    return elementSize;
  }

  protected void calcElementSize() {
    int total = 0;
    for (Variable v : members) {
      total += v.getElementSize() * v.getSize();
    }
    elementSize = total;
  }

  /**
   * Use this when this is a scalar Structure. Its the same as read(), but it extracts the single
   * StructureData out of the Array.
   * @return StructureData for a scalar
   * @throws java.io.IOException on read error
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
   * @param index index into 1D array
   * @return ith StructureData
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if index out of range
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
   * @param start start at this index
   * @param count return this many StructureData
   * @return start - start + count-1 StructureData records in an ArrayStructure
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if start, count out of range
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
   *  @param bufferSize size in bytes to buffer, set < 0 to use default size
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

    /** @return true if more records are available */
    public boolean hasNext() { return count < recnum; }

    /** @return next StructureData record. Do not keep references to it.
     * @throws java.io.IOException on read error
     */
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

      } catch (InvalidRangeException e) {
        log.error("Structure.Iterator.readNext() ",e);
        throw new IllegalStateException("Structure.Iterator.readNext() ",e);
      } // cant happen
      readStart += need;
      readCount = 0;
    }
  }

  ////////////////////////////////////////////

  /** Get String with name and attributes. Used in short descriptions like tooltips.
   * @return  name and attributes String
   */
  public String getNameAndAttributes() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("Structure ");
    getNameAndDimensions(sbuff, false, true);
    sbuff.append("\n");
    for (Attribute att : attributes) {
      sbuff.append("  ").append(getShortName()).append(":");
      sbuff.append(att.toString());
      sbuff.append(";");
      sbuff.append("\n");
    }
    return sbuff.toString();
  }

  @Override
  public String writeCDL(String space, boolean useFullName, boolean strict) {
    StringBuffer buf = new StringBuffer();
    buf.append(space);
    buf.append(dataType.toString());
    buf.append(" {\n");

    String nestedSpace = "  "+space;
    for (Variable v : members) {
      buf.append(v.writeCDL(nestedSpace, useFullName, strict));
    }

    buf.append(space);
    buf.append("} ");
    getNameAndDimensions(buf, useFullName, strict);
    buf.append(";");
    buf.append(extraInfo());
    buf.append("\n");

    for (Attribute att : getAttributes()) {
      buf.append(nestedSpace);
      if (strict) buf.append( NetcdfFile.escapeName(getShortName()));
      buf.append("  :");
      buf.append(att.toString());
      buf.append(";");
      if (!strict && (att.getDataType() != DataType.STRING))
        buf.append(" // ").append(att.getDataType());
      buf.append("\n");
    }

    return buf.toString();
  }

}
