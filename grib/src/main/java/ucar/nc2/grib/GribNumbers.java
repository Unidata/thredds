/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.ma2.DataType;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * Utilities for reading and interpreting GRIB bytes.
 */
public final class GribNumbers {
  /**
   * If missing value is not defined use this value.
   */
  public static final int UNDEFINED = -9999;
  public static final double UNDEFINEDD = -9999.0;

  private static final int[] bitmask = {128, 64, 32, 16, 8, 4, 2, 1};

  /**
   * Test if the given gribBitNumber is set in the test value.
   * @param value test the 8 bits in this value .
   * @param gribBitNumber one based, starting from highest bit. Must be between 1-8.
   * @return true if the given gribBitNumber is set.
   */
  public static boolean testGribBitIsSet(int value, int gribBitNumber) {
    return (value & bitmask[gribBitNumber-1]) != 0;
  }

  /**
   * Test if the given bit is set in the test value.
   * @param value test the 8 bits in this value .
   * @param bit zero based, starting from highest bit. Must be between 0-7.
   * @return true if the given bit is set.
   */
  public static boolean testBitIsSet(int value, int bit) {
    return (value & bitmask[bit]) != 0;
  }

  /**
   * Grib uses this internally to mean missing
   */
  public static final int MISSING = 255;

  public static boolean isUndefined(double d) {
    return Double.compare(d, UNDEFINEDD) == 0;
  }

  /**
   * Convert 2 bytes into a signed integer.
   *
   * @param raf read from here
   * @return integer value
   * @throws IOException on read error
   */
  public static int int2(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();

    return int2(a, b);
  }

  /**
    * Convert unsigned bytes into an integer.
    *
    * @param raf read one byte from here
    * @return integer value
    * @throws IOException on read error
    */
   public static int uint(RandomAccessFile raf) throws IOException {
     int a = raf.read();
     return (int) DataType.unsignedByteToShort((byte) a);
   }

   /**
   * convert 2 bytes to a signed integer.
   *
   * @param a first byte
   * @param b second byte
   * @return int
   */
  public static int int2(int a, int b) {
    if (((a == 0xff) && (b == 0xff))) {  // all bits set to one
      return UNDEFINED;
    }

    return (1 - ((a & 128) >> 6)) * ((a & 127) << 8 | b);
  }

  /**
   * Convert 3 bytes into a signed integer.
   *
   * @param raf read from here
   * @return integer value
   * @throws IOException on read error
   */
  public static int int3(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();
    int c = raf.read();

    return int3(a, b, c);
  }

  /**
   * Convert 3 bytes to signed integer.
   *
   * @param a first byte
   * @param b second byte
   * @param c third byte
   * @return int
   */
  public static int int3(int a, int b, int c) {
    return (1 - ((a & 128) >> 6)) * ((a & 127) << 16 | b << 8 | c);
  }

  /**
   * Convert 4 bytes into a signed integer.
   *
   * @param raf read from here
   * @return integer value
   * @throws IOException on read error
   */
  public static int int4(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();
    int c = raf.read();
    int d = raf.read();

    return int4(a, b, c, d);
  }

  /**
   * Convert 4 bytes into a signed integer.
   *
   * @param a first byte
   * @param b second byte
   * @param c third byte
   * @param d fourth byte
   * @return int
   */
  public static int int4(int a, int b, int c, int d) {
    // all bits set to ones
    if ((a == 0xff) && (b == 0xff) && (c == 0xff) && (d == 0xff)) {
      return UNDEFINED;
    }

    return (1 - ((a & 128) >> 6))
            * ((a & 127) << 24 | b << 16 | c << 8 | d);
  }  // end int4

  /**
   * Convert 2 bytes into an unsigned integer.
   *
   * @param raf read from here
   * @return integer value
   * @throws IOException on read error
   */
  public static int uint2(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();

    return uint2(a, b);
  }

  /**
   * convert 2 bytes to an unsigned integer.
   *
   * @param a first byte
   * @param b second byte
   * @return unsigned int
   */
  public static int uint2(int a, int b) {
    return a << 8 | b;
  }

  /**
   * Convert 3 bytes into an unsigned integer.
   *
   * @param raf read from here
   * @return integer
   * @throws IOException on read error
   */
  public static int uint3(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();
    int c = raf.read();

    return uint3(a, b, c);
  }

  /**
   * Convert 3 bytes into an unsigned int.
   *
   * @param a first byte
   * @param b second byte
   * @param c third byte
   * @return unsigned integer
   */
  public static int uint3(int a, int b, int c) {
    return a << 16 | b << 8 | c;
  }

   /**
   * Convert 4 bytes into a float value.
   *
   * @param raf read from here
   * @return float value
   * @throws IOException on read error
   */
  public static float float4(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();
    int c = raf.read();
    int d = raf.read();

    return float4(a, b, c, d);
  }

  /**
   * Convert 4 bytes to a float.
   *
   * @param a first byte
   * @param b second byte
   * @param c third byte
   * @param d fourth byte
   * @return float
   */
  public static float float4(int a, int b, int c, int d) {
    int sgn, mant, exp;

    mant = b << 16 | c << 8 | d;
    if (mant == 0) {
      return 0.0f;
    }

    sgn = -(((a & 128) >> 6) - 1);
    exp = (a & 127) - 64;

    return (float) (sgn * Math.pow(16.0, exp - 6) * mant);
  }

  /**
   * Convert 8 bytes into a signed long.
   *
   * @param raf RandomAccessFile
   * @return long value
   * @throws IOException on read error
   */
  public static long int8(RandomAccessFile raf) throws IOException {
    int a = raf.read();
    int b = raf.read();
    int c = raf.read();
    int d = raf.read();
    int e = raf.read();
    int f = raf.read();
    int g = raf.read();
    int h = raf.read();

    return (1 - ((a & 128) >> 6))
            * ((long)(a & 127) << 56 | (long) b << 48 | (long) c << 40 | (long) d << 32 | e << 24
            | f << 16 | g << 8 | h);

  }

  /**
   * A signed byte has a sign bit then 1 15-bit value.
   * This is not twos complement (!)
   * @param v convert byte to signed int
   * @return signed int
   */
  public static int convertSignedByte(byte v) {
    int sign = ((v & 0x80) != 0) ? -1 : 1;
    int value = v & 0x7f;
    return sign * value;
  }

  public static int convertSignedByte2(byte v) {
    return (v >= 0) ? (int) v : -(128 + v);
  }

  // count number of bits on in bitmap
  public static int countBits(byte[] bitmap) {
    int bits = 0;
    for (byte b : bitmap) {
      short s = DataType.unsignedByteToShort(b);
      bits += Long.bitCount(s);
    }
    return bits;
  }

}
