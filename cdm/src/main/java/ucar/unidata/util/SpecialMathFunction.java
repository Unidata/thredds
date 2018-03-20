/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util;

import java.lang.ArithmeticException;


import java.lang.Math;

/**
 * Special Math functions.
 * Recoded from online sources for licensing purposes.
 * caron 6/22/2012
 */


public final class SpecialMathFunction {
  private static final double log2 = Math.log(2);

  /**
   * Get the log base 2 of a number
   * @param x a double value
   * @return The log<sub>2</sub> of x
   * @throws ArithmeticException if (x < 0)
   */
  static public double log2(double x) throws ArithmeticException {
    if (x <= 0.0) throw new ArithmeticException("range exception");
    return Math.log(x) / log2;
  }

  static public double atanh(double x) throws ArithmeticException {
    if ((x > 1.0) || (x < -1.0)) {
      throw new ArithmeticException("range exception");
    }
    return 0.5 * Math.log((1.0 + x) / (1.0 - x));
  }
}


