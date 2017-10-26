/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.ma2;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Type-safe enumeration of data types.
 * Do not change the ordering of these enums, as they are used in protobuf messages, only add new ones onto the end.
 *
 * @author john caron
 */
public enum DataType {
  BOOLEAN("boolean", 1, boolean.class, false),
  BYTE("byte", 1, byte.class, false),
  CHAR("char", 1, char.class, false),
  SHORT("short", 2, short.class, false),
  INT("int", 4, int.class, false),
  LONG("long", 8, long.class, false),
  FLOAT("float", 4, float.class, false),
  DOUBLE("double", 8, double.class, false),

  // object types
  SEQUENCE("Sequence", 4, StructureDataIterator.class, false), // 32-bit index
  STRING("String", 4, String.class, false),     // 32-bit index
  STRUCTURE("Structure", 1, StructureData.class, false), // size meaningless

  ENUM1("enum1", 1, byte.class, false), // byte
  ENUM2("enum2", 2, short.class, false), // short
  ENUM4("enum4", 4, int.class, false), // int

  OPAQUE("opaque", 1, ByteBuffer.class, false), // byte blobs

  OBJECT("object", 1, Object.class, false), // added for use with Array

  UBYTE("ubyte", 1, byte.class, true),
  USHORT("ushort", 2, short.class, true),
  UINT("uint", 4, int.class, true),
  ULONG("ulong", 8, long.class, true);

  /**
   * A property of {@link #isIntegral() integral} data types that determines whether they can represent both
   * positive and negative numbers (signed), or only non-negative numbers (unsigned).
   */
  public enum Signedness {
    /** The data type can represent both positive and negative numbers. */
    SIGNED,
    /** The data type can represent only non-negative numbers. */
    UNSIGNED
  }

  private final String niceName;
  private final int size;
  private final Class primitiveClass;
  private final Signedness signedness;

  DataType(String s, int size, Class primitiveClass, boolean isUnsigned) {
    this(s, size, primitiveClass, isUnsigned ? Signedness.UNSIGNED : Signedness.SIGNED);
  }

