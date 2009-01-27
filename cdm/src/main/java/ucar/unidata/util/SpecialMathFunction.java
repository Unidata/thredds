/*
 * $Id: SpecialMathFunction.java 64 2006-07-12 22:30:50Z edavis $
 *
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


package ucar.unidata.util;


import java.lang.ArithmeticException;


import java.lang.Math;

/*
**************************************************************************
**
**    Class  SpecialFunction
**
**************************************************************************
**    Copyright (C) 1996 Leigh Brookshaw
**
**    This program is free software; you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation; either version 2 of the License, or
**    (at your option) any later version.
**
**    This program is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with this program; if not, write to the Free Software
**    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
**************************************************************************
**
**    This class is an extension of java.lang.Math. It includes a number
**    of special functions not found in the Math class.
**
*************************************************************************/


/**
 * This class contains physical constants and special functions not found
 * in the java.lang.Math class.
 * Like the java.lang.Math class this class is final and cannot be
 * subclassed.
 * All physical constants are in cgs units.
 * <p/>
 * <B>NOTE:</B> These special functions do not necessarily use the fastest
 * or most accurate algorithms.
 *
 * @author Leigh Brookshaw
 * @version $Id: SpecialMathFunction.java 64 2006-07-12 22:30:50Z edavis $
 */


public final class SpecialMathFunction extends Object {

  /*
  ** machine constants
  */

  /**
   * _more_
   */
  private static final double MACHEP = 1.11022302462515654042E-16;

  /**
   * _more_
   */
  private static final double MAXLOG = 7.09782712893383996732E2;

  /**
   * _more_
   */
  private static final double MINLOG = -7.451332191019412076235E2;

  /**
   * _more_
   */
  private static final double MAXGAM = 171.624376956302725;

  /**
   * _more_
   */
  private static final double SQTPI = 2.50662827463100050242E0;

  /**
   * _more_
   */
  private static final double SQRTH = 7.07106781186547524401E-1;

  /*
  ** Physical Constants in cgs Units
  */


  /**
   * _more_
   */
  private static final double LOGPI = 1.14472988584940017414;

  /**
   * Boltzman Constant. Units erg/deg(K)
   */
  public static final double BOLTZMAN = 1.3807e-16;

  /**
   * Elementary Charge. Units statcoulomb
   */
  public static final double ECHARGE = 4.8032e-10;

  /**
   * Electron Mass. Units g
   */
  public static final double EMASS = 9.1095e-28;

  /**
   * Proton Mass. Units g
   */
  public static final double PMASS = 1.6726e-24;

  /**
   * Gravitational Constant. Units dyne-cm^2/g^2
   */
  public static final double GRAV = 6.6720e-08;

  /**
   * Planck constant. Units erg-sec
   */
  public static final double PLANCK = 6.6262e-27;

  /**
   * Speed of Light in a Vacuum. Units cm/sec
   */
  public static final double LIGHTSPEED = 2.9979e10;

  /**
   * Stefan-Boltzman Constant. Units erg/cm^2-sec-deg^4
   */
  public static final double STEFANBOLTZ = 5.6703e-5;

  /**
   * Avogadro Number. Units  1/mol
   */
  public static final double AVOGADRO = 6.0220e23;

  /**
   * Gas Constant. Units erg/deg-mol
   */
  public static final double GASCONSTANT = 8.3144e07;

  /**
   * Gravitational Acceleration at the Earths surface. Units cm/sec^2
   */
  public static final double GRAVACC = 980.67;

  /**
   * Solar Mass. Units g
   */
  public static final double SOLARMASS = 1.99e33;

  /**
   * Solar Radius. Units cm
   */
  public static final double SOLARRADIUS = 6.96e10;

  /**
   * Solar Luminosity. Units erg/sec
   */
  public static final double SOLARLUM = 3.90e33;

  /**
   * Solar Flux. Units erg/cm^2-sec
   */
  public static final double SOLARFLUX = 6.41e10;

  /**
   * Astronomical Unit (radius of the Earth's orbit). Units cm
   */
  public static final double AU = 1.50e13;


  /**
   * Don't let anyone instantiate this class.
   */
  private SpecialMathFunction() {
  }

  /*
  ** Function Methods
  */

  /**
   * Get the log base 10 of a number
   * @param x a double value
   * @return The log<sub>10</sub> of x
   * @throws ArithmeticException if (x < 0)
   */
  static public double log10(double x) throws ArithmeticException {
    if (x <= 0.0)
      throw new ArithmeticException("range exception");
    return Math.log(x) / 2.30258509299404568401;
  }

  /**
   * Get the log base 2 of a number
   * @param x a double value
   * @return The log<sub>2</sub> of x
   * @throws ArithmeticException if (x < 0)
   */
  static public double log2(double x) throws ArithmeticException {
    if (x <= 0.0)
      throw new ArithmeticException("range exception");
    return Math.log(x) / log2;
  }
  private static double log2 = Math.log(2);


  /**
   * @param x a double value
   * @return the hyperbolic cosine of the argument
   * @throws ArithmeticException
   */

  static public double cosh(double x) throws ArithmeticException {
    double a;
    a = x;
    if (a < 0.0) {
      a = Math.abs(x);
    }
    a = Math.exp(a);
    return 0.5 * (a + 1 / a);
  }

