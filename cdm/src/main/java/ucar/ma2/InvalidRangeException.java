/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
   Thrown if an attempt is made to use an invalid
   Range to index an array.
 *
 * @author caron
 */

public class InvalidRangeException extends Exception {
  public InvalidRangeException() { super(); }
  public InvalidRangeException(String s) { super(s); }
}