// $Id$
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

import java.util.List;

/**
 * A container for a Structure's data. 
 * Is normally contained within an ArrayStructure, which is an Array of StructureData.
 * This is the abstract supertype for all implementations.
 *
 * @author caron
 * @version $Revision$ $Date$
 */

abstract public class StructureData {

  /**
   * Copy all the data out of 'from' and into a new StructureData.
   * @param from copy from here
   * @return a new StructureData object.
   */
  public static StructureData copy( StructureData from) {
    return new StructureDataW( from);
  }

  /////////////////////////////////////////////////////////
  protected StructureMembers members;

  /**
   * Constructor.
   *
   * @param members    StructureData is always contained in a StructureArray.
   */
  public StructureData(StructureMembers members) {
    this.members = members;
  }

  /**
   * Get name of Structure
   */
  public String getName() {
    return members.getName();
  }

  /**
   * Get StructureMembers object
   */
  public StructureMembers getStructureMembers() {
    return members;
  }

  /**
   * Get List of StructureMembers.Member
   */
  public List getMembers() {
    return members.getMembers();
  }

  /**
   * Get StructureMembers.Member by index
   */
  public StructureMembers.Member getMember(int index) {
    return members.getMember(index);
  }

  /**
   * Find a member by its name.
   *
   * @return StructureMembers.Member matching the name, or null if not found
   */
  public StructureMembers.Member findMember(String memberName) {
    return members.findMember(memberName);
  }