  /**
   * @param x a double value
   * @return the hyperbolic sine of the argument
   * @throws ArithmeticException
   */
  static public double sinh(double x) throws ArithmeticException {
    double a;
    if (x == 0.0) {
      return x;
    }
    a = x;
    if (a < 0.0) {
      a = Math.abs(x);
    }
    a = Math.exp(a);
    if (x < 0.0) {
      return -0.5 * (a - 1 / a);
    } else {
      return 0.5 * (a - 1 / a);
    }
  }

  /**
   * @param x a double value
   * @return the hyperbolic tangent of the argument
   * @throws ArithmeticException
   */
  static public double tanh(double x) throws ArithmeticException {
    double a;
    if (x == 0.0) {
      return x;
    }
    a = x;
    if (a < 0.0) {
      a = Math.abs(x);
    }
    a = Math.exp(2.0 * a);
    if (x < 0.0) {
      return -(1.0 - 2.0 / (a + 1.0));
    } else {
      return (1.0 - 2.0 / (a + 1.0));
    }
  }

  /**
   * @param x a double value
   * @return the hyperbolic arc cosine of the argument
   * @throws ArithmeticException
   */

  static public double acosh(double x) throws ArithmeticException {
    if (x < 1.0) {
      throw new ArithmeticException("range exception");
    }
    return Math.log(x + Math.sqrt(x * x - 1));
  }

  /**
   * Compute the hyperbolic arc sine
   *
   * @param xx a double value
   * @return the hyperbolic arc sine of the argument
   * @throws ArithmeticException
   */
  static public double asinh(double xx) throws ArithmeticException {
    double x;
    int sign;
    if (xx == 0.0) {
      return xx;
    }
    if (xx < 0.0) {
      sign = -1;
      x = -xx;
    } else {
      sign = 1;
      x = xx;
    }
    return sign * Math.log(x + Math.sqrt(x * x + 1));
  }

  /**
   * Compute the hyperbolic arc tangent
   *
   * @param x a double value
   * @return the hyperbolic arc tangent of the argument
   * @throws ArithmeticException
   */
  static public double atanh(double x) throws ArithmeticException {
    if ((x > 1.0) || (x < -1.0)) {
      throw new ArithmeticException("range exception");
    }
    return 0.5 * Math.log((1.0 + x) / (1.0 - x));
  }

  /**
   * @param x a double value
   * @return the Bessel function of order 0 of the argument.
   * @throws ArithmeticException
   */

  static public double j0(double x) throws ArithmeticException {
    double ax;

    if ((ax = Math.abs(x)) < 8.0) {
      double y = x * x;
      double ans1 = 57568490574.0
          + y * (-13362590354.0
          + y * (651619640.7
          + y * (-11214424.18
          + y * (77392.33017
          + y * (-184.9052456)))));
      double ans2 = 57568490411.0
          + y * (1029532985.0
          + y * (9494680.718
          + y * (59272.64853
          + y * (267.8532712 + y * 1.0))));

      return ans1 / ans2;

    } else {
      double z = 8.0 / ax;
      double y = z * z;
      double xx = ax - 0.785398164;
      double ans1 = 1.0
          + y * (-0.1098628627e-2
          + y * (0.2734510407e-4
          + y * (-0.2073370639e-5
          + y * 0.2093887211e-6)));
      double ans2 = -0.1562499995e-1
          + y * (0.1430488765e-3
          + y * (-0.6911147651e-5
          + y * (0.7621095161e-6
          - y * 0.934935152e-7)));

      return Math.sqrt(0.636619772 / ax)
          * (Math.cos(xx) * ans1 - z * Math.sin(xx) * ans2);
    }
  }

  /**
   * @param x a double value
   * @return the Bessel function of order 1 of the argument.
   * @throws ArithmeticException
   */

  static public double j1(double x) throws ArithmeticException {

    double ax;
    double y;
    double ans1, ans2;

    if ((ax = Math.abs(x)) < 8.0) {
      y = x * x;
      ans1 = x * (72362614232.0
          + y * (-7895059235.0
          + y * (242396853.1
          + y * (-2972611.439
          + y
          * (15704.48260
          + y * (-30.16036606))))));
      ans2 = 144725228442.0
          + y * (2300535178.0
          + y * (18583304.74
          + y * (99447.43394
          + y * (376.9991397 + y * 1.0))));
      return ans1 / ans2;
    } else {
      double z = 8.0 / ax;
      double xx = ax - 2.356194491;
      y = z * z;

      ans1 = 1.0
          + y * (0.183105e-2
          + y * (-0.3516396496e-4
          + y * (0.2457520174e-5
          + y * (-0.240337019e-6))));
      ans2 = 0.04687499995
          + y * (-0.2002690873e-3
          + y * (0.8449199096e-5
          + y * (-0.88228987e-6
          + y * 0.105787412e-6)));
      double ans = Math.sqrt(0.636619772 / ax)
          * (Math.cos(xx) * ans1 - z * Math.sin(xx) * ans2);
      if (x < 0.0) {
        ans = -ans;
      }
      return ans;
    }
  }

