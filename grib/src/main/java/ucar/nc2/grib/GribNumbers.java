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

package ucar.nc2.grib;

import ucar.ma2.DataType;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * Utilities for reading and interpreting GRIB bytes.
 *
 * @author Robb Kambic  10/20/04
 * @version 2.0
 */

public final class GribNumbers {

  /**
   * if missing value is not defined use this value.
   */
  public static final int UNDEFINED = -9999;
  public static final double UNDEFINEDD = -9999.0;


  public static final int[] bitmask = {128, 64, 32, 16, 8, 4, 2, 1};

  // get mask for bit number used in the GRIB docs
  // "If bit 8 of the extended flags" ...
  public static int getMaskForBit(int gribBitNumber) {
    return bitmask[gribBitNumber-1];
  }

  public static boolean testBitIsSet(int test, int gribBitNumber) {
    return (test & GribNumbers.getMaskForBit(gribBitNumber)) != 0;
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


  //////////////////////////////////////////////////////////////////////////

  public static void main(String[] args) {
    System.out.printf("byte == convertSignedByte == convertSignedByte2 == hex%n");
    for (int i=125; i<256;i++) {
      byte b = (byte) i;
      System.out.printf("%d == %d == %d == %s%n", b, convertSignedByte(b), convertSignedByte2(b), Long.toHexString((long) i));
      assert convertSignedByte(b) == convertSignedByte2(b) : convertSignedByte(b) +"!=" +convertSignedByte2(b);
    }

    int val = (int) DataType.unsignedByteToShort((byte) -200);
    int val2 = DataType.unsignedShortToInt((short) -200);
    System.out.printf("%d != %d%n", val, val2);
  }

}
