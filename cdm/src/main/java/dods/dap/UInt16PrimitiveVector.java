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
 * A vector of unsigned ints.
 *
 * @version $Revision: 1.1 $
 * @author ndp
 * @see PrimitiveVector
 */
public class UInt16PrimitiveVector extends Int16PrimitiveVector {
  /** Constructs a new <code>UInt16PrimitiveVector</code>. */
  public UInt16PrimitiveVector(BaseType var) {
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
      os.print(((long)getValue(i)) & 0xFFFFL);
      os.print(", ");
    }
    // print last value, if any, without trailing comma
    if(len > 0)
      os.print(((long)getValue(len-1)) & 0xFFFFL);
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
    os.print(((long)getValue(index)) & 0xFFFFL);
  }
}
