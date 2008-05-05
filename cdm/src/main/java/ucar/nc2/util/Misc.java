/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.util;

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

  public static boolean closeEnough( double v1, double v2) {
    if (v1 == v2) return true;
    double diff = (v2 == 0.0) ? Math.abs(v1-v2) :  Math.abs(v1/v2-1);
    return diff < maxReletiveError;
  }

  public static boolean closeEnough( float v1, float v2) {
    if (v1 == v2) return true;
    double diff = (v2 == 0.0) ? Math.abs(v1-v2) :  Math.abs(v1/v2-1);
    return diff < maxReletiveError;
  }

  public static void main(String args[]) {
    /* double val = 1.0e-10;
    while (closeEnough(1.0+val, 1.0))
      val = val*10; */

    double val = -1.9531250532132835E-30;
    double val2 = 1.953125E-30;
    closeEnough(val, val2, 1.0e-5);

    /* while (closeEnough(val+diff, val))
      val = val/10;  */
  }


}