  /**
   * @param n integer order
   * @param x a double value
   * @return the Bessel function of order n of the argument.
   * @throws ArithmeticException
   */
  static public double jn(int n, double x) throws ArithmeticException {
    int j, m;
    double ax, bj, bjm, bjp, sum, tox, ans;
    boolean jsum;

    double ACC = 40.0;
    double BIGNO = 1.0e+10;
    double BIGNI = 1.0e-10;

    if (n == 0) {
      return j0(x);
    }
    if (n == 1) {
      return j1(x);
    }

    ax = Math.abs(x);
    if (ax == 0.0) {
      return 0.0;
    } else if (ax > (double) n) {
      tox = 2.0 / ax;
      bjm = j0(ax);
      bj = j1(ax);
      for (j = 1; j < n; j++) {
        bjp = j * tox * bj - bjm;
        bjm = bj;
        bj = bjp;
      }
      ans = bj;
    } else {
      tox = 2.0 / ax;
      m = 2 * ((n + (int) Math.sqrt(ACC * n)) / 2);
      jsum = false;
      bjp = ans = sum = 0.0;
      bj = 1.0;
      for (j = m; j > 0; j--) {
        bjm = j * tox * bj - bjp;
        bjp = bj;
        bj = bjm;
        if (Math.abs(bj) > BIGNO) {
          bj *= BIGNI;
          bjp *= BIGNI;
          ans *= BIGNI;
          sum *= BIGNI;
        }
        if (jsum) {
          sum += bj;
        }
        jsum = !jsum;
        if (j == n) {
          ans = bjp;
        }
      }
      sum = 2.0 * sum - bj;
      ans /= sum;
    }
    return ((x < 0.0) && (n % 2 == 1))
        ? -ans
        : ans;
  }

  /**
   * @param x a double value
   * @return the Bessel function of the second kind,
   *         of order 0 of the argument.
   * @throws ArithmeticException
   */

  static public double y0(double x) throws ArithmeticException {

    if (x < 8.0) {
      double y = x * x;

      double ans1 = -2957821389.0
          + y * (7062834065.0
          + y * (-512359803.6
          + y * (10879881.29
          + y * (-86327.92757
          + y * 228.4622733))));
      double ans2 = 40076544269.0
          + y * (745249964.8
          + y * (7189466.438
          + y * (47447.26470
          + y * (226.1030244 + y * 1.0))));

      return (ans1 / ans2) + 0.636619772 * j0(x) * Math.log(x);
    } else {
      double z = 8.0 / x;
      double y = z * z;
      double xx = x - 0.785398164;

      double ans1 = 1.0
          + y * (-0.1098628627e-2
          + y * (0.2734510407e-4
          + y * (-0.2073370639e-5
          + y * 0.2093887211e-6)));
      double ans2 = -0.1562499995e-1
          + y * (0.1430488765e-3
          + y * (-0.6911147651e-5
          + y * (0.7621095161e-6
          + y * (-0.934945152e-7))));
      return Math.sqrt(0.636619772 / x)
          * (Math.sin(xx) * ans1 + z * Math.cos(xx) * ans2);
    }
  }

  /**
   * @param x a double value
   * @return the Bessel function of the second kind,
   *         of order 1 of the argument.
   * @throws ArithmeticException
   */
  static public double y1(double x) throws ArithmeticException {

    if (x < 8.0) {
      double y = x * x;
      double ans1 =
          x * (-0.4900604943e13
              + y * (0.1275274390e13
              + y * (-0.5153438139e11
              + y * (0.7349264551e9
              + y * (-0.4237922726e7
              + y * 0.8511937935e4)))));
      double ans2 = 0.2499580570e14
          + y * (0.4244419664e12
          + y * (0.3733650367e10
          + y * (0.2245904002e8
          + y * (0.1020426050e6
          + y * (0.3549632885e3
          + y)))));
      return (ans1 / ans2)
          + 0.636619772 * (j1(x) * Math.log(x) - 1.0 / x);
    } else {
      double z = 8.0 / x;
      double y = z * z;
      double xx = x - 2.356194491;
      double ans1 = 1.0
          + y * (0.183105e-2
          + y * (-0.3516396496e-4
          + y * (0.2457520174e-5
          + y * (-0.240337019e-6))));
      double ans2 = 0.04687499995
          + y * (-0.2002690873e-3
          + y * (0.8449199096e-5
          + y * (-0.88228987e-6
          + y * 0.105787412e-6)));
      return Math.sqrt(0.636619772 / x)
          * (Math.sin(xx) * ans1 + z * Math.cos(xx) * ans2);
    }
  }

  /**
   * @param n integer order
   * @param x a double value
   * @return the Bessel function of the second kind,
   *         of order n of the argument.
   * @throws ArithmeticException
   */
  static public double yn(int n, double x) throws ArithmeticException {
    double by, bym, byp, tox;

    if (n == 0) {
      return y0(x);
    }
    if (n == 1) {
      return y1(x);
    }

    tox = 2.0 / x;
    by = y1(x);
    bym = y0(x);
    for (int j = 1; j < n; j++) {
      byp = j * tox * by - bym;
      bym = by;
      by = byp;
    }
    return by;
  }


  /**
   * @param x a double value
   * @return the factorial of the argument
   * @throws ArithmeticException
   */
  static public double fac(double x) throws ArithmeticException {
    double d = Math.abs(x);
    if (Math.floor(d) == d) {
      return (double) fac((int) x);
    } else {
      return gamma(x + 1.0);
    }
  }

