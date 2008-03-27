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
 * including nested structures, and returns an Array of StructureData, with all of the data in memory.
 * If there is a nested sequence, the sequence data may be read into memory all at once, ot it may be
 * read in increments as the iteration proceeds.
 * <p>
 * Generally, the programmer can assume that the data in one Structure are stored together,
 *  so that it is efficient to read an entire Structure, and then access the Variable data through the
 *  Arrays in the StructureData.
 *
 * @author caron
 */

public class Structure extends Variable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Structure.class);
  static private int defaultBufferSize = 500 * 1000; // 500K bytes

  protected List<Variable> members;
  protected HashMap<String, Variable> memberHash;
  protected boolean isSubset;

   /* Create a Structure "from scratch". Also must call setDimensions().
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
    members = new ArrayList<Variable>();
    memberHash = new HashMap<String, Variable>();
  }

  /** Copy constructor.
   * @param from  copy from this
   * @param reparent : if true, reparent the members, which modifies the original.
   *   In effect, this says "Im not using the original Structure anywhere else".
   */
  protected Structure( Structure from, boolean reparent) {
    super( from);

    members = new ArrayList<Variable>(from.members);
    memberHash = new HashMap<String, Variable>(from.memberHash);
    isSubset = from.isSubset();

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
  public Structure select( List<Variable> members) {
    Structure result = new Structure(this, false);
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
   * Find if this was created from a subset() method.
   * @return true if this is a subset
   */
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
  public Variable addMemberVariable( Variable v) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    members.add( v);
    memberHash.put( v.getShortName(), v);
    //smembers = null;
    v.setParentStructure( this);
    return v;
  }

  /** Set the list of member variables.
   * @param vars this is the list of member variables
   */
  public void setMemberVariables( List<Variable> vars) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    members = new ArrayList<Variable>();
    memberHash = new HashMap<String, Variable>(2*vars.size());
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
        StructureMembers.Member m = smembers.addMember( v2.getShortName(), v2.getDescription(),
            v2.getUnitsString(), v2.getDataType(), v2.getShape());
        if (v2 instanceof Structure)
          m.setStructureMembers( ((Structure)v2).makeStructureMembers());
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

  public void calcElementSize() {
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
   * Use this when this is a one dimensional array of Structures, or you are doing the index calculation yourself for
   * a multidimension array. This will read only the ith structure, and return the data as a StructureData object.
   * @param index index into 1D array
   * @return ith StructureData
   * @throws java.io.IOException on read error
   * @throws ucar.ma2.InvalidRangeException if index out of range
   */
  public StructureData readStructure(int index) throws IOException, ucar.ma2.InvalidRangeException {
    Section section = null; // works for scalars i think

    if (getRank() == 1) {
      section = new Section().appendRange(index,index);

    } else if (getRank() > 1) {
      Index ii = Index.factory(shape); // convert to nD index
      ii.setCurrentCounter(index);
      int[] origin = ii.getCurrentCounter();
      section = new Section();
      for (int i=0;i<origin.length;i++)
        section.appendRange(origin[i], origin[i]);
    }

    Array dataArray = read(section);
    ArrayStructure data = (ArrayStructure) dataArray;
    return data.getStructureData(0);
  }

  /**
   * For rank 1 array of Structures, read count Structures and return the data as an ArrayStructure.
   * Use only when this is a one dimensional array of Structures.
   * @param start start at this index
   * @param count return this many StructureData
   * @return the StructureData recordsfrom start to start+count-1
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
   *  StructureDataIterator ii = structVariable.getStructureIterator();
   *  while (ii.hasNext()) {
        StructureData sdata = ii.next();
      }
      </pre>
   * @return StructureDataIterator over type StructureData
   * @throws java.io.IOException on read error
   * @see #getStructureIterator(int bufferSize)
   */
  public StructureDataIterator getStructureIterator()  throws java.io.IOException {
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
   *  StructureDataIterator ii = structVariable.getStructureIterator(100 * 1000);
   *  while (ii.hasNext()) {
   *    StructureData sdata = ii.next();
   *  }
   * </pre>
   *  @param bufferSize size in bytes to buffer, set < 0 to use default size
   *  @return StructureDataIterator over type StructureData
   * @throws java.io.IOException on read error
   */
  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return new Structure.Iterator(bufferSize);
  }

  /**
   * Iterator over type StructureData.
   */
  protected class Iterator implements StructureDataIterator {
    private int count = 0;
    private int recnum = (int) getSize();
    private int readStart = 0;
    private int readCount = 0;
    private int readAtaTime;
    private ArrayStructure as = null;

    protected Iterator(int bufferSize) {
      setBufferSize( bufferSize);
    }

    /** @return true if more records are available */
    public boolean hasNext() { return count < recnum; }

    /** @return next StructureData record. Do not keep references to it.
     * @throws java.io.IOException on read error
     */
    public StructureData next() throws IOException {
      if (count >= readStart) {
        if (getRank() == 1) readNextRank1();
        else readNextGeneralRank();
      }

      count++;
      return as.getStructureData( readCount++);
    }

    private void readNextRank1() throws IOException {
      int left = Math.min(recnum, readStart+readAtaTime); // dont go over recnum
      int need = left - readStart; // how many to read this time
      try {
        // System.out.println(" read start= "+readStart+" count= "+need);
        as = readStructure( readStart, need);
        if (NetcdfFile.debugStructureIterator)
          System.out.println("readNext "+count+" "+readStart);

      } catch (InvalidRangeException e) {
        log.error("Structure.Iterator.readNext() ",e);
        throw new IllegalStateException("Structure.Iterator.readNext() ",e);
      } // cant happen
      readStart += need;
      readCount = 0;
    }

    private void readNextGeneralRank() throws IOException {
      throw new UnsupportedOperationException();  // not implemented yet - need example to test
    }

    public void setBufferSize(int bytes) {
      if (count > 0) return; // too late
      int structureSize = calcStructureSize();
      if (bytes <= 0)
        bytes = defaultBufferSize;
      readAtaTime = Math.max( 10, bytes / structureSize);
      if (NetcdfFile.debugStructureIterator)
        System.out.println("Iterator structureSize= "+structureSize+" readAtaTime= "+readAtaTime);
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
    buf.append("\n");
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
    buf.append("\n");

    return buf.toString();
  }

}
