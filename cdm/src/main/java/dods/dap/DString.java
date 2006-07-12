////////////////////////////////////////////////////////////////////////////
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
 * Holds a DODS <code>String</code> value.
 *
 * @version $Revision$
 * @author jehamby
 * @see BaseType
 */
public class DString extends BaseType implements ClientIO {
  /** Constructs a new <code>DString</code>. */
  public DString() { 
    super();
    val = "String value has not been set.";
  }

  /**
   * Constructs a new <code>DString</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public DString(String n) { 
    super(n); 
    val = "String value has not been set.";
  }

  /** The value of this <code>DString</code>. */
  private String val;

  /**
   * Get the current value as a <code>String</code>.
   * @return the current value.
   */
  public final String getValue() {
    return val;
  }

  /**
   * Set the current value.
   * @param newVal the new value.
   */
  public final void setValue(String newVal) {
    val = newVal;
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  public String getTypeName() {
    return "String";
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
      os.println(" = \"" + Util.escattr(val) + "\";");
    } else
      os.print("\"" + Util.escattr(val) + "\"");
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
   * @exception DataReadException if a negative string length was read.
   * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  public synchronized void deserialize(DataInputStream source,
				       ServerVersion sv,
				       StatusUI statusUI)
       throws IOException, EOFException, DataReadException {
    int len = source.readInt();
    if (len < 0)
      throw new DataReadException("Negative string length read.");
    int modFour = len%4;
    // number of bytes to pad
    int pad = (modFour != 0) ? (4-modFour) : 0;

    byte byteArray[] = new byte[len];
    
    // With blackdown JDK1.1.8v3 (comes with matlab 6) read() didn't always
    // finish reading a string.  readFully() insures that it gets all <len>
    // characters it requested.  rph 08/20/01.
    
    //source.read(byteArray, 0, len);
    source.readFully(byteArray, 0, len);

    // pad out to a multiple of four bytes
    byte unused;
    for(int i=0; i<pad; i++)
      unused = source.readByte();

    if(statusUI != null)
      statusUI.incrementByteCount(4 + len + pad);

    // convert bytes to a new String using ISO8859_1 (Latin 1) encoding.
    // This was chosen because it converts each byte to its Unicode value
    // with no translation (the first 256 glyphs in Unicode are ISO8859_1)
    try {
      val = new String(byteArray, 0, len, "ISO8859_1");
    }
    catch (UnsupportedEncodingException e) {
      // this should never happen
      System.err.println("ISO8859_1 encoding not supported by this VM!");
      System.exit(1);
    }
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
	// convert String to a byte array using ISO8859_1 (Latin 1) encoding.
	// This was chosen because it converts each byte to its Unicode value
	// with no translation (the first 256 glyphs in Unicode are ISO8859_1)

	try {
	    byte byteArray[] = val.getBytes("ISO8859_1");
	    sink.writeInt(byteArray.length);
	    int modFour = byteArray.length%4;
	    // number of bytes to pad
	    int pad = (modFour != 0) ? (4-modFour) : 0;
	    sink.write(byteArray, 0, byteArray.length);
	    for(int i=0; i<pad; i++) {
		sink.writeByte(pad);
	    }
	}
	catch (UnsupportedEncodingException e) {
	    // this should never happen
	    System.err.println("ISO8859_1 encoding not supported by this VM!");
	    System.exit(1);
	}
    }
}
