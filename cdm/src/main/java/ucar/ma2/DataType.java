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

package ucar.ma2;

import java.nio.ByteBuffer;

/**
 * Type-safe enumeration of data types.
 *
 * @author john caron
 */

public enum DataType {

  BOOLEAN("boolean", 1),
  BYTE("byte", 1),
  CHAR("char", 1),
  SHORT("short", 2),
  INT("int", 4),
  LONG("long", 8),
  FLOAT("float", 4),
  DOUBLE("double", 8),

  // object types
  SEQUENCE("Sequence", 4), // 32-bit index
  STRING("String", 4),     // 32-bit index
  STRUCTURE("Structure", 1), // size meaningless

  ENUM1("enum1", 1), // byte
  ENUM2("enum2", 2), // short
  ENUM4("enum4", 4), // int

  OPAQUE("opaque", 1); // byte blobs

  private String niceName;
  private int size;

  private DataType(String s, int size) {
    this.niceName = s;
    this.size = size;
  }

  /**
   * The DataType name, eg "byte", "float", "String".
   */
  public String toString() {
    return niceName;
  }

  /**
   * Size in bytes of one element of this data type.
   * Strings dont know, so return 0.
   * Structures return 1.
   *
   * @return Size in bytes of one element of this data type.
   */
  public int getSize() {
    return size;
  }

  /*
  * The Object class type: Character, Byte, Float, Double, Short, Integer, Boolean, Long, String, StructureData.
  * @deprecated use getPrimitiveClassType()
  * @return the primitive class type
  */
  public Class getClassType() {
    return getPrimitiveClassType();
  }

  /**
   * The primitive class type: char, byte, float, double, short, int, long, boolean, String, StructureData, StructureDataIterator,
   *   ByteBuffer.
   *
   * @return the primitive class type
   */
  public Class getPrimitiveClassType() {
    if (this == DataType.FLOAT) return float.class;
    if (this == DataType.DOUBLE) return double.class;
    if ((this == DataType.SHORT) || (this == DataType.ENUM2)) return short.class;
    if ((this == DataType.INT) || (this == DataType.ENUM4)) return int.class;
    if ((this == DataType.BYTE) || (this == DataType.ENUM1)) return byte.class;
    if (this == DataType.CHAR) return char.class;
    if (this == DataType.BOOLEAN) return boolean.class;
    if (this == DataType.LONG) return long.class;
    if (this == DataType.STRING) return String.class;
    if (this == DataType.STRUCTURE) return StructureData.class;
    if (this == DataType.SEQUENCE) return StructureDataIterator.class; 
    if (this == DataType.OPAQUE) return ByteBuffer.class;
    return null;
  }


  /**
   * Is String or Char
   *
   * @return true if String or Char
   */
  public boolean isString() {
    return (this == DataType.STRING) || (this == DataType.CHAR);
  }

  /**
   * Is Byte, Float, Double, Int, Short, or Long
   *
   * @return true if numeric
   */
  public boolean isNumeric() {
    return (this == DataType.BYTE) || (this == DataType.FLOAT) || (this == DataType.DOUBLE) || (this == DataType.INT) ||
            (this == DataType.SHORT) || (this == DataType.LONG);
  }

  /**
   * Is Byte, Int, Short, or Long
   *
   * @return true if integral
   */
  public boolean isIntegral() {
    return (this == DataType.BYTE) || (this == DataType.INT) ||(this == DataType.SHORT) || (this == DataType.LONG);
  }

  /**
   * Is Float or Double
   *
   * @return true if floating point type
   */
  public boolean isFloatingPoint() {
    return (this == DataType.FLOAT) || (this == DataType.DOUBLE);
  }

  /**
   * Is this an enumeration types?
   *
   * @return true if ENUM1, 2, or 4
   */
  public boolean isEnum() {
    return (this == DataType.ENUM1) || (this == DataType.ENUM2) || (this == DataType.ENUM4);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Find the DataType that matches this name.
   *
   * @param name find DataType with this name.
   * @return DataType or null if no match.
   */
  public static DataType getType(String name) {
    if (name == null) return null;
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) { // lame!
      return null;
    }
  }

  /**
   * Find the DataType that matches this class.
   *
   * @param c primitive or object class, eg float.class or Float.class
   * @return DataType or null if no match.
   */
  public static DataType getType(Class c) {
    if ((c == float.class) || (c == Float.class)) return DataType.FLOAT;
    if ((c == double.class) || (c == Double.class)) return DataType.DOUBLE;
    if ((c == short.class) || (c == Short.class)) return DataType.SHORT;
    if ((c == int.class) || (c == Integer.class)) return DataType.INT;
    if ((c == byte.class) || (c == Byte.class)) return DataType.BYTE;
    if ((c == char.class) || (c == Character.class)) return DataType.CHAR;
    if ((c == boolean.class) || (c == Boolean.class)) return DataType.BOOLEAN;
    if ((c == long.class) || (c == Long.class)) return DataType.LONG;
    if (c == String.class) return DataType.STRING;
    if (c == StructureData.class) return DataType.STRUCTURE;
    if (c == StructureDataIterator.class) return DataType.SEQUENCE;
    if (c == ByteBuffer.class) return DataType.OPAQUE;
    return null;
  }

  /**
   * widen an unsigned int to a long
   *
   * @param i unsigned int
   * @return equivilent long value
   */
  static public long unsignedIntToLong(int i) {
    return (i < 0) ? (long) i + 4294967296L : (long) i;
  }

  /**
   * widen an unsigned short to an int
   *
   * @param s unsigned short
   * @return equivilent int value
   */
  static public int unsignedShortToInt(short s) {
    return (s & 0xffff);
  }

  /**
   * widen an unsigned byte to a short
   *
   * @param b unsigned byte
   * @return equivilent short value
   */
  static public short unsignedByteToShort(byte b) {
    return (short) (b & 0xff);
  }

}