  /**
   * Compute the factorial of the argument
   *
   * @param j an integer value
   * @return the factorial of the argument
   * @throws ArithmeticException
   */
  static public int fac(int j) throws ArithmeticException {
    int i = j;
    int d = 1;
    if (j < 0) {
      i = Math.abs(j);
    }
    while (i > 1) {
      d *= i--;
    }
    if (j < 0) {
      return -d;
    } else {
      return d;
    }
  }


  /**
   * @param x a double value
   * @return the Gamma function of the value.
   *         <p/>
   *         <FONT size=2>
   *         Converted to Java from<BR>
   *         Cephes Math Library Release 2.2:  July, 1992<BR>
   *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
   *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
   * @throws ArithmeticException
   */
  static public double gamma(double x) throws ArithmeticException {

    double P[] = {
        1.60119522476751861407E-4, 1.19135147006586384913E-3,
        1.04213797561761569935E-2, 4.76367800457137231464E-2,
        2.07448227648435975150E-1, 4.94214826801497100753E-1,
        9.99999999999999996796E-1
    };
    double Q[] = {
        -2.31581873324120129819E-5, 5.39605580493303397842E-4,
        -4.45641913851797240494E-3, 1.18139785222060435552E-2,
        3.58236398605498653373E-2, -2.34591795718243348568E-1,
        7.14304917030273074085E-2, 1.00000000000000000320E0
    };
    double MAXGAM = 171.624376956302725;
    double LOGPI = 1.14472988584940017414;

    double p, z;
    int i;

    double q = Math.abs(x);

    if (q > 33.0) {
      if (x < 0.0) {
        p = Math.floor(q);
        if (p == q) {
          throw new ArithmeticException("gamma: overflow");
        }
        i = (int) p;
        z = q - p;
        if (z > 0.5) {
          p += 1.0;
          z = q - p;
        }
        z = q * Math.sin(Math.PI * z);
        if (z == 0.0) {
          throw new ArithmeticException("gamma: overflow");
        }
        z = Math.abs(z);
        z = Math.PI / (z * stirf(q));

        return -z;
      } else {
        return stirf(x);
      }
    }

    z = 1.0;
    while (x >= 3.0) {
      x -= 1.0;
      z *= x;
    }

    while (x < 0.0) {
      if (x == 0.0) {
        throw new ArithmeticException("gamma: singular");
      } else if (x > -1.E-9) {
        return (z / ((1.0 + 0.5772156649015329 * x) * x));
      }
      z /= x;
      x += 1.0;
    }

    while (x < 2.0) {
      if (x == 0.0) {
        throw new ArithmeticException("gamma: singular");
      } else if (x < 1.e-9) {
        return (z / ((1.0 + 0.5772156649015329 * x) * x));
      }
      z /= x;
      x += 1.0;
    }

    if ((x == 2.0) || (x == 3.0)) {
      return z;
    }

    x -= 2.0;
    p = polevl(x, P, 6);
    q = polevl(x, Q, 7);
    return z * p / q;

  }

  /* Gamma function computed by Stirling's formula.
   * The polynomial STIR is valid for 33 <= x <= 172.

  Cephes Math Library Release 2.2:  July, 1992
  Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier
  Direct inquiries to 30 Frost Street, Cambridge, MA 02140
  */

  /**
   * _more_
   *
   * @param x
   * @return _more_
   * @throws ArithmeticException
   */
  static private double stirf(double x) throws ArithmeticException {
    double STIR[] = {7.87311395793093628397E-4,
        -2.29549961613378126380E-4,
        -2.68132617805781232825E-3,
        3.47222221605458667310E-3,
        8.33333333333482257126E-2,};
    double MAXSTIR = 143.01608;

    double w = 1.0 / x;
    double y = Math.exp(x);

    w = 1.0 + w * polevl(w, STIR, 4);

    if (x > MAXSTIR) {
      /* Avoid overflow in Math.pow() */
      double v = Math.pow(x, 0.5 * x - 0.25);
      y = v * (v / y);
    } else {
      y = Math.pow(x, x - 0.5) / y;
    }
    y = SQTPI * y * w;
    return y;
  }

  /**
   * @param a double value
   * @param x double value
   * @return the Complemented Incomplete Gamma function.
   *         <p/>
   *         <FONT size=2>
   *         Converted to Java from<BR>
   *         Cephes Math Library Release 2.2:  July, 1992<BR>
   *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
   *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
   * @throws ArithmeticException
   */

  static public double igamc(double a,
                             double x) throws ArithmeticException {
    double big = 4.503599627370496e15;
    double biginv = 2.22044604925031308085e-16;
    double ans, ax, c, yc, r, t, y, z;
    double pk, pkm1, pkm2, qk, qkm1, qkm2;

    if ((x <= 0) || (a <= 0)) {
      return 1.0;
    }

    if ((x < 1.0) || (x < a)) {
      return 1.0 - igam(a, x);
    }

    ax = a * Math.log(x) - x - lgamma(a);
    if (ax < -MAXLOG) {
      return 0.0;
    }

    ax = Math.exp(ax);

    /* continued fraction */
    y = 1.0 - a;
    z = x + y + 1.0;
    c = 0.0;
    pkm2 = 1.0;
    qkm2 = x;
    pkm1 = x + 1.0;
    qkm1 = z * x;
    ans = pkm1 / qkm1;

    do {
      c += 1.0;
      y += 1.0;
      z += 2.0;
      yc = y * c;
      pk = pkm1 * z - pkm2 * yc;
      qk = qkm1 * z - qkm2 * yc;
      if (qk != 0) {
        r = pk / qk;
        t = Math.abs((ans - r) / r);
        ans = r;
      } else {
        t = 1.0;
      }

      pkm2 = pkm1;
      pkm1 = pk;
      qkm2 = qkm1;
      qkm1 = qk;
      if (Math.abs(pk) > big) {
        pkm2 *= biginv;
        pkm1 *= biginv;
        qkm2 *= biginv;
        qkm1 *= biginv;
      }
    } while (t > MACHEP);

    return ans * ax;
  }


