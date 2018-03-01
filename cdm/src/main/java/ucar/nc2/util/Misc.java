/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

/**
 * Miscellaneous static routines.
 *
 * @author caron
 */
public class Misc {
  public static final int referenceSize = 4;   // estimates pointer size, in principle JVM dependent
  public static final int objectSize = 16;   // estimates pointer size, in principle JVM dependent

  /**
   * The default maximum {@link #relativeDifference(float, float) relative difference} that two floats can have in
   * order to be deemed {@link #nearlyEquals(float, float) nearly equal}.
   */
  public static final float defaultMaxRelativeDiffFloat = 1.0e-5f;

  /**
   * The default maximum {@link #relativeDifference(double, double) relative difference} that two doubles can have in
   * order to be deemed {@link #nearlyEquals(double, double) nearly equal}.
   */
  public static final double defaultMaxRelativeDiffDouble = 1.0e-8;

  /**
   * Returns the absolute difference between two numbers, i.e. {@code |a - b|}.
   *
   * @param a  first number.
   * @param b  second number.
   * @return   the absolute difference.
   */
  public static float absoluteDifference(float a, float b) {
    return Math.abs(a - b);
  }

  /** Same as {@link #absoluteDifference(float, float)}, but for doubles. */
  public static double absoluteDifference(double a, double b) {
    return Math.abs(a - b);
  }

  /**
   * Returns the relative difference between two numbers, i.e. {@code |a - b| / max(|a|, |b|)}.
   * <p>
   * For cases where {@code a == 0}, {@code b == 0}, or {@code a} and {@code b} are extremely close, traditional
   * relative difference calculation breaks down. So, in those instances, we compute the difference relative to
   * {@link Float#MIN_NORMAL}, i.e. {@code |a - b| / Float.MIN_NORMAL}.
   *
   * @param a  first number.
   * @param b  second number.
   * @return   the relative difference.
   * @see <a href="http://floating-point-gui.de/errors/comparison/">The Floating-Point Guide</a>
   * @see <a href="https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/">
   *          Comparing Floating Point Numbers, 2012 Edition</a>
   */
  public static float relativeDifference(float a, float b) {
    float absDiff = absoluteDifference(a, b);

    if (a == b) {  // shortcut, handles infinities and NaNs
      return 0;
    } else if (a == 0 || b == 0 || absDiff < Float.MIN_NORMAL) {
      return absDiff / Float.MIN_NORMAL;
    } else {
      float maxAbsValue = Math.max(Math.abs(a), Math.abs(b));
      return absDiff / maxAbsValue;
    }
  }

  /** Same as {@link #relativeDifference(float, float)}, but for doubles. */
  public static double relativeDifference(double a, double b) {
    double absDiff = absoluteDifference(a, b);

    if (a == b) {  // shortcut, handles infinities and NaNs
      return 0;
    } else if (a == 0 || b == 0 || absDiff < Double.MIN_NORMAL) {
      return absDiff / Double.MIN_NORMAL;
    } else {
      double maxAbsValue = Math.max(Math.abs(a), Math.abs(b));
      return absDiff / maxAbsValue;
    }
  }

  /** Returns the result of {@link #nearlyEquals(float, float, float)}, with {@link #defaultMaxRelativeDiffFloat}. */
  public static boolean nearlyEquals(float a, float b) {
    return nearlyEquals(a, b, defaultMaxRelativeDiffFloat);
  }

  /**
   * Returns {@code true} if {@code a} and {@code b} are nearly equal. Specifically, it checks whether the
   * {@link #relativeDifference(float, float) relative difference} of the two numbers is less than {@code maxRelDiff}.
   *
   * @param a  first number.
   * @param b  second number.
   * @param maxRelDiff  the maximum {@link #relativeDifference relative difference} the two numbers may have.
   * @return {@code true} if {@code a} and {@code b} are nearly equal.
   */
  public static boolean nearlyEquals(float a, float b, float maxRelDiff) {
    return relativeDifference(a, b) < maxRelDiff;
  }

