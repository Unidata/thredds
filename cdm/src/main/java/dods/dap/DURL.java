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
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Holds a DODS <code>URL</code> value.
 *
 * @version $Revision$
 * @author jehamby
 * @see BaseType
 */
public class DURL extends DString {
  /** Constructs a new <code>DURL</code>. */
  public DURL() { super(); }

  /**
   * Constructs a new <code>DURL</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public DURL(String n) { super(n); }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  public String getTypeName() {
    return "Url";
  }
}