  /**
   * @param a double value
   * @param x double value
   * @return the Incomplete Gamma function.
   *         <p/>
   *         <FONT size=2>
   *         Converted to Java from<BR>
   *         Cephes Math Library Release 2.2:  July, 1992<BR>
   *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
   *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
   * @throws ArithmeticException
   */
  static public double igam(double a, double x) throws ArithmeticException {


    double ans, ax, c, r;

    if ((x <= 0) || (a <= 0)) {
      return 0.0;
    }

    if ((x > 1.0) && (x > a)) {
      return 1.0 - igamc(a, x);
    }

    /* Compute  x**a * exp(-x) / gamma(a)  */
    ax = a * Math.log(x) - x - lgamma(a);
    if (ax < -MAXLOG) {
      return (0.0);
    }

    ax = Math.exp(ax);

    /* power series */
    r = a;
    c = 1.0;
    ans = 1.0;

    do {
      r += 1.0;
      c *= x / r;
      ans += c;
    } while (c / ans > MACHEP);

    return (ans * ax / a);

  }

  /**
   * Returns the area under the left hand tail (from 0 to x)
   * of the Chi square probability density function with
   * v degrees of freedom.
   *
   * @param df degrees of freedom
   * @param x  double value
   * @return the Chi-Square function.
   * @throws ArithmeticException
   */

  static public double chisq(double df,
                             double x) throws ArithmeticException {

    if ((x < 0.0) || (df < 1.0)) {
      return 0.0;
    }

    return igam(df / 2.0, x / 2.0);

  }

  /**
   * Returns the area under the right hand tail (from x to
   * infinity) of the Chi square probability density function
   * with v degrees of freedom:
   *
   * @param df degrees of freedom
   * @param x  double value
   * @return the Chi-Square function.
   *         <p/>
   * @throws ArithmeticException
   */

  static public double chisqc(double df,
                              double x) throws ArithmeticException {

    if ((x < 0.0) || (df < 1.0)) {
      return 0.0;
    }

    return igamc(df / 2.0, x / 2.0);

  }

  /**
   * Returns the sum of the first k terms of the Poisson
   * distribution.
   *
   * @param k number of terms
   * @param x double value
   * @return _more_
   * @throws ArithmeticException
   */

  static public double poisson(int k, double x) throws ArithmeticException {


    if ((k < 0) || (x < 0)) {
      return 0.0;
    }

    return igamc((double) (k + 1), x);
  }

  /**
   * Returns the sum of the terms k+1 to infinity of the Poisson
   * distribution.
   *
   * @param k start
   * @param x double value
   * @return _more_
   * @throws ArithmeticException
   */

  static public double poissonc(int k,
                                double x) throws ArithmeticException {


    if ((k < 0) || (x < 0)) {
      return 0.0;
    }

    return igam((double) (k + 1), x);
  }


  /**
   * @param a double value
   * @return The area under the Gaussian probability density
   *         function, integrated from minus infinity to x:
   * @throws ArithmeticException
   */

  static public double normal(double a) throws ArithmeticException {
    double x, y, z;

    x = a * SQRTH;
    z = Math.abs(x);

    if (z < SQRTH) {
      y = 0.5 + 0.5 * erf(x);
    } else {
      y = 0.5 * erfc(z);
      if (x > 0) {
        y = 1.0 - y;
      }
    }

    return y;
  }


  /**
   * @param a double value
   * @return The complementary Error function
   *         <p/>
   *         <FONT size=2>
   *         Converted to Java from<BR>
   *         Cephes Math Library Release 2.2:  July, 1992<BR>
   *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
   *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
   * @throws ArithmeticException
   */

