/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dap4.core.util;

/**
   Thrown for an attempt to make a forbidden conversion on primitive data values,
   eg boolean to double.
  *
 * @author Caron
 * @author Heimbigner
*/

public final class ConversionException extends RuntimeException
{
  public ConversionException() { super(); }
  public ConversionException(String s) { super(s); }
  public ConversionException(Throwable e) { super(e); }
  public ConversionException(String s, Throwable e) { super(s,e); }
}
