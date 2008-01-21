/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.ma2;

/**
 * Concrete implementation of ArrayStructure, with data storage in individual Arrays in StructureData objects.
 * The ArrayStructure data accessors thus defer to the accessors in the StructureData objects.
 * Using this and StructureDataW is often the easiest to construct, but not very efficient for large arrays
 *  of Structures due to excessive object creation.
 *
 * @author caron
 * @see Array
 */
public class ArrayStructureW extends ArrayStructure {

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * You must completely construct by calling setStructureData()
   *
   * @param members a description of the structure members
   * @param shape   the shape of the Array.
   */
  public ArrayStructureW(StructureMembers members, int[] shape) {
    super(members, shape);
    this.sdata = new StructureData[nelems];
  }

  /**
   * Create a new Array of type StructureData and the given members, shape, and array of StructureData.
   * @param members a description of the structure members
   * @param shape   the shape of the Array.
   * @param sdata   StructureData array, must be
   */

  public ArrayStructureW(StructureMembers members, int[] shape, StructureData[] sdata) {
    super(members, shape);
    if (nelems != sdata.length)
      throw new IllegalArgumentException("StructureData length= "+sdata.length+"!= shape.length="+nelems);
    this.sdata = sdata;
  }

  /**
   * Set one of the StructureData of this ArrayStructure.
   * @param sd set it to this StructureData.
   * @param index which one to set, as an index into 1D backing store.
   */
  public void setStructureData(StructureData sd, int index) {
    sdata[index] = sd;
  }

  protected StructureData makeStructureData( ArrayStructure as, int index) {
    return new StructureDataW( as.getStructureMembers());
  }

  /* public Array createView(Index index) {
    return new ArrayStructureW(members, index, sdata);
  }

  /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections, and factory. Trusted package private.
   *
   * @param members a description of the structure members
   * @param ima     use this IndexArray as the index
   * @param sdata   StructureData array.
   *
  ArrayStructureW(StructureMembers members, Index ima, StructureData[] sdata) {
    super(members, ima);
    this.nelems = sdata.length;
    this.sdata = sdata;
  }   */

  /**
   * Get underlying StructureData primitive array storage. CAUTION! You may invalidate your warrentee!
   */
  public Object getStorage() { return sdata; }

  public Array getArray(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getArray(m);
  }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarDouble(m);
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayDouble(m);
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarFloat(m);
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayFloat(m);
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarByte(m);
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayByte(m);
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarShort(m);
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayShort(m);
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarInt(m);
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayInt(m);
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarLong(m);
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayLong(m);
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarChar(m);
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayChar(m);
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarString(m);
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayString(m);
  }

  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarStructure(m);
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getArrayStructure(m);
  }

}