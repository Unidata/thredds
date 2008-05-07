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
 * Concrete implementation of ArrayStructure, with data access deferred to the StructureData objects.
 * The StructureData objects may be of any subclass.
 * Using ArrayStructureW and StructureDataW is often the easiest to construct, but not efficient for large arrays
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

  public Array getArray(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getArray(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getArray( m.getName());
  }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarDouble(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarDouble( m.getName());
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayDouble(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayDouble( m.getName());
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarFloat(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarFloat( m.getName());
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayFloat(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayFloat( m.getName());
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarByte(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarByte( m.getName());
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayByte(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayByte( m.getName());
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarShort(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarShort( m.getName());
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayShort(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayShort( m.getName());
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarInt(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarInt( m.getName());
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayInt(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayInt( m.getName());
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarLong(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarLong( m.getName());
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayLong(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayLong( m.getName());
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarChar(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarChar( m.getName());
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayChar(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayChar( m.getName());
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarString(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarString( m.getName());
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayString(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayString( m.getName());
  }

  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarStructure(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarStructure( m.getName());
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getArrayStructure(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getArrayStructure( m.getName());
  }

  public ArraySequence getArraySequence(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getArraySequence(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getArraySequence( m.getName());
  }
}