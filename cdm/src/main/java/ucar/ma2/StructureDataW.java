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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A StructureData implementation that has its data self-contained.
 * This is often the easiest to construct, but not very efficient for large arrays of Structures.
 *  *
 * @author caron
 */

public class StructureDataW extends StructureData {
  protected Map<StructureMembers.Member,Array> memberData = new HashMap<StructureMembers.Member,Array>(); // Members

  /**
   * Constructor.
   *
   * @param members    StructureData is always contained in a StructureArray.
   */
  public StructureDataW(StructureMembers members) {
    super(members);
  }

  /** Copy constructor.
   *  This makes a local copy of all the data in the from StrucureData.
   * @param from copy from here
   */
  public StructureDataW (StructureData from) {
    this(from.getStructureMembers());
    List<StructureMembers.Member> members = getMembers();
    for (StructureMembers.Member m : members) {
      Array data = from.getArray(m);
      setMemberData(m, data.copy());  // LOOK wont work for StructureData
    }
  }

  public void setMemberData(StructureMembers.Member m, Array data) {
    if (data == null)
      throw new IllegalArgumentException("data cant be null");

    memberData.put(m, data);
  }

  public void setMemberData(String memberName, Array data) {
    StructureMembers.Member m = members.findMember(memberName);
    if (m == null)
      throw new IllegalArgumentException("illegal member name =" + memberName);
    setMemberData(m, data);
  }

  /**
   * Get member data array of any type as an Array.
   * @param m get data from this StructureMembers.Member.
   * @return Array values.
   */
  public Array getArray(StructureMembers.Member m) {
    if (m == null) throw new IllegalArgumentException("member is null");
    return memberData.get(m);
  }

  public float convertScalarFloat(StructureMembers.Member m) {
    return getScalarFloat(m);
  }

  public double convertScalarDouble(StructureMembers.Member m) {
    return getScalarDouble(m);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get double value. Underlying type must be convertible to double.
   * For more efficiency, use getScalarDouble(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public double getScalarDouble(String memberName) {
    Array data = getArray(memberName);
    return data.getDouble(Index.scalarIndex);
  }

  /**
   * Get member data of type double.
   * @param m get data from this StructureMembers.Member. Must be of type double.
   * @return scalar double value
   */
  public double getScalarDouble(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getDouble(Index.scalarIndex);
  }


  /**
   * Get java double array for a member of type double.
   * @param m get data from this StructureMembers.Member. Must be of type double.
   * @return 1D java array of doubles
   */
  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    Array data = getArray(m);
    return (double []) data.getStorage();
  }

  ////////////////

  /**
   * Get float value. Underlying type must be convertible to float.
   * For more efficiency, use getScalarFloat(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public float getScalarFloat(String memberName) {
    Array data = getArray(memberName);
    return data.getFloat(Index.scalarIndex);
  }

  /**
   * Get member data of type float.
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return scalar double value
   */
  public float getScalarFloat(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getFloat(Index.scalarIndex);
  }

  /**
   * Get java float array for a member of type float.
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return 1D java array of floats
   */
  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    Array data = getArray(m);
    return (float []) data.getStorage();
  }

  /////

  /**
   * Get byte value. Underlying type must be convertible to byte.
   * For more efficiency, use getScalarByte(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public byte getScalarByte(String memberName) {
    Array data = getArray(memberName);
    return data.getByte(Index.scalarIndex);
  }

  /**
   * Get member data of type byte.
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return scalar byte value
   */
  public byte getScalarByte(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getByte(Index.scalarIndex);
  }