  DataType(String s, int size, Class primitiveClass, Signedness signedness) {
    this.niceName = s;
    this.size = size;
    this.primitiveClass = primitiveClass;
    this.signedness = signedness;
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

  /**
   * The primitive class type: char, byte, float, double, short, int, long, boolean, String, StructureData,
   * StructureDataIterator, ByteBuffer.
   *
   * @return the primitive class type
   */
  public Class getPrimitiveClassType() {
    return primitiveClass;
  }

  /**
   * Returns the {@link Signedness signedness} of this data type.
   * For non-{@link #isIntegral() integral} data types, it is guaranteed to be {@link Signedness#SIGNED}.
   *
   * @return  the signedness of this data type.
   */
  public Signedness getSignedness() {
    return signedness;
  }

  /**
   * Returns {@code true} if the data type is {@link Signedness#UNSIGNED unsigned}.
   * For non-{@link #isIntegral() integral} data types, it is guaranteed to be {@code false}.
   *
   * @return  {@code true} if the data type is unsigned.
   */
  public boolean isUnsigned() {
    return signedness == Signedness.UNSIGNED;
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
    return (this == DataType.FLOAT) || (this == DataType.DOUBLE) || isIntegral();
  }

  /**
   * Is Byte, Int, Short, or Long
   *
   * @return true if integral
   */
  public boolean isIntegral() {
    return (this == DataType.BYTE) || (this == DataType.INT) || (this == DataType.SHORT) || (this == DataType.LONG) ||
           (this == DataType.UBYTE) || (this == DataType.UINT) || (this == DataType.USHORT) || (this == DataType.ULONG);
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

  /**
   * Returns a DataType that is related to {@code this}, but with the specified signedness.
   * This method is only meaningful for {@link #isIntegral() integral} data types; if it is called on a non-integral
   * type, then {@code this} is simply returned. Examples:
   * <pre>
   *   assert DataType.INT.withSignedness(DataType.Signedness.UNSIGNED) == DataType.UINT;       // INT to UINT
   *   assert DataType.ULONG.withSignedness(DataType.Signedness.SIGNED) == DataType.LONG;       // ULONG to LONG
   *   assert DataType.SHORT.withSignedness(DataType.Signedness.SIGNED) == DataType.SHORT;      // this: Same signs
   *   assert DataType.STRING.withSignedness(DataType.Signedness.UNSIGNED) == DataType.STRING;  // this: Non-integral
   * </pre>
   *
   * @param signedness  the desired signedness of the returned DataType.
   * @return  a DataType that is related to {@code this}, but with the specified signedness.
   */
  public DataType withSignedness(Signedness signedness) {
    switch (this) {
      case BYTE:
      case UBYTE:
        return signedness == Signedness.UNSIGNED ? UBYTE : BYTE;
      case SHORT:
      case USHORT:
        return signedness == Signedness.UNSIGNED ? USHORT : SHORT;
      case INT:
      case UINT:
        return signedness == Signedness.UNSIGNED ? UINT : INT;
      case LONG:
      case ULONG:
        return signedness == Signedness.UNSIGNED ? ULONG : LONG;
    }
    return this;
  }

  public boolean
  isEnumCompatible(DataType inferred)
  {
    if(inferred == null) return false;
    if(this == inferred) return true;
    switch (this) {
    case ENUM1:
      return inferred == DataType.BYTE || inferred == DataType.STRING;
    case ENUM2:
      return inferred == DataType.SHORT || inferred == DataType.STRING;
    case ENUM4:
      return inferred == DataType.INT || inferred == DataType.STRING;
    default:
      break;
    }
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  static public DataType
  enumTypeize(DataType dt)
  {
    switch (dt) {
    case BYTE:
    case UBYTE:
      return ENUM1;
    case SHORT:
    case USHORT:
      return ENUM2;
    case INT:
    case UINT:
      return ENUM4;
    default: break;
    }
    return dt;
  }

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

  public static DataType getType(Array arr) {
    return getType(arr.getElementType(), arr.isUnsigned());
  }

  /**
   * Find the DataType that matches this class.
   *
   * @param c primitive or object class, eg float.class or Float.class
   * @return DataType or null if no match.
   */
  public static DataType getType(Class c, boolean isUnsigned) {
    if ((c == float.class) || (c == Float.class)) return DataType.FLOAT;
    if ((c == double.class) || (c == Double.class)) return DataType.DOUBLE;
    if ((c == short.class) || (c == Short.class)) return isUnsigned ? DataType.USHORT : DataType.SHORT;
    if ((c == int.class) || (c == Integer.class)) return isUnsigned ? DataType.UINT : DataType.INT;
    if ((c == byte.class) || (c == Byte.class)) return isUnsigned ? DataType.UBYTE : DataType.BYTE;
    if ((c == char.class) || (c == Character.class)) return DataType.CHAR;
    if ((c == boolean.class) || (c == Boolean.class)) return DataType.BOOLEAN;
    if ((c == long.class) || (c == Long.class)) return isUnsigned ? DataType.ULONG : DataType.LONG;
    if (c == String.class) return DataType.STRING;
    if (c == StructureData.class) return DataType.STRUCTURE;
    if (c == StructureDataIterator.class) return DataType.SEQUENCE;
    if (c == ByteBuffer.class) return DataType.OPAQUE;
    return DataType.OBJECT;
  }

  /**
   * convert an unsigned long to a String
   *
   * @param li unsigned int
   * @return equivilent long value
   */
  static public String unsignedLongToString(long li) {
    if (li >= 0) return Long.toString(li);

    // else do the hard part - see http://technologicaloddity.com/2010/09/22/biginteger-as-unsigned-long-in-java/
    byte[] val = new byte[8];
    for (int i = 0; i < 8; i++) {
      val[7 - i] = (byte) ((li) & 0xFF);
      li = li >>> 8;
    }

    BigInteger biggy = new BigInteger(1, val);
    return biggy.toString();
  }

  /**
   * Return a number that is equivalent to the specified value, but represented by the next larger data type.
   * For example, a short will be widened to an int and a long will be widened to a {@link BigInteger}.
   *
   * @param number  a number.
   * @return  a wider Number with the same value.
   */
  // Tested indirectly in TestMAMath.convertUnsigned()
  public static Number widenNumber(Number number) {
    if (number instanceof BigInteger) {
      return number;  // No need to widen a BigInteger.
    } else if (number instanceof Long) {
      return unsignedLongToBigInt(number.longValue());
    } else if (number instanceof Integer) {
      return unsignedIntToLong(number.intValue());
    } else if (number instanceof Short) {
      return unsignedShortToInt(number.shortValue());
    } else if (number instanceof Byte) {
      return unsignedByteToShort(number.byteValue());
    } else {
      throw new IllegalArgumentException(String.format(
              "%s is an unsupported Number subtype.", number.getClass().getSimpleName()));
    }
  }

  static final private BigInteger BIG_UMASK64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);

  /**
   * Widen an unsigned long to a {@link BigInteger}.
   *
   * @param l  an unsigned long
   * @return   the equivalent {@link BigInteger} value.
   */
  // Tested indirectly in TestMAMath.convertUnsigned()
  static public BigInteger unsignedLongToBigInt(long l) {
    BigInteger bi = BigInteger.valueOf(l);
    return bi.and(BIG_UMASK64);
  }

  /**
   * Widen an unsigned int to a long.
   *
   * @param i  an unsigned int.
   * @return   the equivalent long value.
   */
  // Tested indirectly in TestMAMath.convertUnsigned()
  static public long unsignedIntToLong(int i) {
    return (i & 0xffffffffL);
  }

  /**
   * Widen an unsigned short to an int.
   *
   * @param s  an unsigned short.
   * @return   the equivalent int value.
   */
  // Tested indirectly in TestMAMath.convertUnsigned()
  static public int unsignedShortToInt(short s) {
    return (s & 0xffff);
  }

  /**
   * Widen an unsigned byte to a short.
   *
   * @param b  an unsigned byte.
   * @return   the equivalent short value.
   */
  // Tested indirectly in TestMAMath.convertUnsigned()
  static public short unsignedByteToShort(byte b) {
    // b is a byte and 0xFF is an int. The Java spec says: "When operands are of different types,
    // automatic binary numeric promotion occurs with the smaller operand type being converted to the larger."
    // So, for the AND operation, both values will be ints.
    return (short) (b & 0xff);
  }
}
