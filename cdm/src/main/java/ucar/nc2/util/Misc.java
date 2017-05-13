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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Miscellaneous static routines.
 *
 * @author caron
 */
public class Misc {

  public static final int referenceSize = 4;   // estimates pointer size, in principle JVM dependent
  public static final int objectSize = 16;   // estimates pointer size, in principle JVM dependent

  //private static double maxAbsoluteError = 1.0e-6;
  public static final double maxReletiveError = 1.0e-6;

  static public double howClose(double d1, double d2) {
    double pd = (d1 - d2) / d1;
    return Math.abs(pd);
  }


  /* http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
    http://floating-point-gui.de/errors/comparison/
  bool AlmostEqualRelative(float A, float B, float maxRelDiff)
  {
      // Calculate the difference.
      float diff = fabs(A - B);
      A = fabs(A);
      B = fabs(B);
      // Find the largest
      float largest = (B > A) ? B : A;

      if (diff <= largest * maxRelDiff)
          return true;
      return false;
  } */

  /**
   * Check if numbers are equal with given reletive tolerance
   *
   * @param v1         first floating point number
   * @param v2         second floating point number
   * @param maxRelDiff maximum reletive difference
   * @return true if within tolerance
   */
  public static boolean closeEnough(double v1, double v2, double maxRelDiff) {
    if (Double.isNaN(v1) && Double.isNaN(v2)) return true;
    if (Double.isNaN(v1) || Double.isNaN(v2)) return false;   // prob not needed
    if(v1 == v2) return true; // handle infinities
    double diff = Math.abs(v1 - v2);
    double largest = Math.max(Math.abs(v1), Math.abs(v2));
    return diff <= largest * maxRelDiff;
  }

  /**
   * Check if numbers are equal with default tolerance
   *
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @return true if within tolerance
   */
  public static boolean closeEnough(double v1, double v2) {
    return closeEnough(v1, v2, maxReletiveError);
  }

  public static boolean closeEnough(float v1, float v2, float maxRelDiff) {
    if (Float.isNaN(v1) && Float.isNaN(v2)) return true;
    if (Float.isNaN(v1) || Float.isNaN(v2)) return false;   // prob not needed

    float diff = Math.abs(v1 - v2);
    float largest = Math.max(Math.abs(v1), Math.abs(v2));
    return diff <= largest * maxRelDiff;
  }

  /**
   * Check if numbers are equal with default tolerance
   *
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @return true if within tolerance
   */
  public static boolean closeEnough(float v1, float v2) {
    return closeEnough(v1, v2, maxReletiveError);
  }

  /**
   * Check if numbers are equal with given absolute tolerance
   *
   * @param v1         first floating point number
   * @param v2         second floating point number
   * @param maxAbsDiff maximum absolute difference
   * @return true if within tolerance
   */
  public static boolean closeEnoughAbs(double v1, double v2, double maxAbsDiff) {
    return Math.abs(v1 - v2) <= Math.abs(maxAbsDiff);
  }

  public static boolean closeEnoughAbs(float v1, float v2, float maxAbsDiff) {
    return Math.abs(v1 - v2) <= Math.abs(maxAbsDiff);
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

  /**
   * Filename of the user property file read from the "user.home" directory
   * if the "unidata.testdata2.path" and "unidata.upc.share.path" are not
   * available as system properties.
   */
  private static final String threddsPropFileName = "thredds.properties";
  private static final String testdataDirPropName = "unidata.testdata.path";
  private static String testdataDirPath = null;

  public static String getTestdataDirPath() {
    if (testdataDirPath == null)
      testdataDirPath = System.getProperty(testdataDirPropName);  // Check for system property

    if (testdataDirPath == null) {
      File userHomeDirFile = new File(System.getProperty("user.home"));
      File userThreddsPropsFile = new File(userHomeDirFile, threddsPropFileName);
      if (userThreddsPropsFile.exists() && userThreddsPropsFile.canRead()) {
        Properties userThreddsProps = new Properties();
        try (FileInputStream fin = new FileInputStream(userThreddsPropsFile)) {
          userThreddsProps.load(fin);
        } catch (IOException e) {
          System.out.println("**Failed loading user THREDDS property file: " + e.getMessage());
        }
        if (!userThreddsProps.isEmpty()) {
          testdataDirPath = userThreddsProps.getProperty(testdataDirPropName);
        }
      }
    }

    return testdataDirPath;
  }

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
      if (!Misc.closeEnough(raw1[i], raw2[i]) && !Double.isNaN(raw1[i]) && !Double.isNaN(raw2[i])) {
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