  static public double erfc(double a) throws ArithmeticException {
    double x, y, z, p, q;

    double P[] = {
        2.46196981473530512524E-10, 5.64189564831068821977E-1,
        7.46321056442269912687E0, 4.86371970985681366614E1,
        1.96520832956077098242E2, 5.26445194995477358631E2,
        9.34528527171957607540E2, 1.02755188689515710272E3,
        5.57535335369399327526E2
    };
    double Q[] = {
        //1.0
        1.32281951154744992508E1, 8.67072140885989742329E1,
        3.54937778887819891062E2, 9.75708501743205489753E2,
        1.82390916687909736289E3, 2.24633760818710981792E3,
        1.65666309194161350182E3, 5.57535340817727675546E2
    };

    double R[] = {
        5.64189583547755073984E-1, 1.27536670759978104416E0,
        5.01905042251180477414E0, 6.16021097993053585195E0,
        7.40974269950448939160E0, 2.97886665372100240670E0
    };
    double S[] = {
        //1.00000000000000000000E0,
        2.26052863220117276590E0, 9.39603524938001434673E0,
        1.20489539808096656605E1, 1.70814450747565897222E1,
        9.60896809063285878198E0, 3.36907645100081516050E0
    };

    if (a < 0.0) {
      x = -a;
    } else {
      x = a;
    }

    if (x < 1.0) {
      return 1.0 - erf(a);
    }

    z = -a * a;

    if (z < -MAXLOG) {
      if (a < 0) {
        return (2.0);
      } else {
        return (0.0);
      }
    }

    z = Math.exp(z);

    if (x < 8.0) {
      p = polevl(x, P, 8);
      q = p1evl(x, Q, 8);
    } else {
      p = polevl(x, R, 5);
      q = p1evl(x, S, 6);
    }

    y = (z * p) / q;

    if (a < 0) {
      y = 2.0 - y;
    }

    if (y == 0.0) {
      if (a < 0) {
        return 2.0;
      } else {
        return (0.0);
      }
    }


    return y;
  }

  /**
   * @param x double value
   * @return The Error function
   *         <p/>
   *         <FONT size=2>
   *         Converted to Java from<BR>
   *         Cephes Math Library Release 2.2:  July, 1992<BR>
   *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
   *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
   * @throws ArithmeticException
   */

  static public double erf(double x) throws ArithmeticException {
    double y, z;
    double T[] = {9.60497373987051638749E0, 9.00260197203842689217E1,
        2.23200534594684319226E3, 7.00332514112805075473E3,
        5.55923013010394962768E4};
    double U[] = {
        //1.00000000000000000000E0,
        3.35617141647503099647E1, 5.21357949780152679795E2,
        4.59432382970980127987E3, 2.26290000613890934246E4,
        4.92673942608635921086E4
    };

    if (Math.abs(x) > 1.0) {
      return (1.0 - erfc(x));
    }
    z = x * x;
    y = x * polevl(z, T, 4) / p1evl(z, U, 5);
    return y;
  }


  /**
   * _more_
   *
   * @param x
   * @param coef
   * @param N
   * @return _more_
   * @throws ArithmeticException
   */
  static private double polevl(double x, double[] coef,
                               int N) throws ArithmeticException {

    double ans;

    ans = coef[0];

    for (int i = 1; i <= N; i++) {
      ans = ans * x + coef[i];
    }

    return ans;
  }

  /**
   * _more_
   *
   * @param x
   * @param coef
   * @param N
   * @return _more_
   * @throws ArithmeticException
   */
  static private double p1evl(double x, double[] coef,
                              int N) throws ArithmeticException {

    double ans;

    ans = x + coef[0];

    for (int i = 1; i < N; i++) {
      ans = ans * x + coef[i];
    }

    return ans;
  }

  /*
  *
  *      Natural logarithm of gamma function
  *
  */
  /*
  Cephes Math Library Release 2.2:  July, 1992
  Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier
  Direct inquiries to 30 Frost Street, Cambridge, MA 02140
  */


  /**
   * _more_
   *
   * @param x
   * @return _more_
   * @throws ArithmeticException
   */
  static private double lgamma(double x) throws ArithmeticException {
    double p, q, w, z;

    double A[] = {8.11614167470508450300E-4, -5.95061904284301438324E-4,
        7.93650340457716943945E-4, -2.77777777730099687205E-3,
        8.33333333333331927722E-2};
    double B[] = {
        -1.37825152569120859100E3, -3.88016315134637840924E4,
        -3.31612992738871184744E5, -1.16237097492762307383E6,
        -1.72173700820839662146E6, -8.53555664245765465627E5
    };
    double C[] = {
        /* 1.00000000000000000000E0, */
        -3.51815701436523470549E2, -1.70642106651881159223E4,
        -2.20528590553854454839E5, -1.13933444367982507207E6,
        -2.53252307177582951285E6, -2.01889141433532773231E6
    };

    if (x < -34.0) {
      q = -x;
      w = lgamma(q);
      p = Math.floor(q);
      if (p == q) {
        throw new ArithmeticException("lgam: Overflow");
      }
      z = q - p;
      if (z > 0.5) {
        p += 1.0;
        z = p - q;
      }
      z = q * Math.sin(Math.PI * z);
      if (z == 0.0) {
        throw new ArithmeticException("lgamma: Overflow");
      }
      z = LOGPI - Math.log(z) - w;
      return z;
    }

    if (x < 13.0) {
      z = 1.0;
      while (x >= 3.0) {
        x -= 1.0;
        z *= x;
      }
      while (x < 2.0) {
        if (x == 0.0) {
          throw new ArithmeticException("lgamma: Overflow");
        }
        z /= x;
        x += 1.0;
      }
      if (z < 0.0) {
        z = -z;
      }
      if (x == 2.0) {
        return Math.log(z);
      }
      x -= 2.0;
      p = x * polevl(x, B, 5) / p1evl(x, C, 6);
      return (Math.log(z) + p);
    }

    if (x > 2.556348e305) {
      throw new ArithmeticException("lgamma: Overflow");
    }

    q = (x - 0.5) * Math.log(x) - x + 0.91893853320467274178;
    if (x > 1.0e8) {
      return (q);
    }

    p = 1.0 / (x * x);
    if (x >= 1000.0) {
      q += ((7.9365079365079365079365e-4 * p
          - 2.7777777777777777777778e-3) * p + 0.0833333333333333333333) / x;
    } else {
      q += polevl(p, A, 4) / x;
    }
    return q;
  }