  /**
   * Get java byte array for a member of type byte.
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return 1D java array of bytes
   */
  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    Array data = getArray(m);
    return (byte []) data.getStorage();
  }

  /////
  /**
   * Get int value. Underlying type must be convertible to int.
   * For more efficiency, use getScalarDouble(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public int getScalarInt(String memberName) {
    Array data = getArray(memberName);
    return data.getInt(Index.scalarIndex);
  }

  /**
   * Get member data of type int.
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return scalar int value
   */
  public int getScalarInt(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getInt(Index.scalarIndex);
  }

  /**
   * Get java int array for a member of type int.
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return 1D java array of ints
   */
  public int[] getJavaArrayInt(StructureMembers.Member m) {
    Array data = getArray(m);
    return (int []) data.getStorage();
  }

  /////
  /**
   * Get short value. Underlying type must be convertible to short.
   * For more efficiency, use getScalarDouble(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public short getScalarShort(String memberName) {
    Array data = getArray(memberName);
    return data.getShort(Index.scalarIndex);
  }

  /**
   * Get member data of type short.
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return scalar short value
   */
  public short getScalarShort(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getShort(Index.scalarIndex);
  }

  /**
   * Get java short array for a member of type short.
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return 1D java array of shorts
   */
  public short[] getJavaArrayShort(StructureMembers.Member m) {
    Array data = getArray(m);
    return (short []) data.getStorage();
  }


  /////
  /**
   * Get long value. Underlying type must be convertible to long.
   * For more efficiency, use getScalarDouble(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public long getScalarLong(String memberName) {
    Array data = getArray(memberName);
    return data.getLong(Index.scalarIndex);
  }

  /**
   * Get member data of type long.
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return scalar long value
   */
  public long getScalarLong(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getLong(Index.scalarIndex);
  }

  /**
   * Get java long array for a member of type long.
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return 1D java array of longs
   */
  public long[] getJavaArrayLong(StructureMembers.Member m) {
    Array data = getArray(m);
    return (long []) data.getStorage();
  }


/////
  /**
   * Get char value. Underlying type must be convertible to char.
   * For more efficiency, use getScalarDouble(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public char getScalarChar(String memberName) {
    Array data = getArray(memberName);
    return data.getChar(Index.scalarIndex);
  }

  /**
   * Get member data of type char.
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return scalar char value
   */
  public char getScalarChar(StructureMembers.Member m) {
    Array data = getArray(m);
    return data.getChar(Index.scalarIndex);
  }

  /**
   * Get java char array for a member of type char.
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return 1D java array of chars
   */
  public char[] getJavaArrayChar(StructureMembers.Member m) {
    Array data = getArray(m);
    return (char []) data.getStorage();
  }


  /////

  /**
   * Get String value, from rank 0 String or rank 1 char member array.
   * For more efficiency, use getScalarString(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public String getScalarString(String memberName) {
    return getScalarString( findMember(memberName));
  }

  /**
   * Get String value, from rank 0 String or rank 1 char member array.
   * @param m get data from this StructureMembers.Member. Must be of type char or String.
   */
  public String getScalarString(StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRING) {
      Array data = getArray(m);
      return (String) data.getObject(0);
    } else {
      char[] ba = getJavaArrayChar(m);
      int count = 0;
      while (count < ba.length) {
        if (0 == ba[count]) break;
        count++;
      }
      return new String(ba, 0, count);
    }
  }

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
      String[] result = new String[ iter.getNumElems()];
      int count = 0;
      while (iter.hasNext())
        result[count++] =  iter.next();
      return result;
    }

    throw new IllegalArgumentException("getJavaArrayString: not String DataType :"+m.getDataType());
  }

  /**
    * Get member data of type Structure.
    * For more efficiency, use getScalarStructure(StructureMembers.Member m) if possible.
    * @param memberName name of member Variable.
    * @throws IllegalArgumentException if name is not legal member name.
    */
   public StructureData getScalarStructure(String memberName) {
     return getScalarStructure( findMember(memberName));
   }

  /**
   * Get member data of type Structure.
   * @param m get data from this StructureMembers.Member. Must be of type Structure.
   * @return StructureData
   */
  public StructureData getScalarStructure(StructureMembers.Member m) {
    ArrayStructure data = (ArrayStructure) getArray(m);
    return data.getStructureData(0);
  }


  /**
   * Get member data of type Structure.
   * @param m get data from this StructureMembers.Member. Must be of type Structure.
   * @return StructureData
   */
  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return (ArrayStructure) getArray(m);
  }

  public ArraySequence2 getArraySequence(StructureMembers.Member m) {
    throw new UnsupportedOperationException("getArraySequence");
  }


}
