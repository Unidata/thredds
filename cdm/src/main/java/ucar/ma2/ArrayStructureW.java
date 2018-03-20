/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

  // wrap a StructureDatain an ArrrayStructure
  public ArrayStructureW(StructureData sdata) {
    super(sdata.getStructureMembers(), new int[] {1});
    this.sdata = new StructureData[1];
    this.sdata[0] = sdata;
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

  @Override
  public Array getArray(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getArray(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getArray( m.getName());
  }

  @Override
  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarDouble(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarDouble( m.getName());
  }

  @Override
  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayDouble(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayDouble( m.getName());
  }

  @Override
  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarFloat(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarFloat( m.getName());
  }

  @Override
  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayFloat(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayFloat( m.getName());
  }

  @Override
  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarByte(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarByte( m.getName());
  }

  @Override
  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayByte(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayByte( m.getName());
  }

  @Override
  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarShort(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarShort( m.getName());
  }

  @Override
  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayShort(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayShort( m.getName());
  }

  @Override
  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarInt(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarInt( m.getName());
  }

  @Override
  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayInt(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayInt( m.getName());
  }

  @Override
  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarLong(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarLong( m.getName());
  }

  @Override
  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayLong(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayLong( m.getName());
  }

  @Override
  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarChar(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarChar( m.getName());
  }

  @Override
  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayChar(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayChar( m.getName());
  }

  @Override
  public String getScalarString(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarString(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarString( m.getName());
  }

  @Override
  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayString(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayString( m.getName());
  }

  @Override
  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarStructure(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getScalarStructure( m.getName());
  }

  @Override
  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getArrayStructure(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getArrayStructure( m.getName());
  }

  @Override
  public ArraySequence getArraySequence(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getArraySequence(recnum, m);
    StructureData sd = getStructureData(recnum);
    return sd.getArraySequence( m.getName());
  }
}