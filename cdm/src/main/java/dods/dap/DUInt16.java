/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//         
/////////////////////////////////////////////////////////////////////////////
//
// Based on source code and instructions from the work of:
//
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
 * Holds a DODS <code>UInt16</code> value.
 *
 * @version $Revision$
 * @author ndp
 * @see BaseType
 */
public class DUInt16 extends DInt16 {
  /** Constructs a new <code>DUInt16</code>. */
  public DUInt16() { super(); }

  /**
   * Constructs a new <code>DUInt16</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public DUInt16(String n) { super(n); }

  /**
   * Constructs a new <code>UInt16PrimitiveVector</code>.
   * @return a new <code>UInt16PrimitiveVector</code>.
   */
  public PrimitiveVector newPrimitiveVector() {
    return new UInt16PrimitiveVector(this);
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  public String getTypeName() {
    return "UInt16";
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
    long tempVal = ((long)getValue()) & 0xFFFFL;
    if (print_decl_p) {
      printDecl(os, space, false);
      os.println(" = " + tempVal + ";");
    } else
      os.print(tempVal);
  }
}
