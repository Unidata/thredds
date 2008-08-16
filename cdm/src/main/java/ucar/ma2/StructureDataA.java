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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.ma2;

/**
 * A StructureData implementation delegates everything to the containing ArrayStructure.
 *
 * @author caron
 */

public class StructureDataA extends StructureData {
  protected ArrayStructure sa;
  protected int recno;

  /**
   * Constructor.
   *
   * @param sa    StructureData is always contained in a StructureArray.
   * @param recno the recno in the StructureArray.
   */
  public StructureDataA(ArrayStructure sa, int recno) {
    super(sa.getStructureMembers());
    this.sa = sa;
    this.recno = recno;
  }

  public Array getArray(StructureMembers.Member m) {
    return sa.getArray(recno, m);
  }

  public int convertScalarInt(StructureMembers.Member m) {
    return sa.convertScalarInt(recno, m);
  }

  public float convertScalarFloat(StructureMembers.Member m) {
    return sa.convertScalarFloat(recno, m);
  }

  public double convertScalarDouble(StructureMembers.Member m) {
    return sa.convertScalarDouble(recno, m);
  }

  public double getScalarDouble(StructureMembers.Member m) {
    return sa.getScalarDouble(recno, m);
  }

  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return sa.getJavaArrayDouble(recno, m);
  }

  /**
   * Get member data of type float.
   *
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return scalar float value
   */
  public float getScalarFloat(StructureMembers.Member m) {
    return sa.getScalarFloat(recno, m);
  }

  /**
   * Get member data of type float array.
   *
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return 1D array of floats
   */
  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    return sa.getJavaArrayFloat(recno, m);
  }

  /**
   * Get member data of type byte.
   *
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return scalar byte value
   */
  public byte getScalarByte(StructureMembers.Member m) {
    return sa.getScalarByte(recno, m);
  }

  /**
   * Get member data of type byte array.
   *
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return 1D array of bytes
   */
  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    return sa.getJavaArrayByte(recno, m);
  }

  /**
   * Get member data of type short.
   *
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return scalar short value
   */
  public short getScalarShort(StructureMembers.Member m) {
    return sa.getScalarShort(recno, m);
  }

  /**
   * Get member data of type short array.
   *
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return 1D array of shorts
   */
  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return sa.getJavaArrayShort(recno, m);
  }

  /**
   * Get member data of type int.
   *
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return scalar int value
   */
  public int getScalarInt(StructureMembers.Member m) {
    return sa.getScalarInt(recno, m);
  }

  /**
   * Get member data of type int array.
   *
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return 1D array of ints
   */
  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return sa.getJavaArrayInt(recno, m);
  }

  /**
   * Get member data of type long.
   *
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return scalar long value
   */
  public long getScalarLong(StructureMembers.Member m) {
    return sa.getScalarLong(recno, m);
  }

  /**
   * Get member data of type long array.
   *
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return 1D array of longs
   */
  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return sa.getJavaArrayLong(recno, m);
  }

  /**
   * Get member data of type char.
   *
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return scalar char value
   */
  public char getScalarChar(StructureMembers.Member m) {
    return sa.getScalarChar(recno, m);
  }

  /**
   * Get member data of type char array.
   *
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return 1D array of chars
   */
  public char[] getJavaArrayChar(StructureMembers.Member m) {
    return sa.getJavaArrayChar(recno, m);
  }

  /**
   * Get member data of type String or 1D char.
   *
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return scalar char value
   */
  public String getScalarString(StructureMembers.Member m) {
    return sa.getScalarString(recno, m);
  }

  /*
   * Get String value, from rank 0 String or rank 1 char member array.
   * For more efficiency, use getScalarString(StructureMembers.Member m) if possible.
   *
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   *
  public String getScalarString(String memberName) {
    StructureMembers.Member m = findMember(memberName);
    if (null == m) throw new IllegalArgumentException("Member not found= " + memberName);
    if ((m.getDataType() == DataType.CHAR) || (m.getDataType() == DataType.STRING))
      return sa.getScalarString(recno, m);

    Array a = getArray(m);
    if (a == null) throw new IllegalArgumentException("illegal member name =" + memberName);
    if (a instanceof ArrayChar.D1)
      return ((ArrayChar.D1) a).getString();

    assert (a instanceof ArrayObject.D0);
    Object data = ((ArrayObject.D0) a).get();
    assert (data instanceof String) : data.getClass().getName();
    return (String) data;
  }  */

  // LOOK can we optimize ??
  public String[] getJavaArrayString(StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRING) {
      Array data = getArray(m);
      int n = m.getSize();
      String[] result = new String[n];
      for (int i = 0; i < result.length; i++)
        result[i] = (String) data.getObject(i);
      return result;

    } else if (m.getDataType() == DataType.CHAR) {
      ArrayChar data = (ArrayChar) getArray(m);
      ArrayChar.StringIterator iter = data.getStringIterator();
      String[] result = new String[iter.getNumElems()];
      int count = 0;
      while (iter.hasNext())
        result[count++] = iter.next();
      return result;
    }

    throw new IllegalArgumentException("getJavaArrayString: not String DataType :" + m.getDataType());
  }

  public StructureData getScalarStructure(StructureMembers.Member m) {
    return sa.getScalarStructure(recno, m);
  }

  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return sa.getArrayStructure(recno, m);
  }

  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return sa.getArraySequence(recno, m);
  }

}