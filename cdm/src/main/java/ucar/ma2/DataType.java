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
  STRING("String", 1), // LOOK sizes ?
  STRUCTURE("Structure", 1),
  SEQUENCE("Sequence", 4),

  // netcdf4 types
  OPAQUE("opaque", 1), // LOOK KEEP??

  ENUM1("enum1", 1), // byte
  ENUM2("enum2", 2), // short
  ENUM4("enum4", 4); // int

  private String niceName;
  private int size;
  private DataType(String s, int size) {
      this.niceName = s;
      this.size = size;
  }

  /**
   * Find the DataType that matches this name.
   * @param name find DataTYpe with this name.
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
   * @param c primitive or object class, eg float.class or Float.class
   * @return DataType or null if no match.
   */
  public static DataType getType(Class c) {
    if ((c == float.class) || (c == Float.class))
      return DataType.FLOAT;
    if ((c == double.class) || (c == Double.class))
      return DataType.DOUBLE;
    if ((c == short.class) || (c == Short.class))
      return DataType.SHORT;
    if ((c == int.class) || (c == Integer.class))
      return DataType.INT;
    if ((c == byte.class) || (c == Byte.class))
      return DataType.BYTE;
    if ((c == char.class) || (c == Character.class))
      return DataType.CHAR;
    if ((c == boolean.class) || (c == Boolean.class))
      return DataType.BOOLEAN;
    if ((c == long.class) || (c == Long.class))
      return DataType.LONG;
    if (c == String.class)
      return DataType.STRING;
    if (c == StructureData.class)
      return DataType.STRUCTURE;
    return null;
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
    * @return Size in bytes of one element of this data type.
    */
   public int getSize() { return size; }

  /**
   * The primitive class type: char, byte, float, double, short, int, long, boolean, String, StructureData.
   * @return the primitive class type
   */
  public Class getPrimitiveClassType() {
   if (this == DataType.FLOAT)
      return float.class;
    if (this == DataType.DOUBLE)
      return double.class;
    if ((this == DataType.SHORT)  || (this == DataType.ENUM2))
      return short.class;
    if ((this == DataType.INT)  || (this == DataType.ENUM4))
      return int.class;
    if ((this == DataType.BYTE) || (this == DataType.OPAQUE)  || (this == DataType.ENUM1))
      return byte.class;
     if (this == DataType.CHAR)
      return char.class;
     if (this == DataType.BOOLEAN)
      return boolean.class;
    if (this == DataType.LONG)
      return long.class;
    if (this == DataType.STRING)
      return String.class;
    if (this == DataType.STRUCTURE)
      return StructureData.class;
    if (this == DataType.SEQUENCE)
      return ArraySequence.class;
    return null;
  }

  /**
   * The Object class type: Character, Byte, Float, Double, Short, Integer, Boolean, Long, String, StructureData.
   * @return the Object class type
   */
  public Class getClassType() {
    if ((this == DataType.BYTE) || (this == DataType.OPAQUE) || (this == DataType.ENUM1))
      return Byte.class;
    if (this == DataType.FLOAT)
      return Float.class;
    if (this == DataType.DOUBLE)
      return Double.class;
    if ((this == DataType.SHORT) || (this == DataType.ENUM2))
      return Short.class;
    if ((this == DataType.INT) || (this == DataType.ENUM4))
      return Integer.class;
    if (this == DataType.CHAR)
      return Character.class;
    if (this == DataType.BOOLEAN)
      return Boolean.class;
    if (this == DataType.LONG)
      return Long.class;
    if (this == DataType.STRING)
      return String.class;
    if (this == DataType.STRUCTURE)
      return Object.class;
    return null;
  }

  /**
   * Is String or Char
   * @return true if String or Char
   */
  public boolean isString() {
    return (this == DataType.STRING) || (this == DataType.CHAR);
  }

  /**
   * Is Byte, Float, Double, Int, Short, or Long
   * @return true if numeric
   */
  public boolean isNumeric() {
    return (this == DataType.BYTE) || (this == DataType.FLOAT) || (this == DataType.DOUBLE) || (this == DataType.INT) ||
        (this == DataType.SHORT) || (this == DataType.LONG);  
  }

  /**
   * Is tis an enumeration types?
   * @return true if ENUM1, 2, or 4
   */
  public boolean isEnum() {
    return (this == DataType.ENUM1) || (this == DataType.ENUM2)  || (this == DataType.ENUM4);
  }


  /** widen an unsigned int to a long
   * @param i unsigned int
   * @return equivilent long value
   */
  static public long unsignedIntToLong(int i) {
    return (i < 0) ? (long) i + 4294967296L : (long) i;
  }

  /** widen an unsigned short to an int
   * @param s unsigned short
   * @return equivilent int value
   */
   static public int unsignedShortToInt(short s) {
     return (s & 0xffff);
   }

  /** widen an unsigned byte to a short
   * @param b unsigned byte
   * @return equivilent short value
   */
   static public short unsignedByteToShort(byte b) {
     return (short) (b & 0xff);
   }

}