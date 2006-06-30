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
import dods.dap.*;

/**
 * Holds a DODS <code>Boolean</code> value.
 *
 * @version $Revision: 1.1 $
 * @author npotter
 * @see BaseType
 */
public class DBoolean extends BaseType implements ClientIO {
  /** Constructs a new <code>DBoolean</code>. */
  public DBoolean() { super(); }

  /**
   * Constructs a new <code>DBoolean</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public DBoolean(String n) { super(n); }

  /** The value of this <code>DBoolean</code>. */
  private boolean val;

  /**
   * Get the current value as a short (16bit int).
   * @return the current value.
   */
  public final boolean getValue() {
    return val;
  }

  /**
   * Set the current value.
   * @param newVal the new value.
   */
  public final void setValue(boolean newVal) {
    val = newVal;
  }

  /**
   * Constructs a new <code>BooelanPrimitiveVector</code>.
   * @return a new <code>BooelanPrimitiveVector</code>.
   */
  public PrimitiveVector newPrimitiveVector() {
    return new BooleanPrimitiveVector(this);
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  public String getTypeName() {
    return "Boolean";
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
    if (print_decl_p) {
      printDecl(os, space, false);
      os.println(" = " + val + ";");
    } else
      os.print(val);
  }

  /**
   * Reads data from a <code>DataInputStream</code>. This method is only used
   * on the client side of the DODS client/server connection.
   *
   * @param source a <code>DataInputStream</code> to read from.
   * @param sv the <code>ServerVersion</code> returned by the server.
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @exception EOFException if EOF is found before the variable is completely
   *     deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  public synchronized void deserialize(DataInputStream source,
				       ServerVersion sv,
				       StatusUI statusUI)
       throws IOException, EOFException {
    val = source.readBoolean();
    if(statusUI != null)
      statusUI.incrementByteCount(1);
  }

    /**
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * primarily by GUI clients which need to download DODS data, manipulate 
     * it, and then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @exception IOException thrown on any <code>OutputStream</code>
     * exception. 
     */
  public void externalize(DataOutputStream sink) throws IOException {
    sink.writeBoolean(val);
  }
}