  /**
   * Returns the result of {@link #nearlyEquals(double, double, double)}, with {@link #defaultMaxRelativeDiffDouble}.
   */
  public static boolean nearlyEquals(double a, double b) {
    return nearlyEquals(a, b, defaultMaxRelativeDiffDouble);
  }

  /** Same as {@link #nearlyEquals(float, float, float)}, but for doubles. */
  public static boolean nearlyEquals(double a, double b, double maxRelDiff) {
    return relativeDifference(a, b) < maxRelDiff;
  }

  /**
   * Check if two numbers are nearly equal with given absolute tolerance.
   *
   * @param a  first number.
   * @param b  second number.
   * @param maxAbsDiff  the maximum {@link #absoluteDifference absolute difference} the two numbers may have.
   * @return true if within tolerance.
   */
  public static boolean nearlyEqualsAbs(float a, float b, float maxAbsDiff) {
    return absoluteDifference(a, b) <= Math.abs(maxAbsDiff);
  }

  /** Same as {@link #nearlyEqualsAbs(float, float, float)}, but with doubles. */
  public static boolean nearlyEqualsAbs(double a, double b, double maxAbsDiff) {
    return absoluteDifference(a, b) <= Math.abs(maxAbsDiff);
  }


  static public String showInts(int[] inta) {
    if (inta == null) return "null";
    Formatter f = new Formatter();
    for (int i : inta) f.format("%d,", i);
    return f.toString();
  }

  static public String showInts(List<Integer> intList) {
    if (intList == null) return "null";
    Formatter f = new Formatter();
    for (int i : intList) f.format("%d,", i);
    return f.toString();
  }

  static public void showInts(int[] inta, Formatter f) {
    if (inta == null) {
      f.format("null");
      return;
    }
    for (int i : inta) f.format("%d, ", i);
  }

  static public String showBytes(byte[] buff) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0) sbuff.append(" ");
      sbuff.append(ub);
    }
    return sbuff.toString();
  }

  static public void showBytes(byte[] buff, Formatter f) {
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      f.format("%3d ", ub);
    }
  }

  static public int getSize(Iterable ii) {
    if (ii instanceof Collection)
      return ((Collection) ii).size();
    int count = 0;
    for (Object i : ii) count++;
    return count;
  }

  static public List getList(Iterable ii) {
    if (ii instanceof List)
      return (List) ii;
    List<Object> result = new ArrayList<>();
    for (Object i : ii) result.add(i);
    return result;
  }

  //////////////////////////////////////////////////////////////////////

  static public boolean compare(byte[] raw1, byte[] raw2, Formatter f) {
    if (raw1 == null || raw2 == null) return false;

    if (raw1.length != raw2.length) {
      f.format("length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (raw1[i] != raw2[i]) {
        f.format(" %3d : %3d != %3d%n", i + 1, raw1[i], raw2[i]);
        ndiff++;
      }
    }
    f.format("tested %d bytes  diff = %d %n", len, ndiff);
    return ndiff == 0 && (raw1.length == raw2.length);
  }

  static public void compare(float[] raw1, float[] raw2, Formatter f) {
    if (raw1.length != raw2.length) {
      f.format("compareFloat: length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (!Misc.nearlyEquals(raw1[i], raw2[i]) && !Double.isNaN(raw1[i]) && !Double.isNaN(raw2[i])) {
        f.format(" %5d : %3f != %3f%n", i, raw1[i], raw2[i]);
        ndiff++;
      }
    }
    f.format("tested %d floats diff = %d %n", len, ndiff);
  }

  // from Java7
  public static int compare(int x, int y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  public static int compare(long x, long y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  public static String stackTraceToString(StackTraceElement[] stackTrace) {
    StringBuffer buf = new StringBuffer();
    for (StackTraceElement ste : stackTrace) {
      buf.append(ste.toString());
      buf.append("\n");
    }
    return buf.toString();
  }

}
