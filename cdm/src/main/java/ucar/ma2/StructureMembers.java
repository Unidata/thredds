// $Id:StructureMembers.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.ma2;

import ucar.nc2.VariableSimpleIF;

import java.util.*;

/**
 * A Collection of members that comprise a StructureData.
 *  *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class StructureMembers {
  ///////////////////////////////////////////////////////////////////////////
  protected String name;
  protected HashMap memberHash = new HashMap(); // Hash of Members
  protected ArrayList members = new ArrayList(); // List of Members
  protected int structureSize = -1;

  public StructureMembers( String name) {
    this.name = name;
  }

  /**
   * Get the name.
   */
  public String getName() { return name; }

  /**
   * Add a member.
   */
  public void addMember(Member m ) {
    memberHash.put(m.getName(), m);
    members.add( m);
  }

  /**
   * Get the total size of the Structure in bytes.
   */
  public int getStructureSize() {
    if (structureSize < 0)
      calcStructureSize();
    return structureSize;
  }

  public void calcStructureSize() {
    structureSize = 0;
    for (int i = 0; i < members.size(); i++) {
      Member member = (Member) members.get(i);
      structureSize += member.getTotalSize();
    }
  }

  /**
   * Set the total size of the Structure in bytes.
   */
  public void setStructureSize(int structureSize) {
    this.structureSize = structureSize;
  }

  /**
   * Get the list of Member objects.
   */
  public java.util.List getMembers() { return members; }


  /**
   * Get the names of the members.
   *
   * @return List of type String.
   */
  public java.util.List getMemberNames() {
    ArrayList memberNames = new ArrayList();  // Strings
    for (int i = 0; i < members.size(); i++) {
      Member m = (Member) members.get(i);
      memberNames.add(m.getName());
    }
    return memberNames;
  }

  /**
   * Get the index-th member
   * @param index of member
   * @return Member
   */
  public Member getMember(int index) {
    return (Member) members.get(index);
  }

  /**
   * Find the member by its name.
   * @return Member matching the name, or null if not found
   */
  public Member findMember(String memberName) {
    if (memberName == null) return null;
    return (Member) memberHash.get( memberName);
  }

 /**
 * A member of a StructureData.
 */
  static public class Member {
    private VariableSimpleIF v;
    private String name, desc, units;
    private DataType dtype;
    private int  size = 1;
    private int[] shape;
    private StructureMembers members;

    // optional, use depends on ArrayStructure subclass
    private Object dataObject, dataObject2;
    private int dataParam;

    public Member(VariableSimpleIF v2) {
      this( v2.getShortName(), v2.getDescription(),
          v2.getUnitsString(), v2.getDataType(), v2.getShape());
      this.v = v2;
    }

    public Member(String name, String desc, String units, DataType dtype, int[] shape) {
      this.name = name;
      this.desc = desc;
      this.units = units;
      this.dtype = dtype;
      this.shape = shape;
      this.size = (int) Index.computeSize(shape);
    }

   /** If member is type Structure, you mest set its constituent members */
   public void setStructureMembers( StructureMembers members) { this.members = members; }
   public StructureMembers getStructureMembers( ) { return members; }

   public void setVariableSimple(VariableSimpleIF v2) {
     this.v = v2;
   }

   public void setShape( int[] shape) {
      this.shape = shape;
      this.size = (int) Index.computeSize(shape);
   }

    /**
     * Get the name.
     */
    public String getName() {
      return name;
    }

    /**
     * Get the units string, if any.
     */
    public String getUnitsString() {
      return (v == null) ? units : v.getUnitsString();
    }

    /**
     * Get the description, if any.
     */
    public String getDescription() {
      return (v == null) ? desc : v.getDescription();
    }

    /**
     * Get the DataType.
     */
    public DataType getDataType() {
      return dtype;
    }

    /**
     * Get the array shape. This does not have to match the VariableSimpleIF.
     */
    public int[] getShape() {
      return shape;
    }

   /**
     * Get the total array length. This does not have to match the VariableSimpleIF.
     */
    public int getSize() {
      return size;
    }

    /**
     * Get the total size in bytes. This does not have to match the VariableSimpleIF.
     */
    public int getTotalSize() {
      return size * getDataType().getSize();
    }

    /**
     * Is this a scalar (size == 1).
     * This does not have to match the VariableSimpleIF.
     */
    public boolean isScalar() { return size == 1; }


    public double convertScaleOffsetMissing(double value) {
      return (v == null) ? value : v.convertScaleOffsetMissing( value);
    }

    ////////////////////////////////////////////////


    /** Get the data parameter value, for use behind the scenes. */
    public int getDataParam() { return dataParam; }
    /** Set the data parameter value, for use behind the scenes. */
    public void setDataParam(int dataParam) { this.dataParam = dataParam; }

   /** Get an opaque data object, for use behind the scenes. May be null */
   public Object getDataObject() { return dataObject; }
   /** Set an opaque data object, for use behind the scenes.  */
   public void setDataObject( Object o) { this.dataObject = o; }

   /** Get an opaque data object, for use behind the scenes. May be null */
   public Object getDataObject2() { return dataObject2; }
   /** Set an opaque data object, for use behind the scenes.  */
   public void setDataObject2( Object o) { this.dataObject2 = o; }
  }

}
