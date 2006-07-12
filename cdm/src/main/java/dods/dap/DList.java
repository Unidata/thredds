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
import java.io.DataInputStream;
import java.io.PrintWriter;

/**
 * This class implements a simple list of DODS data
 * types. A list is a simple sequence of data items, without the
 * sophisticated subsetting and array indexing features of an Array.
 * <p>
 * DODS does not support Lists of Lists. This restriction is enforced by the
 * DDS parser.
 *
 * @version $Revision$
 * @author jehamby
 * @see BaseType
 * @see DVector
 */
public class DList extends DVector {
  /** Constructs a new <code>DList</code>. */
  public DList() { super(); }

  /**
   * Constructs a new <code>DList</code> with the given name.
   *
   * @param n the name of the variable.
   */
  public DList(String n) { super(n); }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  public String getTypeName() {
    return "List";
  }

  
}
