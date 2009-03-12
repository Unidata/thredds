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

package ucar.nc2.util;

import java.util.Formatter;

/**
 * Miscellaneous static routines.
 *
 * @author caron
 */
public class Misc {

  private static boolean show = false;
  //private static double maxAbsoluteError = 1.0e-6;
  private static double maxReletiveError = 1.0e-6;

  /*  http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
  public static boolean closeEnough( double v1, double v2) {
    double d1 = Math.abs(v1-v2);
    if (d1 < maxAbsoluteError) return true;

    double diff = (Math.abs(v2) > Math.abs(v1)) ? Math.abs((v1-v2)/v2) :  Math.abs((v1-v2)/v1);
    return diff < maxReletiveError;
  } */

  /**
   * Check if numbers are equal with tolerance
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @param tol reletive tolerence
   * @return true if within tolerance
   */
  public static boolean closeEnough( double v1, double v2, double tol) {
    if (show) {
      double d1 = Math.abs(v1-v2);
      double d3 = Math.abs(v1/v2);
      double d2 = Math.abs((v1/v2)-1);
      System.out.println("v1= "+v1+" v2="+v2+" diff="+d1+" abs(v1/v2)="+d3+" abs(v1/v2-1)="+d2);
    }

    double diff = (v2 == 0.0) ? Math.abs(v1-v2) : Math.abs(v1/v2-1);
    return diff < tol;
  }

  /**
   * Check if numbers are equal with default tolerance
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @return true if within tolerance
   */
  public static boolean closeEnough( double v1, double v2) {
    if (v1 == v2) return true;
    double diff = (v2 == 0.0) ? Math.abs(v1-v2) :  Math.abs(v1/v2-1);
    return diff < maxReletiveError;
  }

  /**
   * Check if numbers are equal with default tolerance
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @return true if within tolerance
   */
  public static boolean closeEnough( float v1, float v2) {
    if (v1 == v2) return true;
    double diff = (v2 == 0.0) ? Math.abs(v1-v2) :  Math.abs(v1/v2-1);
    return diff < maxReletiveError;
  }

  /** test */
  public static void main(String args[]) {
    long val1 = -1;
    long val2 = 234872309;
    int val3 = 2348;
    int val4 = 32;
    Formatter f = new Formatter(System.out);
    f.format("  address            dataPos            offset size\n");
    f.format("  %#-18x %#-18x %5d  %4d%n", val1, val2, val3, val4);

  }


}
