/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

/**
 * Proxy for another StructureData.
 * Does nothing, is intended to be overridden.
 *
 * @author caron
 * @since 8/20/13
 */
public class StructureDataProxy extends StructureData {
  protected StructureData org;

  protected StructureDataProxy( StructureData org) {
    super(org.getStructureMembers());
    this.org = org;
  }

  public StructureDataProxy( StructureMembers members, StructureData org) {
    super(members);
    this.org = org;
  }

  public StructureData getOriginalStructureData() {
    return org;
  }

  public Array getArray(StructureMembers.Member m) {
    return org.getArray(m.getName());
  }

  public float convertScalarFloat(StructureMembers.Member m) {
    return org.convertScalarFloat(m.getName());
  }

  public double convertScalarDouble(StructureMembers.Member m) {
    return org.convertScalarDouble(m.getName());
  }

  public int convertScalarInt(StructureMembers.Member m) {
    return org.convertScalarInt(m.getName());
  }

  public long convertScalarLong(StructureMembers.Member m) {
    return org.convertScalarLong(m.getName());
  }

  public double getScalarDouble(StructureMembers.Member m) {
    return org.getScalarDouble(m.getName());
  }

  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return org.getJavaArrayDouble(m.getName());
  }

  public float getScalarFloat(StructureMembers.Member m) {
    return org.getScalarFloat(m.getName());
  }

  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    return org.getJavaArrayFloat(m.getName());
  }

  public byte getScalarByte(StructureMembers.Member m) {
    return org.getScalarByte(m.getName());
  }

  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    return org.getJavaArrayByte(m.getName());
  }

  public int getScalarInt(StructureMembers.Member m) {
    return org.getScalarInt(m.getName());
  }

  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return org.getJavaArrayInt(m.getName());
  }

  public short getScalarShort(StructureMembers.Member m) {
    return org.getScalarShort(m.getName());
  }

  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return org.getJavaArrayShort(m.getName());
  }

  public long getScalarLong(StructureMembers.Member m) {
    return org.getScalarLong(m.getName());
  }

  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return org.getJavaArrayLong(m.getName());
  }

  public char getScalarChar(StructureMembers.Member m) {
    return org.getScalarChar(m.getName());
  }

  public char[] getJavaArrayChar(StructureMembers.Member m) {
    return org.getJavaArrayChar(m.getName());
  }

  public String getScalarString(StructureMembers.Member m) {
    return org.getScalarString(m.getName());
  }

  public String[] getJavaArrayString(StructureMembers.Member m) {
    return org.getJavaArrayString(m.getName());
  }

  public StructureData getScalarStructure(StructureMembers.Member m) {
    return org.getScalarStructure(m.getName());
  }

  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return org.getArrayStructure(m.getName());
  }

  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return org.getArraySequence(m.getName());
  }

  public Object getScalarObject( StructureMembers.Member m) {
    return org.getScalarObject(m.getName());
  }
}