  /**
   * @param aa double value
   * @param bb double value
   * @param xx double value
   * @return The Incomplete Beta Function evaluated from zero to xx.
   *         <p/>
   *         <FONT size=2>
   *         Converted to Java from<BR>
   *         Cephes Math Library Release 2.3:  July, 1995<BR>
   *         Copyright 1984, 1995 by Stephen L. Moshier<BR>
   *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
   * @throws ArithmeticException
   */

  public static double ibeta(double aa, double bb,
                             double xx) throws ArithmeticException {
    double a, b, t, x, xc, w, y;
    boolean flag;

    if ((aa <= 0.0) || (bb <= 0.0)) {
      throw new ArithmeticException("ibeta: Domain error!");
    }

    if ((xx <= 0.0) || (xx >= 1.0)) {
      if (xx == 0.0) {
        return 0.0;
      }
      if (xx == 1.0) {
        return 1.0;
      }
      throw new ArithmeticException("ibeta: Domain error!");
    }

    flag = false;
    if ((bb * xx) <= 1.0 && (xx <= 0.95)) {
      t = pseries(aa, bb, xx);
      return t;
    }

    w = 1.0 - xx;

    /* Reverse a and b if x is greater than the mean. */
    if (xx > (aa / (aa + bb))) {
      flag = true;
      a = bb;
      b = aa;
      xc = xx;
      x = w;
    } else {
      a = aa;
      b = bb;
      xc = w;
      x = xx;
    }

    if (flag && (b * x) <= 1.0 && (x <= 0.95)) {
      t = pseries(a, b, x);
      if (t <= MACHEP) {
        t = 1.0 - MACHEP;
      } else {
        t = 1.0 - t;
      }
      return t;
    }

    /* Choose expansion for better convergence. */
    y = x * (a + b - 2.0) - (a - 1.0);
    if (y < 0.0) {
      w = incbcf(a, b, x);
    } else {
      w = incbd(a, b, x) / xc;
    }

    /* Multiply w by the factor
a      b   _             _     _
x  (1-x)   | (a+b) / ( a | (a) | (b) ) .   */

    y = a * Math.log(x);
    t = b * Math.log(xc);
    if ((a + b) < MAXGAM && (Math.abs(y) < MAXLOG)
        && (Math.abs(t) < MAXLOG)) {
      t = Math.pow(xc, b);
      t *= Math.pow(x, a);
      t /= a;
      t *= w;
      t *= gamma(a + b) / (gamma(a) * gamma(b));
      if (flag) {
        if (t <= MACHEP) {
          t = 1.0 - MACHEP;
        } else {
          t = 1.0 - t;
        }
      }
      return t;
    }
    /* Resort to logarithms.  */
    y += t + lgamma(a + b) - lgamma(a) - lgamma(b);
    y += Math.log(w / a);
    if (y < MINLOG) {
      t = 0.0;
    } else {
      t = Math.exp(y);
    }

    if (flag) {
      if (t <= MACHEP) {
        t = 1.0 - MACHEP;
      } else {
        t = 1.0 - t;
      }
    }
    return t;
  }

  /* Continued fraction expansion #1
  * for incomplete beta integral
  */

  /**
   * _more_
   *
   * @param a
   * @param b
   * @param x
   * @return _more_
   * @throws ArithmeticException
   */
  private static double incbcf(double a, double b,
                               double x) throws ArithmeticException {
    double xk, pk, pkm1, pkm2, qk, qkm1, qkm2;
    double k1, k2, k3, k4, k5, k6, k7, k8;
    double r, t, ans, thresh;
    int n;
    double big = 4.503599627370496e15;
    double biginv = 2.22044604925031308085e-16;

    k1 = a;
    k2 = a + b;
    k3 = a;
    k4 = a + 1.0;
    k5 = 1.0;
    k6 = b - 1.0;
    k7 = k4;
    k8 = a + 2.0;

    pkm2 = 0.0;
    qkm2 = 1.0;
    pkm1 = 1.0;
    qkm1 = 1.0;
    ans = 1.0;
    r = 1.0;
    n = 0;
    thresh = 3.0 * MACHEP;
    do {
      xk = -(x * k1 * k2) / (k3 * k4);
      pk = pkm1 + pkm2 * xk;
      qk = qkm1 + qkm2 * xk;
      pkm2 = pkm1;
      pkm1 = pk;
      qkm2 = qkm1;
      qkm1 = qk;

      xk = (x * k5 * k6) / (k7 * k8);
      pk = pkm1 + pkm2 * xk;
      qk = qkm1 + qkm2 * xk;
      pkm2 = pkm1;
      pkm1 = pk;
      qkm2 = qkm1;
      qkm1 = qk;

      if (qk != 0) {
        r = pk / qk;
      }
      if (r != 0) {
        t = Math.abs((ans - r) / r);
        ans = r;
      } else {
        t = 1.0;
      }

      if (t < thresh) {
        return ans;
      }

      k1 += 1.0;
      k2 += 1.0;
      k3 += 2.0;
      k4 += 2.0;
      k5 += 1.0;
      k6 -= 1.0;
      k7 += 2.0;
      k8 += 2.0;

      if ((Math.abs(qk) + Math.abs(pk)) > big) {
        pkm2 *= biginv;
        pkm1 *= biginv;
        qkm2 *= biginv;
        qkm1 *= biginv;
      }
      if ((Math.abs(qk) < biginv) || (Math.abs(pk) < biginv)) {
        pkm2 *= big;
        pkm1 *= big;
        qkm2 *= big;
        qkm1 *= big;
      }
    } while (++n < 300);

    return ans;
  }

