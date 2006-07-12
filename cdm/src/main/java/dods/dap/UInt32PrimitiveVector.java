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
 * A vector of unsigned ints.
 *
 * @version $Revision$
 * @author jehamby
 * @see PrimitiveVector
 */
public class UInt32PrimitiveVector extends Int32PrimitiveVector {
  /** Constructs a new <code>UInt32PrimitiveVector</code>. */
  public UInt32PrimitiveVector(BaseType var) {
    super(var);
  }

  /**
   * Prints the value of all variables in this vector.  This
   * method is primarily intended for debugging DODS applications and
   * text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   */
  public void printVal(PrintWriter os, String space) {
    int len = getLength();
    for(int i=0; i<len-1; i++) {
      // to print properly, cast to long and convert to unsigned
      os.print(((long)getValue(i)) & 0xFFFFFFFFL);
      os.print(", ");
    }
    // print last value, if any, without trailing comma
    if(len > 0)
      os.print(((long)getValue(len-1)) & 0xFFFFFFFFL);
  }

  /**
   * Prints the value of a single variable in this vector.
   * method is used by <code>DArray</code>'s <code>printVal</code> method.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param index the index of the variable to print.
   * @see DArray#printVal(PrintWriter, String, boolean)
   */
 public void printSingleVal(PrintWriter os, int index) {
    // to print properly, cast to long and convert to unsigned
    os.print(((long)getValue(index)) & 0xFFFFFFFFL);
  }
}