  /**
   * @deprecated use getArray(String memberName), or getArray(StructureMembers.Member m)
   */
  public Array findMemberArray(String memberName) {
   return getArray(memberName);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////


  /**
   * Get member data array of any type as an Array.
   * @param m get data from this StructureMembers.Member.
   * @return Array values.
   */
  abstract public Array getArray(StructureMembers.Member m);

  /**
   * Get member data array of any type as an Array.
   * For more efficiency, use getArray(StructureMembers.Member m).
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public Array getArray(String memberName) {
    StructureMembers.Member m = members.findMember(memberName);
    if (m == null) throw new IllegalArgumentException("illegal member name =" + memberName);
    return getArray(m);
  }

  /**
   * Get member data array of any type as an Object, eg, Float, Double, String etc.
   * @param m get data from this StructureMembers.Member.
   * @return value as Float, Double, etc..
   */
  public Object getScalarObject( StructureMembers.Member m) {
    DataType dataType = m.getDataType();
    //boolean isScalar = m.isScalar();

    if (dataType == DataType.DOUBLE) {
        return new Double( getScalarDouble( m));

    } else if (dataType == DataType.FLOAT) {
      return new Float( getScalarFloat( m));

    } else if (dataType == DataType.BYTE) {
      return new Byte( getScalarByte( m));

    } else if (dataType == DataType.SHORT) {
      return new Short( getScalarShort( m));

    } else if (dataType == DataType.INT) {
      return new Integer( getScalarInt( m));

    } else if (dataType == DataType.LONG) {
      return new Long( getScalarLong( m));

    } else if (dataType == DataType.CHAR) {
      return getScalarString( m);

    } else if (dataType == DataType.STRING) {
      return getScalarString( m);

    } else if (dataType == DataType.STRUCTURE) {
      return getScalarStructure( m);
    }

     throw new RuntimeException("Dont have implemenation for "+dataType);
  }

  /**
   * @deprecated use getScalarObject(m)
   */
  public Object getObject(StructureMembers.Member m) {
    return getScalarObject( m);
  }

  /**
   * Get scalar value as a float, with conversion as needed. Underlying type must be convertible to float.
   * @param m member Variable.
   * @throws ForbiddenConversionException if not convertible to float.
   */
  abstract public float convertScalarFloat(StructureMembers.Member m);

  /**
   * Get scalar value as a double, with conversion as needed. Underlying type must be convertible to double.
   * @param m member Variable.
   * @throws ForbiddenConversionException if not convertible to double.
   */
  abstract public double convertScalarDouble(StructureMembers.Member m);

  /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get double value. Underlying type must be convertible to double.
   * For more efficiency, use getScalarDouble(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public double getScalarDouble(String memberName) {
    Array data = getArray(memberName);
    return data.getDouble(Array.scalarIndex);
  }

  /**
   * Get member data of type double.
   * @param m get data from this StructureMembers.Member. Must be of type double.
   * @return scalar double value
   */
  abstract public double getScalarDouble(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayDouble(m)
   */
  public double[] getArrayDouble(StructureMembers.Member m) {
    return getJavaArrayDouble(m);
  }

  /**
   * Get java double array for a member of type double.
   * @param m get data from this StructureMembers.Member. Must be of type double.
   * @return 1D java array of doubles
   */
  abstract public double[] getJavaArrayDouble(StructureMembers.Member m);

  ////////////////

  /**
   * Get float value. Underlying type must be convertible to float.
   * For more efficiency, use getScalarFloat(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public float getScalarFloat(String memberName) {
    Array data = getArray(memberName);
    return data.getFloat(Array.scalarIndex);
  }

  /**
   * Get member data of type float.
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return scalar double value
   */
  abstract public float getScalarFloat(StructureMembers.Member m);

  /**
   * Get java float array for a member of type float.
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return 1D java array of floats
   */
  abstract public float[] getJavaArrayFloat(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayFloat(m)
   */
  public float[] getArrayFloat(StructureMembers.Member m) {
    return getJavaArrayFloat(m);
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
    return data.getByte(Array.scalarIndex);
  }

  /**
   * Get member data of type byte.
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return scalar byte value
   */
  abstract public byte getScalarByte(StructureMembers.Member m);

    /**
   * Get java byte array for a member of type byte.
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return 1D java array of bytes
   */
  abstract public byte[] getJavaArrayByte(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayByte(m)
   */
  public byte[] getArrayByte(StructureMembers.Member m) {
    return getJavaArrayByte(m);
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
    return data.getInt(Array.scalarIndex);
  }

  /**
   * Get member data of type int.
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return scalar int value
   */
  abstract public int getScalarInt(StructureMembers.Member m);

  /**
   * Get java int array for a member of type int.
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return 1D java array of ints
   */
  abstract public int[] getJavaArrayInt(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayInt(m)
   */
    public int[] getArrayInt(StructureMembers.Member m) {
    return getJavaArrayInt(m);
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
    return data.getShort(Array.scalarIndex);
  }

  /**
   * Get member data of type short.
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return scalar short value
   */
  abstract public short getScalarShort(StructureMembers.Member m);

  /**
   * Get java short array for a member of type short.
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return 1D java array of shorts
   */
  abstract public short[] getJavaArrayShort(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayShort(m)
   */
  public short[] getArrayShort(StructureMembers.Member m) {
    return getJavaArrayShort(m);
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
    return data.getLong(Array.scalarIndex);
  }

  /**
   * Get member data of type long.
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return scalar long value
   */
  abstract public long getScalarLong(StructureMembers.Member m);

  /**
   * Get java long array for a member of type long.
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return 1D java array of longs
   */
  abstract public long[] getJavaArrayLong(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayLong(m)
   */
  public long[] getArrayLong(StructureMembers.Member m) {
    return getJavaArrayLong(m);
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
    return data.getChar(Array.scalarIndex);
  }

  /**
   * Get member data of type char.
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return scalar char value
   */
  abstract public char getScalarChar(StructureMembers.Member m);

  /**
   * Get java char array for a member of type char.
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return 1D java array of chars
   */
  abstract public char[] getJavaArrayChar(StructureMembers.Member m);

  /**
   * @deprecated use getJavaArrayChar(m)
   */
  public char[] getArrayChar(StructureMembers.Member m) {
    return getJavaArrayChar(m);
  }

  /////

  /**
   * Get String value, from rank 0 String or rank 1 char member array.
   * For more efficiency, use getScalarString(StructureMembers.Member m) if possible.
   * @param memberName name of member Variable.
   * @throws IllegalArgumentException if name is not legal member name.
   */
  public String getScalarString(String memberName) {
    StructureMembers.Member m = findMember(memberName);
    if (null == m) throw new IllegalArgumentException("Member not found= " + memberName);
    return getScalarString(m);
  }

  /**
   * Get String value, from rank 0 String or rank 1 char member array.
   * @param m get data from this StructureMembers.Member. Must be of type char or String.
   */
  abstract public String getScalarString(StructureMembers.Member m);

  /**
   * Get java array of Strings for a member of type char or String.
   * @param m get data from this StructureMembers.Member. Must be of type char or String.
   * @return 1D java array of String
   */
  abstract public String[] getJavaArrayString(StructureMembers.Member m);

  /**
    * Get member data of type Structure.
    * For more efficiency, use getScalarStructure(StructureMembers.Member m) if possible.
    * @param memberName name of member Variable.
    * @throws IllegalArgumentException if name is not legal member name.
    */
  public StructureData getScalarStructure(String memberName) {
    StructureMembers.Member m = findMember(memberName);
    if (null == m) throw new IllegalArgumentException("Member not found= " + memberName);
    return getScalarStructure(m);
  }

  /**
   * Get member data of type Structure.
   * @param m get data from this StructureMembers.Member. Must be of type Structure.
   * @return StructureData
   */
  abstract public StructureData getScalarStructure(StructureMembers.Member m);

  /**
   * Get ArrayStructure for a member of type Structure.
   * @param m get data from this StructureMembers.Member. Must be of type Structure.
   * @return ArrayStructure
   */
  abstract public ArrayStructure getArrayStructure(StructureMembers.Member m);
  
}