  /* Continued fraction expansion #2
  * for incomplete beta integral
  */

  /**
   * _more_
   *
   * @param a
   * @param b
   * @param x
   * @return _more_
   * @throws ArithmeticException
   */
  static private double incbd(double a, double b,
                              double x) throws ArithmeticException {
    double xk, pk, pkm1, pkm2, qk, qkm1, qkm2;
    double k1, k2, k3, k4, k5, k6, k7, k8;
    double r, t, ans, z, thresh;
    int n;
    double big = 4.503599627370496e15;
    double biginv = 2.22044604925031308085e-16;

    k1 = a;
    k2 = b - 1.0;
    k3 = a;
    k4 = a + 1.0;
    k5 = 1.0;
    k6 = a + b;
    k7 = a + 1.0;
    ;
    k8 = a + 2.0;

    pkm2 = 0.0;
    qkm2 = 1.0;
    pkm1 = 1.0;
    qkm1 = 1.0;
    z = x / (1.0 - x);
    ans = 1.0;
    r = 1.0;
    n = 0;
    thresh = 3.0 * MACHEP;
    do {
      xk = -(z * k1 * k2) / (k3 * k4);
      pk = pkm1 + pkm2 * xk;
      qk = qkm1 + qkm2 * xk;
      pkm2 = pkm1;
      pkm1 = pk;
      qkm2 = qkm1;
      qkm1 = qk;

      xk = (z * k5 * k6) / (k7 * k8);
      pk = pkm1 + pkm2 * xk;
      qk = qkm1 + qkm2 * xk;
      pkm2 = pkm1;
      pkm1 = pk;
      qkm2 = qkm1;
      qkm1 = qk;

      if (qk != 0) {
        r = pk / qk;
      }
      if (r != 0) {
        t = Math.abs((ans - r) / r);
        ans = r;
      } else {
        t = 1.0;
      }

      if (t < thresh) {
        return ans;
      }

      k1 += 1.0;
      k2 -= 1.0;
      k3 += 2.0;
      k4 += 2.0;
      k5 += 1.0;
      k6 += 1.0;
      k7 += 2.0;
      k8 += 2.0;

      if ((Math.abs(qk) + Math.abs(pk)) > big) {
        pkm2 *= biginv;
        pkm1 *= biginv;
        qkm2 *= biginv;
        qkm1 *= biginv;
      }
      if ((Math.abs(qk) < biginv) || (Math.abs(pk) < biginv)) {
        pkm2 *= big;
        pkm1 *= big;
        qkm2 *= big;
        qkm1 *= big;
      }
    } while (++n < 300);

    return ans;
  }

  /* Power series for incomplete beta integral.
Use when b*x is small and x not too close to 1.  */

  /**
   * _more_
   *
   * @param a
   * @param b
   * @param x
   * @return _more_
   * @throws ArithmeticException
   */
  static private double pseries(double a, double b,
                                double x) throws ArithmeticException {
    double s, t, u, v, n, t1, z, ai;

    ai = 1.0 / a;
    u = (1.0 - b) * x;
    v = u / (a + 1.0);
    t1 = v;
    t = u;
    n = 2.0;
    s = 0.0;
    z = MACHEP * ai;
    while (Math.abs(v) > z) {
      u = (n - b) * x / n;
      t *= u;
      v = t / (a + n);
      s += v;
      n += 1.0;
    }
    s += t1;
    s += ai;

    u = a * Math.log(x);
    if ((a + b) < MAXGAM && (Math.abs(u) < MAXLOG)) {
      t = gamma(a + b) / (gamma(a) * gamma(b));
      s = s * t * Math.pow(x, a);
    } else {
      t = lgamma(a + b) - lgamma(a) - lgamma(b) + u + Math.log(s);
      if (t < MINLOG) {
        s = 0.0;
      } else {
        s = Math.exp(t);
      }
    }
    return s;
  }

}

/*
 *  Change History:
 *  $Log: SpecialMathFunction.java,v $
 *  Revision 1.10  2006/05/05 19:19:37  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.9  2005/05/13 18:32:44  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.8  2004/08/26 12:07:03  dmurray
 *  more javadoc fixes
 *
 *  Revision 1.7  2004/08/23 17:27:26  dmurray
 *  silence some javadoc warnings
 *
 *  Revision 1.6  2004/08/19 21:34:47  jeffmc
 *  Scratch log4j
 *
 *  Revision 1.5  2004/02/27 21:18:55  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.4  2004/01/29 17:37:43  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.3  1999/06/03 01:44:21  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:23  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:50  caron
 *  startAgain
 *
 * # Revision 1.3  1998/12/14  17:12:08  russ
 * # Add comment for accumulating change histories.
 * #
 */






