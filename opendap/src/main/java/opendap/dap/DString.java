/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////


package opendap.dap;

import java.io.*;

/**
 * Holds a OPeNDAP <code>String</code> value.
 *
 * @author jehamby
 * @version $Revision: 21071 $
 * @see BaseType
 */
public class DString extends BaseType implements ClientIO {
    /**
     * Constructs a new <code>DString</code>.
     */
    public DString() {
        super();
        val = "String value has not been set.";
    }

    /**
     * Constructs a new <code>DString</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public DString(String n) {
        super(n);
        val = "String value has not been set.";
    }

    /**
     * The value of this <code>DString</code>.
     */
    private String val;

    /**
     * Get the current value as a <code>String</code>.
     *
     * @return the current value.
     */
    public final String getValue() {
        return val;
    }

    /**
     * Set the current value.
     *
     * @param newVal the new value.
     */
    public final void setValue(String newVal) {
        val = newVal;
    }

    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    public String getTypeName() {
        return "String";
    }


    /**
     * Prints the value of the variable, with its declaration.  This
     * function is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     *
     * @param os           the <code>PrintWriter</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
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
     * on the client side of the OPeNDAP client/server connection.
     *
     * @param source   a <code>DataInputStream</code> to read from.
     * @param sv       the <code>ServerVersion</code> returned by the server.
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @throws EOFException      if EOF is found before the variable is completely
     *                           deserialized.
     * @throws IOException       thrown on any other InputStream exception.
     * @throws DataReadException if a negative string length was read.
     * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
     */
    public synchronized void deserialize(DataInputStream source,
                                         ServerVersion sv,
                                         StatusUI statusUI)
            throws IOException, EOFException, DataReadException {
        int dap_len = source.readInt();

        //System.out.println("DString deserialize string dap_length: "+ dap_len);

        if (dap_len < 0)
            throw new DataReadException("Negative string length (dap_length: "+ dap_len +") read.");
        if (dap_len > Short.MAX_VALUE)
            throw new DataReadException("DString deserialize string length (dap_length: "+ dap_len +") too large.");



        int modFour = dap_len % 4;
        // number of bytes to pad
        int pad = (modFour != 0) ? (4 - modFour) : 0;

        byte byteArray[] = new byte[dap_len];

        // With blackdown JDK1.1.8v3 (comes with matlab 6) read() didn't always
        // finish reading a string.  readFully() insures that it gets all <dap_len>
        // characters it requested.  rph 08/20/01.

        //source.read(byteArray, 0, dap_len);
        source.readFully(byteArray, 0, dap_len);

        // pad out to a multiple of four bytes
        byte unused;
        for (int i = 0; i < pad; i++)
            unused = source.readByte();

        if (statusUI != null)
            statusUI.incrementByteCount(4 + dap_len + pad);

        // convert bytes to a new String using ISO8859_1 (Latin 1) encoding.
        // This was chosen because it converts each byte to its Unicode value
        // with no translation (the first 256 glyphs in Unicode are ISO8859_1)
        try {
            val = new String(byteArray, 0, dap_len, "ISO8859_1");
        }
        catch (UnsupportedEncodingException e) {
            // this should never happen
            throw new UnsupportedEncodingException("ISO8859_1 encoding not supported by this VM!");
        }
    }

    /**
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * primarily by GUI clients which need to download OPeNDAP data, manipulate
     * it, and then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @throws IOException thrown on any <code>OutputStream</code>
     *                     exception.
     */
    public void externalize(DataOutputStream sink) throws IOException {
        // convert String to a byte array using ISO8859_1 (Latin 1) encoding.
        // This was chosen because it converts each byte to its Unicode value
        // with no translation (the first 256 glyphs in Unicode are ISO8859_1)

        try {
            byte byteArray[] = val.getBytes("ISO8859_1");
            sink.writeInt(byteArray.length);
            int modFour = byteArray.length % 4;
            // number of bytes to pad
            int pad = (modFour != 0) ? (4 - modFour) : 0;
            sink.write(byteArray, 0, byteArray.length);
            for (int i = 0; i < pad; i++) {
                sink.writeByte(0);
            }
        }
        catch (UnsupportedEncodingException e) {
            // this should never happen
            throw new UnsupportedEncodingException("ISO8859_1 encoding not supported by this VM!");
        }
    }
}


