/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;
import java.io.*;

/**
 * Holds a DODS <code>UInt32</code> value.
 *
 * @version $Revision$
 * @author jehamby
 * @see BaseType
 */
public class DUInt32 extends DInt32 {
  /** Constructs a new <code>DUInt32</code>. */
  public DUInt32() { super(); }

  /**
   * Constructs a new <code>DUInt32</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public DUInt32(String n) { super(n); }

  /**
   * Constructs a new <code>UInt32PrimitiveVector</code>.
   * @return a new <code>UInt32PrimitiveVector</code>.
   */
  public PrimitiveVector newPrimitiveVector() {
    return new UInt32PrimitiveVector(this);
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  public String getTypeName() {
    return "UInt32";
  }



  /**
   * Prints the value of the variable, with its declaration.  This
   * function is primarily intended for debugging DODS applications and
   * text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @param print_decl_p a boolean value controlling whether the
   *    variable declaration is printed as well as the value.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   */
  public void printVal(PrintWriter os, String space, boolean print_decl_p) {
    // to print properly, cast to long and convert unsigned to signed
    long tempVal = ((long)getValue()) & 0xFFFFFFFFL;
    if (print_decl_p) {
      printDecl(os, space, false);
      os.println(" = " + tempVal + ";");
    } else
      os.print(tempVal);
  }
}
