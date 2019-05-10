/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.shapefile;

/**
 * The Swap class provides static methods for swapping the bytes of chars,
 * shorts, ints, longs, floats, and doubles.
 *
 * @author Kirk Waters
 * @author Russ Rew, 1998, added documentation
 */

public class Swap {

  /**
   * Returns the short resulting from swapping 2 bytes at a specified
   * offset in a byte array.
   *
   * @param b      the byte array
   * @param offset the offset of the first byte
   * @return the short represented by the bytes
   *         <code>b[offset+1], b[offset]</code>
   */
  public static short swapShort(byte[] b, int offset) {
    // 2 bytes
    int low = b[offset] & 0xff;
    int high = b[offset + 1] & 0xff;
    return (short) (high << 8 | low);
  }

  /**
   * Returns the int resulting from reversing 4 bytes at a specified
   * offset in a byte array.
   *
   * @param b      the byte array
   * @param offset the offset of the first byte
   * @return the int represented by the bytes
   *         <code>b[offset+3], b[offset+2], ..., b[offset]</code>
   */
  public static int swapInt(byte[] b, int offset) {
    // 4 bytes
    int accum = 0;
    for (int shiftBy = 0, i = offset; shiftBy < 32; shiftBy += 8, i++)
      accum |= (b[i] & 0xff) << shiftBy;
    return accum;
  }

  /**
   * Returns the long resulting from reversing 8 bytes at a specified
   * offset in a byte array.
   *
   * @param b      the byte array
   * @param offset the offset of the first byte
   * @return the long represented by the bytes
   *         <code>b[offset+7], b[offset+6], ..., b[offset]</code>
   */
  public static long swapLong(byte[] b, int offset) {
    // 8 bytes
    long accum = 0;
    long shiftedval;
    for (int shiftBy = 0, i = offset; shiftBy < 64; shiftBy += 8, i++) {
      shiftedval = ((long) (b[i] & 0xff)) << shiftBy;
      accum |= shiftedval;
    }
    return accum;
  }

  /**
   * Returns the float resulting from reversing 4 bytes at a specified
   * offset in a byte array.
   *
   * @param b      the byte array
   * @param offset the offset of the first byte
   * @return the float represented by the bytes
   *         <code>b[offset+3], b[offset+2], ..., b[offset]</code>
   */
  public static float swapFloat(byte[] b, int offset) {
    int accum = 0;
    for (int shiftBy = 0, i = offset; shiftBy < 32; shiftBy += 8, i++)
      accum |= (b[i] & 0xff) << shiftBy;
    return Float.intBitsToFloat(accum);
  }

  /**
   * Returns the double resulting from reversing 8 bytes at a specified
   * offset in a byte array.
   *
   * @param b      the byte array
   * @param offset the offset of the first byte
   * @return the double represented by the bytes
   *         <code>b[offset+7], b[offset+6], ..., b[offset]</code>
   */
  public static double swapDouble(byte[] b, int offset) {
    long accum = 0;
    long shiftedval;
    for (int shiftBy = 0, i = offset; shiftBy < 64; shiftBy += 8, i++) {
      shiftedval = ((long) (b[i] & 0xff)) << shiftBy;
      accum |= shiftedval;
    }
    return Double.longBitsToDouble(accum);
  }

  /**
   * Returns the char resulting from swapping 2 bytes at a specified
   * offset in a byte array.
   *
   * @param b      the byte array
   * @param offset the offset of the first byte
   * @return the char represented by the bytes
   *         <code>b[offset+1], b[offset]</code>
   */
  public static char swapChar(byte[] b, int offset) {
    // 2 bytes
    int low = b[offset] & 0xff;
    int high = b[offset + 1] & 0xff;
    return (char) (high << 8 | low);
  }

  /**
   * Returns the short resulting from swapping 2 bytes of a specified
   * short.
   *
   * @param s input value for which byte reversal is desired
   * @return the value represented by the bytes of <code>s</code>
   *         reversed
   */
  public static short swapShort(short s) {
    return (swapShort(shortToBytes(s), 0));
  }

  /**
   * Returns the int resulting from reversing 4 bytes of a specified
   * int.
   *
   * @param v input value for which byte reversal is desired
   * @return the value represented by the bytes of <code>v</code>
   *         reversed
   */
  public static int swapInt(int v) {
    return (swapInt(intToBytes(v), 0));
  }

  /**
   * Returns the long resulting from reversing 8 bytes of a specified
   * long.
   *
   * @param l input value for which byte reversal is desired
   * @return the value represented by the bytes of <code>l</code>
   *         reversed
   */
  public static long swapLong(long l) {
    return (swapLong(longToBytes(l), 0));
  }

  /**
   * Returns the float resulting from reversing 4 bytes of a specified
   * float.
   *
   * @param v input value for which byte reversal is desired
   * @return the value represented by the bytes of <code>v</code>
   *         reversed
   */
  public static float swapFloat(float v) {
    int l = swapInt(Float.floatToIntBits(v));
    return (Float.intBitsToFloat(l));
  }

  /**
   * Returns the double resulting from reversing 8 bytes of a specified
   * double.
   *
   * @param v input value for which byte reversal is desired
   * @return the value represented by the bytes of <code>v</code>
   *         reversed
   */
  public static double swapDouble(double v) {
    long l = swapLong(Double.doubleToLongBits(v));
    return (Double.longBitsToDouble(l));
  }

  /**
   * Convert a short to an array of 2 bytes.
   *
   * @param v input value
   * @return the corresponding array of bytes
   */
  public static byte[] shortToBytes(short v) {
    byte[] b = new byte[2];
    int allbits = 255;
    for (int i = 0; i < 2; i++) {
      b[1 - i] = (byte) ((v & (allbits << i * 8)) >> i * 8);
    }
    return b;
  }

  /**
   * Convert an int to an array of 4 bytes.
   *
   * @param v input value
   * @return the corresponding array of bytes
   */
  public static byte[] intToBytes(int v) {
    byte[] b = new byte[4];
    int allbits = 255;
    for (int i = 0; i < 4; i++) {
      b[3 - i] = (byte) ((v & (allbits << i * 8)) >> i * 8);
    }
    return b;
  }

  /**
   * Convert a long to an array of 8 bytes.
   *
   * @param v input value
   * @return the corresponding array of bytes
   */
  public static byte[] longToBytes(long v) {
    byte[] b = new byte[8];
    long allbits = 255;
    for (int i = 0; i < 8; i++) {
      b[7 - i] = (byte) ((v & (allbits << i * 8)) >> i * 8);
    }
    return b;
  }
}
