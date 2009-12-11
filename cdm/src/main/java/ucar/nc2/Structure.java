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
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Structure.class);
  static protected int defaultBufferSize = 500 * 1000; // 500K bytes

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
   */
  protected Structure( Structure from) { // , boolean reparent) {
    super( from);

    members = new ArrayList<Variable>(from.members);
    memberHash = new HashMap<String, Variable>(from.memberHash);
    isSubset = from.isSubset();

    /* if (reparent) {
      for (Variable v : members) {
        v.setParentStructure(this);
      }
    } */
  }

  /**
   * Create a subset of the Structure consisting only of the given member variables
   * @param memberNames list of Variable names, already a member
   * @return Structure containing just those members
   */
  public Structure select( List<String> memberNames) {
    Structure result = (Structure) copy();
    List<Variable> members = new ArrayList<Variable>();
    for (String name : memberNames) {
      Variable m = findVariable(name);
      if (null != m) members.add(m);
    }
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
   * Create a subset of the Structure consisting only of the one member variable
   * @param varName name of member Variable
   * @return containing just that member
   */
  public Structure select( String varName) {
    List<String> memberNames = new ArrayList<String>(1);
    memberNames.add(varName);
    return  select(memberNames);
  }

  /**
   * Find if this was created from a subset() method.
   * @return true if this is a subset
   */
  public boolean isSubset() { return isSubset; }

  // for section and slice
  @Override
  protected Variable copy() {
    return new Structure(this);
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
   * @return the added variable
   */
  public Variable addMemberVariable( Variable v) {
    if (isImmutable()) throw new IllegalStateException("Cant modify");
    members.add( v);
    memberHash.put( v.getShortName(), v);
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
      if (v.getShortName() == null)
          System.out.println("HEY null short name");
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
  public java.util.List<Variable> getVariables() {
    return isImmutable() ?  members : new ArrayList<Variable>(members);
  }

  /** Get the number of variables contained directly in this Structure.
   * @return number of member variables
   */
  public int getNumberOfMemberVariables() {
    return members.size();
  }

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

  /**
   * Force recalculation of size of one element of this structure - equals the sum of sizes of its members.
   * This is used only by low level classes like IOSPs.
   */
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

  /** Iterator over all the data in a Structure.
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
    return getStructureIterator(defaultBufferSize);
  }

  /**
   * Get an efficient iterator over all the data in the Structure. 
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
    return (getRank() < 2) ? new Structure.IteratorRank1(bufferSize) : new Structure.Iterator(bufferSize);
  }

  /**
   * Iterator over type StructureData, rank 1 (common) case
   */
  private class IteratorRank1 implements StructureDataIterator {
    private int count = 0;
    private int recnum = (int) getSize();
    private int readStart = 0;
    private int readCount = 0;
    private int readAtaTime;
    private ArrayStructure as = null;

    protected IteratorRank1(int bufferSize) {
      setBufferSize( bufferSize);
    }

    @Override
    public boolean hasNext() { return count < recnum; }

    @Override
    public StructureDataIterator reset() {
      count = 0;
      readStart = 0;
      readCount = 0;
      return this;
    }

    @Override
    public StructureData next() throws IOException {
      if (count >= readStart) {
        readNext();
      }

      count++;
      return as.getStructureData( readCount++);
    }

    @Override
    public int getCurrentRecno() {
      return count-1;
    }

    private void readNext() throws IOException {
      int left = Math.min(recnum, readStart+readAtaTime); // dont go over recnum
      int need = left - readStart; // how many to read this time
      try {
        // System.out.println(" read start= "+readStart+" count= "+need);
        as = readStructure( readStart, need);
        if (NetcdfFile.debugStructureIterator)
          System.out.println("readNext "+count+" "+readStart);

      } catch (InvalidRangeException e) {
        log.error("Structure.IteratorRank1.readNext() ",e);
        throw new IllegalStateException("Structure.Iterator.readNext() ",e);
      } // cant happen
      readStart += need;
      readCount = 0;
    }

    @Override
    public void setBufferSize(int bytes) {
      if (count > 0) return; // too late
      int structureSize = calcStructureSize();
      if (structureSize <= 0)
        structureSize = 1; // no members in the psuedo-structure LOOK is this ok?
      if (bytes <= 0)
        bytes = defaultBufferSize;
      readAtaTime = Math.max( 10, bytes / structureSize);
      if (NetcdfFile.debugStructureIterator)
        System.out.println("Iterator structureSize= "+structureSize+" readAtaTime= "+readAtaTime);
    }

  }

  /**
   * Iterator over type StructureData, general case
   */
  private class Iterator implements StructureDataIterator {
    private int count; // done so far
    private int total; // total to do
    private int readStart; // current buffer starts at
    private int readCount; // count within the current buffer [0,readAtaTime)
    private int outerCount;  // over the outer Dimension
    private int readAtaTime;
    private ArrayStructure as = null;

    protected Iterator(int bufferSize) {
      reset();
    }

    @Override
    public boolean hasNext() {
      return count < total;
    }

    @Override
    public StructureDataIterator reset() {
      count = 0;
      total = (int) getSize();
      readStart = 0;
      readCount = 0;
      outerCount = 0;
      readAtaTime = (int) getSize() / shape[0];
      return this;
    }

    @Override
    public int getCurrentRecno() {
      return count-1;
    }

    @Override
    public StructureData next() throws IOException {
      if (count >= readStart)
        readNextGeneralRank();

      count++;
      return as.getStructureData( readCount++);
    }

    private void readNextGeneralRank() throws IOException {

      try {
        Section section = new Section(shape);
        section.setRange( 0, new Range(outerCount, outerCount));

        as = (ArrayStructure) read( section);

        if (NetcdfFile.debugStructureIterator)
          System.out.println("readNext inner="+outerCount+" total="+outerCount);

        outerCount++;

      } catch (InvalidRangeException e) {
        log.error("Structure.Iterator.readNext() ",e);
        throw new IllegalStateException("Structure.Iterator.readNext() ",e);
      } // cant happen

      readStart += as.getSize();
      readCount = 0;
    }

    public void setBufferSize(int bytes) {
      // ignored
    }
  }

  ////////////////////////////////////////////

  /** Get String with name and attributes. Used in short descriptions like tooltips.
   * @return  name and attributes String
   */
  public String getNameAndAttributes() {
    Formatter sbuff = new Formatter();
    sbuff.format("Structure ");
    getNameAndDimensions(sbuff, false, true);
    sbuff.format("\n");
    for (Attribute att : attributes) {
      sbuff.format("  %s:%s;\n",getShortName(), att.toString());
    }
    return sbuff.toString();
  }

  @Override
  protected void writeCDL(Formatter buf, String indent, boolean useFullName, boolean strict) {
    buf.format("\n%s%s {\n", indent, dataType);

    String nestedSpace = "  "+indent;
    for (Variable v : members)
      v.writeCDL(buf, nestedSpace, useFullName, strict);

    buf.format("%s} ", indent);
    getNameAndDimensions(buf, useFullName, strict);
    buf.format(";%s\n", extraInfo());

    for (Attribute att : getAttributes()) {
      buf.format("%s  ", nestedSpace);
      if (strict) buf.format( NetcdfFile.escapeName(getShortName()));
      buf.format(":%s;", att.toString(strict));
      if (!strict && (att.getDataType() != DataType.STRING))
        buf.format(" // %s", att.getDataType());
      buf.format("\n");
    }
    buf.format("\n");
  }

}
